package com.sphere.shortvideos.helper.ad

import com.sphere.shortvideos.GlobalConstants
import com.sphere.shortvideos.baseui.GenericActivity
import com.sphere.shortvideos.dialogs.ShowAdLimitDialogFragment
import com.sphere.shortvideos.helper.localEvent
import com.sphere.shortvideos.helper.mmkv.MMKVData
import com.sphere.shortvideos.helper.risk.RiskHelper
import com.sphere.shortvideos.logError
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.json.JSONObject

object AdUtils {
    private var historyAdNum by MMKVData(0)
    private var adPostPvTagNum by MMKVData(5)
    var allAdShowNum by MMKVData(0)

    fun adShow() {
        allAdShowNum++
        historyAdNum++
        if (historyAdNum >= adPostPvTagNum) {
            localEvent("pv_dall", hashMapOf("ad" to adPostPvTagNum))
            adPostPvTagNum += 5
        }
    }

    val adScope by lazy { CoroutineScope(Dispatchers.Main + SupervisorJob() + CoroutineExceptionHandler { _, _ -> }) }
    val launchHolder = AdHolder(LaunchPosition)
    val unlockHolder = AdHolder(UnlockPosition)
    val rewardHolder = AdHolder(RewardPosition)
    private val sceneBidLaunchHolder = AdHolder(SceneBidPosition(LaunchPosition), "_scene_bid")
    private val sceneBidUnlockHolder = AdHolder(SceneBidPosition(UnlockPosition), "_scene_bid")
    private val sceneBidRewardHolder = AdHolder(SceneBidPosition(RewardPosition), "_scene_bid")
    var isInBack = true

    private var isSwitchIntAd = false //显示激励广告没有的情况下使用unlockHolder广告位逻辑
    private var isSenseBidOpen = false
    private var lastAdJson = ""
    private var requestFailTimes = 5
    private var requestIntervalSec = 10

    fun initData(json: String = GlobalConstants.NEW_DEFAULT_AD_LOCAL_JSON) {
        val adJson = json.ifBlank { GlobalConstants.NEW_DEFAULT_AD_LOCAL_JSON }
        if (lastAdJson == adJson) return
        logError("initData--->$adJson")
        runCatching {
            JSONObject(adJson).apply {
                isSwitchIntAd = optBoolean("dlmsf_switch")
                val isOpen = optBoolean("dlmsf_req_s", false)
                isSenseBidOpen = optBoolean("dlmsf_sencebid", false)
                requestFailTimes = optInt("fail_times", 0)
                requestIntervalSec = optInt("request_interval", 1)
                launchHolder.initHolder(LaunchPosition.aliasName.formatBean(this))
                unlockHolder.initHolder(UnlockPosition.aliasName.formatBean(this))
                rewardHolder.initHolder(RewardPosition.aliasName.formatBean(this))
                val sceneBidObj = optJSONObject("dlmsf_scene_bid") ?: JSONObject()
                sceneBidLaunchHolder.initHolder(LaunchPosition.aliasName.formatBean(sceneBidObj))
                sceneBidUnlockHolder.initHolder(UnlockPosition.aliasName.formatBean(sceneBidObj))
                sceneBidRewardHolder.initHolder(RewardPosition.aliasName.formatBean(sceneBidObj))
                if (isOpen) {
                    allHolders().forEach { it.updateRequestRetryConfig(requestFailTimes, requestIntervalSec, isOpen) }
                }
            }
            lastAdJson = adJson
        }
    }

    private fun String.formatBean(obj: JSONObject): List<AdItemBean> {
        val result = mutableListOf<AdItemBean>()
        val jsonArray = obj.optJSONArray(this) ?: return result
        runCatching {
            for (i in 0 until jsonArray.length()) {
                val itemObj = jsonArray.getJSONObject(i)
                val formatKey = itemObj.optString("ugebepat").ifBlank { itemObj.optString("dsty") }
                val format = when (formatKey) {
                    "open", "op" -> AppOpenFormat
                    "reward", "rv" -> RewardFormat
                    else -> InterstitialFormat
                }
                val adId = itemObj.optString("byfxjhld")/*.ifBlank { itemObj.optString("dsid") }*/
                val source = itemObj.optString("dmhytwql")/*.ifBlank { itemObj.optString("amtt") }*/
                val timeout = itemObj.optInt("gqqvwedz", 3000 /*itemObj.optInt("dsad", 0)*/)
                val weight = itemObj.optInt("wdzqbsbt", 0/*itemObj.optInt("dsei", 0)*/)
                result.add(AdItemBean(
                    adId = adId,
                    source = source,
                    format = format,
                    timeout = timeout,
                    weight = weight,
                ))
            }
        }
        return result
    }

    fun showRateAd(activity: GenericActivity,
                   dismiss: () -> Unit = {},
                   noAd: () -> Unit = {},
                   adPosId: String,
                   isRate: () -> Unit = {}) {
        if (DramaIntAdHelper.fetchIsShowRateAd().not()) {
            logError("fetchIsShowRateAd failed-->")
            dismiss.invoke()
            return
        }
        isRate.invoke()
        val holder = selectBestHolder(unlockHolders())
        if (holder != null) {
            holder.showFullAd(activity, onAdDismissed = dismiss, adPosId = adPosId)
        } else {
            noAd.invoke()
            preloadUnlock()
        }
    }

    fun perLoadRvAd() {
        preloadReward()
        if (isSwitchIntAd) {
            preloadUnlock()
        }
    }

    fun showRvAd(activity: GenericActivity, adPosId: String, dismiss: (isRewardSuccess: Boolean) -> Unit = {}) {
        if (RiskHelper.isAdLimit()) {
            ShowAdLimitDialogFragment({
                localEvent("see_you_tommorow")
                dismiss.invoke(false)
            }).show(activity.supportFragmentManager, "limit_ad")
            return
        }
        var isRewardCall = false
        val dis = {
            dismiss.invoke(isRewardCall)
            preloadReward()
            preloadUnlock()
        }
        val rewardShowHolder = selectBestHolder(rewardBidHolders())
        if (rewardShowHolder != null) {
            var showRVAdTime = 0L
            val isRewardHolder = rewardShowHolder.isRewardHolder()
            rewardShowHolder.showFullAd(activity,
                adPosId = adPosId,
                rewardCall = { isRewardCall = true },
                onAdDismissed = {
                    val showDuration = System.currentTimeMillis() - showRVAdTime
                    RiskHelper.closeRvEvent(showDuration)
                    if (!isRewardHolder && showDuration > 5000) {
                        isRewardCall = true
                    }
                    dis.invoke()
                },
                onAdShowed = {
                    showRVAdTime = System.currentTimeMillis()
                })
            return
        } else if (isSwitchIntAd && isSenseBidOpen.not()) {
            val holder = selectBestHolder(unlockHolders())
            if (holder != null) {
                val time = System.currentTimeMillis()
                holder.showFullAd(activity, adPosId = adPosId, onAdDismissed = {
                    if (System.currentTimeMillis() - time > 5000) {
                        isRewardCall = true
                    }
                    dis.invoke()
                })
                return
            }
        }
        dis.invoke()
    }

    fun preloadLaunch() {
        launchHolder.preloadIfCan()
        sceneBidLaunchHolder.preloadIfCan()
    }

    fun preloadUnlock() {
        unlockHolder.preloadIfCan()
        sceneBidUnlockHolder.preloadIfCan()
    }

    fun preloadReward() {
        rewardHolder.preloadIfCan()
        sceneBidRewardHolder.preloadIfCan()
        if (isSenseBidOpen) preloadUnlock()
    }

    fun isLaunchAdHaveCache() = launchBidHolders().any { it.isAdHaveCache() }

    fun showLaunchAd(activity: GenericActivity,
                     adPosId: String,
                     onAdDismissed: () -> Unit = {},
                     onAdShowed: () -> Unit = {}) {
        val holder = selectBestHolder(launchBidHolders())
        if (holder == null) {
            onAdDismissed()
            preloadLaunch()
        } else {
            holder.showFullAd(activity, adPosId = adPosId, onAdDismissed = onAdDismissed, onAdShowed = onAdShowed)
        }
    }

    fun isUnlockAdHaveCache() = unlockHolders().any { it.isAdHaveCache() }

    fun showUnlockAd(activity: GenericActivity,
                     adPosId: String,
                     canShowAd: () -> Boolean = { RiskHelper.isAdLimit().not() },
                     onAdDismissed: () -> Unit = {},
                     onAdShowed: () -> Unit = {}) {
        val holder = selectBestHolder(unlockHolders())
        if (holder == null) {
            onAdDismissed()
            preloadUnlock()
        } else {
            holder.showFullAd(activity,
                adPosId = adPosId,
                canShowAd = canShowAd,
                onAdDismissed = onAdDismissed,
                onAdShowed = onAdShowed)
        }
    }

    private fun selectBestHolder(holders: List<AdHolder>): AdHolder? {
        val candidates = holders.mapNotNull { holder ->
            holder.peekCachedAd()?.let { holder to it }
        }
        logError("Ad cp--> compareAd position: ${
            candidates.joinToString(prefix = "[", postfix = "]") { (_, ad) ->
                "${ad.position.adSense}/${ad.adBean.source}/${ad.adBean.format.aliasName}/${ad.adBean.adId}/${
                    ad.bidEcpm
                }"
            }
        }")
        val pair = candidates.maxWithOrNull { first, second -> compareAd(first.second, second.second) }
        val holder = pair?.first
        pair?.let {
            val se = pair.second
            logError("Ad cp--> compareAd success position->-${se.adBean.source}--${holder?.position?.adSense}-${se.adBean.format.aliasName}  --ecpm=${se.bidEcpm} --adid=${se.adBean.adId}")
        }
        return holder
    }

    private fun launchBidHolders() = buildList {
        add(launchHolder)
        add(sceneBidLaunchHolder)
        if (isSenseBidOpen) addAll(unlockHolders())
    }

    private fun unlockHolders() = listOf(unlockHolder, sceneBidUnlockHolder)

    private fun rewardBidHolders() = buildList {
        add(rewardHolder)
        add(sceneBidRewardHolder)
        if (isSenseBidOpen) addAll(unlockHolders())
    }

    private fun AdHolder.isRewardHolder() = this == rewardHolder || this == sceneBidRewardHolder

    private fun compareAd(first: BaseController, second: BaseController): Int {
        val firstEcpm = first.bidEcpm
        val secondEcpm = second.bidEcpm
        return firstEcpm.compareTo(secondEcpm)
    }

    private fun formatBidEcpm(value: Double) = "%.10f".format(java.util.Locale.US, value).trimEnd('0').trimEnd('.')

    private fun allHolders() = listOf(
        launchHolder,
        unlockHolder,
        rewardHolder,
        sceneBidLaunchHolder,
        sceneBidUnlockHolder,
        sceneBidRewardHolder,
    )

}
