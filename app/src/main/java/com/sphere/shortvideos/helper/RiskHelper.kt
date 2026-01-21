package com.sphere.shortvideos.helper

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import com.sphere.shortvideos.helper.mmkv.MMKVData
import com.sphere.shortvideos.riskBean
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Date：2026/1/20
 * Describe: 风控检测助手
 */
object RiskHelper {
    private var numRvShow by MMKVData(0)
    private var curDayRvShowNum by MMKVData(0)

    fun showRvEvent() {
        numRvShow++
        curDayRvShowNum++
    }

    /**
     * 检测数字联盟
     * @param context 上下文
     * @return true 表示异常，false 表示正常
     */
    fun checkNumberUnion(): Boolean {
        if (riskBean?.ui?.number != 1) return false
        if (HelperCheckTU.riskDevType != 0) return true
        return false
    }

    /**
     * 检测用户异常行为
     * @param context 上下文
     * @return true 表示异常，false 表示正常
     */
    fun checkBehavior(context: Context): Boolean {
        if (riskBean?.ui?.behavior != 1) return false

        // TODO: 实现行为检测逻辑
        // 根据 behavior 配置中的规则进行检测
        return false
    }


    /**
     * 检测设备风险
     * @param context 上下文
     * @param riskTypes 需要检测的风险类型列表
     * @return true 表示存在风险，false 表示安全
     */
    fun checkDeviceRisk(context: Context, riskTypes: List<String>): Boolean {
        // 检查 UI 配置中设备检测是否开启
        if (riskBean?.ui?.device != 1) return false
        return riskTypes.any { type ->
            when (type.lowercase()) {
                "vpn" -> checkVpn(context)
                "root" -> checkRoot()
                "sim" -> checkSim(context)
                "simulator" -> checkSimulator()
                "googleplay" -> checkGooglePlay(context).not()
                "developer" -> checkDeveloperMode(context)
                "ip" -> checkIp()
                else -> false
            }
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
        val paths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/app/Superuser.apk",
            "/system/app/SuperSU.apk",
            "/system/app/kinguser.apk",
            "/system/bin/install-recovery.sh",
            "/system/etc/init.sh"
        )

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
    private fun checkSimulator(): Boolean {
        // 检测常见模拟器特征
        val buildFingerprints = arrayOf(
            "generic",
            "sdk",
            "android",
            "emulator"
        )

        val buildHardware = arrayOf(
            "goldfish",
            "qemu"
        )

        val fingerprint = Build.FINGERPRINT.lowercase()
        val hardware = Build.HARDWARE.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()
        val device = Build.DEVICE.lowercase()

        return buildFingerprints.any { fingerprint.contains(it) } ||
                buildHardware.any { hardware.contains(it) } ||
                manufacturer.contains("genymotion") ||
                model.contains("emulator") ||
                model.contains("android sdk built for x86") ||
                device.contains("emulator")
    }

    /**
     * 检测 Google Play 安装来源
     */
    private fun checkGooglePlay(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return false // Android 11+ 无法获取安装来源
        }
        val packageManager = context.packageManager

        @Suppress("DEPRECATION")
        val installerPackageName = packageManager.getInstallerPackageName(context.packageName)
        return installerPackageName != "com.android.vending" &&
                installerPackageName != "com.google.android.gms"
    }

    /**
     * 检测开发者模式
     */
    private fun checkDeveloperMode(context: Context): Boolean {
        return Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0
        ) != 0
    }

    /**
     * 检测 IP 地址（用户自行实现）
     */
    fun checkIp(): Boolean {
        // TODO: 由用户自行实现 IP 检测逻辑
        // 可参考以下思路：
        // 1. 获取当前设备的 IP 地址
        // 2. 判断是否为代理/VPN IP
        // 3. 检查 IP 是否在风控黑名单中
        // 4. 判断 IP 所在地是否为异常区域
        return false
    }

    /**
     * 检查是否需要展示广告
     * @param context 上下文
     * @return true 可以展示，false 不展示
     */
    fun canShowAd(context: Context): Boolean {
        val riskConfig = riskBean ?: return true
        val behavior = riskConfig.behavior ?: return true

        // 检查每日广告展示上限
        // TODO: 需要结合本地存储记录广告展示次数
        val dailyShowLimit = behavior.adDailyShow
        // val todayShowCount = getTodayAdShowCount()

        return true
    }

    /**
     * 检查广告展示间隔
     * @param lastShowTime 上次展示时间
     * @return true 可以展示，false 需要等待
     */
    fun canShowAdAfterInterval(lastShowTime: Long): Boolean {
        val riskConfig = riskBean ?: return true
        val behavior = riskConfig.behavior

        val duration = behavior.adShortShow.duration
        val currentTime = System.currentTimeMillis()

        return (currentTime - lastShowTime) >= duration * 1000
    }

    /**
     * 检查广告关闭间隔
     * @param closeTime 关闭时间
     * @return true 可以展示，false 需要等待
     */
    fun canShowAdAfterClose(closeTime: Long): Boolean {
        val riskConfig = riskBean ?: return true
        val behavior = riskConfig.behavior

        val duration = behavior.adShortClose.duration
        val currentTime = System.currentTimeMillis()

        return (currentTime - closeTime) >= duration * 1000
    }
}
