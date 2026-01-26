package com.sphere.shortvideos.baseui

import android.app.Activity
import android.graphics.Color
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.sphere.shortvideos.R
import com.sphere.shortvideos.databinding.LayoutMoneyTopViewBinding
import com.sphere.shortvideos.databinding.LayoutTaskChildBinding
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
    ivPTag.setBackgroundResource(WithdrawAmountHelper.fetchGetMoneyProgress())
    layout.setOnClickListener {

    }
}

fun LayoutTaskChildBinding.setTaskInfo() {
    ivWatchAd.setOnClickListener {
        // todo 显示广告
    }
    bgH5.setOnClickListener {
        // todo 显示H5
    }

}