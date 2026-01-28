package com.sphere.shortvideos

import android.app.Application
import com.bytedance.sdk.shortplay.api.PSSDK
import com.google.android.gms.ads.MobileAds
import com.sphere.shortvideos.helper.AppLifecycleManager
import com.sphere.shortvideos.helper.OtherHelper
import com.sphere.shortvideos.helper.HelperCheckTU
import com.sphere.shortvideos.helper.InstallReferrerManager
import com.sphere.shortvideos.helper.LauageTools
import com.sphere.shortvideos.helper.RemoteConfHelper
import com.sphere.shortvideos.helper.WithdrawAmountHelper
import com.sphere.shortvideos.helper.localEvent
import com.sphere.shortvideos.helper.mmkv.MMKVRepository
import com.sphere.shortvideos.notification.NotificationHelper
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.random.Random

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        mApp = this
        MMKV.initialize(this)
        registerActivityLifecycleCallbacks(AppLifecycleManager)
        initAdmob()
        RemoteConfHelper().fetch()
        initPSSDK()
        InstallReferrerManager.fetch()
        initBackgroundActive()
        HelperCheckTU.requestHerUser(this@App)
        OtherHelper.registerInfo(this)
        mFbAndAdjustHelper.initAdjust(this)
    }

    private fun initBackgroundActive() {
        CoroutineScope(Dispatchers.IO).launch {
            startFlowTicker(Random.nextLong(1000L, 10000L), 20 * 60000L).filter {
                System.currentTimeMillis() - MMKVRepository.lastSessionActive >= (70 * 60000L)
            }.collect {
                localEvent("active_back_session")
                MMKVRepository.lastSessionActive = System.currentTimeMillis()
            }
        }
    }

    private fun initAdmob() {
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@App) {}
        }
    }

    private fun initPSSDK() {
        val builder = PSSDK.Config.Builder()
        builder.appId("8403349")
            .vodAppId("713016")
            .securityKey("edd282c3e2fe35a6f305c508e8ec79d5")
            .licenseAssertPath("l-1237-ch-vod-a-713016.lic")
            .debug(isDebugMode)
        PSSDK.init(this, builder.build()) { success, errorInfo ->
            logError("onInitFinished() called with: success = [$success], errorInfo = [$errorInfo]")
        }
        PSSDK.setEligibleAudience(true)
    }

    override fun onTerminate() {
        super.onTerminate()
        NotificationHelper.unregisterReceiver(this)
    }

}