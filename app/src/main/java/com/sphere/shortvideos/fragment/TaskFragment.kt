package com.sphere.shortvideos.fragment

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.sphere.shortvideos.activity.MainActivity
import com.sphere.shortvideos.baseui.GenericFragment
import com.sphere.shortvideos.baseui.setTaskInfo
import com.sphere.shortvideos.databinding.FragmentTaskBinding
import com.sphere.shortvideos.helper.HelperRewardShow
import com.sphere.shortvideos.helper.ad.AdUtils
import com.sphere.shortvideos.helper.task.TaskHelper
import com.sphere.shortvideos.view.AnimViewHelper
import com.sphere.shortvideos.vm.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Date：2026/1/21
 * Describe:
 */
class TaskFragment : GenericFragment<FragmentTaskBinding>() {
    private var curPopMoney = 0.0
    private var isInit = true
    private val viewModel by activityViewModels<MainViewModel>()
    private var popAnimator1: ObjectAnimator? = null
    private var popAnimator2: ObjectAnimator? = null

    override fun bindView(inflater: LayoutInflater, container: ViewGroup?): FragmentTaskBinding {
        return FragmentTaskBinding.inflate(inflater, container, false)
    }

    override fun initUI() {
        binding.tvWithdraw.setOnClickListener {
            (activity as? MainActivity)?.jumpWallet()
        }
        HelperRewardShow.moneyAnimLiveData.observe(viewLifecycleOwner) { moneyText ->
            binding.tvMoney.text = moneyText
        }
        binding.layoutPop1.setOnClickListener {
            if (curPopMoney > 0) {
                viewModel.addMoneyNotExChange(curPopMoney)
                AnimViewHelper.flyToTarget(binding.layoutPop1, binding.iv1, end = {
                    lifecycleScope.launch {
                        delay(Random.nextLong(3000, 8000))
                        binding.layoutPop1.visibility = View.VISIBLE
                    }
                })
                showPopAd()
            }
        }

        binding.layoutPop2.setOnClickListener {
            if (curPopMoney > 0) {
                viewModel.addMoneyNotExChange(curPopMoney)
                AnimViewHelper.flyToTarget(binding.layoutPop2, binding.iv1, end = {
                    lifecycleScope.launch {
                        delay(Random.nextLong(6000, 16000))
                        binding.layoutPop2.visibility = View.VISIBLE
                    }
                })
                showPopAd()
            }
        }
    }

    private fun showPopAd() {
        (activity as? MainActivity)?.let {
            AdUtils.showRateAd(it)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isInit) {
            isInit = false
            setupWatchCoins()
        }
        refreshMoney()
        binding.tvMoney.text = viewModel.fetchCurMoney()
        startPopFloatAnim()
    }

    private fun refreshMoney() {
        val bean = TaskHelper.fetchTaskPopReward()
        curPopMoney = bean.first
        val mo = bean.second
        binding.tvPopReward1.text = mo
        binding.tvPopReward2.text = mo
    }

    private fun setupWatchCoins() {
        (activity as? MainActivity)?.let {
            binding.layoutTask.setTaskInfo(it, receiverMoneyEvent = { reward, sourceView ->
                AnimViewHelper.playCoinFlyAnim(sourceView, binding.iv1)
            })
        }
    }

    override fun onDestroyView() {
        stopPopFloatAnim()
        super.onDestroyView()
    }

    override fun onPause() {
        stopPopFloatAnim()
        super.onPause()
    }

    private fun startPopFloatAnim() {
        val offset = dpToPx(30f)
        if (popAnimator1 == null) {
            popAnimator1 = ObjectAnimator.ofFloat(binding.layoutPop1, View.TRANSLATION_Y, 0f, offset, 0f).apply {
                duration = 2500L
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.RESTART
                interpolator = AccelerateDecelerateInterpolator()
            }
        }
        if (popAnimator2 == null) {
            popAnimator2 = ObjectAnimator.ofFloat(binding.layoutPop2, View.TRANSLATION_Y, 0f, -offset, 0f).apply {
                duration = 3000L
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.RESTART
                interpolator = AccelerateDecelerateInterpolator()
            }
        }
        popAnimator1?.start()
        popAnimator2?.start()
    }

    private fun stopPopFloatAnim() {
        popAnimator1?.cancel()
        popAnimator2?.cancel()
        popAnimator1 = null
        popAnimator2 = null
        binding.layoutPop1.translationY = 0f
        binding.layoutPop2.translationY = 0f
    }

    private fun dpToPx(value: Float): Float {
        return value * resources.displayMetrics.density
    }

}
