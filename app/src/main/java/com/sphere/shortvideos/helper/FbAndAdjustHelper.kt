package com.sphere.shortvideos.helper

import android.content.Context
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.sphere.shortvideos.helper.mmkv.MMKVRepository
import com.sphere.shortvideos.isDebugMode
import com.sphere.shortvideos.logError

/**
 * Date：2026/1/20
 * Describe:
 */
class FbAndAdjustHelper {
    private val adjustKey = "4qedga65udq8"

    private val defC = """{
            "app_id":"3616318175247400",
            "client_token": "3616318175247400"
        }
    """.trimIndent()

    fun initFb(str: String) {
//        val js = str.ifBlank { defC }
//        runCatching {
//            JSONObject(js).apply {
//                val fbStr = optString("app_id")
//                val token = optString("client_token")
//                if (fbStr.isBlank() || token.isBlank()) return
//                if (FacebookSdk.isInitialized()) return
//                FacebookSdk.setApplicationId(fbStr)
//                FacebookSdk.setClientToken(token)
//                FacebookSdk.sdkInitialize(mApp)
//                AppEventsLogger.activateApp(mApp)
//            }
//        }
    }

    fun initAdjust(context: Context) {
        val environment =
            AdjustConfig.ENVIRONMENT_SANDBOX //        if (BuildConfig.DEBUG) AdjustConfig.ENVIRONMENT_SANDBOX else AdjustConfig.ENVIRONMENT_PRODUCTION
        // todo modify adjust key
        val config = AdjustConfig(context, adjustKey, environment)

        Adjust.addGlobalCallbackParameter("customer_user_id", EventData.distinctId)

        config.setOnAttributionChangedListener { attribution ->
            logError("setOnAttributionChangedListener--->$attribution")
            // 判断用户类型：如果 network 包含 "Organic" 则为黑名单用户，否则为买量用户
            val network = attribution.network ?: ""
            val isBlacklist = network.contains("Organic", ignoreCase = false)
            MMKVRepository.isBlacklistUser = isBlacklist
            val info = parsePaidChannelByAdjustNetwork(network)
            if (MMKVRepository.adjustPaidChannel != info) {
                MMKVRepository.adjustPaidChannel = info
                RemoteConfHelper().fetchAdRemote()
            }
            logError(": network=$network, isBlacklistUser=$isBlacklist --${MMKVRepository.adjustPaidChannel}")
            localEvent("adjust_suc", hashMapOf("adjust_user" to if (isBlacklist) 0 else 1, "net_info" to network))
        }

        localEvent("adjust_req")
        Adjust.initSdk(config)
    }

    private fun parsePaidChannelByAdjustNetwork(network: String): String {
        val n = if (isDebugMode) {
            listOf("Facebook Int", "Instagram Installs","").random()
        } else {
            network.trim()
        }
        if (n.isEmpty()) return "unknown"
        logError("parsePaidChannelByAdjustNetwork-->$n")
        val lower = n.lowercase()
        return when {
            lower.contains("mintegral") || lower.contains("mtg") -> "mtg"
            // Facebook / Meta
            lower.contains("facebook", true)
                    || lower.contains("meta")
                    || lower.contains("instagram", true)
                    || lower.contains("fb4a") -> "fb"
            // TikTok / ByteDance 常见写法
            lower.contains("tiktok", true)
                    || lower.contains("bytedance", true)
                    || lower.contains("pangle")
                    || lower.contains("tik tok") -> "tt"

            else -> "unknown"
        }
    }
}