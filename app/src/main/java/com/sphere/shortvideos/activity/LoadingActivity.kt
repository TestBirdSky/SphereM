package com.sphere.shortvideos.activity

import android.view.View
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.sphere.shortvideos.GlobalConstants

import com.sphere.shortvideos.baseui.GenericBindActivity
import com.sphere.shortvideos.databinding.ActivityLoadingBinding
import com.sphere.shortvideos.helper.MoneyCacheHelper
import com.sphere.shortvideos.helper.AppHelper
import com.sphere.shortvideos.helper.localEvent
import com.sphere.shortvideos.helper.mmkv.MMKVData
import com.sphere.shortvideos.helper.mmkv.MMKVRepository
import com.sphere.shortvideos.helper.permission.PostPermission
import com.sphere.shortvideos.helper.session
import com.sphere.shortvideos.nextView
import com.sphere.shortvideos.notification.NotificationHelper
import com.sphere.shortvideos.vm.LoadingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoadingActivity : GenericBindActivity<ActivityLoadingBinding>() {
    private var isFirstGoLoadingPage by MMKVData(true)
    private val mPostPermission = PostPermission(this)
    private val viewModel by viewModels<LoadingViewModel>()
    private var lastNotificationId = -1

    override val binding by lazy {
        topMar = 0
        ActivityLoadingBinding.inflate(layoutInflater)
    }

    override fun initUI() {
        checkNotification()
        viewModel.umpCompletedLiveData.observe(this) {
            if (isFirstGoLoadingPage.not()) {
                viewModel.waitAdLoading(this)
            }
        }
        viewModel.nextLiveData.observe(this) {
            openMain()
        }
        if (MMKVRepository.isNeedRequestUMP) {
            MMKVRepository.isNeedRequestUMP = false
            viewModel.getUmpIfNeed(this)
        } else {
            viewModel.umpCompletedLiveData.postValue(true)
        }
        if (isFirstGoLoadingPage) {
            binding.tvStart.visibility = View.VISIBLE
            binding.tvPp.visibility = View.VISIBLE
            binding.tvStart.setOnClickListener {
                localEvent("launch_start")
                openMain()
            }
            binding.tvPp.setOnClickListener {
                CustomTabsIntent.Builder().build().launchUrl(this, GlobalConstants.PRIVACY_POLICY.toUri())
            }
        }
        session()
        MMKVRepository.checkCueDay()
    }

    private fun openMain() {
        nextView<MainActivity> {
            if (lastNotificationId > 0) putExtra(NotificationHelper.NOTIFICATION_ID_KEY, lastNotificationId)
        }
        finish()
    }

    override fun onBackActioned() = Unit

    override fun onDestroy() {
        super.onDestroy()
        AppHelper.isNeedFetch = true
        isFirstGoLoadingPage = false
    }

    override fun onResume() {
        super.onResume()
        mPostPermission.requestPermission {}
        localEvent("launch_page")
        lifecycleScope.launch {
            delay(1000)
            NotificationHelper.showNotificationService(this@LoadingActivity)
        }
    }

    private fun checkNotification() {
        val notificationId = intent.getIntExtra(NotificationHelper.NOTIFICATION_ID_KEY, -1)
        lastNotificationId = notificationId
        if (notificationId != -1) {
            runCatching {
                NotificationManagerCompat.from(this).cancel(notificationId)
            }
            NotificationHelper.clickNotiEvent(notificationId)
        }
    }
}