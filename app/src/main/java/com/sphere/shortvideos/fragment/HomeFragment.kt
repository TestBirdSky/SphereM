package com.sphere.shortvideos.fragment

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bytedance.sdk.shortplay.api.PSSDK
import com.bytedance.sdk.shortplay.api.ShortPlay.ShortPlayCategory
import com.google.android.material.tabs.TabLayoutMediator
import com.sphere.shortvideos.R
import com.sphere.shortvideos.baseui.GenericFragment
import com.sphere.shortvideos.baseui.refreshView
import com.sphere.shortvideos.databinding.FragmentHomeBinding
import com.sphere.shortvideos.vm.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.getValue

class HomeFragment : GenericFragment<FragmentHomeBinding>() {

    private lateinit var viewPagerAdapter: FragmentStateAdapter
    private val viewModel by activityViewModels<MainViewModel>()

    private val categories by lazy {
        mutableListOf<ShortPlayCategory>().apply {
            add(ShortPlayCategory(-2, getString(R.string.category_hot)))
            add(ShortPlayCategory(-1, getString(R.string.category_new)))
        }
    }
    private var isInitCategory: Boolean = false

    override fun bindView(inflater: LayoutInflater, container: ViewGroup?) =
        run { FragmentHomeBinding.inflate(inflater, container, false) }

    override fun initUI() {
        viewPagerAdapter = object : FragmentStateAdapter(childFragmentManager, lifecycle) {
            override fun createFragment(position: Int): Fragment =
                HomeDramaListFragment.newInstance(categories[position].id)

            override fun getItemCount(): Int = categories.size
        }
        binding.viewPager.offscreenPageLimit = categories.size
        binding.viewPager.adapter = viewPagerAdapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = categories.getOrNull(position)?.name ?: ""
        }.attach()
        registerViewModel()
        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadData() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (isInitCategory) return@launch
            isInitCategory = true
            val result = requestCategoryList()
            if (result.isNullOrEmpty()) {
                isInitCategory = false
            } else {
                isInitCategory = true
                val data = result.filter { it.count > 0 && it.name.isNullOrBlank().not() }
                categories.addAll(data)
                withContext(Dispatchers.Main) {
                    binding.viewPager.offscreenPageLimit = categories.size
                    viewPagerAdapter.notifyItemRangeInserted(2, data.size)
                }
            }
        }
    }

    private suspend fun requestCategoryList(): List<ShortPlayCategory>? = suspendCancellableCoroutine { continuation ->
        PSSDK.requestCategoryList(PSSDK.getContentLanguages()?.getOrNull(0) ?: "",
            object : PSSDK.CategoryListResultListener {
                override fun onFail(e: PSSDK.ErrorInfo?) = continuation.resume(null)
                override fun onSuccess(result: PSSDK.FeedListLoadResult<ShortPlayCategory>?) {
                    continuation.resume(result?.dataList ?: listOf())
                }
            })
    }

    private fun registerViewModel() {
        viewModel.curGetMoneyStr.observe(this) { pair ->
            activity?.let {
                binding.layoutMoney.refreshView(pair.first, pair.second, it as AppCompatActivity)
            }
        }
    }

}