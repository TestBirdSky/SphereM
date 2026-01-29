package com.sphere.shortvideos.view

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.view.animation.AccelerateDecelerateInterpolator

object AnimViewHelper {
    fun playWelcomeBonusAnim(animView: View, rewardView: View) {
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
            start()
        }
    }

    fun slideInFromTop(view: View, durationMs: Long = 1200L) {
        view.post {
            val startY = -view.height.toFloat()
            view.translationY = startY
            ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, startY, 0f).apply {
                duration = durationMs
                start()
            }
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
     */
    fun playClaimablePulseAnim(view: View, isClaimable: Boolean) {
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
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.15f, 1f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.15f, 1f)
        val scaleAnim = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY).apply {
            duration = 1200L
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
        }
        val alphaAnim = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0.6f, 1f).apply {
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

    private fun playCoinFlyAnim(animView: ImageView,
                                startView: ImageView,
                                targetView: View,
                                durationMs: Long,
                                scaleTo: Float,
                                end: (() -> Unit)?) {
        val rootView = (startView.rootView as? ViewGroup) ?: return
        animView.post {
            val rootLocation = IntArray(2)
            val startLocation = IntArray(2)
            val endLocation = IntArray(2)
            rootView.getLocationInWindow(rootLocation)
            startView.getLocationInWindow(startLocation)
            targetView.getLocationInWindow(endLocation)

            val startX = startLocation[0] - rootLocation[0] + startView.width / 2f - animView.width / 2f
            val startY = startLocation[1] - rootLocation[1] + startView.height / 2f - animView.height / 2f
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

    /**
     * 复制一个临时View飞向目标，命中后给目标反馈动画
     * 推荐：轻微放大回弹，反馈更明显且不突兀
     */
    fun playCoinFlyWithHitAnim(
        animView: ImageView,
        targetView: View,
        scaleTo: Float = 0.8f,
        durationMs: Long = 750L,
        hitScale: Float = 1.2f,
        hitDurationMs: Long = 350L,
        end: (() -> Unit)? = null
    ) {
        animView.post {
            val rootView = (animView.rootView as? ViewGroup) ?: return@post
            val drawable = animView.drawable ?: animView.background ?: return@post
            val width = animView.width.takeIf { it > 0 } ?: animView.measuredWidth
            val height = animView.height.takeIf { it > 0 } ?: animView.measuredHeight
            if (width <= 0 || height <= 0) return@post

            val flyView = ImageView(animView.context).apply {
                setImageDrawable(drawable)
                layoutParams = ViewGroup.LayoutParams(width, height)
                visibility = View.INVISIBLE
                alpha = 0f
            }
            rootView.addView(flyView)
            playCoinFlyAnim(flyView, animView, targetView, durationMs, scaleTo, {
                rootView.removeView(flyView)
                playHitPulseAnim(targetView, hitScale, hitDurationMs)
                end?.invoke()
            })
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
