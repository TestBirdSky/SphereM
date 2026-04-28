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
    var isInBack = true

    private var isSwitchIntAd = false //显示激励广告没有的情况下使用unlockHolder广告位逻辑
    private var lastAdJson = ""
    private var requestFailTimes = 5
    private var requestIntervalSec = 10

    fun initData(json: String = GlobalConstants.NEW_DEFAULT_AD_LOCAL_JSON) {
        val adJson = json.ifBlank { GlobalConstants.NEW_DEFAULT_AD_LOCAL_JSON }
        if (lastAdJson == adJson) return
        runCatching {
            JSONObject(adJson).apply {
                isSwitchIntAd = optBoolean("dlmsf_switch")
                requestFailTimes = optInt("fail_times", 5).coerceAtLeast(1)
                requestIntervalSec = optInt("request_interval", 10).coerceAtLeast(1)
                launchHolder.initHolder(LaunchPosition.aliasName.formatBean(this))
                unlockHolder.initHolder(UnlockPosition.aliasName.formatBean(this))
                rewardHolder.initHolder(RewardPosition.aliasName.formatBean(this))
                launchHolder.updateRequestRetryConfig(requestFailTimes, requestIntervalSec)
                unlockHolder.updateRequestRetryConfig(requestFailTimes, requestIntervalSec)
                rewardHolder.updateRequestRetryConfig(requestFailTimes, requestIntervalSec)
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
                val timeout = itemObj.optInt("gqqvwedz", 0 /*itemObj.optInt("dsad", 0)*/)
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
        if (unlockHolder.isAdHaveCache()) {
            unlockHolder.showFullAd(activity, onAdDismissed = dismiss, adPosId = adPosId)
        } else {
            noAd.invoke()
            unlockHolder.preloadIfCan()
        }
    }

    fun perLoadRvAd() {
        rewardHolder.preloadIfCan()
        if (isSwitchIntAd) {
            unlockHolder.preloadIfCan()
        }
    }

    fun showRvAd(activity: GenericActivity,
                 adPosId: String, dismiss: (isRewardSuccess: Boolean) -> Unit = {}) {
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
            rewardHolder.preloadIfCan()
            unlockHolder.preloadIfCan()
        }
        if (rewardHolder.isAdHaveCache()) {
            var showRVAdTime = 0L
            rewardHolder.showFullAd(activity,
                adPosId = adPosId,
                rewardCall = { isRewardCall = true },
                onAdDismissed = {
                    RiskHelper.closeRvEvent(System.currentTimeMillis() - showRVAdTime)
                    dis.invoke()
                },
                onAdShowed = {
                    showRVAdTime = System.currentTimeMillis()
                })
            return
        } else if (isSwitchIntAd) {
            if (unlockHolder.isAdHaveCache()) {
                val time = System.currentTimeMillis()
                unlockHolder.showFullAd(activity, adPosId = adPosId, onAdDismissed = {
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

}