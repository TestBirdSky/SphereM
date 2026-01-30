package com.sphere.shortvideos.view

import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import com.sphere.shortvideos.R
import com.sphere.shortvideos.activity.MainActivity
import com.sphere.shortvideos.databinding.LayoutMoneyTopViewBinding
import com.sphere.shortvideos.databinding.LayoutTaskChildBinding
import com.sphere.shortvideos.adapter.SevenDayRewardAdapter
import com.sphere.shortvideos.baseui.GenericActivity
import com.sphere.shortvideos.helper.HelperRewardShow
import com.sphere.shortvideos.helper.MoneyCacheHelper
import com.sphere.shortvideos.helper.task.TaskHelper
import com.sphere.shortvideos.helper.WithdrawAmountHelper
import com.sphere.shortvideos.helper.ad.AdUtils
import com.sphere.shortvideos.helper.localEvent
import androidx.core.graphics.toColorInt
import com.sphere.shortvideos.dialogs.TaskInfoDialogFragment

/**
 * Date：2026/1/21
 * Describe:
 */

fun TextView.setColorText(fullText: String, firStart: Int = 0, endIndex: Int = 5) {
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


fun TextView.setColorText(fullText: String, tagString: String, color: Int) {
    val start = fullText.indexOf(tagString)
    if (start >= 0) {
        val end = start + tagString.length
        val spannableString = SpannableString(fullText)
        val highlightSpan = ForegroundColorSpan(color)
        spannableString.setSpan(highlightSpan, start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        text = spannableString
    } else {
        text = fullText
    }
}


fun LayoutMoneyTopViewBinding.initView(activity: MainActivity, tag: String, wallClose: () -> Unit = {}) {
    ivPTag.setBackgroundResource(WithdrawAmountHelper.fetchMoneyBankIcon())
    AnimViewHelper.applyPressBounceEffect(layout)
    layout.setOnClickListener {
        localEvent("earn_banner_c", hashMapOf("from" to tag))
        TaskInfoDialogFragment(activity).run {
            onClose = wallClose
            show(activity.supportFragmentManager, "task_fragment")
        }
    }
}

fun LayoutMoneyTopViewBinding.refreshViewTagMoney(tagMoney: String) {
    val fullText = root.context.getString(R.string.withdraw_tips, tagMoney)
    tvTips.setColorText(fullText, tagMoney, "#49F32B".toColorInt())
}

fun LayoutTaskChildBinding.setTaskInfo(
    activity: GenericActivity,
    receiverMoneyEvent: (Double, ImageView) -> Unit,
) {
    AnimViewHelper.applyPressGrayOverlay(ivWatchAd)
    ivWatchAd.setOnClickListener {
        localEvent("billetera_pm_ad")
        ivWatchAd.isClickable = false
        AdUtils.showRvAd(activity, dismiss = { isRewardSuccess ->
            ivWatchAd.isClickable = true
            if (isRewardSuccess) {
                receiverMoneyEvent.invoke(MoneyCacheHelper.fetchRvAdReward().first, ivAnimMoney)
            }
        })
    }
    AnimViewHelper.playClaimablePulseAnim(tvAdGo, true, 0.95f, 1.05f) //    bgH5.setOnClickListener {
    //
    //    }
    val moneyWatchRv = MoneyCacheHelper.fetchRvAdReward()
    val tagStr = moneyWatchRv.second
    val fullText = root.context.getString(R.string.get_tips, tagStr)
    tvAdGo.setColorText(fullText, tagStr, Color.parseColor("#F3CD0C")) // 观看广告按钮：高光扫光动画（1-2秒间隔，400ms扫过）
    AnimViewHelper.startWatchAdShineAnim(ivWatchAd, viewWatchAdShine)

    isAnim = false
    setWatchLayout(activity, receiverMoneyEvent)
    set7DayReword(receiverMoneyEvent)

}

private var isAnim = false

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
            localEvent("billetera_signin")
            daySignSuccess.invoke(reward, view)
            root.post { set7DayReword(daySignSuccess) }
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
    AnimViewHelper.playClaimablePulseAnim(ivSmallMoney1, TaskHelper.canClaimWatchReward(0))
    AnimViewHelper.playClaimablePulseAnim(ivSmallMoney2, TaskHelper.canClaimWatchReward(1))
    AnimViewHelper.playClaimablePulseAnim(ivSmallMoney3, TaskHelper.canClaimWatchReward(2))
    AnimViewHelper.playClaimablePulseAnim(ivBigMoney1, TaskHelper.canClaimWatchReward(3))
    AnimViewHelper.playClaimablePulseAnim(ivBigMoney2, TaskHelper.canClaimWatchReward(4))
    AnimViewHelper.playClaimablePulseAnim(ivBigMoney3, TaskHelper.canClaimWatchReward(5))
    fun clickReward(index: Int, view: ImageView) {
        val reward = TaskHelper.clickWatchReward(index)
        if (reward != null) {
            localEvent("billetera_time")
            watchTimeArriver.invoke(reward, view)
            isAnim = true
            setWatchLayout(context, watchTimeArriver)
        }
    }
    progressView.setProgress((((curIndexReward + 1) / 6.0) * 100).toInt(), isAnim)
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
