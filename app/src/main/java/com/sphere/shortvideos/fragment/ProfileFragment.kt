package com.sphere.shortvideos.fragment

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bytedance.sdk.shortplay.api.PSSDK
import com.bytedance.sdk.shortplay.api.ShortPlay
import com.sphere.shortvideos.GlobalConstants
import com.sphere.shortvideos.R
import com.sphere.shortvideos.activity.DramaCollectionActivity
import com.sphere.shortvideos.activity.DramaHistoryActivity
import com.sphere.shortvideos.activity.PangleDramaPlayActivity
import com.sphere.shortvideos.baseui.GenericFragment
import com.sphere.shortvideos.databinding.FragmentProfileBinding
import com.sphere.shortvideos.fragment.adapters.HistoryAdapter
import com.sphere.shortvideos.fromJson
import com.sphere.shortvideos.nextView
import com.sphere.shortvideos.showToast
import com.sphere.shortvideos.vm.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ProfileFragment : GenericFragment<FragmentProfileBinding>() {

    private lateinit var mAdapter: HistoryAdapter
    private val viewModel by activityViewModels<MainViewModel>()

    override fun bindView(inflater: LayoutInflater, container: ViewGroup?) = run {
        FragmentProfileBinding.inflate(inflater, container, false)
    }

    override fun initUI() {
        binding.btnClearCache.setOnClickListener {
            PSSDK.clearLocalCache()
            binding.textCacheSize.text = "0 B"
            requireContext().showToast(R.string.cache_cleared)
        }
        binding.btnHistory.setOnClickListener {
            requireContext().nextView<DramaHistoryActivity>()
        }
        binding.btnCollection.setOnClickListener {
            requireContext().nextView<DramaCollectionActivity>()
        }
        binding.btnPrivacy.setOnClickListener {
            CustomTabsIntent.Builder().build().launchUrl(requireContext(), GlobalConstants.PRIVACY_POLICY.toUri())
        }
        initHistoryAdapter()
        viewModel.historyLiveData.observe(this) {
            mAdapter.submitList(it.take(3))
        }
    }

    private fun initHistoryAdapter() {
        mAdapter = HistoryAdapter(requireContext()) { item ->
            if (item.isPangle) {
                requireContext().nextView<PangleDramaPlayActivity> {
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

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.Main) {
            binding.textCacheSize.text = "${Formatter.formatFileSize(requireContext(), getPSSdkCache())}"
        }
    }

    private fun getPSSdkCache(): Long = runCatching {
        val sdkDir = File(requireContext().cacheDir, "pssdk")
        if (!sdkDir.exists()) return 0L
        val fileList = sdkDir.walkTopDown().filter { it.isFile }
        val totalSize = fileList.sumOf { it.length() }
        return@runCatching totalSize
    }.getOrNull() ?: 0L


}