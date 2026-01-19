package com.sphere.shortvideos.helper.ad

import com.sphere.shortvideos.baseui.GenericActivity

data class AdItemBean(
    val adId: String,
    val source: String,
    val format: AdFormat,
    val timeout: Int,
    val weight: Int,
) {
    fun buildController(position: AdPosition): AdmobFullAd {
        return when (format) {
            AppOpenFormat, InterstitialFormat -> AdmobFullAd(position, this)
        }
    }
}

sealed class AdPosition {
    abstract val aliasName: String
}

data object LaunchPosition : AdPosition() {
    override val aliasName: String get() = "ds_launch"
}

data object UnlockPosition : AdPosition() {
    override val aliasName: String get() = "ds_unlock_int"
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

interface IAdController {

    fun preload(onLoaded: (Boolean) -> Unit)

    fun showFullScreenAd(activity: GenericActivity, onDismissed: () -> Unit, onAdShowed: () -> Unit)

    fun destroyAd()

}