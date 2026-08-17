package com.sphere.shortvideos.helper.risk

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import com.google.gson.Gson
import com.sphere.shortvideos.bean.BehaviorConfig
import com.sphere.shortvideos.bean.RiskBean
import com.sphere.shortvideos.helper.AppHelper
import com.sphere.shortvideos.helper.localEvent
import com.sphere.shortvideos.helper.mmkv.MMKVData
import com.sphere.shortvideos.mApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * Date：2026/1/20
 * Describe: 风控检测助手
 */
object RiskHelper {
    private var riskBean: RiskBean? = null
    private var numRvShow by MMKVData(0) //展示激励的次数
    private var curDayRvShowNum by MMKVData(0)
    private var sphereAdShortShowNum by MMKVData(0) // 展示过短的次数
    private var sphereAdShortCloseNum by MMKVData(0) // 关闭过短的次数
    private var rvShowShortNum = 0
    private var timeShowRv = 0L
    private const val SECOND = 1000L

    fun refreshRiskBean(str: String) {
        runCatching {
            val bean = Gson().fromJson(str, RiskBean::class.java)
            RiskHelper.riskBean = bean
        }
    }

    private fun fetchRiskBean(): RiskBean {
        if (riskBean == null) {
            refreshRiskBean(AppHelper.decrypt(RISK_BEAN_STR_EN, "123".toInt()))
        }
        return RiskHelper.riskBean!!
    }

    private fun fetchBehavior(): BehaviorConfig {
        return fetchRiskBean().behavior
    }

    private fun isShowRisk(time: Long): Boolean {
        return time < fetchBehavior().adShortShow.duration * SECOND
    }


    fun showRvEvent() {
        numRvShow++
        curDayRvShowNum++
        if (isShowRisk(System.currentTimeMillis() - timeShowRv)) {
            sphereAdShortShowNum++
        }
        timeShowRv = System.currentTimeMillis()
    }


    fun closeRvEvent(time: Long) {
        if (time < fetchBehavior().adShortClose.duration * SECOND) {
            sphereAdShortCloseNum++
        }
    }

    fun isAdLimit(): Boolean {
        if (checkNumberUnion()) {
            localEvent("risk_chance", hashMapOf("risk_from" to "number"))
            return true
        }
        if (isBehaviorLimit()) return true
        if (checkDeviceIsRisk()) return true
        return false
    }

    /**
     * 检测数字联盟
     * @param context 上下文
     * @return true 表示异常，false 表示正常
     */
    private fun checkNumberUnion(): Boolean {
        if (riskBean?.ui?.number != 1) return false
        if (HelperRiskNetCheck.riskDevType != 0) return true
        return false
    }

    private fun isBehaviorLimit(): Boolean {
        if (fetchRiskBean().ui.behavior == 0) return false
        val bean = fetchBehavior()
        if (sphereAdShortCloseNum >= bean.adShortClose.value) {
            localEvent("risk_chance", hashMapOf("risk_from" to "ad_short_close"))
            return true
        }
        if (sphereAdShortShowNum >= bean.adShortShow.value) {
            localEvent("risk_chance", hashMapOf("risk_from" to "ad_short_show"))
            return true
        }
        if (numRvShow < bean.wrongDeemAdLess) {
            localEvent("risk_chance", hashMapOf("risk_from" to "wrong_deem_ad_less"))
            return true
        } //        if (numRvShow > bean.wrongDeemAdMore) {
        //            return true
        //        }
        if (curDayRvShowNum > bean.adDailyShow) {
            return true
        }
        return false
    }

    private fun checkDeviceIsRisk(): Boolean {
        return checkDeviceRisk(mApp, fetchRiskBean().device)
    }

    /**
     * 检测设备风险
     * @param context 上下文
     * @param riskTypes 需要检测的风险类型列表
     * @return true 表示存在风险，false 表示安全
     */
    private fun checkDeviceRisk(context: Context, riskTypes: List<String>): Boolean { // 检查 UI 配置中设备检测是否开启
        if (riskBean?.ui?.device != 1) return false
        return riskTypes.any { type ->
            val status = when (type.lowercase()) {
                "vpn" -> checkVpn(context)
                "root" -> checkRoot()
                "sim" -> checkSim(context)
                "simulator" -> checkSimulator()
                "googleplay" -> checkNotGooglePlay(context)
                "developer" -> checkDeveloperMode(context)
                "ip" -> checkIpRisk()
                else -> false
            }
            if (status) {
                localEvent("risk_chance", hashMapOf("risk_from" to type.lowercase()))
            }
            status
        }
    }

    fun eventSession(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            delay(1000)
            localEvent("session_custom",
                hashMapOf(
                    "vpn" to if (checkVpn(context)) 1 else 0,
                    "root" to if (checkRoot()) 1 else 0,
                    "sim" to if (checkSim(context)) 1 else 0,
                    "simulator" to if (checkSimulator()) 1 else 0,
                    "googleplay" to if (checkNotGooglePlay(context)) 0 else 1,
                    "developer" to if (checkDeveloperMode(context)) 1 else 0,
                ))
        }
    }

    /**
     * 检测 VPN
     */
    private fun checkVpn(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }

    /**
     * 检测 Root（对齐 BoRisk：异常环境 / Xposed）
     */
    private fun checkRoot(): Boolean {
        return isAbnormalEnv() || isXposed()
    }

    /** 对齐 BoRiskUtils.isAbnormalEnv：ro.secure / su 路径 / test-keys */
    private fun isAbnormalEnv(): Boolean {
        if (getSystemProperty("ro.secure", "1") == "0") return true
        val paths = arrayOf(
            "/su",
            "/su/bin/su",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/system/bin/cufsdosck",
            "/system/xbin/cufsdosck",
            "/system/bin/cufsmgr",
            "/system/xbin/cufsmgr",
            "/system/bin/cufaevdd",
            "/system/xbin/cufaevdd",
            "/system/bin/conbb",
            "/system/xbin/conbb",
        )
        if (paths.any { File(it).exists() }) return true
        return Build.TAGS?.contains("test-keys") == true
    }

    /** 对齐 BoRiskUtils.isXposed */
    private fun isXposed(): Boolean {
        return try {
            val field = ClassLoader.getSystemClassLoader()
                .loadClass("de.robv.android.xposed.XposedBridge")
                .getDeclaredField("disableHooks")
            field.isAccessible = true
            field.set(null, true)
            true
        } catch (_: Throwable) {
            false
        }
    }

    @SuppressLint("PrivateApi")
    private fun getSystemProperty(key: String, defaultValue: String): String {
        return try {
            Class.forName("android.os.SystemProperties")
                .getMethod("get", String::class.java)
                .invoke(null, key) as? String ?: defaultValue
        } catch (_: Throwable) {
            defaultValue
        }
    }

    /**
     * 检测 SIM 卡
     */
    private fun checkSim(context: Context): Boolean {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return telephonyManager.simState == TelephonyManager.SIM_STATE_ABSENT
    }

    /**
     * 检测模拟器
     */
    private fun checkSimulator(): Boolean { // 检测常见模拟器特征
        val buildFingerprints = arrayOf("generic", "sdk", "android", "emulator")

        val buildHardware = arrayOf("goldfish", "qemu")

        val fingerprint = Build.FINGERPRINT.lowercase()
        val hardware = Build.HARDWARE.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()
        val device = Build.DEVICE.lowercase()

        return buildFingerprints.any { fingerprint.contains(it) } || buildHardware.any { hardware.contains(it) } || manufacturer.contains(
            "genymotion") || model.contains("emulator") || model.contains("android sdk built for x86") || device.contains(
            "emulator")
    }

    /**
     * 检测 Google Play 安装来源（对齐 BoRisk store）
     * 仅安装来源为 com.android.vending 视为商店安装；返回 true 表示非商店安装（有风险）
     */
    private fun checkNotGooglePlay(context: Context): Boolean {
        return getInstallerPackage(context) != "com.android.vending"
    }

    /** 对齐 BoRiskUtils.getInstaller：API 30+ 用 InstallSourceInfo */
    private fun getInstallerPackage(context: Context): String {
        return try {
            val packageManager = context.packageManager
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(context.packageName)
            }
            installer.orEmpty()
        } catch (_: Throwable) {
            ""
        }
    }

    /**
     * 检测开发者模式（对齐 BoRisk：开发者选项 或 ADB 调试开启）
     */
    private fun checkDeveloperMode(context: Context): Boolean {
        return try {
            val resolver = context.contentResolver
            val developerEnabled =
                Settings.Global.getInt(resolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0
            val adbEnabled = Settings.Global.getInt(resolver, Settings.Global.ADB_ENABLED, 0) != 0
            developerEnabled || adbEnabled
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * 检测 IP 地址（用户自行实现）
     */
    private fun checkIpRisk(): Boolean {
        return HelperRiskNetCheck.checkIpStatus == "true"
    }

}