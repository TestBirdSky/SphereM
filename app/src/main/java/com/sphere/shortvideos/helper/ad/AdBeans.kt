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
    abstract val aliasName: String
}

data object LaunchPosition : AdPosition() {
    override val aliasName: String get() = "dlmsf_launch"
}

data object UnlockPosition : AdPosition() {
    override val aliasName: String get() = "dlmsf_int"
}

data object RewardPosition : AdPosition() {
    override val aliasName: String get() = "dlmsf_rv"
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