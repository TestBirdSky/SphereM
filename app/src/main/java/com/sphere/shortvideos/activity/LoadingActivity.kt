package com.sphere.shortvideos.activity

import android.view.View
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.sphere.shortvideos.GlobalConstants
import com.sphere.shortvideos.R

import com.sphere.shortvideos.baseui.GenericBindActivity
import com.sphere.shortvideos.baseui.setColor
import com.sphere.shortvideos.databinding.ActivityLoadingBinding
import com.sphere.shortvideos.helper.MoneyCacheHelper
import com.sphere.shortvideos.helper.OtherHelper
import com.sphere.shortvideos.helper.mmkv.MMKVData
import com.sphere.shortvideos.helper.mmkv.MMKVRepository
import com.sphere.shortvideos.helper.permission.PermissionHelper
import com.sphere.shortvideos.helper.permission.PostPermission
import com.sphere.shortvideos.helper.session
import com.sphere.shortvideos.nextView
import com.sphere.shortvideos.vm.LoadingViewModel

class LoadingActivity : GenericBindActivity<ActivityLoadingBinding>() {
    private var isFirstGoLoadingPage by MMKVData(true)
    private val mPostPermission = PostPermission(this)
    private val viewModel by viewModels<LoadingViewModel>()

    override val binding by lazy {
        topMar = 0
        ActivityLoadingBinding.inflate(layoutInflater)
    }

    override fun initUI() {
        viewModel.umpCompletedLiveData.observe(this) {
            if (isFirstGoLoadingPage.not()) {
                viewModel.waitAdLoading(this)
            }
        }
        viewModel.nextLiveData.observe(this) {
            nextView<MainActivity>()
            finish()
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
                nextView<MainActivity>()
                finish()
            }
            binding.tvPp.setOnClickListener {
                CustomTabsIntent.Builder().build().launchUrl(this, GlobalConstants.PRIVACY_POLICY.toUri())
            }
        }
        session()
        MMKVRepository.checkCueDay()
        // todo test
        MoneyCacheHelper.addWatchVideoTime(60000 * 30)
        nextView<MainActivity>()
        finish()
    }

    override fun onBackActioned() = Unit

    override fun onDestroy() {
        super.onDestroy()
        OtherHelper.isNeedFetch = true
        isFirstGoLoadingPage = false
    }

    override fun onResume() {
        super.onResume()
        mPostPermission.requestPermission {}
    }
}