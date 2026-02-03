package com.sphere.shortvideos.helper

import android.content.Context
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.adjust.sdk.AdjustConfig
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.google.android.gms.ads.AdValue
import com.sphere.shortvideos.helper.mmkv.MMKVRepository
import com.sphere.shortvideos.logError
import com.sphere.shortvideos.mApp
import org.json.JSONObject

/**
 * Date：2026/1/20
 * Describe:
 */
class FbAndAdjustHelper {
    // todo add release fb
    private val facebookId = "3616318175247400"//"1749622378999013"
    private val token = "sssjsjijsj" //"edf43a4f06bcd5d32187c6a1bd91012f"
    private val adjustKey = "4qedga65udq8"

    fun initFb(str: String) {
        JSONObject(str).apply {
            val fbStr = optString("app_id", facebookId)
            val token = optString("client_token", token)
            if (fbStr.isBlank() || token.isBlank()) return
            if (FacebookSdk.isInitialized()) return
            FacebookSdk.setApplicationId(fbStr)
            FacebookSdk.setClientToken(token)
            FacebookSdk.sdkInitialize(mApp)
            AppEventsLogger.activateApp(mApp)
        }
    }

    fun initAdjust(context: Context) {
        val environment = AdjustConfig.ENVIRONMENT_SANDBOX //        if (BuildConfig.DEBUG) AdjustConfig.ENVIRONMENT_SANDBOX else AdjustConfig.ENVIRONMENT_PRODUCTION
        // todo modify adjust key
        val config = AdjustConfig(context, adjustKey, environment)

        Adjust.addGlobalCallbackParameter("customer_user_id", EventData.distinctId)

        config.setOnAttributionChangedListener { attribution ->
            logError("setOnAttributionChangedListener--->$attribution")
            // 判断用户类型：如果 network 包含 "Organic" 则为黑名单用户，否则为买量用户
            val network = attribution.network ?: ""
            val isBlacklist = network.contains("Organic", ignoreCase = false)
            MMKVRepository.isBlacklistUser = isBlacklist
            logError(": network=$network, isBlacklistUser=$isBlacklist")
            localEvent("adjust_suc",hashMapOf("adjust_user" to  if (isBlacklist) 0 else 1))
        }

        localEvent("adjust_req")
        Adjust.initSdk(config)
    }
}