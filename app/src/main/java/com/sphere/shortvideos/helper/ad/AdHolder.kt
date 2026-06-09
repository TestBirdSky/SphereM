package com.sphere.shortvideos.helper.ad

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.chartboost.sdk.impl.fa
import com.google.android.gms.ads.MobileAds
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sphere.shortvideos.baseui.GenericActivity
import com.sphere.shortvideos.databinding.LayoutProgressbarBinding
import com.sphere.shortvideos.helper.localEvent
import com.sphere.shortvideos.helper.mmkv.mmkvIns
import com.sphere.shortvideos.helper.risk.RiskHelper
import com.sphere.shortvideos.logError
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AdHolder(val position: AdPosition, private val retryKeySuffix: String = "") {

    private val sourceList = mutableListOf<AdItemBean>()
    private val cacheList = mutableListOf<BaseController>()
    private var loading = false
    private var onAdLoaded: (Boolean) -> Unit = {}
    private var loadingTime = 0L
    private var failTimesThreshold = Int.MAX_VALUE
    private var requestIntervalSec = 0
    private var isOpen = false

    private val failCountKey = "ad_fail_count_${position.adSense}$retryKeySuffix"
    private val lastFailTimeKey = "ad_last_fail_ms_${position.adSense}$retryKeySuffix"

    fun isAdHaveCache() = cacheList.isNotEmpty()

    fun peekCachedAd() = cacheList.firstOrNull()

    fun initHolder(data: List<AdItemBean>) {
        sourceList.clear()
        sourceList.addAll(data.sortedByDescending { it.weight })
    }

    fun updateRequestRetryConfig(failTimes: Int, requestInterval: Int, isOpen: Boolean) {
        failTimesThreshold = failTimes
        requestIntervalSec = requestInterval
        this.isOpen = isOpen
    }

    fun preloadIfCan() {
        if (sourceList.isEmpty()) return
        if (RiskHelper.isAdLimit()) {
            logError("ad limit-->")
            return
        }
        AdUtils.adScope.launch {
            if (sourceList.isEmpty()) return@launch
            if (position != LaunchPosition) {
                if (isStoppedByBackgroundFailure()) {
                    logError("stop preload by background fail rule: ${position.aliasName}")
                    return@launch
                }
                val waitTime = isCanLoadAdIfNeeded()
                if (waitTime > 0) {
                    logError("stop preload not in period$waitTime  ${position.aliasName}")
                    return@launch
                }
            }
            removeExpiredAd()
            if (cacheList.isNotEmpty()) return@launch
            if (loading && System.currentTimeMillis() - loadingTime < 60000 * 4) {
                logError("ad is loading $position")
                return@launch
            }
            loadingTime = System.currentTimeMillis()
            loading = true
            loadAd(0)
        }
    }

    fun showFullAd(activity: GenericActivity, adPosId: String = "", canShowAd: () -> Boolean = {
        RiskHelper.isAdLimit().not()
    }, onAdDismissed: () -> Unit = {}, onAdShowed: () -> Unit = {}, rewardCall: (() -> Unit)? = null) {
        AdUtils.adScope.launch {
            if (!canShowAd()) {
                onAdDismissed()
                return@launch
            }
            val ad = cacheList.removeFirstOrNull()
            if (null == ad) {
                onAdDismissed()
                onAdLoaded = {}
                preloadIfCan()
                return@launch
            } else {
                val dialog = showAdDialog(activity)
                delay(1000L)
                dialog?.dismiss()
                ad.onUserEarnedReward = rewardCall
                position.aliasName = adPosId.ifBlank { position.aliasName }
                ad.showFullScreenAd(activity, onAdDismissed, onAdShowed)
                logError("Ad cp--> show ad event ${position.aliasName} ")
                localEvent("ds_ad_impression", hashMapOf("ad_pos_id" to adPosId))
                onAdLoaded = {}
                preloadIfCan()
            }
        }
    }

    private fun removeExpiredAd() {
        runCatching {
            if (cacheList.isEmpty()) return
            val item = cacheList.firstOrNull() ?: return
            if (item.isAdExpired()) cacheList.remove(item)
        }
    }

    private fun loadAd(index: Int) {
        val adItem = sourceList.getOrNull(index)
        if (null == adItem) {
            loading = false
            loadingTime = 0
            onRequestFailed()
            onAdLoaded(false) // 加载完成
            if (position != LaunchPosition && isAdHaveCache().not()) {
                AdUtils.adScope.launch {
                    delay(3000)
                    val waitTime = isCanLoadAdIfNeeded()
                    if (waitTime > 0) {
                        delay(waitTime)
                    }
                    preloadIfCan()
                }
            }
            return
        }

        // 在 preload 之前判断平台是否就绪
        if (!isPlatformReady(adItem.source)) { // 平台未就绪，跳过当前广告项，继续下一个
            loadAd(index + 1)
            return
        }

        val adEntity = adItem.buildController(position)
        adEntity.preload { success ->
            if (success) {
                onRequestSuccess()
                localEvent("ad_return",
                    hashMapOf(
                        "ad_code_id" to adEntity.adBean.adId,
                        "ad_format" to adEntity.adBean.format.aliasName,
                        "ad_platform" to adEntity.adBean.source,
                        "ad_sense" to position.adSense,
                        "loaded_revenue" to adEntity.cachedBidEcpm
                    ))
                cacheList.add(adEntity)
                loading = false
                loadingTime = 0
                onAdLoaded(true)
            } else loadAd(index + 1)
        }
    }

    private fun isPlatformReady(source: String): Boolean {
        val sourceLower = source.lowercase()

        return when (sourceLower) {
            "topon" -> {
                true
            }

            in listOf("max", "applovin") -> {
                return false
            }

            in listOf("pangle", "pag", "pagm") -> true

            else -> { // Admob 或其他平台
                val status = MobileAds.getInitializationStatus()
                status?.adapterStatusMap?.isNotEmpty() == true
            }
        }
    }

    private fun showAdDialog(activity: Activity): AlertDialog? {
        val binding = LayoutProgressbarBinding.inflate(LayoutInflater.from(activity),
            activity.window.decorView as ViewGroup,
            false)
        return MaterialAlertDialogBuilder(activity).setView(binding.root).setCancelable(false).show()
    }

    private val mApp = com.sphere.shortvideos.mApp

    private fun onRequestSuccess() {
        if (position == LaunchPosition) return
        mmkvIns.encode(failCountKey, 0)
        mmkvIns.encode(lastFailTimeKey, 0L)
    }

    private fun onRequestFailed() {
        if (position == LaunchPosition) return
        if (isOpen.not()) return
        val currentFailCount = mmkvIns.decodeInt(failCountKey, 0) + 1
        mmkvIns.encode(failCountKey, currentFailCount)
        mmkvIns.encode(lastFailTimeKey, System.currentTimeMillis())
    }

    private fun isCanLoadAdIfNeeded(): Long {
        if (isOpen.not()) return 0
        val failCount = mmkvIns.decodeInt(failCountKey, 0)
        val lastFailAt = mmkvIns.decodeLong(lastFailTimeKey, 0L)
        if (lastFailAt <= 0L) return 0L
        if (failCount <= failTimesThreshold) {
            return 0L
        }
        val step = (failCount / failTimesThreshold) - 1
        if (step < 0) return 0L
        val intervalSec = requestIntervalSec.toLong().times(1L shl step.coerceAtMost(20))
        val nextAt = lastFailAt + intervalSec * 1000L
        return (nextAt - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    private fun isStoppedByBackgroundFailure(): Boolean {
        if (AdUtils.isInBack.not()) return false
        val failCount = mmkvIns.decodeInt(failCountKey, 0)
        return failCount >= failTimesThreshold
    }

}
