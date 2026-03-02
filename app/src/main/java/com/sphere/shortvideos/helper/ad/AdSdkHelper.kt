package com.sphere.shortvideos.helper.ad

import android.content.Context
import com.applovin.sdk.AppLovinMediationProvider
import com.applovin.sdk.AppLovinPrivacySettings
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkInitializationConfiguration
import com.sphere.shortvideos.helper.AppHelper
import com.sphere.shortvideos.isDebugMode
import com.sphere.shortvideos.logError
import com.thinkup.core.api.TUSDK

/**
 * Date：2026/3/2
 * Describe:
 */
object AdSdkHelper {
    // todo modify
    private val toponAppid = "h670e13c4e3ab6"
    private val toponAppKey = "ac360a993a659579a11f6df50b9e78639"

    fun initMaxAndTopon(context: Context) {
        val initConfig = AppLovinSdkInitializationConfiguration.builder(fetchMaxSdkKey())
            .setMediationProvider(AppLovinMediationProvider.MAX)
            // Perform any additional configuration/setting changes
            .build()
        AppLovinSdk.getInstance(context).initialize(initConfig) { sdkConfig ->
            // Start loading ads
            logError("initMaxAndTopon-->AppLovinSdk success")
        }

        AppLovinPrivacySettings.setHasUserConsent(true)
        AppLovinPrivacySettings.setDoNotSell(false)
        TUSDK.init(context, toponAppid, toponAppKey)

        if (isDebugMode) { // todo del
            TUSDK.setNetworkLogDebug(isDebugMode)
            AppLovinSdk.getInstance(context).showMediationDebugger()
        }
    }

    private fun fetchMaxSdkKey(): String {
        val str = if (isDebugMode) { // todo del
            //"X543WfAHWlX3y2WhK43AglhL3DKgtU05A-xOkaLuTFZs_2tkZPoD0TN8HQfObaOJrflJ-twPgNIHW9LvFLk_te"
            "JElITysaPTQrECRPBU4rFDdITz0bEBQwTzg3GwgpTEk9UQQzFx0wCSg6Jg8jTggXJiwTOEwoMkQ0LRozHh0zNg4aEDZRCAssGzI1NCtFMAo6MBcjCBk="
        } else {
            "MSs2BhQSOSwINw0EMDcuMD0QKg4oBS0aM04qBCsmKwgqBCMvBigrPyMxGxMmMEsXKDcyCEUITzEjMxs1Jk5IEj4kLiQEKhhFExstOQxLSk1KKCsaTz8="
        }
        return AppHelper.decrypt(str, "124".toInt())
    }
}