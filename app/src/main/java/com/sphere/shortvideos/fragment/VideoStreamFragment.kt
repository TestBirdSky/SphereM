package com.sphere.shortvideos.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bytedance.sdk.shortplay.api.ShortPlay
import com.sphere.shortvideos.baseui.GenericFragment
import com.sphere.shortvideos.databinding.FragmentVideoStreamBinding
import com.sphere.shortvideos.helper.localEvent
import com.sphere.shortvideos.vm.StreamViewModel
import kotlin.div
import kotlin.text.toInt

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
            binding.refreshLayout.isRefreshing =
                false //            binding.errorView.findViewById<TextView>(com.pssdk.publish_module.R.id.pssdk_error_tip)?.text = getString(R.string.network_abnormality)
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

    private var pageResumeAtMs: Long = 0L
    override fun onResume() {
        super.onResume()
        autoRefresh()
        pageResumeAtMs = System.currentTimeMillis()
        localEvent("foru_page")
    }

    override fun onPause() {
        super.onPause()
        val staySec = ((System.currentTimeMillis() - pageResumeAtMs) / 1000L).toInt().coerceAtLeast(0)
        if (staySec > 0) {
            localEvent("foru_page_stay", hashMapOf("stay_times" to staySec))
        }
    }

    fun pauseCurrentVideo() {
        // Fragment 可能已经从 Activity 分离，这时不能再通过 childFragmentManager 查找子 Fragment
        if (!isAdded || isDetached || view == null) return
        val currentIndex = binding.viewPager.currentItem
        val current = childFragmentManager.findFragmentByTag("f$currentIndex")
        (current as? PangleVideoContainerFragment)?.pausePlay()
    }

    fun resumeCurrentVideo() {
        // 防御性判断：只有在 Fragment 已经 attach 且 View 存在时，才访问 childFragmentManager
        if (!isAdded || isDetached || view == null) return
        val currentIndex = binding.viewPager.currentItem
        val current = childFragmentManager.findFragmentByTag("f$currentIndex")
        (current as? PangleVideoContainerFragment)?.resumePlay()
    }

    inner class FeedListAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragmentManager, lifecycle) {

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
            binding.viewPager.post {
                if (!this@VideoStreamFragment.isAdded) return@post
                try {
                    notifyDataSetChanged()
                } catch (e: Exception) {
                    (binding.viewPager.adapter as? FragmentStateAdapter)?.notifyDataSetChanged()
                }
                val lastIndex = (datas.size - 1).coerceAtLeast(0)
                val current = binding.viewPager.currentItem
                if (current > lastIndex) {
                    binding.viewPager.setCurrentItem(lastIndex, false)
                }
            }
        }

        fun getItemData(position: Int) = datas.getOrNull(position)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}