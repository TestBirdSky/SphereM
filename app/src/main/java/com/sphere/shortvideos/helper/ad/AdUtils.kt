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

    var allAdShowNum by MMKVData(0)
    val adScope by lazy { CoroutineScope(Dispatchers.Main + SupervisorJob() + CoroutineExceptionHandler { _, _ -> }) }
    val launchHolder = AdHolder(LaunchPosition)
    val unlockHolder = AdHolder(UnlockPosition)
    val rewardHolder = AdHolder(RewardPosition)

    private var isSwitchIntAd = false //显示激励广告没有的情况下使用unlockHolder广告位逻辑


    fun initData(json: String = GlobalConstants.NEW_DEFAULT_AD_LOCAL_JSON) {
        val adJson = json.ifBlank { GlobalConstants.NEW_DEFAULT_AD_LOCAL_JSON }
        runCatching {
            JSONObject(adJson).apply {
                isSwitchIntAd = optBoolean("dlmsf_switch")
                launchHolder.initHolder(LaunchPosition.aliasName.formatBean(this))
                unlockHolder.initHolder(UnlockPosition.aliasName.formatBean(this))
                rewardHolder.initHolder(RewardPosition.aliasName.formatBean(this))
            }
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
                   adPositionName: String,
                   isRate: () -> Unit = {}) {
        if (DramaIntAdHelper.fetchIsShowRateAd().not()) {
            logError("fetchIsShowRateAd failed-->")
            dismiss.invoke()
            return
        }
        isRate.invoke()
        if (unlockHolder.isAdHaveCache()) {
            unlockHolder.showFullAd(activity, onAdDismissed = dismiss, adPositionName = adPositionName)
        } else {
            unlockHolder.preloadIfCan()
        }
    }

    fun showRvAd(activity: GenericActivity,
                 adPositionName: String, dismiss: (isRewardSuccess: Boolean) -> Unit = {}) {
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
            rewardHolder.showFullAd(activity, adPositionName = adPositionName, rewardCall = { isRewardCall = true }, onAdDismissed = {
                RiskHelper.closeRvEvent(System.currentTimeMillis() - showRVAdTime)
                dis.invoke()
            }, onAdShowed = {
                showRVAdTime = System.currentTimeMillis()
            })
            return
        } else if (isSwitchIntAd) {
            if (unlockHolder.isAdHaveCache()) {
                val time = System.currentTimeMillis()
                unlockHolder.showFullAd(activity, adPositionName = adPositionName, onAdDismissed = {
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