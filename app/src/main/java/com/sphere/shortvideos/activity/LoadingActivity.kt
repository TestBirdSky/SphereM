package com.sphere.shortvideos.activity

import androidx.activity.viewModels
import com.sphere.shortvideos.baseui.GenericBindActivity
import com.sphere.shortvideos.databinding.ActivityLoadingBinding
import com.sphere.shortvideos.helper.mmkv.MMKVRepository
import com.sphere.shortvideos.helper.session
import com.sphere.shortvideos.nextView
import com.sphere.shortvideos.vm.LoadingViewModel

class LoadingActivity : GenericBindActivity<ActivityLoadingBinding>() {

    private val viewModel by viewModels<LoadingViewModel>()

    override val binding by lazy { ActivityLoadingBinding.inflate(layoutInflater) }

    override fun initUI() {
        viewModel.umpCompletedLiveData.observe(this) {
            viewModel.waitAdLoading(this)
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
        session()
    }

    override fun onBackActioned() = Unit

}