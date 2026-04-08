package com.sphere.shortvideos.activity

import android.view.View
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.sphere.shortvideos.GlobalConstants

import com.sphere.shortvideos.baseui.GenericBindActivity
import com.sphere.shortvideos.databinding.ActivityLoadingBinding
import com.sphere.shortvideos.helper.AppHelper
import com.sphere.shortvideos.helper.localEvent
import com.sphere.shortvideos.helper.mmkv.MMKVData
import com.sphere.shortvideos.helper.mmkv.MMKVRepository
import com.sphere.shortvideos.helper.permission.PostPermission
import com.sphere.shortvideos.logError
import com.sphere.shortvideos.nextView
import com.sphere.shortvideos.notification.NotificationHelper
import com.sphere.shortvideos.view.AnimViewHelper
import com.sphere.shortvideos.vm.LoadingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoadingActivity : GenericBindActivity<ActivityLoadingBinding>() {
    private var isReq = true
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
        logError("isFirstGoLoadingPage--->$isFirstGoLoadingPage")
        if (isFirstGoLoadingPage) {
            binding.progressHorizontal.visibility = View.INVISIBLE
            binding.tvDes2.visibility = View.INVISIBLE
            binding.firstLayout.visibility = View.VISIBLE
            binding.tvPp.visibility = View.VISIBLE
            // 启动按钮提醒：复用已有的呼吸脉冲动画
            AnimViewHelper.playClaimablePulseAnim(binding.tvStart, true, 0.98f, 1.06f)
            binding.tvStart.setOnClickListener {
                localEvent("launch_start")
                openMain()
            }
            binding.tvPp.setOnClickListener {
                CustomTabsIntent.Builder().build().launchUrl(this, GlobalConstants.PRIVACY_POLICY.toUri())
            }
        }
        localEvent("launch_page")
        MMKVRepository.checkCueDay()
    }

    private fun openMain() {
        isFirstGoLoadingPage = false
        nextView<MainActivity> {
            if (lastNotificationId > 0) putExtra(NotificationHelper.NOTIFICATION_ID_KEY, lastNotificationId)
        }
        finish()
    }

    override fun onBackActioned() = Unit

    override fun onDestroy() {
        super.onDestroy()
        AppHelper.isIceLuncher = false
    }

    override fun onResume() {
        super.onResume()
        if (isReq) {
            isReq = false
            mPostPermission.requestPermission {
                lifecycleScope.launch {
                    delay(500)
                    NotificationHelper.showOrUpdateNotificationService(this@LoadingActivity)
                }
            }
        }
    }

    private fun checkNotification() {
        val notificationId = intent.getIntExtra(NotificationHelper.NOTIFICATION_ID_KEY, -1)
        lastNotificationId = notificationId
        if (notificationId != -1) {
            //            runCatching {
            //                NotificationManagerCompat.from(this).cancel(notificationId)
            //            }
            NotificationHelper.clickNotiEvent(notificationId)
        }
    }
}