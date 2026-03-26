package com.sphere.shortvideos.dialogs.withdraw

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.sphere.shortvideos.R
import com.sphere.shortvideos.activity.MainActivity
import com.sphere.shortvideos.baseui.GenericActivity
import com.sphere.shortvideos.databinding.DialogWithdrawApplyTransitionBinding
import com.sphere.shortvideos.helper.localEvent
import com.sphere.shortvideos.helper.ad.AdUtils
import com.sphere.shortvideos.helper.withdraw.WithdrawalActionHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class WithdrawApplyTransitionDialogFragment : DialogFragment() {

    private var _binding: DialogWithdrawApplyTransitionBinding? = null
    private val binding get() = _binding!!

    private var flowJob: Job? = null
    private var revealControlsJob: Job? = null
    private var dotsAnimatorSet: AnimatorSet? = null
    private var progressAnimator: ValueAnimator? = null

    private var hasMovedToStage2 = false
    private var isRvRequesting = false

    var dismissEvent: () -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.TransparentMaterialDialog)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogWithdrawApplyTransitionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        localEvent("withdrawal_progress")
        setupStaticUi()
        enterStage1AndStartFlow()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            window.setBackgroundDrawableResource(R.color.color_dialog)
        }
        dialog?.setCanceledOnTouchOutside(false)
    }

    private fun setupStaticUi() = with(binding) {
        tvSkipNow.setOnClickListener {
            localEvent("withdrawal_progress_c")
            WithdrawalActionHelper.taskFinish()
            showRewardVideoAndFastForward()
        }
        ivClose.setOnClickListener {
            WithdrawalActionHelper.taskFinish()
            dismissEvent()
            dismissAllowingStateLoss()
        }
        btnWatchMore.setOnClickListener {
            WithdrawalActionHelper.taskFinish()
            jumpToForYou()
        }
    }

    private fun jumpToForYou() {
        val act = activity
        if (act is MainActivity) {
            act.jumpToVideoTab()
            dismissAllowingStateLoss()
            return
        }
        act?.startActivity(Intent(act, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        })
        dismissAllowingStateLoss()
    }

    private fun enterStage1AndStartFlow() = with(binding) {
        hasMovedToStage2 = false
        isRvRequesting = false
        progressApply.max = 100
        progressApply.progress = 0
        progressApply.isVisible = true
        tvProcessingDesc.isVisible = true
        groupStage1.isVisible = true
        groupStage2.isVisible = false
        groupStage3.isVisible = false
        tvSkipNow.isVisible = false
        tvSkipAdTag.isVisible = false
        ivClose.isVisible = false
        startDotsPulseAnim()

        revealControlsJob?.cancel()
        revealControlsJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(CONTROLS_REVEAL_DELAY_MS)
            if (_binding == null || hasMovedToStage2) return@launch
            tvSkipNow.isVisible = true
            tvSkipAdTag.isVisible = true
            // 第一步、第二步（进度未到第三段）不显示关闭按钮，仅第三段及之后显示
            syncStage1CloseVisibility()
        }

        flowJob?.cancel()
        flowJob = viewLifecycleOwner.lifecycleScope.launch {
            runStage1ProgressRhythm()
            if (_binding == null || hasMovedToStage2) return@launch
            moveToStage2ThenStage3()
        }
    }

    private suspend fun runStage1ProgressRhythm() {
        val current = binding.progressApply.progress.coerceIn(0, 100)
        runStage1ProgressRhythmFrom(current)
    }

    private suspend fun runStage1ProgressRhythmFrom(fromProgress: Int) {
        var current = fromProgress.coerceIn(0, 100)
        syncStage1CloseVisibility()
        for (step in STAGE1_STEPS) {
            if (current >= step.end) continue

            val remainRatio = ((step.end - current).toFloat() / (step.end - step.start).toFloat()).coerceIn(0f, 1f)
            val remainDuration = (step.duration * remainRatio).toLong().coerceAtLeast(1L)
            animateProgressTo(step.end, remainDuration)
            current = step.end
            if (step.pauseAfter > 0) delay(step.pauseAfter)
        }
    }

    private suspend fun animateProgressTo(target: Int, duration: Long) {
        val pb = binding.progressApply
        progressAnimator?.cancel()
        suspendCancellableCoroutine { cont ->
            val animator = ValueAnimator.ofInt(pb.progress, target).apply {
                this.duration = duration
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener {
                    val p = it.animatedValue as Int
                    pb.progress = p
                    syncStage1CloseVisibility()
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (cont.isActive) cont.resume(Unit)
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        if (cont.isActive) cont.resume(Unit)
                    }
                })
            }
            progressAnimator = animator
            cont.invokeOnCancellation { animator.cancel() }
            animator.start()
        }
    }

    /** Stage1：前两段进度不显示关闭；从第三段起点（与 [STAGE1_STEPS] 第 3 步 start 一致）起显示。 */
    private fun syncStage1CloseVisibility() {
        if (_binding == null || hasMovedToStage2) return
        val p = binding.progressApply.progress
        binding.ivClose.isVisible = p >= STAGE1_STEP3_START_PROGRESS
    }

    private fun showRewardVideoAndFastForward() {
        if (isRvRequesting) return
        val activity = activity as? GenericActivity ?: return
        isRvRequesting = true // Skip 触发后先暂停自动进度，避免广告期间自行跳到后续状态。
        flowJob?.cancel()
        progressAnimator?.cancel()
        localEvent("ad_chance", params = hashMapOf("ad_pos_id" to RV_AD_POSITION_NAME))
        AdUtils.showRvAd(
            activity = activity,
            adPositionName = RV_AD_POSITION_NAME,
        ) { isRewardSuccess ->
            isRvRequesting = false
            if (_binding == null || hasMovedToStage2) return@showRvAd
            if (!isRewardSuccess) {
                resumeStage1FlowIfNeeded()
                return@showRvAd
            }
            viewLifecycleOwner.lifecycleScope.launch {
                moveToStage2ThenStage3()
            }
        }
    }

    private fun resumeStage1FlowIfNeeded() {
        if (_binding == null || hasMovedToStage2) return
        val current = binding.progressApply.progress.coerceIn(0, 100)
        flowJob?.cancel()
        flowJob = viewLifecycleOwner.lifecycleScope.launch {
            runStage1ProgressRhythmFrom(current)
            if (_binding == null || hasMovedToStage2) return@launch
            moveToStage2ThenStage3()
        }
    }

    private suspend fun moveToStage2ThenStage3() = with(binding) {
        if (hasMovedToStage2) return
        hasMovedToStage2 = true
        revealControlsJob?.cancel()
        progressAnimator?.cancel()
        progressApply.progress = 100
        stopDotsPulseAnim()

        groupStage1.isVisible = false
        groupStage2.isVisible = true
        groupStage3.isVisible = false
        progressApply.isVisible = true
        tvSkipNow.isVisible = false
        tvSkipAdTag.isVisible = false
        ivClose.isVisible = false

        playStage2EnterAnim()
        delay(STAGE2_REMAIN_MS_AFTER_ENTER)
        localEvent("queue_c")
        if (_binding == null) return

        groupStage1.isVisible = false
        groupStage2.isVisible = false
        groupStage3.isVisible = true
        progressApply.isVisible = false
        ivClose.isVisible = true
    }

    private fun startDotsPulseAnim() = with(binding) {
        stopDotsPulseAnim()
        dotsAnimatorSet = AnimatorSet().apply {
            val d1 = createDotAnimator(viewDot1, 0)
            val d2 = createDotAnimator(viewDot2, 180)
            val d3 = createDotAnimator(viewDot3, 360)
            val d4 = createDotAnimator(viewDot4, 540)
            playTogether(d1, d2, d3, d4)
            start()
        }
    }

    private fun createDotAnimator(dot: View, delay: Long): ObjectAnimator {
        return ObjectAnimator.ofFloat(dot, View.TRANSLATION_Y, 0f, -18f, 0f).apply {
            duration = 650
            startDelay = delay
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
    }

    private fun playStage2EnterAnim() = with(binding) {
        ivStage2.alpha = 0f
        ivStage2.scaleX = 0.78f
        ivStage2.scaleY = 0.78f
        tvStage2Desc.alpha = 0f

        val iconAlpha = ObjectAnimator.ofFloat(ivStage2, View.ALPHA, 0f, 1f).apply {
            duration = STAGE2_ENTER_ANIM_MS
        }
        val iconScaleX = ObjectAnimator.ofFloat(ivStage2, View.SCALE_X, 0.78f, 1f).apply {
            duration = STAGE2_ENTER_ANIM_MS
            interpolator = AccelerateDecelerateInterpolator()
        }
        val iconScaleY = ObjectAnimator.ofFloat(ivStage2, View.SCALE_Y, 0.78f, 1f).apply {
            duration = STAGE2_ENTER_ANIM_MS
            interpolator = AccelerateDecelerateInterpolator()
        }
        val textAlpha = ObjectAnimator.ofFloat(tvStage2Desc, View.ALPHA, 0f, 1f).apply {
            duration = STAGE2_ENTER_ANIM_MS
        }
        AnimatorSet().apply {
            playTogether(iconAlpha, iconScaleX, iconScaleY, textAlpha)
            start()
        }
    }

    private fun stopDotsPulseAnim() {
        dotsAnimatorSet?.cancel()
        dotsAnimatorSet = null
    }

    override fun onDestroyView() {
        progressAnimator?.cancel()
        progressAnimator = null
        stopDotsPulseAnim()
        flowJob?.cancel()
        flowJob = null
        revealControlsJob?.cancel()
        revealControlsJob = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private data class StageStep(
            val start: Int,
            val end: Int,
            val duration: Long,
            val pauseAfter: Long,
        )

        // 规则：前 13 秒进度最多到 40%，之后再走到 100%
        private val STAGE1_STEPS = listOf(
            StageStep(start = 0, end = 40, duration = 13_000L, pauseAfter = 0L),
            StageStep(start = 40, end = 72, duration = 2600L, pauseAfter = 700L),
            StageStep(start = 72, end = 92, duration = 2000L, pauseAfter = 600L),
            StageStep(start = 92, end = 100, duration = 1600L, pauseAfter = 0L),
        )

        /** 第三段进度起始 = 第一步+第二步结束后；此前不展示关闭按钮 */
        private const val STAGE1_STEP3_START_PROGRESS = 72

        private const val CONTROLS_REVEAL_DELAY_MS = 3000L
        private const val STAGE2_ENTER_ANIM_MS = 320L
        private const val STAGE2_REMAIN_MS_AFTER_ENTER = 1500L
        private const val RV_AD_POSITION_NAME = "dlmsf_withdraw_skip_rv"
    }
}
