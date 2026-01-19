package com.sphere.shortvideos.helper.ad

import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.sphere.shortvideos.baseui.GenericActivity
import com.sphere.shortvideos.helper.RevenueHelper
import com.sphere.shortvideos.logError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class BaseController(val position: AdPosition, val adBean: AdItemBean) : IAdController {

    var loadedTimeMills = System.currentTimeMillis()

    fun isAdExpired() = if (0 == adBean.timeout) false else (System.currentTimeMillis() - loadedTimeMills) >= (adBean.timeout * 1000L)

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
                AppOpenAd.load(AdRequest.Builder(adBean.adId).build(), object : AdLoadCallback<AppOpenAd> {
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
                InterstitialAd.load(AdRequest.Builder(adBean.adId).build(), object : AdLoadCallback<InterstitialAd> {
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
                val callback = object : AppOpenAdEventCallback {
                    override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                        onAdDismissed(activity, onDismissed)
                    }

                    override fun onAdShowedFullScreenContent() {
                        onAdShowed()
                    }

                    override fun onAdDismissedFullScreenContent() = onAdDismissed(activity, onDismissed)
                    override fun onAdPaid(value: AdValue) {
                        RevenueHelper.onAdmobRevenueCallback(value, adBean, position, ad.getResponseInfo().loadedAdSourceResponseInfo)
                    }
                }
                ad.adEventCallback = callback
                ad.show(activity)
            }

            is InterstitialAd -> {
                val callback = object : InterstitialAdEventCallback {
                    override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                        onAdDismissed(activity, onDismissed)
                    }

                    override fun onAdShowedFullScreenContent() {
                        onAdShowed()
                    }

                    override fun onAdDismissedFullScreenContent() = onAdDismissed(activity, onDismissed)
                    override fun onAdPaid(value: AdValue) {
                        RevenueHelper.onAdmobRevenueCallback(value, adBean, position, ad.getResponseInfo().loadedAdSourceResponseInfo)
                    }
                }
                ad.adEventCallback = callback
                ad.show(activity)
            }

            else -> onAdDismissed(activity, onDismissed)
        }
    }

    override fun destroyAd() = Unit

}



