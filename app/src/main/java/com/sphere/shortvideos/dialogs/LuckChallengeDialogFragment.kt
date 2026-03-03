package com.sphere.shortvideos.dialogs

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.Gravity
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.fragment.app.DialogFragment
import com.sphere.shortvideos.R
import com.sphere.shortvideos.databinding.DialogLuckChallengeBinding
import com.sphere.shortvideos.helper.MoneyCacheHelper
import com.sphere.shortvideos.helper.SoundHelper
import com.sphere.shortvideos.helper.WithdrawAmountHelper
import com.sphere.shortvideos.helper.localEvent
import com.sphere.shortvideos.view.AnimViewHelper
import kotlin.random.Random

/**
 * Date：2026/1/27
 * Describe: Luck challenge dialog
 */
class LuckChallengeDialogFragment : DialogFragment() {

    private var rate = 3

    /** 激励广告基础奖励金额（倍率加载完后用于公式展示） */
    private var baseRewardAmount = 0.0
    var onResult: ((Double) -> Unit)? = null

    private var _binding: DialogLuckChallengeBinding? = null
    private val binding get() = _binding!!

    private val rates = listOf(3, 4, 5, 6, 7, 8)
    private val normalBg = listOf(R.drawable.shape_rate_purple1,
        R.drawable.shape_rate_purple2,
        R.drawable.shape_rate_blue,
        R.drawable.shape_rate_green,
        R.drawable.shape_rate_orange,
        R.drawable.shape_rate_red)
    private val selectedBg = listOf(R.drawable.shape_rate_purple1_selected,
        R.drawable.shape_rate_purple2_selected,
        R.drawable.shape_rate_blue_selected,
        R.drawable.shape_rate_green_selected,
        R.drawable.shape_rate_orange_selected,
        R.drawable.shape_rate_red_selected)
    private val rateViews = mutableListOf<TextView>()
    private var rollAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.TransparentMaterialDialog)
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogLuckChallengeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SoundHelper.playDialogAppear(requireContext()) // 弹窗出现音效
        AnimViewHelper.playWelcomeBonusAnim(binding.ivAnim, binding.ivRewardBox, {
            startRateRoll()
        })
        AnimViewHelper.playCelebrateAnim(binding.ivAnim2, 1000L)
        val reward = MoneyCacheHelper.fetchRvAdReward()
        baseRewardAmount = reward.first
        val money = WithdrawAmountHelper.fetchCurMoneyAndGetMoneyMinValue()
        val progressText =
            WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(money.first) + "/" + WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(
                money.second)
        binding.tvPro.text = progressText
        binding.progressView.progress =
            WithdrawAmountHelper.fetchGetMoneyProgress() // 初始显示基础奖励，倍率加载完后改为公式：R$1,5 ✖️6 = R$xx
        binding.tvRewardValue.text = reward.second
        // 转盘倍率结束后才显示按钮
        binding.btnClaim.visibility = View.GONE
        binding.btnClaim.alpha = 0f
        binding.btnClaim.scaleX = 0.8f
        binding.btnClaim.scaleY = 0.8f
        AnimViewHelper.applyPressBounceEffect(binding.btnClaim)
        binding.btnClaim.setOnClickListener {
            localEvent("wheel_pop_c")
            onResult?.invoke(rate * reward.first)
            dismissAllowingStateLoss()
        }
        localEvent("wheel_pop")
        setupRateViews()
    }

    private fun setupRateViews() {
        val container = LinearLayout(requireContext()).apply {
            layoutParams = FrameLayoutParams(matchParent = true)
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        rateViews.clear()
        rates.forEachIndexed { index, rate ->
            val tv = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(40f).toInt(), dpToPx(24f).toInt()).apply {
                    if (index != rates.lastIndex) {
                        marginEnd = dpToPx(3f).toInt()
                    }
                }
                gravity = Gravity.CENTER
                text = "X${formatRate(rate)}"
                setTextColor(requireContext().getColor(R.color.white))
                textSize = 12f
                background = requireContext().getDrawable(normalBg[index])
            }
            rateViews.add(tv)
            container.addView(tv)
        }
        binding.ivAnimParent.removeAllViews()
        binding.ivAnimParent.addView(container)
    }

    private fun startRateRoll() {
        rollAnimator?.cancel()
        val currentBinding = _binding ?: return
        SoundHelper.playDoubleRoll(requireContext()) // 翻倍滚动音效
        currentBinding.ivArrow.apply {
            visibility = View.VISIBLE
            alpha = 0.6f
            scaleX = 1f
            scaleY = 1f
        }
        moveArrowToIndex(0)
        val totalSteps = rates.size * 12
        rollAnimator = ValueAnimator.ofInt(0, totalSteps).apply {
            duration = 2800L // 整体放慢，开始不会太冲
            interpolator = AccelerateDecelerateInterpolator() // 开始慢、中间快、结束慢，更流畅
            addUpdateListener { animator ->
                val binding = _binding ?: return@addUpdateListener
                val index = (animator.animatedValue as Int) % rates.size
                highlightIndex(index)
                moveArrowToIndex(index)
            }
            doOnEnd {
                val binding = _binding ?: return@doOnEnd
                val finalIndex = Random.nextInt(rates.size)
                highlightIndex(finalIndex)
                rate = rates[finalIndex]
                val totalAmount = rate * baseRewardAmount
                val str = "${WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(baseRewardAmount)} X$rate = ${
                    WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(totalAmount)
                }"
                binding.tvRewardValue.text = str
                // 箭头平滑滑到最终位置，避免突然定住
                smoothMoveArrowToIndex(finalIndex) {
                    val binding = _binding ?: return@smoothMoveArrowToIndex
                    binding.ivArrow.animate().alpha(1f).setDuration(150L).start()
                    showClaimButtonWithAnim()
                }
            }
            start()
        }
    }

    /** 箭头平滑移动到指定倍率位置，结束后回调 */
    private fun smoothMoveArrowToIndex(index: Int, onEnd: () -> Unit) {
        val child = rateViews.getOrNull(index) ?: run {
            moveArrowToIndex(index)
            onEnd()
            return
        }
        val currentBinding = _binding ?: run {
            onEnd()
            return
        }
        currentBinding.ivAnimParent.post {
            val binding = _binding ?: run {
                onEnd()
                return@post
            }
            val container = binding.ivAnimParent.getChildAt(0) ?: run {
                moveArrowToIndex(index)
                onEnd()
                return@post
            }
            val parentCenter = binding.ivAnimParent.width / 2f
            val targetTx = container.left + child.left + child.width / 2f - parentCenter
            val startTx = binding.ivArrow.translationX
            ObjectAnimator.ofFloat(binding.ivArrow, View.TRANSLATION_X, startTx, targetTx).apply {
                duration = 320L
                interpolator = DecelerateInterpolator(2f)
                doOnEnd { onEnd() }
                start()
            }
        }
    }

    /** 转盘结束后显示领取按钮：淡入 + 缩放弹入 */
    private fun showClaimButtonWithAnim() {
        val binding = _binding ?: return
        binding.btnClaim.visibility = View.VISIBLE
        binding.btnClaim.alpha = 0f
        binding.btnClaim.scaleX = 0.8f
        binding.btnClaim.scaleY = 0.8f
        val alphaAnim = ObjectAnimator.ofFloat(binding.btnClaim, View.ALPHA, 0f, 1f).apply {
            duration = 280L
        }
        val scaleXAnim = ObjectAnimator.ofFloat(binding.btnClaim, View.SCALE_X, 0.8f, 1f).apply {
            duration = 350L
            interpolator = OvershootInterpolator(1.4f)
        }
        val scaleYAnim = ObjectAnimator.ofFloat(binding.btnClaim, View.SCALE_Y, 0.8f, 1f).apply {
            duration = 350L
            interpolator = OvershootInterpolator(1.4f)
        }
        AnimatorSet().apply {
            playTogether(alphaAnim, scaleXAnim, scaleYAnim)
            start()
        }
    }

    private fun highlightIndex(index: Int) {
        if (_binding == null) return
        rateViews.forEachIndexed { i, view ->
            val selected = i == index
            view.background = requireContext().getDrawable(if (selected) selectedBg[i] else normalBg[i])
        }
    }

    private fun moveArrowToIndex(index: Int) {
        val child = rateViews.getOrNull(index) ?: return
        val currentBinding = _binding ?: return
        currentBinding.ivAnimParent.post {
            val binding = _binding ?: return@post
            val container = binding.ivAnimParent.getChildAt(0) ?: return@post
            val parentCenter = binding.ivAnimParent.width / 2f
            val childCenter = container.left + child.left + child.width / 2f
            binding.ivArrow.translationX = childCenter - parentCenter
        }
    }

    private fun formatRate(rate: Int): String {
        return rate.toString()
    }

    private fun FrameLayoutParams(matchParent: Boolean): FrameLayout.LayoutParams {
        return FrameLayout.LayoutParams(if (matchParent) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.MATCH_PARENT)
    }

    private fun dpToPx(value: Float): Float {
        return value * resources.displayMetrics.density
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
        dialog?.setCanceledOnTouchOutside(false)
    }

    override fun onDestroyView() {
        rollAnimator?.cancel()
        rollAnimator = null
        // 清除所有 Handler 回调，防止在 View 销毁后执行
        _binding?.ivAnimParent?.removeCallbacks(null)
        rateViews.clear()
        super.onDestroyView()
        _binding = null
    }
}
