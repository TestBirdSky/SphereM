package com.sphere.shortvideos.helper.withdraw

import android.content.Intent
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.AbsoluteSizeSpan
import android.view.View
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.mbridge.msdk.foundation.fragment.BaseFragment
import com.sphere.shortvideos.R
import com.sphere.shortvideos.activity.MainActivity
import com.sphere.shortvideos.baseui.GenericFragment
import com.sphere.shortvideos.databinding.LayoutWithdrawalActionBinding
import com.sphere.shortvideos.dialogs.withdraw.InsufficientRevDialogFragment
import com.sphere.shortvideos.dialogs.withdraw.MakingMoneyDialogFragment
import com.sphere.shortvideos.dialogs.withdraw.PaymentInformationDialogFragment
import com.sphere.shortvideos.dialogs.withdraw.WithdrawalInfoDialogFragment
import com.sphere.shortvideos.helper.DialogFragmentDisplayHelper
import com.sphere.shortvideos.helper.LauageTools
import com.sphere.shortvideos.helper.MoneyCacheHelper
import com.sphere.shortvideos.helper.WithdrawAmountHelper
import com.sphere.shortvideos.helper.localEvent
import com.sphere.shortvideos.view.setColorText
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.math.RoundingMode

/**
 * [R.layout.layout_withdrawal_action] 专用：金额输入、一键填满余额、最低提现提示，并驱动外层提现按钮可用状态与提交校验。
 */
class LayoutWithdrawalActionController(
    private val fragment: GenericFragment<*>,
    private val binding: LayoutWithdrawalActionBinding,
) {
    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            onInputChanged?.invoke()
        }
    }

    private var onInputChanged: (() -> Unit)? = null

    private val host get() = fragment.requireContext()

    var taskEvent: () -> Unit = {}

    /**
     * @param mainWithdrawButton 外层 [fragment_wallte] 的提现按钮（无有效输入时应不可点）
     */
    fun attach(mainWithdrawButton: View) {
        onInputChanged = { updateMainWithdrawButton(mainWithdrawButton) }

        binding.tvUnit.text = resolveUnitSymbol()
        refreshMinTips()
        applyHintTextSize()

        binding.etMoney.removeTextChangedListener(textWatcher)
        binding.etMoney.addTextChangedListener(textWatcher)

        binding.btnWithAll.setOnClickListener {
            localEvent("withdrawal_all")
            val cur = MoneyCacheHelper.fetchCurMoney()
            binding.etMoney.setText(formatInputAmountNoRoundUp(cur))
            binding.etMoney.setSelection(binding.etMoney.text?.length ?: 0)
        }

        binding.etMoney.isFocusableInTouchMode = true
        binding.etMoney.isCursorVisible = true

        updateMainWithdrawButton(mainWithdrawButton)
    }

    fun detach() {
        binding.etMoney.removeTextChangedListener(textWatcher)
        onInputChanged = null
    }

    /** 余额等变化时刷新最低提现说明 */
    fun refreshMinTips() {
        val minShown = WithdrawAmountHelper.fetchWithdrawMinMoney()
        val full = host.getString(R.string.for_security_the_min_with, minShown)
        binding.tvTips.setColorText(full, minShown, "#32C752".toColorInt())
    }

    fun refreshMainWithdrawButton(mainWithdrawButton: View) {
        updateMainWithdrawButton(mainWithdrawButton)
    }

    /**
     * @return true 表示已处理（弹窗或 Toast）；false 表示不应发生（按钮应处于 disabled）
     */
    fun handleMainWithdrawClick(fragmentManager: FragmentManager): Boolean {
        val input = binding.etMoney.text?.toString()?.trim().orEmpty()
        if (input.isEmpty()) return false

        val amount = parseAmountNumber(input) ?: return false
        val pair = WithdrawAmountHelper.fetchCurMoneyAndGetMoneyMinValue()
        val balance = pair.first
        val minWithdraw = pair.second
        val jumpForyou = {
            val act = fragment.activity
            if (act != null) {
                if (act is MainActivity) {
                    act.jumpToVideoTab()
                } else {
                    act.startActivity(Intent(act, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    })
                }
            }
        }
        if (amount + EPSILON < minWithdraw) {
            DialogFragmentDisplayHelper.show(fragmentManager, MakingMoneyDialogFragment().apply {
                onClaim = {
                    jumpForyou()
                }
            }, "making_moneying")
            return true
        }
        if (amount - EPSILON > balance) {
            DialogFragmentDisplayHelper.show(fragmentManager, InsufficientRevDialogFragment().apply {
                onClaim = {
                    jumpForyou()
                }
            })
            return true
        }

        WithdrawalActionHelper.withdrawalValue = amount
        if (WithdrawalActionHelper.havaBaseInfo()) {
            showInfoDialog(fragmentManager)
        } else {
            PaymentInformationDialogFragment().apply {
                subClick = {
                    showInfoDialog(fragmentManager)
                }
            }.show(fragmentManager, "sss")
        }
        return true
    }

    private fun showInfoDialog(fragmentManager: FragmentManager) {
        localEvent("withdrawal_Initiate")
        DialogFragmentDisplayHelper.show(fragmentManager, WithdrawalInfoDialogFragment().apply {
            onAction = {
                taskEvent()
            }
        }, "info_show")
    }

    private fun updateMainWithdrawButton(mainWithdrawButton: View) {
        val hasInput = binding.etMoney.text?.toString()?.trim().orEmpty().isNotEmpty()
        mainWithdrawButton.isEnabled = hasInput
        mainWithdrawButton.setBackgroundResource(
            if (hasInput) R.drawable.shape_bg_ye else R.drawable.shape_bg_withdraw_disabled,
        )
    }

    private fun resolveUnitSymbol(): String = WithdrawAmountHelper.resolveMoneyUnit()

    /** EditText 输入 16sp（XML），hint 单独 11sp */
    private fun applyHintTextSize() {
        val hintText = host.getString(R.string.enter_withdrawal_amount)
        val span = SpannableString(hintText).apply {
            setSpan(AbsoluteSizeSpan(11, true), 0, length, 0)
        }
        binding.etMoney.hint = span
    }

    /**
     * “全部提现”输入框展示值：按本地化格式，但不进位，避免显示值大于真实余额。
     */
    private fun formatInputAmountNoRoundUp(value: Double): String {
        val formatter = NumberFormat.getNumberInstance(LauageTools.getAppLocale()).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
            roundingMode = RoundingMode.DOWN
        }
        return formatter.format(value)
    }

    companion object {
        private const val EPSILON = 1e-6

        /** 仅解析用户输入的数值（不含货币符号） */
        fun parseAmountNumber(raw: String): Double? {
            val locale = LauageTools.getAppLocale()
            val symbols = DecimalFormatSymbols.getInstance(locale)
            val localeDecimal = symbols.decimalSeparator
            val localeGrouping = symbols.groupingSeparator

            // 去掉货币符号与空白，仅保留数字和常见分隔符
            val compact = raw.trim().replace(Regex("[\\s\\u00A0]"), "")
                .filter { it.isDigit() || it == ',' || it == '.' || it == '-' || it == localeDecimal || it == localeGrouping }
            if (compact.isEmpty()) return null

            val commaIndex = compact.lastIndexOf(',')
            val dotIndex = compact.lastIndexOf('.')
            val decimalChar = when {
                commaIndex >= 0 && dotIndex >= 0 -> if (commaIndex > dotIndex) ',' else '.'
                commaIndex >= 0 -> if (localeDecimal == ',') ',' else null
                dotIndex >= 0 -> if (localeDecimal == '.') '.' else null
                else -> null
            }

            val normalized = buildString(compact.length) {
                compact.forEachIndexed { index, ch ->
                    when {
                        ch.isDigit() || (index == 0 && ch == '-') -> append(ch)
                        decimalChar != null && ch == decimalChar -> append('.')
                        else -> { // 分组符号等直接忽略
                        }
                    }
                }
            }
            if (normalized.isEmpty() || normalized == "-" || normalized == ".") return null
            return normalized.toDoubleOrNull()
        }
    }
}
