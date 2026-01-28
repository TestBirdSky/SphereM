package com.sphere.shortvideos.helper.ad

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.ads.MobileAds
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sphere.shortvideos.baseui.GenericActivity
import com.sphere.shortvideos.databinding.LayoutProgressbarBinding
import com.sphere.shortvideos.helper.localEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AdHolder(private val position: AdPosition) {

    private val sourceList = mutableListOf<AdItemBean>()
    private val cacheList = mutableListOf<BaseController>()
    private var loading = false
    private var onAdLoaded: (Boolean) -> Unit = {}

    fun isAdHaveCache() = cacheList.isNotEmpty()

    fun initHolder(data: List<AdItemBean>) {
        sourceList.clear()
        sourceList.addAll(data.sortedByDescending { it.weight })
    }

    fun preloadIfCan() {
        AdUtils.adScope.launch {
            if (!isAdmobReady() || sourceList.isEmpty()) return@launch
            removeExpiredAd()
            if (cacheList.isNotEmpty() || loading) return@launch
            loading = true
            loadAd(0)
        }
    }

    fun showFullAd(
        activity: GenericActivity,
        eventName: String = position.aliasName,
        canShowAd: () -> Boolean = { true },
        onAdDismissed: () -> Unit = {},
        onAdShowed: () -> Unit = {},
    ) {
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
                ad.showFullScreenAd(activity, onAdDismissed, onAdShowed)
                localEvent("ds_ad_impression", hashMapOf("ad_pos_id" to eventName))
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
        val adEntity = adItem?.buildController(position)
        if (null == adEntity) {
            loading = false
            onAdLoaded(false)
            return
        }
        adEntity.preload { success ->
            if (success) {
                cacheList.add(adEntity)
                loading = false
                onAdLoaded(true)
            } else loadAd(index + 1)
        }
    }

    private fun showAdDialog(activity: Activity): AlertDialog? {
        val binding = LayoutProgressbarBinding.inflate(LayoutInflater.from(activity),
            activity.window.decorView as ViewGroup,
            false)
        return MaterialAlertDialogBuilder(activity).setView(binding.root).setCancelable(false).show()
    }

    private fun isAdmobReady(): Boolean {
        val status = MobileAds.getInitializationStatus()
        return status?.adapterStatusMap?.isNotEmpty() == true
    }

}