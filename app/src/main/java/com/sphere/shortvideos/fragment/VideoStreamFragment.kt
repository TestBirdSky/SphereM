package com.sphere.shortvideos.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bytedance.sdk.shortplay.api.ShortPlay
import com.sphere.shortvideos.R
import com.sphere.shortvideos.baseui.GenericFragment
import com.sphere.shortvideos.databinding.FragmentVideoStreamBinding
import com.sphere.shortvideos.vm.StreamViewModel

class VideoStreamFragment : GenericFragment<FragmentVideoStreamBinding>() {

    private val viewModel by viewModels<StreamViewModel>()
    private var feedListAdapter: FeedListAdapter? = null
    private var initialized: Boolean = false

    override fun bindView(inflater: LayoutInflater, container: ViewGroup?) = run {
        FragmentVideoStreamBinding.inflate(inflater, container, false)
    }

    override fun initUI() {
        viewModel.onErrorLiveData.observe(this) {
            initialized = false
            binding.refreshLayout.isRefreshing = false
            binding.errorView.findViewById<TextView>(com.pssdk.publish_module.R.id.pssdk_error_tip)?.text = getString(R.string.network_abnormality)
            binding.errorView.isVisible = true
        }
        viewModel.refreshLiveData.observe(this) { result ->
            binding.refreshLayout.isRefreshing = false
            binding.errorView.isVisible = false
            if (result.isNotEmpty()) {
                feedListAdapter?.submitData(result)
            } else {
                initialized = false
            }
        }
        binding.errorView.setRetryClickListener {
            binding.errorView.isVisible = false
            autoRefresh()
        }
        binding.refreshLayout.setOnRefreshListener {
            viewModel.loadData()
        }
        initAdapter()
        autoRefresh()
    }

    private fun initAdapter() {
        feedListAdapter = FeedListAdapter(childFragmentManager, lifecycle)
        binding.viewPager.offscreenPageLimit = 5
        binding.viewPager.adapter = feedListAdapter
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {

            }
        })
    }

    private fun autoRefresh() {
        if (!initialized) {
            initialized = true
            binding.refreshLayout.isRefreshing = true
            viewModel.loadData()
        }
    }

    override fun onResume() {
        super.onResume()
        autoRefresh()
    }

    inner class FeedListAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {

        val datas = mutableListOf<Any>()

        override fun createFragment(position: Int): Fragment {
            val item = datas.getOrNull(position)
            return when (item) {
                is ShortPlay -> {
                    PangleVideoContainerFragment.newInstance(item)
                }

                else -> Fragment()
            }
        }

        override fun getItemCount(): Int = datas.size

        fun submitData(data: List<Any>) {
            datas.clear()
            datas.addAll(data)
            binding.viewPager.adapter = feedListAdapter
        }

        fun getItemData(position: Int) = datas.getOrNull(position)
    }

}