package com.sphere.shortvideos.helper.withdraw

import com.sphere.shortvideos.helper.LauageTools

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
            label = "CPF / Email / Mobile Number / Random Key",
            hint = "E.g. 12345678901"
        )

        "PagBank" -> FieldCopy(
            label = "Email / CPF",
            hint = "E.g. user@email.com"
        )

        "PayPal" -> FieldCopy(
            label = "Email / Mobile Number",
            hint = "E.g. user@email.com"
        )

        else -> defaultCopy()
    }

    /**
     * TODO(产品/你方补充)：印尼 DANA / OVO / GoPay 的「账户类型」标题与 Hint 以正式表格为准；
     * 当前为占位文案，后续请在此方法内替换。
     */
    private fun indonesiaCopy(methodName: String): FieldCopy = when (methodName) {
        "DANA", "OVO", "GoPay" -> {
            FieldCopy(
                label = "Mobile Number",
                hint = "E.g. 081234567890"
            )
        }

        else -> defaultCopy()
    }

    private fun englishDefaultCopy(methodName: String): FieldCopy = when (methodName) {
        "PayPal" -> FieldCopy(
            label = "Email / Mobile Number",
            hint = "E.g. user@gmail.com"
        )

        "Cash App" -> FieldCopy(
            label = "\$Cashtag / Email / Mobile Number",
            hint = "E.g. 1234567890"
        )

        "Google Pay" -> FieldCopy(
            label = "Gmail / Mobile Number",
            hint = "E.g. example@gmail.com"
        )

        else -> defaultCopy()
    }

    private fun defaultCopy() = FieldCopy(
        label = "Email / Mobile Number",
        hint = "Enter your account"
    )
}