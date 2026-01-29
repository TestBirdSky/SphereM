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
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.sphere.shortvideos.mApp
import com.sphere.shortvideos.baseui.GenericActivity
import com.sphere.shortvideos.helper.RevenueHelper
import com.sphere.shortvideos.helper.localEvent
import com.sphere.shortvideos.helper.risk.RiskHelper
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

    fun showAdEvent(ad: Any) {
        AdUtils.allAdShowNum++
        when (adBean.format) {
            AppOpenFormat -> {

            }

            InterstitialFormat -> {

            }

            RewardFormat -> {
                RiskHelper.showRvEvent()
            }
        }
    }

    fun adLoadFiledEvent(reason: String) {
        localEvent("ad_return_fail",
            hashMapOf(
                "ad_code_id" to adBean.adId,
                "ad_format" to adBean.format,
                "ad_platform" to adBean.source,
                "reason" to reason,
            ))
    }

    fun adShowFiledEvent(reason: String) {
        localEvent("ad_impression_fail",
            hashMapOf(
                "ad_code_id" to adBean.adId,
                "ad_format" to adBean.format,
                "ad_platform" to adBean.source,
                "reason" to reason,
            ))
    }

    var onUserEarnedReward: (() -> Unit)? = null
}

class AdmobFullAd(position: AdPosition, adBean: AdItemBean) : BaseController(position, adBean) {

    var fullAd: Any? = null

    override fun preload(onLoaded: (Boolean) -> Unit) {
        adLogger("begin loading")
        localEvent("ad_request",
            hashMapOf(
                "ad_code_id" to adBean.adId,
                "ad_format" to adBean.format,
                "ad_platform" to adBean.source,
            ))
        when (adBean.format) {
            AppOpenFormat -> {
                AppOpenAd.load(mApp, adBean.adId, AdRequest.Builder().build(), object : AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        adLogger("onAdLoad success")
                        loadedTimeMills = System.currentTimeMillis()
                        fullAd = ad
                        onLoaded(true)
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        adLogger("onAdLoadFailed:${adError.message}")
                        adLoadFiledEvent(adError.message)
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
                            adLoadFiledEvent(adError.message)
                            onLoaded(false)
                        }
                    })
            }

            RewardFormat -> {
                RewardedAd.load(mApp, adBean.adId, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        adLogger("onAdLoad success")
                        loadedTimeMills = System.currentTimeMillis()
                        fullAd = ad
                        onLoaded(true)
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        adLogger("onAdLoadFailed:${adError.message}")
                        adLoadFiledEvent(adError.message)
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
                        adShowFiledEvent(adError.message)
                    }

                    override fun onAdShowedFullScreenContent() {
                        onAdShowed()
                        showAdEvent(ad)
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
                        adShowFiledEvent(adError.message)
                    }

                    override fun onAdShowedFullScreenContent() {
                        onAdShowed()
                        showAdEvent(ad)
                    }

                    override fun onAdDismissedFullScreenContent() = onAdDismissed(activity, onDismissed)
                }
                ad.fullScreenContentCallback = callback
                ad.setOnPaidEventListener { value: AdValue ->
                    RevenueHelper.onAdmobRevenueCallback(value, adBean, position, ad.responseInfo)
                }
                ad.show(activity)
            }

            is RewardedAd -> {
                val callback = object : FullScreenContentCallback() {
                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        onAdDismissed(activity, onDismissed)
                        adShowFiledEvent(adError.message)
                    }

                    override fun onAdShowedFullScreenContent() {
                        onAdShowed()
                        showAdEvent(ad)
                    }

                    override fun onAdDismissedFullScreenContent() = onAdDismissed(activity, onDismissed)
                }
                ad.fullScreenContentCallback = callback
                ad.setOnPaidEventListener { value: AdValue ->
                    RevenueHelper.onAdmobRevenueCallback(value, adBean, position, ad.responseInfo)
                }
                ad.show(activity) { _: RewardItem -> // reward callback handled by caller if needed
                    onUserEarnedReward?.invoke()
                }
            }

            else -> onAdDismissed(activity, onDismissed)
        }
    }

    override fun destroyAd() = Unit

}



