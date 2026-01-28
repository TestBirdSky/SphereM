package com.sphere.shortvideos.helper.ad

import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.sphere.shortvideos.mApp
import com.sphere.shortvideos.baseui.GenericActivity
import com.sphere.shortvideos.helper.RevenueHelper
import com.sphere.shortvideos.logError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class BaseController(val position: AdPosition, val adBean: AdItemBean) : IAdController {

    var loadedTimeMills = System.currentTimeMillis()

    fun isAdExpired() =
        if (0 == adBean.timeout) false else (System.currentTimeMillis() - loadedTimeMills) >= (adBean.timeout * 1000L)

    fun adLogger(text: String?) {
        logError("${position.aliasName}: ${adBean.format.aliasName}, ${adBean.adId}, $text")
    }

    fun onAdDismissed(activity: GenericActivity, dismissed: () -> Unit) {
        activity.lifecycleScope.launch(Dispatchers.Main) {
            while (!activity.getActivityState()) delay(200)
            dismissed()
        }
    }
}

class AdmobFullAd(position: AdPosition, adBean: AdItemBean) : BaseController(position, adBean) {

    var fullAd: Any? = null

    override fun preload(onLoaded: (Boolean) -> Unit) {
        adLogger("begin loading")
        when (adBean.format) {
            AppOpenFormat -> {
                AppOpenAd.load(
                    mApp,
                    adBean.adId,
                    AdRequest.Builder().build(),
                    object : AppOpenAdLoadCallback() {
                        override fun onAdLoaded(ad: AppOpenAd) {
                            adLogger("onAdLoad success")
                            loadedTimeMills = System.currentTimeMillis()
                            fullAd = ad
                            onLoaded(true)
                        }

                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            adLogger("onAdLoadFailed:${adError.message}")
                            onLoaded(false)
                        }
                    })
            }

            InterstitialFormat -> {
                InterstitialAd.load(mApp,
                    adBean.adId,
                    AdRequest.Builder().build(),
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            adLogger("onAdLoad success")
                            loadedTimeMills = System.currentTimeMillis()
                            fullAd = ad
                            onLoaded(true)
                        }

                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            adLogger("onAdLoadFailed:${adError.message}")
                            onLoaded(false)
                        }
                    })
            }
        }
    }

    override fun showFullScreenAd(activity: GenericActivity, onDismissed: () -> Unit, onAdShowed: () -> Unit) {
        val ad = fullAd
        if (null == ad) {
            onAdDismissed(activity, onDismissed)
            return
        }
        when (ad) {
            is AppOpenAd -> {
                val callback = object : FullScreenContentCallback() {
                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        onAdDismissed(activity, onDismissed)
                    }

                    override fun onAdShowedFullScreenContent() {
                        onAdShowed()
                    }

                    override fun onAdDismissedFullScreenContent() = onAdDismissed(activity, onDismissed)
                }
                ad.fullScreenContentCallback = callback
                ad.setOnPaidEventListener { value: AdValue ->
                    RevenueHelper.onAdmobRevenueCallback(value, adBean, position, ad.responseInfo)
                }
                ad.show(activity)
            }

            is InterstitialAd -> {
                val callback = object : FullScreenContentCallback() {
                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        onAdDismissed(activity, onDismissed)
                    }

                    override fun onAdShowedFullScreenContent() {
                        onAdShowed()
                    }

                    override fun onAdDismissedFullScreenContent() = onAdDismissed(activity, onDismissed)
                }
                ad.fullScreenContentCallback = callback
                ad.setOnPaidEventListener { value: AdValue ->
                    RevenueHelper.onAdmobRevenueCallback(value, adBean, position, ad.responseInfo)
                }
                ad.show(activity)
            }

            else -> onAdDismissed(activity, onDismissed)
        }
    }

    override fun destroyAd() = Unit

}



