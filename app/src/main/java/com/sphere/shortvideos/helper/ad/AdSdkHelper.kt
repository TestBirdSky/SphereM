package com.sphere.shortvideos.helper.ad

import android.content.Context
import com.bytedance.sdk.openadsdk.api.PAGMInitSuccessModel
import com.bytedance.sdk.openadsdk.api.init.PAGMConfig
import com.bytedance.sdk.openadsdk.api.init.PAGMSdk
import com.bytedance.sdk.openadsdk.api.model.PAGErrorModel
import com.sphere.shortvideos.R
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
    private val toponAppid = "h69a15e091d94e"
    private val toponAppKey = "a0d6539a3a0d3f97a0d203c4088cfb455"

    fun initMaxAndTopon(context: Context) {
        //        val initConfig = AppLovinSdkInitializationConfiguration.builder(fetchMaxSdkKey())
        //            .setMediationProvider(AppLovinMediationProvider.MAX)
        //            // Perform any additional configuration/setting changes
        //            .build()
        //        AppLovinSdk.getInstance(context).initialize(initConfig) { sdkConfig ->
        //            // Start loading ads
        //            logError("initMaxAndTopon-->AppLovinSdk success")
        //        }

        //        AppLovinPrivacySettings.setHasUserConsent(true)
        //        AppLovinPrivacySettings.setDoNotSell(false)
        TUSDK.init(context, toponAppid, toponAppKey)
        initPag(context)
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

    var isInitPAGSuccess = false
    private fun initPag(context: Context) {
        val mPAGMConfig = PAGMConfig.Builder()
            //                        .appId("8580262")  // todo modify
            .appId("8778233")
            .appIcon(R.drawable.ic_splash_logo)
            .debugLog(isDebugMode)
            .build()
        PAGMSdk.init(context, mPAGMConfig, object : PAGMSdk.PAGMInitCallback {
            override fun success(p0: PAGMInitSuccessModel?) {
                logError("PAGMSdk init success")
                isInitPAGSuccess = true
            }

            override fun fail(p0: PAGErrorModel?) {
                logError("PAGMSdk init fail: ${p0?.errorCode}-${p0?.errorMessage}")
            }
        })
    }

}
