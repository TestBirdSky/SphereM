package com.sphere.shortvideos.helper.risk

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
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

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
     * 检测 Root
     */
    private fun checkRoot(): Boolean {
        val paths = arrayOf("/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/app/Superuser.apk",
            "/system/app/SuperSU.apk",
            "/system/app/kinguser.apk",
            "/system/bin/install-recovery.sh",
            "/system/etc/init.sh")

        return paths.any { File(it).exists() } || checkCommandExists("su")
    }

    private fun checkCommandExists(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("which $command")
            BufferedReader(InputStreamReader(process.inputStream)).readLine() != null
        } catch (e: Exception) {
            false
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
     * 检测 Google Play 安装来源
     * 返回true则不是googlepaly安装
     */
    private fun checkNotGooglePlay(context: Context): Boolean {
        runCatching {
            val packageManager = context.packageManager
            @Suppress("DEPRECATION") val installerPackageName =
                packageManager.getInstallerPackageName(context.packageName)
            return installerPackageName != "com.android.vending" && installerPackageName != "com.google.android.gms"
        }
        return false
    }

    /**
     * 检测开发者模式
     */
    private fun checkDeveloperMode(context: Context): Boolean {
        return Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0
    }

    /**
     * 检测 IP 地址（用户自行实现）
     */
    private fun checkIpRisk(): Boolean {
        return HelperRiskNetCheck.checkIpStatus == "false"
    }

}