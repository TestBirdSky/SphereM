package com.sphere.shortvideos.view

import android.animation.AnimatorSet
import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.View

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
}
