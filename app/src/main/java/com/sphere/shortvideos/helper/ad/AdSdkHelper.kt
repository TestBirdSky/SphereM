package com.sphere.shortvideos.helper.ad

import android.content.Context
import com.applovin.sdk.AppLovinMediationProvider
import com.applovin.sdk.AppLovinPrivacySettings
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkInitializationConfiguration
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.sphere.shortvideos.helper.AppHelper
import com.sphere.shortvideos.isDebugMode
import com.sphere.shortvideos.logError
import com.thinkup.core.api.TUSDK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Date：2026/3/2
 * Describe:
 */
object AdSdkHelper {
    // todo modify
    private val toponAppid = "h69a541a5d2be9"
    private val toponAppKey = "aa852d2fd740df6ceaed83624969af407"

    fun initMaxAndTopon(context: Context) {
        val initConfig = AppLovinSdkInitializationConfiguration.builder(fetchMaxSdkKey())
            .setMediationProvider(AppLovinMediationProvider.MAX)
            // Perform any additional configuration/setting changes
            .build()
        AppLovinSdk.getInstance(context).initialize(initConfig) { sdkConfig ->
            // Start loading ads
            logError("initMaxAndTopon-->AppLovinSdk success")
            AdUtils.apply {
                launchHolder.preloadIfCan()
                unlockHolder.preloadIfCan()
                rewardHolder.preloadIfCan()
            }
        }

        AppLovinPrivacySettings.setHasUserConsent(true)
        AppLovinPrivacySettings.setDoNotSell(false)
        TUSDK.init(context, toponAppid, toponAppKey)

        if (isDebugMode) { // todo del
            TUSDK.setNetworkLogDebug(isDebugMode)
//            AppLovinSdk.getInstance(context).showMediationDebugger()
//            com.thinkup.debug.api.TUDebuggerUITest.showDebuggerUI(context);
        }
    }

    private fun fetchMaxSdkKey(): String {
        //MWJzhnEPtKqxLKRLAlVrTyQfO2VxWZWtVx_SzTWC_MgoZL7kTKNt9t3M_OgIZ24nBXRXxVd9ogQEp7616TWf3C
        val str =
            "MSs2BhQSOSwINw0EMDcuMD0QKg4oBS0aM04qBCsmKwgqBCMvBigrPyMxGxMmMEsXKDcyCEUITzEjMxs1Jk5IEj4kLiQEKhhFExstOQxLSk1KKCsaTz8="
        return AppHelper.decrypt(str, "124".toInt())
    }
}