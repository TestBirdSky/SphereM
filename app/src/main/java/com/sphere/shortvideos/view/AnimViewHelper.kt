package com.sphere.shortvideos.view

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.animation.AnimatorListenerAdapter
import android.graphics.drawable.ColorDrawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.os.Handler
import android.os.Looper
import com.sphere.shortvideos.R
import com.sphere.shortvideos.helper.SoundHelper
import kotlin.random.Random
import androidx.core.graphics.drawable.toDrawable

object AnimViewHelper {

    // ========== 常见 App 按钮按压样式（参考） ==========
    // 1. iOS/微信：按下仅透明度 0.6~0.7，松手恢复。代码少，复用高，无缩放。
    // 2. Material/抖音：按下 scale 0.96 + alpha 0.9，松手回弹。代码中，复用高，手感好。
    // 3. 淘宝/支付宝：按下 scale 0.95，松手快速回弹。代码少，复用高。
    // 4. 游戏类：按下 scale 0.9，松手明显回弹 1.05→1.0。代码中，复用高，反馈强。
    // 下面实现「按压透明度 + 松手回弹」，一种写法全局复用，任意 View 一行绑定。

    private data class PressBounceHolder(
        var pressAnim: Animator? = null,
        var releaseAnim: Animator? = null
    )

    private data class PressGrayOverlayHolder(
        val overlayDrawable: ColorDrawable,
        var animator: Animator? = null
    )

    private data class ShineAnimHolder(
        val animator: Animator?,
        val runnable: Runnable,
        val attachListener: View.OnAttachStateChangeListener
    )

    /**
     * 给任意 View 加上「按压变透明 + 松手回弹」的点击反馈，不拦截点击事件。
     * @param view 目标 View（Button、ImageView、布局等）
     * @param pressAlpha 按下时透明度，默认 0.85f
     * @param pressScale 按下时缩放，默认 0.96f
     * @param pressDurationMs 按下动画时长
     * @param releaseDurationMs 松手回弹动画时长（回弹由 Overshoot 实现）
     */
    @JvmOverloads
    fun applyPressBounceEffect(
        view: View,
        pressAlpha: Float = 0.85f,
        pressScale: Float = 0.98f,
        pressDurationMs: Long = 80L,
        releaseDurationMs: Long = 120L
    ) {
        val holder = (view.tag as? PressBounceHolder) ?: PressBounceHolder().also { view.tag = it }
        fun cancelRunning() {
            holder.pressAnim?.cancel()
            holder.releaseAnim?.cancel()
        }
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    cancelRunning()
                    val scaleX = ObjectAnimator.ofFloat(v, View.SCALE_X, v.scaleX, pressScale).apply {
                        duration = pressDurationMs
                        interpolator = AccelerateDecelerateInterpolator()
                    }
                    val scaleY = ObjectAnimator.ofFloat(v, View.SCALE_Y, v.scaleY, pressScale).apply {
                        duration = pressDurationMs
                        interpolator = AccelerateDecelerateInterpolator()
                    }
                    val alpha = ObjectAnimator.ofFloat(v, View.ALPHA, v.alpha, pressAlpha).apply {
                        duration = pressDurationMs
                        interpolator = AccelerateDecelerateInterpolator()
                    }
                    holder.pressAnim = AnimatorSet().apply {
                        playTogether(scaleX, scaleY, alpha)
                        start()
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    cancelRunning()
                    val scaleX = ObjectAnimator.ofFloat(v, View.SCALE_X, v.scaleX, 1f).apply {
                        duration = releaseDurationMs
                        interpolator = OvershootInterpolator(1.8f)
                    }
                    val scaleY = ObjectAnimator.ofFloat(v, View.SCALE_Y, v.scaleY, 1f).apply {
                        duration = releaseDurationMs
                        interpolator = OvershootInterpolator(1.8f)
                    }
                    val alpha = ObjectAnimator.ofFloat(v, View.ALPHA, v.alpha, 1f).apply {
                        duration = releaseDurationMs
                        interpolator = AccelerateDecelerateInterpolator()
                    }
                    holder.releaseAnim = AnimatorSet().apply {
                        playTogether(scaleX, scaleY, alpha)
                        start()
                    }
                }
            }
            false
        }
    }

    /**
     * 给任意 View 加上「按下时灰色蒙层」的点击反馈，不拦截点击事件。
     * 按下时在 View 上叠一层半透明灰蒙层，松手后淡出并移除。
     * @param view 目标 View
     * @param overlayColor 蒙层颜色（含透明度），默认半透明黑 #50000000
     * @param pressDurationMs 蒙层出现时长
     * @param releaseDurationMs 蒙层消失时长
     */
    @JvmOverloads
    fun applyPressGrayOverlay(
        view: View,
        overlayColor: Int = 0x50814FC9,
        pressDurationMs: Long = 80L,
        releaseDurationMs: Long = 120L
    ) {
        val holder = view.getTag(R.id.press_gray_overlay_holder) as? PressGrayOverlayHolder
            ?: PressGrayOverlayHolder(overlayColor.toDrawable())
                .also { view.setTag(R.id.press_gray_overlay_holder, it) }
        val overlayDrawable = holder.overlayDrawable
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    holder.animator?.cancel()
                    if (v.width <= 0 || v.height <= 0) return@setOnTouchListener false
                    overlayDrawable.setBounds(0, 0, v.width, v.height)
                    overlayDrawable.alpha = 0
                    v.overlay.add(overlayDrawable)
                    holder.animator = ValueAnimator.ofInt(0, 255).apply {
                        duration = pressDurationMs
                        interpolator = AccelerateDecelerateInterpolator()
                        addUpdateListener { overlayDrawable.alpha = it.animatedValue as Int }
                        start()
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    holder.animator?.cancel()
                    holder.animator = ValueAnimator.ofInt(overlayDrawable.alpha, 0).apply {
                        duration = releaseDurationMs
                        interpolator = AccelerateDecelerateInterpolator()
                        addUpdateListener { overlayDrawable.alpha = it.animatedValue as Int }
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                v.overlay.remove(overlayDrawable)
                                holder.animator = null
                            }
                        })
                        start()
                    }
                }
            }
            false
        }
    }

    /**
     * 播放欢迎奖励进场动画，结束后：第一个 View 慢慢持续旋转，第二个 View 不停呼吸放大。
     * @param animView 第一个 View（进场动画 + 结束后慢速旋转）
     * @param rewardView 第二个 View（从 0 放大到 1 + 结束后呼吸放大）
     */
    fun playWelcomeBonusAnim(animView: View, rewardView: View, end: (() -> Unit)?=null) {
        animView.alpha = 0.3f
        animView.scaleX = 0.1f
        animView.scaleY = 0.1f
        rewardView.alpha = 1f
        rewardView.scaleX = 0f
        rewardView.scaleY = 0f

        val alphaAnim = ObjectAnimator.ofFloat(animView, View.ALPHA, 0.3f, 1f).apply {
            duration = 1000L
        }

        val scaleX = PropertyValuesHolder.ofKeyframe(
            View.SCALE_X,
            Keyframe.ofFloat(0f, 0.1f),
            Keyframe.ofFloat(0.7f, 1.5f),
            Keyframe.ofFloat(1f, 1f)
        )
        val scaleY = PropertyValuesHolder.ofKeyframe(
            View.SCALE_Y,
            Keyframe.ofFloat(0f, 0.1f),
            Keyframe.ofFloat(0.7f, 1.5f),
            Keyframe.ofFloat(1f, 1f)
        )
        val scaleAnim = ObjectAnimator.ofPropertyValuesHolder(animView, scaleX, scaleY).apply {
            duration = 1300L
        }

        val rewardScaleX = ObjectAnimator.ofFloat(rewardView, View.SCALE_X, 0f, 1f).apply {
            duration = 1500L
        }
        val rewardScaleY = ObjectAnimator.ofFloat(rewardView, View.SCALE_Y, 0f, 1f).apply {
            duration = 1500L
        }

        AnimatorSet().apply {
            playTogether(alphaAnim, scaleAnim, rewardScaleX, rewardScaleY)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // 第一个 View：慢慢持续旋转
                    val rotationAnim = ObjectAnimator.ofFloat(animView, View.ROTATION, 0f, 360f).apply {
                        duration = 4000L
                        repeatCount = ObjectAnimator.INFINITE
                        interpolator = AccelerateDecelerateInterpolator()
                    }
                    animView.setTag(R.id.welcome_bonus_rotation_holder, rotationAnim)
                    rotationAnim.start()
                    // 第二个 View：不停呼吸放大，透明度 0.9～1
                    playClaimablePulseAnim(rewardView, true, 0.96f, 1.08f, 0.9f, 1f)
                    end?.invoke()
                }
            })
            start()
        }
    }

    /**
     * 庆祝动画1：弹跳进入（Bounce In）
     * 从上方弹跳下来，带弹性回弹效果
     */
    private fun playCelebrateBounceIn(view: View, durationMs: Long = 1000L) {
        view.post {
            val startY = -view.height.toFloat()
            view.translationY = startY
            view.alpha = 0f
            view.scaleX = 0.8f
            view.scaleY = 0.8f
            val translateY = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, startY, 0f).apply {
                interpolator = OvershootInterpolator(2.5f)
            }
            val alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f)
            val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 0.8f, 1f).apply {
                interpolator = OvershootInterpolator(1.8f)
            }
            val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.8f, 1f).apply {
                interpolator = OvershootInterpolator(1.8f)
            }
            AnimatorSet().apply {
                playTogether(translateY, alpha, scaleX, scaleY)
                this.duration = durationMs
                start()
            }
        }
    }

    /**
     * 庆祝动画2：烟花绽放（Fireworks）
     * 缩放 + 透明度 + 轻微旋转，像烟花绽放
     */
    private fun playCelebrateFireworks(view: View, durationMs: Long = 1000L) {
        view.post {
            view.alpha = 0f
            view.scaleX = 0f
            view.scaleY = 0f
            view.rotation = -15f
            val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 0f, 1.2f, 1f).apply {
                interpolator = OvershootInterpolator(1.5f)
            }
            val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0f, 1.2f, 1f).apply {
                interpolator = OvershootInterpolator(1.5f)
            }
            val alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).apply {
                interpolator = DecelerateInterpolator()
            }
            val rotation = ObjectAnimator.ofFloat(view, View.ROTATION, -15f, 5f, 0f).apply {
                interpolator = DecelerateInterpolator()
            }
            AnimatorSet().apply {
                playTogether(scaleX, scaleY, alpha, rotation)
                this.duration = durationMs
                start()
            }
        }
    }

    /**
     * 庆祝动画：随机选择弹跳进入或烟花绽放
     * @param view 目标 View
     * @param durationMs 动画时长，默认 1000ms
     */
    fun playCelebrateAnim(view: View, durationMs: Long = 1000L) {
        when (Random.nextBoolean()) {
            true -> playCelebrateBounceIn(view, durationMs)
            false -> playCelebrateFireworks(view, durationMs)
        }
    }

    /**
     * 显示动画：缩放 0→1，透明度 0.8→1
     * @param view 目标 View
     * @param durationMs 动画时长，默认 280ms
     * @param end 动画结束回调，可为 null
     */
    @JvmOverloads
    fun playShowScaleAlphaAnim(
        view: View,
        durationMs: Long = 480L,
        end: (() -> Unit)? = null
    ) {
        view.visibility = View.VISIBLE
        view.scaleX = 0f
        view.scaleY = 0f
        view.alpha = 0.8f
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 0f, 1f).apply {
            interpolator = AccelerateDecelerateInterpolator()
        }
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0f, 1f).apply {
            interpolator = AccelerateDecelerateInterpolator()
        }
        val alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0.8f, 1f).apply {
            interpolator = AccelerateDecelerateInterpolator()
        }
        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            this.duration = durationMs
            if (end != null) addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { end.invoke() }
            })
            start()
        }
    }

    fun flyToTarget(
        animView: View,
        targetView: View,
        durationMs: Long = 800L,
        end: (() -> Unit)? = null
    ) {
        animView.isClickable = false
        animView.post {
            val startLocation = IntArray(2)
            val endLocation = IntArray(2)
            animView.getLocationInWindow(startLocation)
            targetView.getLocationInWindow(endLocation)

            val dx = endLocation[0] - startLocation[0] + (targetView.width - animView.width) / 2f
            val dy = endLocation[1] - startLocation[1] + (targetView.height - animView.height) / 2f

            animView.apply {
                visibility = View.VISIBLE
                alpha = 1f
                scaleX = 1f
                scaleY = 1f
                translationX = 0f
                translationY = 0f
            }

            val shrinkDuration = (durationMs * 0.25f).toLong().coerceAtLeast(120L)
            val moveDuration = durationMs - shrinkDuration

            val shrinkX = ObjectAnimator.ofFloat(animView, View.SCALE_X, 1f, 0.6f).apply {
                duration = shrinkDuration
                interpolator = AccelerateDecelerateInterpolator()
            }
            val shrinkY = ObjectAnimator.ofFloat(animView, View.SCALE_Y, 1f, 0.6f).apply {
                duration = shrinkDuration
                interpolator = AccelerateDecelerateInterpolator()
            }

            val xAnim = ObjectAnimator.ofFloat(animView, View.TRANSLATION_X, 0f, dx).apply {
                duration = moveDuration
                interpolator = AccelerateDecelerateInterpolator()
            }
            val yAnim = ObjectAnimator.ofFloat(animView, View.TRANSLATION_Y, 0f, dy).apply {
                duration = moveDuration
                interpolator = AccelerateDecelerateInterpolator()
            }
            val alphaAnim = ObjectAnimator.ofFloat(animView, View.ALPHA, 1f, 0.2f).apply {
                duration = moveDuration
                interpolator = AccelerateDecelerateInterpolator()
            }

            AnimatorSet().apply {
                play(shrinkX).with(shrinkY)
                playTogether(xAnim, yAnim, alphaAnim)
                play(xAnim).after(shrinkX)
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        animView.isClickable = true
                        animView.visibility = View.INVISIBLE
                        animView.alpha = 1f
                        animView.scaleX = 1f
                        animView.scaleY = 1f
                        animView.translationX = 0f
                        animView.translationY = 0f
                        end?.invoke()
                    }
                })
                start()
            }
        }
    }

    /**
     * 可领取状态的呼吸闪动动画（作用在任意View上）
     * @param view 需要展示动画的View
     * @param isClaimable 是否可领取，false 会停止并重置动画
     * @param minAlpha 透明度最小值（呼吸时最低 alpha），默认 0.6f
     * @param maxAlpha 透明度最大值，默认 1f
     */
    @JvmOverloads
    fun playClaimablePulseAnim(
        view: View,
        isClaimable: Boolean,
        minScale: Float = 1f,
        maxScale: Float = 1.15f,
        minAlpha: Float = 0.6f,
        maxAlpha: Float = 1f
    ) {
        val animator = view.tag as? AnimatorSet
        if (!isClaimable) {
            animator?.cancel()
            view.scaleX = 1f
            view.scaleY = 1f
            view.alpha = 1f
            view.translationX = 0f
            view.tag = null
            return
        }
        if (animator?.isRunning == true) return
        val safeMin = minScale.coerceAtLeast(0.6f)
        val safeMax = maxScale.coerceAtMost(1.4f).coerceAtLeast(safeMin)
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, safeMin, safeMax, safeMin)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, safeMin, safeMax, safeMin)
        val scaleAnim = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY).apply {
            duration = 1200L
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
        }
        val safeAlphaMin = minAlpha.coerceIn(0f, 1f)
        val safeAlphaMax = maxAlpha.coerceIn(0f, 1f).coerceAtLeast(safeAlphaMin)
        val alphaAnim = ObjectAnimator.ofFloat(view, View.ALPHA, safeAlphaMax, safeAlphaMin, safeAlphaMax).apply {
            duration = 1200L
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
        }
        val newAnimator = AnimatorSet().apply {
            playTogether(scaleAnim, alphaAnim)
        }
        view.tag = newAnimator
        newAnimator.start()
    }

    /**
     * 观看广告按钮：从左到右流水感扫光（封装调用）
     * @param targetView 需要扫光的容器
     * @param shineView 高光层View（放在容器内部）
     */
    fun startWatchAdShineAnim(
        targetView: View,
        shineView: View,
        minDelayMs: Long = 5000L,
        maxDelayMs: Long = 8500L,
        durationMs: Long = 400L
    ) {
        stopWatchAdShineAnim(shineView)
        val safeMinDelay = minDelayMs.coerceAtLeast(0L)
        val safeMaxDelay = maxDelayMs.coerceAtLeast(safeMinDelay)

        lateinit var repeatRunnable: Runnable
        val attachListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                stopWatchAdShineAnim(shineView)
            }
        }

        repeatRunnable = Runnable {
            if (targetView.width == 0 || shineView.width == 0) {
                targetView.post(repeatRunnable)
                return@Runnable
            }
            if (!targetView.isShown) {
                shineView.visibility = View.INVISIBLE
                shineView.postDelayed(repeatRunnable, safeMaxDelay)
                return@Runnable
            }

            val startX = -shineView.width.toFloat()
            val endX = targetView.width.toFloat()
            shineView.translationX = startX
            shineView.translationY = 0f
            shineView.rotation = 0f
            shineView.visibility = View.VISIBLE

            // 从左到右水平流动
            val moveX = ObjectAnimator.ofFloat(shineView, View.TRANSLATION_X, startX, endX).apply {
                duration = durationMs
                interpolator = AccelerateDecelerateInterpolator()
            }
            // 轻微上下波动，模拟流水起伏
            val wavePx = (2.5f * shineView.resources.displayMetrics.density).toFloat()
            val moveY = ObjectAnimator.ofFloat(shineView, View.TRANSLATION_Y, 0f, wavePx, -wavePx * 0.6f, 0f).apply {
                duration = durationMs
                interpolator = AccelerateDecelerateInterpolator()
            }
            // 透明度：流入 → 保持亮 → 流出
            val alphaAnim = ObjectAnimator.ofFloat(shineView, View.ALPHA, 0f, 0.92f, 0.92f, 0f).apply {
                duration = durationMs
                interpolator = AccelerateDecelerateInterpolator()
            }
            // 轻微变宽再收，像水流过
            val scaleXAnim = ObjectAnimator.ofFloat(shineView, View.SCALE_X, 0.92f, 1.05f, 0.98f).apply {
                duration = durationMs
                interpolator = AccelerateDecelerateInterpolator()
            }
            val animator = AnimatorSet().apply {
                playTogether(moveX, moveY, alphaAnim, scaleXAnim)
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        shineView.visibility = View.INVISIBLE
                        shineView.alpha = 1f
                        shineView.scaleX = 1f
                        shineView.translationY = 0f
                        val nextDelay = Random.nextLong(safeMinDelay, safeMaxDelay + 1)
                        shineView.postDelayed(repeatRunnable, nextDelay)
                    }
                })
            }
            shineView.tag = ShineAnimHolder(animator, repeatRunnable, attachListener)
            animator.start()
        }

        shineView.tag = ShineAnimHolder(null, repeatRunnable, attachListener)
        shineView.addOnAttachStateChangeListener(attachListener)
        targetView.postDelayed(repeatRunnable, safeMinDelay)
    }

    fun stopWatchAdShineAnim(shineView: View) {
        val holder = shineView.tag as? ShineAnimHolder ?: return
        shineView.removeCallbacks(holder.runnable)
        holder.animator?.cancel()
        shineView.removeOnAttachStateChangeListener(holder.attachListener)
        shineView.translationX = 0f
        shineView.translationY = 0f
        shineView.visibility = View.INVISIBLE
        shineView.tag = null
    }

    /**
     * 复制一个临时View做飞行动画，原View保持不动
     * @param copyView 要复制的View
     * @param targetView 飞向的目标View
     * @param scaleTo 先缩放到该比例再飞，默认0.8
     * @param durationMs 动画时长，默认600毫秒
     * @param end 结束回调
     */
    fun playCoinFlyCopyAnim(
        copyView: ImageView,
        targetView: View,
        scaleTo: Float = 0.8f,
        durationMs: Long = 800L,
        end: (() -> Unit)? = null
    ) {
        val rootView = (copyView.rootView as? ViewGroup) ?: return
        val drawable = copyView.drawable ?: copyView.background ?: return
        val animView = ImageView(copyView.context).apply {
            setImageDrawable(drawable)
            layoutParams = ViewGroup.LayoutParams(copyView.width, copyView.height)
            visibility = View.INVISIBLE
            alpha = 0f
        }
        rootView.addView(animView)
        playCoinFlyAnim(animView, copyView, targetView, durationMs, scaleTo, {
            rootView.removeView(animView)
            end?.invoke()
        })
    }

    private fun playCoinFlyAnim(
        animView: ImageView,
        startView: ImageView,
        targetView: View,
        durationMs: Long,
        scaleTo: Float,
        end: (() -> Unit)?,
        startOffsetX: Float = 0f,
        startOffsetY: Float = 0f
    ) {
        val rootView = (startView.rootView as? ViewGroup) ?: return
        animView.post {
            val rootLocation = IntArray(2)
            val startLocation = IntArray(2)
            val endLocation = IntArray(2)
            rootView.getLocationInWindow(rootLocation)
            startView.getLocationInWindow(startLocation)
            targetView.getLocationInWindow(endLocation)

            val startX = startLocation[0] - rootLocation[0] + startView.width / 2f - animView.width / 2f + startOffsetX
            val startY =
                startLocation[1] - rootLocation[1] + startView.height / 2f - animView.height / 2f + startOffsetY
            val endX = endLocation[0] - rootLocation[0] + targetView.width / 2f - animView.width / 2f
            val endY = endLocation[1] - rootLocation[1] + targetView.height / 2f - animView.height / 2f

            animView.apply {
                scaleX = 1f
                scaleY = 1f
                x = startX
                y = startY
                alpha = 1f
                visibility = View.VISIBLE
            }

            val shrinkDuration = (durationMs * 0.25f).toLong().coerceAtLeast(120L)
            val moveDuration = (durationMs - shrinkDuration).coerceAtLeast(0L)
            val safeScale = scaleTo.coerceIn(0.2f, 1f)

            val shrinkX = ObjectAnimator.ofFloat(animView, View.SCALE_X, 1f, safeScale).apply {
                duration = shrinkDuration
                interpolator = AccelerateDecelerateInterpolator()
            }
            val shrinkY = ObjectAnimator.ofFloat(animView, View.SCALE_Y, 1f, safeScale).apply {
                duration = shrinkDuration
                interpolator = AccelerateDecelerateInterpolator()
            }
            val xAnim = ObjectAnimator.ofFloat(animView, View.X, startX, endX).apply {
                duration = moveDuration
                interpolator = AccelerateDecelerateInterpolator()
            }
            val yAnim = ObjectAnimator.ofFloat(animView, View.Y, startY, endY).apply {
                duration = moveDuration
                interpolator = AccelerateDecelerateInterpolator()
            }
            val alphaAnim = ObjectAnimator.ofFloat(animView, View.ALPHA, 1f, 0.2f).apply {
                duration = moveDuration
                interpolator = AccelerateDecelerateInterpolator()
            }
            AnimatorSet().apply {
                play(shrinkX).with(shrinkY)
                playTogether(xAnim, yAnim, alphaAnim)
                play(xAnim).after(shrinkX)
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        end?.invoke()
                    }
                })
                start()
            }
        }
    }

    /** drawable-hdpi 中 4 张飞钱图，用于多枚硬币飞行动画 */
    private val FLY_COIN_DRAWABLE_IDS = intArrayOf(
        R.drawable.ic_fly_money1,
        R.drawable.ic_fly_money2,
        R.drawable.ic_fly_money3,
        R.drawable.ic_fly_money4
    )

    /**
     * 多枚硬币（3～6 张）一张一张飞向目标，命中后给目标反馈动画。
     * 硬币从 4 张图随机选取，按序错开起飞，避免飞得太集中。
     */
    fun playCoinFlyWithHitAnim(
        animView: ImageView,
        targetView: View,
        scaleTo: Float = 0.8f,
        durationMs: Long = 750L,
        hitScale: Float = 1.2f,
        hitDurationMs: Long = 250L,
        end: (() -> Unit)? = null
    ) {
        animView.post {
            val rootView = (animView.rootView as? ViewGroup) ?: return@post
            val width = animView.width.takeIf { it > 0 } ?: animView.measuredWidth
            val height = animView.height.takeIf { it > 0 } ?: animView.measuredHeight
            if (width <= 0 || height <= 0) return@post

            SoundHelper.playCoinShrink(animView.context) // 金币收缩音效
            val count = Random.nextInt(4) + 3 // 3～6 枚
            val flyViews = mutableListOf<ImageView>()
            for (i in 0 until count) {
                val resId = FLY_COIN_DRAWABLE_IDS.random()
                val flyView = ImageView(animView.context).apply {
                    setImageResource(resId)
                    layoutParams = ViewGroup.LayoutParams(width, height)
                    visibility = View.INVISIBLE
                    alpha = 0f
                }
                rootView.addView(flyView)
                flyViews.add(flyView)
            }

            // 一张一张飞：按序号错开起飞，间隔约 durationMs/4，避免扎堆
            val stepMs = (durationMs / 4).coerceAtLeast(120L)
            var completed = 0
            val handler = Handler(Looper.getMainLooper())
            for ((index, flyView) in flyViews.withIndex()) {
                val startDelayMs = index * stepMs
                handler.postDelayed({
                    playCoinFlyAnim(flyView, animView, targetView, durationMs, scaleTo, {
                        playHitPulseAnim(targetView, hitScale, hitDurationMs) // 每枚到达都触发撞击效果
                        completed++
                        if (completed == count) {
                            flyViews.forEach { rootView.removeView(it) }
                            end?.invoke()
                        }
                    }, 0f, 0f)
                }, startDelayMs)
            }
        }
    }

    /**
     * 被击中反馈动画：轻微放大回弹
     */
    fun playHitPulseAnim(
        targetView: View,
        hitScale: Float = 1.1f,
        hitDurationMs: Long = 220L
    ) {
        targetView.post {
            targetView.animate().cancel()
            val scale = hitScale.coerceIn(1.0f, 1.3f)
            val scaleX = ObjectAnimator.ofFloat(targetView, View.SCALE_X, 1f, scale, 1f)
            val scaleY = ObjectAnimator.ofFloat(targetView, View.SCALE_Y, 1f, scale, 1f)
            AnimatorSet().apply {
                playTogether(scaleX, scaleY)
                duration = hitDurationMs
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        }
    }
}
