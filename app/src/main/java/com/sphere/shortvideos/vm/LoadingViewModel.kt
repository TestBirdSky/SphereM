package com.sphere.shortvideos.vm

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.sphere.shortvideos.baseui.GenericActivity
import com.sphere.shortvideos.helper.AppHelper
import com.sphere.shortvideos.helper.EventData
import com.sphere.shortvideos.helper.ad.AdUtils
import com.sphere.shortvideos.helper.ad.AdUtils.rewardHolder
import com.sphere.shortvideos.helper.ad.AdUtils.unlockHolder
import com.sphere.shortvideos.helper.ad.LaunchPosition
import com.sphere.shortvideos.helper.localEvent
import com.sphere.shortvideos.logError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoadingViewModel : ViewModel() {
    var posiIdName = if (AppHelper.isIceLuncher) "dlmsf_launch_cold" else "dlmsf_launch_hot"
    // v1.0.9 新增需求
    var adContext = if (AppHelper.isIceLuncher) "launcher" else "hot"
    private var consentInformation: ConsentInformation? = null
    private var waitLoadingJob: Job? = null
    val umpCompletedLiveData = MutableLiveData<Boolean>()
    val nextLiveData = MutableLiveData<Boolean>()

    fun refreshAdContext(string: String) {
        AdUtils.launchHolder.position.adContext = string
    }

    fun waitAdLoading(activity: GenericActivity) {
        logError("waitAdLoading--->")
        preload(true)
        waitLoadingJob?.cancel()
        waitLoadingJob = viewModelScope.launch(Dispatchers.IO) {
            repeat(150) { num ->
                delay(100L)
                if (activity.getActivityState() && num >= 20 && AdUtils.launchHolder.isAdHaveCache()) {
                    waitLoadingJob?.cancel()
                    unlockHolder.preloadIfCan()
                    rewardHolder.preloadIfCan()
                    AdUtils.launchHolder.showFullAd(activity, onAdDismissed = {
                        nextLiveData.postValue(true)
                    }, adPosId = posiIdName)
                } else if (num % 10 == 0) {
                    preload()
                }
            }
            unlockHolder.preloadIfCan()
            rewardHolder.preloadIfCan()
            nextLiveData.postValue(true)
        }
    }

    fun getUmpIfNeed(activity: AppCompatActivity) {
        runCatching {
            if (euCountry.any { it == EventData.firstCountry }) {
                consentInformation = UserMessagingPlatform.getConsentInformation(activity)
                consentInformation?.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().build(), {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { umpCompletedLiveData.postValue(true) }
                }, { umpCompletedLiveData.postValue(false) })
            } else {
                umpCompletedLiveData.postValue(false)
            }
        }.onFailure {
            umpCompletedLiveData.postValue(false)
        }
    }

    private fun preload(needChance: Boolean = false) {
        if (needChance) localEvent("ad_chance", hashMapOf("ad_pos_id" to posiIdName, "ad_context" to adContext))
        AdUtils.run {
            launchHolder.preloadIfCan()
        }
    }

    private val euCountry by lazy {
        listOf(
            "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT",
            "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE", "NO", "IS", "LI", "CH", "GB"
        )
    }

}