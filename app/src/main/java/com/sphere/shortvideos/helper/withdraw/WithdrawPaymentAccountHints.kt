package com.sphere.shortvideos.helper.withdraw

import com.sphere.shortvideos.R
import com.sphere.shortvideos.helper.LauageTools
import com.sphere.shortvideos.mApp

/**
 * 提现「收款账户」输入：标题（账户类型说明）与 Hint，随地区 + 所选渠道变化。
 * 文案按产品表格英文化；韩国渠道未接入故不包含。
 *
 * **范围（现阶段）**：仅按 [com.sphere.shortvideos.helper.LauageTools] 三类地区（巴西 / 印尼 / 其余英语默认区）+
 * [com.sphere.shortvideos.helper.WithdrawAmountHelper.fetchWithdrawPaymentMethods] 中**已有渠道列表**维护；泰国、西语专用渠道等
 * 待产品接入对应列表后再扩展本文件。
 */
object WithdrawPaymentAccountHints {

    data class FieldCopy(val label: String, val hint: String)

    fun forMethod(methodName: String): FieldCopy {
        return when {
            LauageTools.isBrazil() -> brazilCopy(methodName)
            LauageTools.isIndonesia() -> indonesiaCopy(methodName)
            else -> englishDefaultCopy(methodName)
        }
    }

    private fun brazilCopy(methodName: String): FieldCopy = when (methodName) {
        "PIX" -> FieldCopy(
            label = mApp.getString(R.string.withdraw_account_label_cpf_email_mobile_random),
            hint = "E.g. 12345678901"
        )

        "PagBank" -> FieldCopy(
            label = mApp.getString(R.string.withdraw_account_label_email_or_cpf),
            hint = "E.g. user@email.com"
        )

        "PayPal" -> FieldCopy(
            label = mApp.getString(R.string.withdraw_account_label_email_or_mobile),
            hint = "E.g. user@email.com"
        )

        else -> defaultCopy()
    }

    private fun indonesiaCopy(methodName: String): FieldCopy = when (methodName) {
        "DANA", "OVO", "GoPay" -> {
            FieldCopy(
                label = mApp.getString(R.string.withdraw_account_label_mobile_number),
                hint = "E.g. 081234567890"
            )
        }

        else -> defaultCopy()
    }

    private fun englishDefaultCopy(methodName: String): FieldCopy = when (methodName) {
        "PayPal" -> FieldCopy(
            label = mApp.getString(R.string.withdraw_account_label_email_or_mobile),
            hint = "E.g. user@gmail.com"
        )

        "Cash App" -> FieldCopy(
            label = mApp.getString(R.string.withdraw_account_label_cashtag_email_or_mobile),
            hint = "E.g. 1234567890"
        )

        "Google Pay" -> FieldCopy(
            label = mApp.getString(R.string.withdraw_account_label_gmail_or_mobile),
            hint = "E.g. example@gmail.com"
        )

        else -> defaultCopy()
    }

    private fun defaultCopy() = FieldCopy(
        label = mApp.getString(R.string.withdraw_account_label_email_or_mobile),
        hint = ""
    )
}