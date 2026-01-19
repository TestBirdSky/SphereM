package com.sphere.shortvideos.activity

import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.bytedance.sdk.shortplay.api.ShortPlay
import com.sphere.shortvideos.GlobalConstants
import com.sphere.shortvideos.R
import com.sphere.shortvideos.baseui.GenericBindActivity
import com.sphere.shortvideos.databinding.ActivityDramaHistoryBinding
import com.sphere.shortvideos.fragment.adapters.HistoryAdapter
import com.sphere.shortvideos.fromJson
import com.sphere.shortvideos.nextView
import com.sphere.shortvideos.vm.MainViewModel

class DramaHistoryActivity : GenericBindActivity<ActivityDramaHistoryBinding>() {

    private val viewModel by viewModels<MainViewModel>()
    private lateinit var mAdapter: HistoryAdapter

    override val binding by lazy { ActivityDramaHistoryBinding.inflate(layoutInflater) }

    override fun initUI() {
        binding.imageBack.setOnClickListener { onBackActioned() }
        initHistoryAdapter()
        viewModel.historyLiveData.observe(this) {
            if (it.isNullOrEmpty()) {
                binding.emptyView.textErrorMsg.text = getString(R.string.no_data)
                binding.emptyView.imageEmpty.setImageResource(R.drawable.ic_empty_view_second)
                binding.emptyView.root.isVisible = true
            } else {
                binding.emptyView.root.isVisible = false
                mAdapter.submitList(it)
            }
        }
        viewModel.collectHistoryRoom()
    }

    private fun initHistoryAdapter() {
        mAdapter = HistoryAdapter(activity) { item ->
            if (item.isPangle) {
                nextView<PangleDramaPlayActivity> {
                    putExtra(GlobalConstants.EXTRA_KEY_SHORT_PLAY, item.dataJson.fromJson<ShortPlay>())
                    putExtra(GlobalConstants.EXTRA_KEY_DRAMA_HISTORY, item)
                }
            } else {
                Unit
            }
        }
        binding.listHistory.itemAnimator = null
        binding.listHistory.adapter = mAdapter
    }

}