package com.sphere.shortvideos.fragment

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.fragment.app.activityViewModels
import com.sphere.shortvideos.activity.MainActivity
import com.sphere.shortvideos.baseui.GenericFragment
import com.sphere.shortvideos.baseui.setTaskInfo
import com.sphere.shortvideos.databinding.FragmentTaskBinding
import com.sphere.shortvideos.dialogs.LuckChallengeDialogFragment
import com.sphere.shortvideos.helper.MoneyCacheHelper
import com.sphere.shortvideos.helper.WithdrawAmountHelper
import com.sphere.shortvideos.helper.task.TaskHelper
import com.sphere.shortvideos.vm.MainViewModel

/**
 * Date：2026/1/21
 * Describe:
 */
class TaskFragment : GenericFragment<FragmentTaskBinding>() {
    private var isInit = true
    private val viewModel by activityViewModels<MainViewModel>()
    private var moneyAnimator: ValueAnimator? = null
    private var popAnimator1: ObjectAnimator? = null
    private var popAnimator2: ObjectAnimator? = null

    override fun bindView(inflater: LayoutInflater, container: ViewGroup?): FragmentTaskBinding {
        return FragmentTaskBinding.inflate(inflater, container, false)
    }

    override fun initUI() {
        binding.tvWithdraw.setOnClickListener {
            (activity as? MainActivity)?.jumpWallet()
        }
        binding.layoutPop1.setOnClickListener {
        }
        binding.layoutPop2.setOnClickListener {

        }
    }

    override fun onResume() {
        super.onResume()
        if (isInit) {
            isInit = false
            setupWatchCoins()
        }
        val mo = TaskHelper.fetchTaskPopReward().second
        binding.tvPopReward1.text = mo
        binding.tvPopReward2.text = mo
        binding.tvMoney.text = viewModel.fetchCurMoney()
        startPopFloatAnim()
    }

    private fun setupWatchCoins() {
        activity?.let {
            binding.layoutTask.setTaskInfo(it, daySignSuccess = { reward, sourceView ->
                onRewardReceived(reward, sourceView)
            }, watchTimeArriver = { reward, sourceView ->
                onRewardReceived(reward, sourceView)
            })
        }
    }

    private fun onRewardReceived(reward: Double, sourceView: ImageView) {
        if (reward <= 0) {
            binding.tvMoney.text = viewModel.fetchCurMoney()
            return
        }
        playCoinFlyAnim(sourceView, {
            playMoneyIncreaseAnim(reward)
        })
    }

    private fun playMoneyIncreaseAnim(reward: Double) {
        moneyAnimator?.cancel()
        val endValue = MoneyCacheHelper.fetchCurMoney()
        val startValue = (endValue - reward).coerceAtLeast(0.0)
        moneyAnimator = ValueAnimator.ofFloat(startValue.toFloat(), endValue.toFloat()).apply {
            duration = 600L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val value = (animator.animatedValue as Float).toDouble()
                binding.tvMoney.text = WithdrawAmountHelper.moneyFormatAddUnit(value)
            }
            start()
        }
    }

    private fun playCoinFlyAnim(sourceView: ImageView, end: () -> Unit) {
        val animView = binding.ivAnim
        animView.setImageDrawable(sourceView.drawable)
        animView.post {
            val rootLocation = IntArray(2)
            val startLocation = IntArray(2)
            val endLocation = IntArray(2)
            binding.root.getLocationInWindow(rootLocation)
            sourceView.getLocationInWindow(startLocation)
            binding.iv1.getLocationInWindow(endLocation)

            val startX = startLocation[0] - rootLocation[0] + sourceView.width / 2f - animView.width / 2f
            val startY = startLocation[1] - rootLocation[1] + sourceView.height / 2f - animView.height / 2f
            val endX = endLocation[0] - rootLocation[0] + binding.iv1.width / 2f - animView.width / 2f
            val endY = endLocation[1] - rootLocation[1] + binding.iv1.height / 2f - animView.height / 2f

            animView.apply {
                visibility = View.VISIBLE
                alpha = 1f
                scaleX = 1f
                scaleY = 1f
                x = startX
                y = startY
                animate().cancel()
                animate().x(endX).y(endY).alpha(0.2f).setDuration(600L).withEndAction {
                    visibility = View.GONE
                    alpha = 1f
                    end.invoke()
                }.start()
            }
        }
    }

    override fun onDestroyView() {
        moneyAnimator?.cancel()
        moneyAnimator = null
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
