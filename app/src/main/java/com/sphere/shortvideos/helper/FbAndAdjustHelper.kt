package com.sphere.shortvideos.helper

import android.content.Context
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.sphere.shortvideos.mApp
import org.json.JSONObject

/**
 * Date：2026/1/20
 * Describe:
 */
class FbAndAdjustHelper {
    // todo add release fb
    private val facebookId = "1749622378999013"
    private val token = "edf43a4f06bcd5d32187c6a1bd91012f"
    private val adjustKey = "ih2pm2dr3k74"

    fun initFb(str: String) {
        JSONObject(str).apply {
            val fbStr = optString("app_id", facebookId)
            val token = optString("client_token", token)
            if (fbStr.isBlank()) return
            if (token.isBlank()) return
            if (FacebookSdk.isInitialized()) return
            FacebookSdk.setApplicationId(fbStr)
            FacebookSdk.setClientToken(token)
            FacebookSdk.sdkInitialize(mApp)
            AppEventsLogger.activateApp(mApp)
        }
    }

    fun initAdjust(context: Context) {
        val environment = AdjustConfig.ENVIRONMENT_SANDBOX
        //        if (BuildConfig.DEBUG) AdjustConfig.ENVIRONMENT_SANDBOX else AdjustConfig.ENVIRONMENT_PRODUCTION
        // todo modify adjust key
        val config = AdjustConfig(context, adjustKey, environment)

        Adjust.addGlobalCallbackParameter("customer_user_id", EventData.distinctId)

        config.setOnAttributionChangedListener {}

        Adjust.initSdk(config)
    }
}