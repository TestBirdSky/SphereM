package com.sphere.shortvideos.helper.ad

import com.sphere.shortvideos.baseui.GenericActivity

data class AdItemBean(
    val adId: String,
    val source: String,
    val format: AdFormat,
    val timeout: Int,
    val weight: Int,
) {
    fun buildController(position: AdPosition): BaseController {
        return when (format) {
            AppOpenFormat, InterstitialFormat, RewardFormat -> {
                when (source.lowercase()) {
                    "topon" -> ToponFullAd(position, this)
                    "max", "applovin" -> MaxFullAd(position, this)
                    else -> AdmobFullAd(position, this)
                }
            }
        }
    }
}

sealed class AdPosition {
    abstract var aliasName: String // ad_pos_id
    abstract val adSense: String

    open var adContext = "" //针对开屏广告增加的参数
}

data object LaunchPosition : AdPosition() {
    override var aliasName: String = "dlmsf_launch"

    override val adSense: String
        get() = "Launch"

    override var adContext = ""

}

data object UnlockPosition : AdPosition() {
    override var aliasName: String = "dlmsf_int"
    override val adSense: String
        get() = "Interstitial"
}

data object RewardPosition : AdPosition() {
    override var aliasName: String = "dlmsf_rv"
    override val adSense: String
        get() = "Reward"
}

sealed class AdFormat {
    abstract val aliasName: String
}

data object AppOpenFormat : AdFormat() {
    override val aliasName: String get() = "open"
}

data object InterstitialFormat : AdFormat() {
    override val aliasName: String get() = "interstitial"
}

data object RewardFormat : AdFormat() {
    override val aliasName: String get() = "reward"
}

interface IAdController {

    fun preload(onLoaded: (Boolean) -> Unit)

    fun showFullScreenAd(activity: GenericActivity, onDismissed: () -> Unit, onAdShowed: () -> Unit)

    fun destroyAd()

}