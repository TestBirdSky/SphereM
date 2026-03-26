package com.sphere.shortvideos.dialogs.withdraw

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.sphere.shortvideos.R
import com.sphere.shortvideos.databinding.DialogFlipCardBinding
import com.sphere.shortvideos.helper.localEvent

/**
 * 翻卡弹窗：
 * 1) 先执行 3 秒高亮筛选动画（用户可提前点击任一卡结束）
 * 2) 触发翻牌动画，且命中结果固定为 "Withdrawal Instantly"
 * 3) 展示黄色 Next 按钮（无倒计时）
 */
class FlipCardDialogFragment : DialogFragment() {

    private var _binding: DialogFlipCardBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    /** 外层槽位：承载选中框 + 缩放/3D 翻转 */
    private val cards by lazy { listOf(binding.card1, binding.card2, binding.card3) }
    /** 内层卡面：ic_card_bg / ic_open_card_bg 只切换这一层 */
    private val cardInners by lazy { listOf(binding.cardInner1, binding.cardInner2, binding.cardInner3) }
    private val cardTexts by lazy { listOf(binding.tvCard1, binding.tvCard2, binding.tvCard3) }
    private val selectedBorders by lazy { listOf(binding.ivSelected1, binding.ivSelected2, binding.ivSelected3) }

    private var selectedIndex = 0
    private var cycleRunnable: Runnable? = null
    private var cycleTick = 0
    private var fingerAnimator: ObjectAnimator? = null
    private var locked = false
    private var revealed = false
    private var closed = false

    var dismissEvent: () -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.TransparentMaterialDialog)
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogFlipCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        localEvent("withdrawal_card")
        cards.forEachIndexed { index, card ->
            card.setOnClickListener {
                if (locked) return@setOnClickListener
                locked = true
                revealed = true
                stopFingerAnim()
                binding.ivFingerAnim.visibility = View.GONE
                localEvent("withdrawal_ins", hashMapOf("index" to index))
                selectedIndex = index
                stopCycle()
                highlightSelected(selectedIndex)
                revealResult()
            }
        }
        binding.ivClose.setOnClickListener { closeDialog() }
        startCycle()
        startFingerAnim()
    }

    private fun startCycle() {
        cycleTick = 0
        cycleRunnable = object : Runnable {
            override fun run() {
                if (closed || locked || _binding == null) return
                selectedIndex = cycleTick % 3
                highlightSelected(selectedIndex)
                cycleTick++
                // 仅做引导高亮，不自动翻牌；必须用户点击才翻开。
                handler.postDelayed(this, CARD_CYCLE_INTERVAL_MS)
            }
        }
        handler.post(cycleRunnable!!)
    }

    private fun stopCycle() {
        cycleRunnable?.let { handler.removeCallbacks(it) }
        cycleRunnable = null
    }

    private fun revealResult() {
        // 保证结果一定为 Withdrawal Instantly：放在被选中的卡上
        val values = MutableList(3) { getString(R.string.flip_card_result_failed) }
        values[selectedIndex] = getString(R.string.flip_card_result_instantly)
        values[(selectedIndex + 1) % 3] = getString(R.string.flip_card_result_invite)
        values[(selectedIndex + 2) % 3] = getString(R.string.flip_card_result_failed)
        highlightSelected(selectedIndex)
        // 先翻选中的卡，翻完后再翻其余两张
        flipCard(cards[selectedIndex], cardInners[selectedIndex], cardTexts[selectedIndex], values[selectedIndex], true) {
            val restIndexes = (0..2).filter { it != selectedIndex }
            var restFinished = 0
            restIndexes.forEach { idx ->
                flipCard(cards[idx], cardInners[idx], cardTexts[idx], values[idx], false) {
                    restFinished++
                    if (restFinished == restIndexes.size) {
                        applyRevealedCardScale(selectedIndex)
                        showNext()
                    }
                }
            }
        }
    }

    private fun flipCard(
        outer: View,
        inner: View,
        text: View,
        targetText: String,
        isSelectedCard: Boolean,
        end: () -> Unit,
    ) {
        val half = ObjectAnimator.ofFloat(outer, View.ROTATION_Y, 0f, 90f).apply {
            duration = 130L
            interpolator = DecelerateInterpolator()
        }
        val half2 = ObjectAnimator.ofFloat(outer, View.ROTATION_Y, -90f, 0f).apply {
            duration = 130L
            interpolator = DecelerateInterpolator()
        }
        half.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                inner.setBackgroundResource(R.drawable.ic_open_card_bg)
                (text as? TextView)?.let { tv ->
                    tv.text = targetText
                    val size = if (isSelectedCard) FLIP_CARD_TEXT_SIZE_REVEALED_SP else FLIP_CARD_TEXT_SIZE_UNSELECTED_REVEALED_SP
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
                    applyResultTextStyle(tv, targetText)
                }
                half2.start()
            }
        })
        half2.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                end.invoke()
            }
        })
        half.start()
    }

    private fun highlightSelected(index: Int) {
        cards.forEachIndexed { i, card ->
            val toScale = if (revealed) {
                if (i == index) REVEALED_SELECTED_SCALE else REVEALED_UNSELECTED_SCALE
            } else {
                if (i == index) PRE_REVEAL_SELECTED_SCALE else PRE_REVEAL_UNSELECTED_SCALE
            }
            card.animate().scaleX(toScale).scaleY(toScale).setDuration(CARD_SCALE_ANIM_MS).start()
            card.alpha = if (i == index) 1f else 0.85f
        }
        selectedBorders.forEachIndexed { i, v ->
            v.visibility = if (i == index) View.VISIBLE else View.GONE
        }
    }

    private fun applyRevealedCardScale(selected: Int) {
        cards.forEachIndexed { i, card ->
            val toScale = if (i == selected) REVEALED_SELECTED_SCALE else REVEALED_UNSELECTED_SCALE
            card.animate().scaleX(toScale).scaleY(toScale).setDuration(CARD_SCALE_ANIM_MS).start()
        }
    }

    private fun showNext() {
        binding.btnNext.visibility = View.VISIBLE
        binding.ivClose.visibility = View.VISIBLE
        binding.btnNext.setOnClickListener { closeDialog() }
    }

    private fun applyResultTextStyle(tv: TextView, targetText: String) {
        val instantly = getString(R.string.flip_card_result_instantly)
        val invite = getString(R.string.flip_card_result_invite)
        val failed = getString(R.string.flip_card_result_failed)

        tv.paint.shader = null
        when (targetText) {
            instantly -> {
                tv.post {
                    if (_binding == null) return@post
                    val w = tv.width.toFloat().coerceAtLeast(1f)
                    tv.paint.shader = LinearGradient(
                        0f,
                        0f,
                        w,
                        0f,
                        Color.parseColor("#9411EC"),
                        Color.parseColor("#EA00FF"),
                        Shader.TileMode.CLAMP,
                    )
                    tv.invalidate()
                }
            }

            invite -> {
                tv.setTextColor(Color.parseColor("#975606"))
            }

            failed -> {
                tv.setTextColor(Color.parseColor("#7C7C7C"))
            }
        }
    }

    private fun closeDialog() {
        if (closed) return
        closed = true
        stopCycle()
        stopFingerAnim()
        dismissEvent()
        dismissAllowingStateLoss()
    }

    private fun startFingerAnim() {
        if (_binding == null || fingerAnimator != null) return
        binding.ivFingerAnim.visibility = View.VISIBLE
        binding.ivFingerAnim.scaleX = 0.8f
        binding.ivFingerAnim.scaleY = 0.8f
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.8f, 1.3f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.8f, 1.3f)
        fingerAnimator = ObjectAnimator.ofPropertyValuesHolder(binding.ivFingerAnim, scaleX, scaleY).apply {
            duration = 1000L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }
    }

    private fun stopFingerAnim() {
        fingerAnimator?.cancel()
        fingerAnimator = null
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            window.setBackgroundDrawableResource(R.color.color_dialog)
        }
        dialog?.setCanceledOnTouchOutside(false)
    }

    override fun onDestroyView() {
        closeDialog()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /** 翻牌后（正面结果文案）字号；背面在布局中为 14sp */
        private const val FLIP_CARD_TEXT_SIZE_REVEALED_SP = 13f
        /** 翻牌后未选中两张卡的字号 */
        private const val FLIP_CARD_TEXT_SIZE_UNSELECTED_REVEALED_SP = 10f
        private const val CARD_CYCLE_INTERVAL_MS = 220L
        private const val CARD_SCALE_ANIM_MS = 180L
        private const val PRE_REVEAL_SELECTED_SCALE = 1.10f
        private const val PRE_REVEAL_UNSELECTED_SCALE = 1f
        private const val REVEALED_SELECTED_SCALE = 1.10f
        private const val REVEALED_UNSELECTED_SCALE = 0.94f
    }
}
