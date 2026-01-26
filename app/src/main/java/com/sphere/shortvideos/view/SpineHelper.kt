package com.sphere.shortvideos.view

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import com.esotericsoftware.spine.android.SpineController
import com.esotericsoftware.spine.android.SpineView

/**
 * Date：2026/1/23
 * Describe:
 */
class SpineHelper {
    private fun createController(): SpineController {
        return SpineController { spineController ->
            spineController.getAnimationState().setAnimation(0, "animation", true)
        }
    }

    fun addViewMoney1(parent: ViewGroup, context: Context) {
        val spineView = SpineView.loadFromAssets(
            "money1/skeleton.atlas",
            "money1/skeleton.json",
            context,
            createController()
        )
        spineView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        parent.removeAllViews()
        parent.addView(spineView)
    }

    fun addViewMoney2(parent: ViewGroup, context: Context) {
        val spineView = SpineView.loadFromAssets(
            "money2/skeleton.atlas",
            "money2/skeleton.json",
            context,
            createController()
        )
        spineView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        parent.removeAllViews()
        parent.addView(spineView)
    }
    fun addViewWallet(parent: ViewGroup, context: Context) {
        val spineView = SpineView.loadFromAssets(
            "wallet/skeleton.atlas",
            "wallet/skeleton.json",
            context,
            createController()
        )
        spineView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        parent.removeAllViews()
        parent.addView(spineView)
    }


}