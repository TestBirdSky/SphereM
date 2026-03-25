package com.sphere.shortvideos.dialogs.withdraw

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.DialogFragment
import com.sphere.shortvideos.R
import com.sphere.shortvideos.databinding.DialogFlipCardBinding
import com.sphere.shortvideos.helper.localEvent

/**
 * 翻卡弹窗：
 * 1) 先执行 3 秒高亮筛选动画（用户可提前点击任一卡结束）
 * 2) 触发翻牌动画，且命中结果固定为 "Withdrawal Instantly"
 * 3) 展示黄色 Next 按钮并倒计时 5 秒，超时自动关闭
 */
class FlipCardDialogFragment : DialogFragment() {

    private var _binding: DialogFlipCardBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private val cards by lazy { listOf(binding.card1, binding.card2, binding.card3) }
    private val cardTexts by lazy { listOf(binding.tvCard1, binding.tvCard2, binding.tvCard3) }

    private var selectedIndex = 0
    private var cycleRunnable: Runnable? = null
    private var cycleTick = 0
    private var locked = false
    private var closed = false
    private var countdownRunnable: Runnable? = null
    private var countdownSec = 5

    var dismissEvent: () -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.TransparentMaterialDialog)
        isCancelable = true
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
                localEvent("withdrawal_ins", hashMapOf("index" to index))
                selectedIndex = index
                stopCycle()
                highlightSelected(selectedIndex)
                revealResult()
            }
        }
        binding.ivClose.setOnClickListener { closeDialog() }
        startCycle()
    }

    private fun startCycle() {
        cycleTick = 0
        val totalTicks = 3000 / 120
        cycleRunnable = object : Runnable {
            override fun run() {
                if (closed || locked || _binding == null) return
                selectedIndex = cycleTick % 3
                highlightSelected(selectedIndex)
                cycleTick++
                if (cycleTick >= totalTicks) {
                    locked = true
                    revealResult()
                } else {
                    handler.postDelayed(this, 120L)
                }
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
        flipCard(cards[selectedIndex], cardTexts[selectedIndex], values[selectedIndex]) {
            val restIndexes = (0..2).filter { it != selectedIndex }
            var restFinished = 0
            restIndexes.forEach { idx ->
                flipCard(cards[idx], cardTexts[idx], values[idx]) {
                    restFinished++
                    if (restFinished == restIndexes.size) {
                        showNextWithCountdown()
                    }
                }
            }
        }
    }

    private fun flipCard(card: View, text: View, targetText: String, end: () -> Unit) {
        val half = ObjectAnimator.ofFloat(card, View.ROTATION_Y, 0f, 90f).apply {
            duration = 130L
            interpolator = DecelerateInterpolator()
        }
        val half2 = ObjectAnimator.ofFloat(card, View.ROTATION_Y, -90f, 0f).apply {
            duration = 130L
            interpolator = DecelerateInterpolator()
        }
        half.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                card.setBackgroundResource(R.drawable.ic_open_card_bg)
                (text as? android.widget.TextView)?.text = targetText
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
            val toScale = if (i == index) 1.12f else 1f
            card.animate().scaleX(toScale).scaleY(toScale).setDuration(90L).start()
            card.alpha = if (i == index) 1f else 0.85f
        }
    }

    private fun showNextWithCountdown() {
        binding.btnNext.visibility = View.VISIBLE
        binding.ivClose.visibility = View.VISIBLE
        countdownSec = 5
        updateNextText()
        binding.btnNext.setOnClickListener { closeDialog() }
        startCountdown()
    }

    private fun startCountdown() {
        countdownRunnable = object : Runnable {
            override fun run() {
                if (closed || _binding == null) return
                countdownSec--
                if (countdownSec <= 0) {
                    closeDialog()
                    return
                }
                updateNextText()
                handler.postDelayed(this, 1000L)
            }
        }
        handler.postDelayed(countdownRunnable!!, 1000L)
    }

    private fun updateNextText() {
        binding.btnNext.text = getString(R.string.flip_card_next_countdown, countdownSec)
    }

    private fun closeDialog() {
        if (closed) return
        closed = true
        stopCycle()
        countdownRunnable?.let { handler.removeCallbacks(it) }
        countdownRunnable = null
        dismissEvent()
        dismissAllowingStateLoss()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onDestroyView() {
        closeDialog()
        super.onDestroyView()
        _binding = null
    }
}
