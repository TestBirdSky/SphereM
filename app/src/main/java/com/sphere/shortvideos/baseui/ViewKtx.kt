package com.sphere.shortvideos.baseui

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.sphere.shortvideos.R
import com.sphere.shortvideos.databinding.LayoutMoneyTopViewBinding
import com.sphere.shortvideos.databinding.LayoutTaskChildBinding
import com.sphere.shortvideos.adapter.SevenDayRewardAdapter
import com.sphere.shortvideos.helper.MoneyCacheHelper
import com.sphere.shortvideos.helper.task.TaskHelper
import com.sphere.shortvideos.helper.WithdrawAmountHelper

/**
 * Date：2026/1/21
 * Describe:
 */

fun TextView.setColor(fullText: String, firStart: Int = 0, endIndex: Int = 5) {
    val spannableString = SpannableString(fullText)

    // 3. 设置第一部分文本颜色（红色，索引0-4）
    val redSpan = ForegroundColorSpan(context.getColor(R.color.color_f5))
    spannableString.setSpan(redSpan, firStart,          // 起始索引（包含）
        endIndex,          // 结束索引（不包含）
        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)

    // 4. 设置第二部分文本颜色（蓝色，索引5-9）
    val blueSpan = ForegroundColorSpan(context.getColor(R.color.color_theme))
    spannableString.setSpan(blueSpan, endIndex,          // 起始索引（包含）
        spannableString.length,          // 结束索引（不包含）
        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE) // 5. 将设置好颜色的文本设置给TextView
    text = spannableString
}

fun LayoutMoneyTopViewBinding.refreshView(money: String, tagMoney: String, activity: AppCompatActivity) {
    tvCurMoney.text = money
    val fullText = root.context.getString(R.string.withdraw_tips, tagMoney)
    val start = fullText.indexOf(tagMoney)
    if (start >= 0) {
        val end = start + tagMoney.length
        val spannableString = SpannableString(fullText)
        val highlightSpan = ForegroundColorSpan(Color.parseColor("#49F32B"))
        spannableString.setSpan(highlightSpan, start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        tvTips.text = spannableString
    } else {
        tvTips.text = fullText
    }
    ivPTag.setBackgroundResource(WithdrawAmountHelper.fetchMoneyBankIcon())
    layout.setOnClickListener {

    }
}

fun LayoutTaskChildBinding.setTaskInfo(activity: Context,
                                       daySignSuccess: (Double, ImageView) -> Unit,
                                       watchTimeArriver: (Double, ImageView) -> Unit) {
    ivWatchAd.setOnClickListener { // todo 显示广告
    }
    bgH5.setOnClickListener { // todo 显示H5
    }
    setWatchLayout(activity, watchTimeArriver)
    set7DayReword(daySignSuccess)

}

private fun LayoutTaskChildBinding.set7DayReword(daySignSuccess: (Double, ImageView) -> Unit) {
    val states = TaskHelper.fetchSignInStates()
    val items = states.map { state ->
        val reward = state.reward
        val rewardText = if (reward > 0) {
            "+${WithdrawAmountHelper.formatMoney(reward)}"
        } else {
            "--"
        }
        val status = when (state.status) {
            TaskHelper.SignInStatus.CLAIMED -> SevenDayRewardAdapter.SignInStatus.CLAIMED
            TaskHelper.SignInStatus.CLAIMABLE -> SevenDayRewardAdapter.SignInStatus.CLAIMABLE
            TaskHelper.SignInStatus.UNCLAIMED -> SevenDayRewardAdapter.SignInStatus.UNCLAIMED
        }
        SevenDayRewardAdapter.SevenDayRewardItem(state.day, rewardText, status)
    }
    val adapter = (rvList.adapter as? SevenDayRewardAdapter) ?: SevenDayRewardAdapter().also {
        rvList.adapter = it
        val manager = GridLayoutManager(root.context, 4)
        manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (position == 6) 2 else 1
            }
        }
        rvList.layoutManager = manager
    }
    adapter.onItemClick = { _, view ->
        val reward = TaskHelper.claimSignInReward()
        if (reward != null) {
            daySignSuccess.invoke(reward, view)
            set7DayReword(daySignSuccess)
        }
    }
    adapter.submitList(items)
}

private fun LayoutTaskChildBinding.setWatchLayout(context: Context, watchTimeArriver: (Double, ImageView) -> Unit) {
    val bean = MoneyCacheHelper.fetchAllWatchReword()
    tvDes1.text = bean[0].second
    tvDes2.text = bean[1].second
    tvDes3.text = bean[2].second
    tvDes4.text = bean[3].second
    tvDes5.text = bean[4].second
    tvDes6.text = bean[5].second
    val curIndexReward = TaskHelper.fetchCurClaimedIndex()
    applyClaimableAnim(ivSmallMoney1, TaskHelper.canClaimWatchReward(0))
    applyClaimableAnim(ivSmallMoney2, TaskHelper.canClaimWatchReward(1))
    applyClaimableAnim(ivSmallMoney3, TaskHelper.canClaimWatchReward(2))
    applyClaimableAnim(ivBigMoney1, TaskHelper.canClaimWatchReward(3))
    applyClaimableAnim(ivBigMoney2, TaskHelper.canClaimWatchReward(4))
    applyClaimableAnim(ivBigMoney3, TaskHelper.canClaimWatchReward(5))
    fun clickReward(index: Int, view: ImageView) {
        val reward = TaskHelper.clickWatchReward(index)
        if (reward != null) {
            watchTimeArriver.invoke(reward, view)
            setWatchLayout(context, watchTimeArriver)
        }
    }
    progressView.progress = (((curIndexReward + 1) / 6.0) * 100).toInt()
    when (curIndexReward) {
        0 -> {
            ivSmallMoney1.setImageResource(R.drawable.ic_coin_low_grey)
            ivSelected1.setBackgroundResource(R.drawable.ic_check)
            tvDes1.setTextColor(context.getColor(R.color.color_9b))
        }

        1 -> {
            ivSmallMoney1.setImageResource(R.drawable.ic_coin_low_grey)
            ivSmallMoney2.setImageResource(R.drawable.ic_coin_low_grey)
            ivSelected1.setBackgroundResource(R.drawable.ic_check)
            ivSelected2.setBackgroundResource(R.drawable.ic_check)
            tvDes1.setTextColor(context.getColor(R.color.color_9b))
            tvDes2.setTextColor(context.getColor(R.color.color_9b))
        }

        2 -> {
            ivSmallMoney1.setImageResource(R.drawable.ic_coin_low_grey)
            ivSmallMoney2.setImageResource(R.drawable.ic_coin_low_grey)
            ivSmallMoney3.setImageResource(R.drawable.ic_coin_low_grey)
            ivSelected1.setBackgroundResource(R.drawable.ic_check)
            ivSelected2.setBackgroundResource(R.drawable.ic_check)
            ivSelected3.setBackgroundResource(R.drawable.ic_check)
            tvDes1.setTextColor(context.getColor(R.color.color_9b))
            tvDes2.setTextColor(context.getColor(R.color.color_9b))
            tvDes3.setTextColor(context.getColor(R.color.color_9b))
        }

        3 -> {
            ivSmallMoney1.setImageResource(R.drawable.ic_coin_low_grey)
            ivSmallMoney2.setImageResource(R.drawable.ic_coin_low_grey)
            ivSmallMoney3.setImageResource(R.drawable.ic_coin_low_grey)
            ivBigMoney1.setImageResource(R.drawable.ic_coin_high_grey)
            ivSelected1.setBackgroundResource(R.drawable.ic_check)
            ivSelected2.setBackgroundResource(R.drawable.ic_check)
            ivSelected3.setBackgroundResource(R.drawable.ic_check)
            ivSelect4.setBackgroundResource(R.drawable.ic_check)
            tvDes1.setTextColor(context.getColor(R.color.color_9b))
            tvDes2.setTextColor(context.getColor(R.color.color_9b))
            tvDes3.setTextColor(context.getColor(R.color.color_9b))
            tvDes4.setTextColor(context.getColor(R.color.color_9b))
        }

        4 -> {
            ivSmallMoney1.setImageResource(R.drawable.ic_coin_low_grey)
            ivSmallMoney2.setImageResource(R.drawable.ic_coin_low_grey)
            ivSmallMoney3.setImageResource(R.drawable.ic_coin_low_grey)
            ivBigMoney1.setImageResource(R.drawable.ic_coin_high_grey)
            ivBigMoney2.setImageResource(R.drawable.ic_coin_high_grey)
            ivSelected1.setBackgroundResource(R.drawable.ic_check)
            ivSelected2.setBackgroundResource(R.drawable.ic_check)
            ivSelected3.setBackgroundResource(R.drawable.ic_check)
            ivSelect4.setBackgroundResource(R.drawable.ic_check)
            ivSelect5.setBackgroundResource(R.drawable.ic_check)
            tvDes1.setTextColor(context.getColor(R.color.color_9b))
            tvDes2.setTextColor(context.getColor(R.color.color_9b))
            tvDes3.setTextColor(context.getColor(R.color.color_9b))
            tvDes4.setTextColor(context.getColor(R.color.color_9b))
            tvDes5.setTextColor(context.getColor(R.color.color_9b))
        }

        5 -> {
            ivSmallMoney1.setImageResource(R.drawable.ic_coin_low_grey)
            ivSmallMoney2.setImageResource(R.drawable.ic_coin_low_grey)
            ivSmallMoney3.setImageResource(R.drawable.ic_coin_low_grey)
            ivBigMoney1.setImageResource(R.drawable.ic_coin_high_grey)
            ivBigMoney2.setImageResource(R.drawable.ic_coin_high_grey)
            ivBigMoney3.setImageResource(R.drawable.ic_coin_high_grey)
            ivSelected1.setBackgroundResource(R.drawable.ic_check)
            ivSelected2.setBackgroundResource(R.drawable.ic_check)
            ivSelected3.setBackgroundResource(R.drawable.ic_check)
            ivSelect4.setBackgroundResource(R.drawable.ic_check)
            ivSelect5.setBackgroundResource(R.drawable.ic_check)
            ivSelect6.setBackgroundResource(R.drawable.ic_check)
            tvDes1.setTextColor(context.getColor(R.color.color_9b))
            tvDes2.setTextColor(context.getColor(R.color.color_9b))
            tvDes3.setTextColor(context.getColor(R.color.color_9b))
            tvDes4.setTextColor(context.getColor(R.color.color_9b))
            tvDes5.setTextColor(context.getColor(R.color.color_9b))
            tvDes6.setTextColor(context.getColor(R.color.color_9b))
        }
    }
    ivSmallMoney1.setOnClickListener {
        clickReward(0, ivSmallMoney1)
    }
    ivSmallMoney2.setOnClickListener {
        clickReward(1, ivSmallMoney2)
    }
    ivSmallMoney3.setOnClickListener {
        clickReward(2, ivSmallMoney3)
    }
    ivBigMoney1.setOnClickListener {
        clickReward(3, ivBigMoney1)
    }
    ivBigMoney2.setOnClickListener {
        clickReward(4, ivBigMoney2)
    }
    ivBigMoney3.setOnClickListener {
        clickReward(5, ivBigMoney3)
    }
}

private fun applyClaimableAnim(view: ImageView, isClaimable: Boolean) {
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