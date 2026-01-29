package com.sphere.shortvideos.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.sphere.shortvideos.adapter.WithdrawAmountAdapter
import com.sphere.shortvideos.adapter.WithdrawMethodAdapter
import com.sphere.shortvideos.baseui.GenericFragment
import com.sphere.shortvideos.databinding.FragmentWallteBinding
import com.sphere.shortvideos.databinding.ItemWithUserInfoBinding
import com.sphere.shortvideos.helper.MoneyCacheHelper
import com.sphere.shortvideos.helper.WithdrawAmountHelper
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.sphere.shortvideos.R
import com.sphere.shortvideos.helper.localEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * Date：2026/1/21
 * Describe:
 */
class WithdrawFragment : GenericFragment<FragmentWallteBinding>() {
    private val listPayInfo = arrayListOf("Paypal", "PayBank", "OVO", "DANA", "PIX")
    private val methodAdapter = WithdrawMethodAdapter()
    private val amountAdapter = WithdrawAmountAdapter()
    private var marqueeAnimator: ValueAnimator? = null

    override fun bindView(inflater: LayoutInflater, container: ViewGroup?): FragmentWallteBinding {
        return FragmentWallteBinding.inflate(inflater, container, false)
    }

    override fun initUI() {
        setupWithdrawMethods()
        setupWithdrawAmounts()
        binding.tvMoney.text = WithdrawAmountHelper.moneyFormatAddUnit(MoneyCacheHelper.fetchCurMoney())
        setupUserInfoMarquee()
        binding.tvWithdraw.setOnClickListener {
            val cur = MoneyCacheHelper.fetchCurMoney()
            val m = amountAdapter.fetchWithdrawMoney()
            localEvent("withdraw_withdraw")
            if (cur < m) {
                Toast.makeText(context, getString(R.string.cant_with_tips), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
        }
    }

    private fun setupWithdrawMethods() {
        binding.rvWithdraw.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvWithdraw.adapter = methodAdapter

    }

    private fun setupWithdrawAmounts() {
        binding.rvWithdrawMoney.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvWithdrawMoney.adapter = amountAdapter
        amountAdapter.submitList(WithdrawAmountHelper.fetchWithdrawAmounts())
    }

    private fun setupUserInfoMarquee() {
        val container = binding.layoutWithUserInfo
        container.removeAllViews()
        marqueeAnimator?.cancel()
        marqueeAnimator = null
        val scrollView = HorizontalScrollView(requireContext()).apply {
            layoutParams =
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val track = LinearLayout(requireContext()).apply {
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
            val itemBinding = ItemWithUserInfoBinding.inflate(layoutInflater, track, false)
            itemBinding.tvMoney.text = WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(amounts.random())
            itemBinding.tvUserName.text = buildMaskedName()
            itemBinding.tvPayType.text = getString(R.string.arrived_via, listPayInfo.random())
            itemBinding.tvDay.text = SimpleDateFormat("yyyy.M.d", Locale.getDefault()).format(Date())
            val lp = LinearLayout.LayoutParams(itemWidth, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                marginEnd = gap
            }
            itemBinding.root.layoutParams = lp
            baseItems.add(itemBinding.root)
            track.addView(itemBinding.root)
        }
        baseItems.forEach { view ->
            val clone = ItemWithUserInfoBinding.inflate(layoutInflater, track, false).apply {
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

    private fun buildMaskedName(): String {
        val pool = "abcdefghijklmnopqrstuvwxyz0123456789"
        val raw = buildString {
            repeat(10) {
                append(pool[Random.nextInt(pool.length)])
            }
        }
        return raw.take(2) + "******" + raw.takeLast(2)
    }

    private fun dpToPx(value: Float): Float {
        return value * resources.displayMetrics.density
    }

    private fun startMarquee(scrollView: HorizontalScrollView, track: LinearLayout, step: Int, count: Int) {
        track.post {
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

    override fun onDestroyView() {
        marqueeAnimator?.cancel()
        marqueeAnimator = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        localEvent("withdraw_page")
    }
}