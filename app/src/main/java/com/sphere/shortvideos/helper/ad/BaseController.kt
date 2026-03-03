package com.sphere.shortvideos.helper.ad

import androidx.lifecycle.lifecycleScope
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxError
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
import com.sphere.shortvideos.baseui.GenericActivity
import com.sphere.shortvideos.helper.RevenueHelper
import com.sphere.shortvideos.helper.localEvent
import com.sphere.shortvideos.helper.risk.RiskHelper
import com.sphere.shortvideos.logError
import com.sphere.shortvideos.mApp
import com.thinkup.core.api.TUAdInfo
import com.thinkup.splashad.api.TUSplashAdEZListener
import com.thinkup.splashad.api.TUSplashAdExtraInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
                "ad_format" to adBean.format.aliasName,
                "ad_platform" to adBean.source,
                "reason" to reason,
                "ad_sense" to position.adSense
            ))
    }

    fun adShowFiledEvent(reason: String) {
        localEvent("ad_impression_fail",
            hashMapOf(
                "ad_code_id" to adBean.adId,
                "ad_format" to adBean.format.aliasName,
                "ad_platform" to adBean.source,
                "reason" to reason,
                "ad_sense" to position.adSense,
            ))
    }

    var onUserEarnedReward: (() -> Unit)? = null

    protected fun postAdReqEvent() {
        localEvent("ad_request",
            hashMapOf(
                "ad_code_id" to adBean.adId,
                "ad_format" to adBean.format.aliasName,
                "ad_platform" to adBean.source,
                "ad_sense" to position.adSense
            ))
    }
}

class AdmobFullAd(position: AdPosition, adBean: AdItemBean) : BaseController(position, adBean) {

    var fullAd: Any? = null

    override fun preload(onLoaded: (Boolean) -> Unit) {
        adLogger("begin loading")
        postAdReqEvent()
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

class ToponFullAd(position: AdPosition, adBean: AdItemBean) : BaseController(position, adBean) {

    private var fullAd: Any? = null

    override fun preload(onLoaded: (Boolean) -> Unit) {
        adLogger("begin loading topon")
        postAdReqEvent()

        when (adBean.format) {
            AppOpenFormat -> { // TopOn 开屏广告加载 - TUSplashAd
                fullAd = com.thinkup.splashad.api.TUSplashAd(mApp, adBean.adId, object : TUSplashAdEZListener() {
                    override fun onAdLoaded() {
                        adLogger("topon splash onAdLoad success")
                        loadedTimeMills = System.currentTimeMillis()
                        onLoaded(true)
                    }

                    override fun onNoAdError(p0: com.thinkup.core.api.AdError?) {
                        adLogger("topon splash onAdLoadFailed: $p0")
                        adLoadFiledEvent("${p0?.code}-${p0?.desc}")
                        onLoaded(false)
                    }

                    override fun onAdShow(p0: TUAdInfo?) {}

                    override fun onAdClick(p0: TUAdInfo?) {}

                    override fun onAdDismiss(p0: TUAdInfo?, p1: TUSplashAdExtraInfo?) {}

                }).apply {
                    loadAd()
                }
            }

            InterstitialFormat -> { // TopOn 插屏广告加载 - TUInterstitial
                val interstitialAd = com.thinkup.interstitial.api.TUInterstitial(mApp, adBean.adId)
                interstitialAd.setAdListener(object : com.thinkup.interstitial.api.TUInterstitialListener {
                    override fun onInterstitialAdLoaded() {
                        adLogger("topon interstitial onAdLoad success")
                        loadedTimeMills = System.currentTimeMillis()
                        fullAd = interstitialAd
                        onLoaded(true)
                    }

                    override fun onInterstitialAdLoadFail(p0: com.thinkup.core.api.AdError?) {
                        adLogger("topon interstitial onAdLoadFailed: $p0")
                        adLoadFiledEvent("${p0?.code}-${p0?.desc}")
                        onLoaded(false)
                    }

                    override fun onInterstitialAdClicked(p0: TUAdInfo?) {}

                    override fun onInterstitialAdShow(p0: TUAdInfo?) {}

                    override fun onInterstitialAdClose(p0: TUAdInfo?) {
                    }

                    override fun onInterstitialAdVideoStart(p0: TUAdInfo?) {
                    }

                    override fun onInterstitialAdVideoEnd(p0: TUAdInfo?) {
                    }

                    override fun onInterstitialAdVideoError(p0: com.thinkup.core.api.AdError?) {
                    }
                })
                interstitialAd.load()
            }

            RewardFormat -> { // TopOn 激励视频广告加载 - TURewardVideoAd
                val rewardVideoAd = com.thinkup.rewardvideo.api.TURewardVideoAd(mApp, adBean.adId)
                rewardVideoAd.setAdListener(object : com.thinkup.rewardvideo.api.TURewardVideoListener {
                    override fun onRewardedVideoAdLoaded() {
                        adLogger("topon reward video onAdLoad success")
                        loadedTimeMills = System.currentTimeMillis()
                        fullAd = rewardVideoAd
                        onLoaded(true)
                    }

                    override fun onRewardedVideoAdFailed(p0: com.thinkup.core.api.AdError?) {
                        adLogger("topon reward onAdLoadFailed: $p0")
                        adLoadFiledEvent("${p0?.code}-${p0?.desc}")
                        onLoaded(false)
                    }

                    override fun onRewardedVideoAdPlayStart(p0: TUAdInfo?) = Unit

                    override fun onRewardedVideoAdPlayEnd(p0: TUAdInfo?) = Unit

                    override fun onRewardedVideoAdPlayFailed(p0: com.thinkup.core.api.AdError?, p1: TUAdInfo?) = Unit

                    override fun onRewardedVideoAdClosed(p0: TUAdInfo?) = Unit

                    override fun onRewardedVideoAdPlayClicked(p0: TUAdInfo?) = Unit

                    override fun onReward(p0: TUAdInfo?) = Unit
                })
                rewardVideoAd.load()
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
            is com.thinkup.splashad.api.TUSplashAd -> {
                ad.setAdListener(object : TUSplashAdEZListener() {
                    override fun onAdLoaded() {}
                    override fun onNoAdError(p0: com.thinkup.core.api.AdError?) {
                        onAdDismissed(activity, onDismissed)
                        adShowFiledEvent("${p0?.code}-${p0?.desc}")
                    }

                    override fun onAdShow(p0: TUAdInfo?) {
                        onAdShowed()
                        showAdEvent(ad) // TopOn 广告价值上报
                        p0?.let {
                            RevenueHelper.onToponRevenueCallback(p0, adBean, position)
                        }
                    }

                    override fun onAdClick(p0: TUAdInfo?) {}
                    override fun onAdDismiss(p0: TUAdInfo?, p1: TUSplashAdExtraInfo?) {
                        onAdDismissed(activity, onDismissed)
                    }
                })
                val viewGroup = activity.window.decorView as? android.view.ViewGroup
                    ?: activity.findViewById<android.view.ViewGroup>(android.R.id.content)
                ad.show(activity, viewGroup)
            }

            is com.thinkup.interstitial.api.TUInterstitial -> {
                ad.setAdListener(object : com.thinkup.interstitial.api.TUInterstitialListener {
                    override fun onInterstitialAdLoaded() {}
                    override fun onInterstitialAdLoadFail(p0: com.thinkup.core.api.AdError?) {}

                    override fun onInterstitialAdShow(p0: TUAdInfo?) {
                        onAdShowed()
                        showAdEvent(ad) // TopOn 广告价值上报
                        p0?.let {
                            RevenueHelper.onToponRevenueCallback(p0, adBean, position)
                        }
                    }

                    override fun onInterstitialAdClicked(p0: TUAdInfo?) {}

                    override fun onInterstitialAdClose(p0: TUAdInfo?) {
                        onAdDismissed(activity, onDismissed)
                    }

                    override fun onInterstitialAdVideoStart(p0: TUAdInfo?) {}
                    override fun onInterstitialAdVideoEnd(p0: TUAdInfo?) {}
                    override fun onInterstitialAdVideoError(p0: com.thinkup.core.api.AdError?) {
                        onAdDismissed(activity, onDismissed)
                        adShowFiledEvent("${p0?.code}-${p0?.desc}")
                    }
                })
                ad.show(activity)
            }

            is com.thinkup.rewardvideo.api.TURewardVideoAd -> {
                ad.setAdListener(object : com.thinkup.rewardvideo.api.TURewardVideoListener {
                    override fun onRewardedVideoAdLoaded() {}
                    override fun onRewardedVideoAdFailed(p0: com.thinkup.core.api.AdError?) {}

                    override fun onRewardedVideoAdPlayStart(p0: TUAdInfo?) {
                        onAdShowed()
                        showAdEvent(ad) // TopOn 广告价值上报
                        p0?.let {
                            RevenueHelper.onToponRevenueCallback(p0, adBean, position)
                        }
                    }

                    override fun onRewardedVideoAdPlayEnd(p0: TUAdInfo?) {}

                    override fun onRewardedVideoAdPlayFailed(p0: com.thinkup.core.api.AdError?, p1: TUAdInfo?) {
                        onAdDismissed(activity, onDismissed)
                        adShowFiledEvent("${p0?.code}-${p0?.desc}")
                    }

                    override fun onRewardedVideoAdClosed(p0: TUAdInfo?) {
                        onAdDismissed(activity, onDismissed)
                    }

                    override fun onRewardedVideoAdPlayClicked(p0: TUAdInfo?) {}

                    override fun onReward(p0: TUAdInfo?) {
                        onUserEarnedReward?.invoke()
                    }
                })
                ad.show(activity)
                destroyAd()
            }

            else -> onAdDismissed(activity, onDismissed)
        }
    }

    override fun destroyAd() {
        fullAd = null
    }
}

class MaxFullAd(position: AdPosition, adBean: AdItemBean) : BaseController(position, adBean) {
    private val mScopeMain by lazy { CoroutineScope(Dispatchers.Main) }
    private var mJob: Job? = null
    private var fullAd: Any? = null

    override fun preload(onLoaded: (Boolean) -> Unit) {
        adLogger("begin loading max")
        postAdReqEvent()
        when (adBean.format) {
            AppOpenFormat -> { // MAX AppOpen 广告加载
                val appOpenAd = com.applovin.mediation.ads.MaxAppOpenAd(adBean.adId)
                appOpenAd.setListener(object : com.applovin.mediation.MaxAdListener {
                    override fun onAdLoaded(maxAd: com.applovin.mediation.MaxAd) {
                        adLogger("max appopen onAdLoad success")
                        loadedTimeMills = System.currentTimeMillis()
                        fullAd = appOpenAd
                        onLoaded(true)
                    }

                    override fun onAdLoadFailed(adUnitId: String, error: com.applovin.mediation.MaxError) {
                        adLogger("max appopen onAdLoadFailed: ${error.message}")
                        adLoadFiledEvent(error.message)
                        onLoaded(false)
                    }

                    override fun onAdDisplayed(maxAd: com.applovin.mediation.MaxAd) {}
                    override fun onAdHidden(maxAd: com.applovin.mediation.MaxAd) {}
                    override fun onAdClicked(maxAd: com.applovin.mediation.MaxAd) {}
                    override fun onAdDisplayFailed(p0: MaxAd, p1: MaxError) {
                    }

                })
                appOpenAd.loadAd()
            }

            InterstitialFormat -> { // MAX 插屏广告加载
                mJob?.cancel()
                mJob = mScopeMain.launch {
                    delay(60000)
                    adLoadFiledEvent("max_load_timeout")
                    onLoaded(false)
                }
                val interstitialAd = com.applovin.mediation.ads.MaxInterstitialAd(adBean.adId)
                interstitialAd.setListener(object : com.applovin.mediation.MaxAdListener {
                    override fun onAdLoaded(maxAd: com.applovin.mediation.MaxAd) {
                        adLogger("max interstitial onAdLoad success")
                        loadedTimeMills = System.currentTimeMillis()
                        fullAd = interstitialAd
                        mJob?.cancel()
                        onLoaded(true)
                    }

                    override fun onAdDisplayed(maxAd: com.applovin.mediation.MaxAd) {}
                    override fun onAdHidden(maxAd: com.applovin.mediation.MaxAd) {}
                    override fun onAdClicked(maxAd: com.applovin.mediation.MaxAd) {}
                    override fun onAdLoadFailed(adUnitId: String, error: com.applovin.mediation.MaxError) {
                        adLogger("max interstitial onAdLoadFailed: ${error.message}")
                        adLoadFiledEvent(error.message)
                        mJob?.cancel()
                        onLoaded(false)
                    }

                    override fun onAdDisplayFailed(p0: MaxAd, p1: MaxError) {
                    }

                })
                interstitialAd.loadAd()
            }

            RewardFormat -> { // MAX 激励广告加载
                mJob?.cancel()
                mJob = mScopeMain.launch {
                    delay(60000)
                    adLoadFiledEvent("max_load_timeout")
                    onLoaded(false)
                }
                val rewardedAd = com.applovin.mediation.ads.MaxRewardedAd.getInstance(adBean.adId)
                rewardedAd.setListener(object : com.applovin.mediation.MaxRewardedAdListener {
                    override fun onAdLoaded(maxAd: com.applovin.mediation.MaxAd) {
                        adLogger("max rewarded onAdLoad success")
                        loadedTimeMills = System.currentTimeMillis()
                        fullAd = rewardedAd
                        mJob?.cancel()
                        onLoaded(true)
                    }

                    override fun onAdDisplayed(maxAd: com.applovin.mediation.MaxAd) {}
                    override fun onAdHidden(maxAd: com.applovin.mediation.MaxAd) {}
                    override fun onAdClicked(maxAd: com.applovin.mediation.MaxAd) {}
                    override fun onAdLoadFailed(adUnitId: String, error: com.applovin.mediation.MaxError) {
                        adLogger("max rewarded onAdLoadFailed: ${error.message}")
                        adLoadFiledEvent(error.message)
                        mJob?.cancel()
                        onLoaded(false)
                    }

                    override fun onAdDisplayFailed(p0: MaxAd, p1: MaxError) {}
                    override fun onUserRewarded(maxAd: MaxAd, reward: com.applovin.mediation.MaxReward) {}
                })
                rewardedAd.loadAd()
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
            is com.applovin.mediation.ads.MaxAppOpenAd -> {
                val listener = object : com.applovin.mediation.MaxAdListener {
                    override fun onAdLoaded(maxAd: com.applovin.mediation.MaxAd) {}
                    override fun onAdDisplayed(maxAd: com.applovin.mediation.MaxAd) {
                        onAdShowed()
                        showAdEvent(ad) // MAX 广告价值上报
                        RevenueHelper.onMaxRevenueCallback(maxAd, adBean, position)
                    }

                    override fun onAdHidden(maxAd: com.applovin.mediation.MaxAd) {
                        onAdDismissed(activity, onDismissed)
                    }

                    override fun onAdClicked(maxAd: com.applovin.mediation.MaxAd) {}
                    override fun onAdLoadFailed(adUnitId: String, error: com.applovin.mediation.MaxError) {}
                    override fun onAdDisplayFailed(p0: MaxAd, p1: MaxError) {
                        onAdDismissed(activity, onDismissed)
                        adShowFiledEvent(p1.message)
                    }
                }
                ad.setListener(listener)
                ad.showAd()
            }

            is com.applovin.mediation.ads.MaxInterstitialAd -> {
                val listener = object : com.applovin.mediation.MaxAdListener {
                    override fun onAdLoaded(maxAd: MaxAd) {}
                    override fun onAdDisplayed(maxAd: MaxAd) {
                        onAdShowed()
                        showAdEvent(ad) // MAX 广告价值上报
                        RevenueHelper.onMaxRevenueCallback(maxAd, adBean, position)
                    }

                    override fun onAdHidden(maxAd: com.applovin.mediation.MaxAd) {
                        onAdDismissed(activity, onDismissed)
                    }

                    override fun onAdClicked(maxAd: com.applovin.mediation.MaxAd) {}
                    override fun onAdLoadFailed(adUnitId: String, error: com.applovin.mediation.MaxError) {}
                    override fun onAdDisplayFailed(p0: MaxAd, error: MaxError) {
                        onAdDismissed(activity, onDismissed)
                        adShowFiledEvent(error.message)
                    }
                }
                ad.setListener(listener)
                ad.showAd(activity)
            }

            is com.applovin.mediation.ads.MaxRewardedAd -> {
                val listener = object : com.applovin.mediation.MaxRewardedAdListener {
                    override fun onAdLoaded(maxAd: com.applovin.mediation.MaxAd) {
                        logError("max reward 11 onAdLoaded-->")
                    }

                    override fun onAdDisplayed(maxAd: com.applovin.mediation.MaxAd) {
                        onAdShowed()
                        showAdEvent(ad) // MAX 广告价值上报
                        RevenueHelper.onMaxRevenueCallback(maxAd, adBean, position)
                    }

                    override fun onAdHidden(maxAd: MaxAd) {
                        onAdDismissed(activity, onDismissed)
                    }

                    override fun onAdClicked(maxAd: MaxAd) {}
                    override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                        logError("max reward onAdLoadFailed-->${error.message}")
                    }

                    override fun onAdDisplayFailed(adUnitId: MaxAd, error: MaxError) {
                        onAdDismissed(activity, onDismissed)
                        adShowFiledEvent(error.message)
                    }

                    override fun onUserRewarded(maxAd: com.applovin.mediation.MaxAd,
                                                reward: com.applovin.mediation.MaxReward) {
                        logError("onUserRewarded-->")
                        onUserEarnedReward?.invoke()
                    }
                }
                ad.setListener(listener)
                ad.showAd(activity)
            }

            else -> onAdDismissed(activity, onDismissed)
        }
    }

    override fun destroyAd() {
        fullAd = null
    }
}



