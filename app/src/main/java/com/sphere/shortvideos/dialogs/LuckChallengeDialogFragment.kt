package com.sphere.shortvideos.dialogs

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.Gravity
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
    var onResult: ((Double) -> Unit)? = null

    private var _binding: DialogLuckChallengeBinding? = null
    private val binding get() = _binding!!

    private val rates = listOf(3, 4, 5, 6, 7, 8)
    private val normalBg = listOf(
        R.drawable.shape_rate_purple1,
        R.drawable.shape_rate_purple2,
        R.drawable.shape_rate_blue,
        R.drawable.shape_rate_green,
        R.drawable.shape_rate_orange,
        R.drawable.shape_rate_red
    )
    private val selectedBg = listOf(
        R.drawable.shape_rate_purple1_selected,
        R.drawable.shape_rate_purple2_selected,
        R.drawable.shape_rate_blue_selected,
        R.drawable.shape_rate_green_selected,
        R.drawable.shape_rate_orange_selected,
        R.drawable.shape_rate_red_selected
    )
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
        AnimViewHelper.playWelcomeBonusAnim(binding.ivAnim, binding.ivRewardBox)
        AnimViewHelper.slideInFromTop(binding.ivAnim2, 1200L)
        binding.ivArrow.visibility = View.INVISIBLE
        val reward = MoneyCacheHelper.fetchRvAdReward()
        val money = WithdrawAmountHelper.fetchCurMoneyAndGetMoneyMinValue()
        val progressText = WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(money.first) +
                "/" +
                WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(money.second)
        binding.tvPro.text = progressText
        binding.progressView.progress = WithdrawAmountHelper.fetchGetMoneyProgress()
        binding.tvRewardValue.text = reward.second
        binding.btnClaim.setOnClickListener {
            localEvent("wheel_pop_c")
            onResult?.invoke(rate * reward.first)
            dismissAllowingStateLoss()
        }
        localEvent("wheel_pop")
        setupRateViews()
        startRateRoll()
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
        val totalSteps = rates.size * 12
        rollAnimator = ValueAnimator.ofInt(0, totalSteps).apply {
            duration = 2000L
            addUpdateListener { animator ->
                val index = (animator.animatedValue as Int) % rates.size
                highlightIndex(index)
            }
            doOnEnd {
                val finalIndex = Random.nextInt(rates.size)
                highlightIndex(finalIndex)
                moveArrowToIndex(finalIndex)
                binding.ivArrow.apply {
                    alpha = 0f
                    scaleX = 0.7f
                    scaleY = 0.7f
                    visibility = View.VISIBLE
                    animate().cancel()
                    animate().alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200L)
                        .start()
                }
                rate = rates[finalIndex]

            }
            start()
        }
    }

    private fun highlightIndex(index: Int) {
        rateViews.forEachIndexed { i, view ->
            val selected = i == index
            view.background = requireContext().getDrawable(
                if (selected) selectedBg[i] else normalBg[i]
            )
        }
    }

    private fun moveArrowToIndex(index: Int) {
        binding.ivAnimParent.post {
            val parentCenter = binding.ivAnimParent.width / 2f
            val child = rateViews.getOrNull(index) ?: return@post
            val childCenter = child.left + child.width / 2f
            binding.ivArrow.translationX = childCenter - parentCenter
        }
    }

    private fun formatRate(rate: Int): String {
        return rate.toString()
    }

    private fun FrameLayoutParams(matchParent: Boolean): FrameLayout.LayoutParams {
        return FrameLayout.LayoutParams(
            if (matchParent) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
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
        rateViews.clear()
        super.onDestroyView()
        _binding = null
    }
}
