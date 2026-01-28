package com.sphere.shortvideos.helper.ad

import com.sphere.shortvideos.GlobalConstants
import com.sphere.shortvideos.baseui.GenericActivity
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.json.JSONObject

object AdUtils {

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

    fun showUnlockAd(activity: GenericActivity) {
        if (unlockHolder.isAdHaveCache()) {
            unlockHolder.showFullAd(activity)
        } else {
            unlockHolder.preloadIfCan()
        }
    }

    fun showRateAd(activity: GenericActivity, dismiss: () -> Unit={}) {
        if (DramaIntAdHelper.fetchIsShowRateAd().not()) {
            dismiss.invoke()
            return
        }
        if (unlockHolder.isAdHaveCache()) {
            unlockHolder.showFullAd(activity, onAdDismissed = dismiss)
        } else {
            unlockHolder.preloadIfCan()
        }
    }


    fun showRvAd(activity: GenericActivity) {
        if (rewardHolder.isAdHaveCache()) {
            rewardHolder.showFullAd(activity, onAdDismissed = {

            })
        } else if (isSwitchIntAd) {
            showUnlockAd(activity)
        } else {
            rewardHolder.preloadIfCan()
        }
    }

}