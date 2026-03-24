package com.sphere.shortvideos.helper.withdraw

import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.sphere.shortvideos.R
import com.sphere.shortvideos.databinding.ItemWithUserInfoBinding
import com.sphere.shortvideos.helper.WithdrawAmountHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * 提现页底部「用户到账信息」横向跑马灯：构建子视图、克隆无缝滚动、动画生命周期。
 */
class WithdrawUserInfoMarqueeController(
    private val host: Fragment,
) {

    private var marqueeAnimator: ValueAnimator? = null

    private val listPayInfo: List<String>
        get() = WithdrawAmountHelper.fetchWithdrawPaymentMethods().map { it.name }

    /**
     * 在 [container]（一般为 [R.id.layout_with_user_info]）内填充跑马灯并启动滚动。
     */
    fun setup(container: FrameLayout) {
        if (!host.isAdded) return
        val inflater = host.layoutInflater
        container.removeAllViews()
        stop()

        val scrollView = HorizontalScrollView(host.requireContext()).apply {
            layoutParams =
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val track = LinearLayout(host.requireContext()).apply {
            layoutParams =
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
            orientation = LinearLayout.HORIZONTAL
        }
        val amounts = WithdrawAmountHelper.fetchWithdrawAmounts()
        val itemWidth = dpToPx(213f).toInt()
        val gap = dpToPx(10f).toInt()
        container.layoutParams = container.layoutParams.apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
        }
        val baseItems = mutableListOf<View>()
        repeat(3) {
            val itemBinding = ItemWithUserInfoBinding.inflate(inflater, track, false)
            itemBinding.tvMoney.text = WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(amounts.random())
            itemBinding.tvUserName.text = buildMaskedName()
            itemBinding.tvPayType.text = host.getString(R.string.arrived_via, listPayInfo.random())
            itemBinding.tvDay.text = SimpleDateFormat("yyyy.M.d", Locale.getDefault()).format(Date())
            val lp = LinearLayout.LayoutParams(itemWidth, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                marginEnd = gap
            }
            itemBinding.root.layoutParams = lp
            baseItems.add(itemBinding.root)
            track.addView(itemBinding.root)
        }
        baseItems.forEach { view ->
            val clone = ItemWithUserInfoBinding.inflate(inflater, track, false).apply {
                tvMoney.text = (view.findViewById<TextView>(R.id.tv_money)).text
                tvUserName.text = (view.findViewById<TextView>(R.id.tv_user_name)).text
                tvPayType.text = (view.findViewById<TextView>(R.id.tv_pay_type)).text
                tvDay.text = (view.findViewById<TextView>(R.id.tv_day)).text
            }
            val lp = LinearLayout.LayoutParams(itemWidth, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                marginEnd = gap
            }
            clone.root.layoutParams = lp
            track.addView(clone.root)
        }
        scrollView.addView(track)
        container.addView(scrollView)
        startMarquee(scrollView, track, itemWidth + gap, 3)
    }

    fun stop() {
        marqueeAnimator?.cancel()
        marqueeAnimator = null
    }

    private fun buildMaskedName(): String {
        val pool = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val raw = buildString {
            repeat(10) {
                append(pool[Random.nextInt(pool.length)])
            }
        }
        return raw.take(2) + "******" + raw.takeLast(2)
    }

    private fun dpToPx(value: Float): Float {
        val res = host.context?.resources ?: return value
        return value * res.displayMetrics.density
    }

    private fun startMarquee(scrollView: HorizontalScrollView, track: LinearLayout, step: Int, count: Int) {
        track.post {
            if (!host.isAdded || host.view == null || host.context == null) return@post
            if (step <= 0) return@post
            val totalWidth = step * count
            val speedPxPerSec = dpToPx(40f)
            val durationMs = ((totalWidth / speedPxPerSec) * 1000L).toLong().coerceAtLeast(3000L)
            marqueeAnimator = ValueAnimator.ofFloat(0f, totalWidth.toFloat()).apply {
                duration = durationMs
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                addUpdateListener {
                    val value = (it.animatedValue as Float) % totalWidth
                    scrollView.scrollTo(value.toInt(), 0)
                }
                interpolator = LinearInterpolator()
                start()
            }
        }
    }
}
