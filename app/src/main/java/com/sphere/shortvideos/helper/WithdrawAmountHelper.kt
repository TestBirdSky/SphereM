package com.sphere.shortvideos.helper

import com.chartboost.sdk.impl.fa
import com.sphere.shortvideos.R
import com.sphere.shortvideos.helper.MoneyCacheHelper.fetchCurMoney
import com.sphere.shortvideos.logError

/**
 * 提现金额档位与货币换算
 * 提现平台按地区显示：巴西 PIX/PagBank/PayPal，美国 PayPal/Cash App/Google Pay，印尼 DANA/OVO/GoPay
 */
object WithdrawAmountHelper {

    /** 单个提现方式：唯一 id + 展示名 + 选中/未选中图标 */
    data class WithdrawPaymentMethod(
        val id: String,
        val name: String,
        val iconSelected: Int,
        val iconNormal: Int,
    )

    /**
     * 按当前地区返回提现平台列表（用于横向列表与跑马灯文案）
     * 巴西：PIX，PagBank，PayPal
     * 美国（英语）：PayPal，Cash App，Google Pay
     * 印尼：DANA，OVO，GoPay
     */
    fun fetchWithdrawPaymentMethods(): List<WithdrawPaymentMethod> {
        return when {
            LauageTools.isBrazil() -> listOf(
                WithdrawPaymentMethod("pix", "PIX", R.drawable.ic_pix_w, R.drawable.ic_pix_b),
                WithdrawPaymentMethod("pagbank", "PagBank", R.drawable.ic_pagbank_w, R.drawable.ic_pagbank_b),
                WithdrawPaymentMethod("paypal", "PayPal", R.drawable.ic_paypal_w, R.drawable.ic_paypal_b),
            )

            LauageTools.isEnglish() -> listOf(
                WithdrawPaymentMethod("paypal", "PayPal", R.drawable.ic_paypal_w, R.drawable.ic_paypal_b),
                WithdrawPaymentMethod("cash_app", "Cash App", R.drawable.ic_cashapp_w, R.drawable.ic_cashapp_b),
                WithdrawPaymentMethod("google_pay", "Google Pay", R.drawable.ic_pay_w, R.drawable.ic_pay_b),
            )

            LauageTools.isIndonesia() -> listOf(
                WithdrawPaymentMethod("dana", "DANA", R.drawable.ic_daa_w, R.drawable.ic_daa_b),
                WithdrawPaymentMethod("ovo", "OVO", R.drawable.ic_ovo_w, R.drawable.ic_ovo_b),
                WithdrawPaymentMethod("gopay", "GoPay", R.drawable.ic_gopay_w, R.drawable.ic_gopay_b),
            )

            else -> listOf(
                WithdrawPaymentMethod("paypal", "PayPal", R.drawable.ic_paypal_w, R.drawable.ic_paypal_b),
                WithdrawPaymentMethod("cash_app", "Cash App", R.drawable.ic_cashapp_w, R.drawable.ic_cashapp_b),
                WithdrawPaymentMethod("google_pay", "Google Pay", R.drawable.ic_pay_w, R.drawable.ic_pay_b),
            )
        }
    }

    /** 按 id 查找当前地区可用的提现方式（id 不区分大小写） */
    fun findWithdrawPaymentMethodById(id: String): WithdrawPaymentMethod {
        val list = fetchWithdrawPaymentMethods()
        return fetchWithdrawPaymentMethods().firstOrNull { it.id.equals(id, ignoreCase = true) }
            ?: list.first()
    }

    private val defLow = WithdrawTier(brl = 240, usd = 48, idr = 720_000)
    private val tiers = listOf(defLow,
        WithdrawTier(brl = 360, usd = 72, idr = 1_080_000),
        WithdrawTier(brl = 480, usd = 96, idr = 1_440_000),
        WithdrawTier(brl = 600, usd = 120, idr = 1_800_000),
        WithdrawTier(brl = 720, usd = 144, idr = 2_160_000))
    const val BRL_UNIT = "R$"
    const val IDR_UNIT = "Rp"
    const val DEFAULT_UNIT = "$"

    /** 美元为 1 倍率（基准） */
    const val USD_BASE = 1.0

    /** 1 USD 换算成 BRL 的倍率 */
    const val BRL_PER_USD = 5.0

    /** 1 USD 换算成 IDR 的倍率 */
    const val IDR_PER_USD = 15000.0

    /** 1 BRL = x USD（由倍率反推） */
    const val USD_PER_BRL = USD_BASE / BRL_PER_USD

    /** 1 BRL = x IDR（由倍率反推） */
    const val IDR_PER_BRL = IDR_PER_USD / BRL_PER_USD

    fun fetchCurMoneyAndWithdrawNeedMoney(): Pair<String, String> {
        val curMoney = fetchCurMoney()
        val curMoneyStr = moneyFormatAddUnit(curMoney)
        val needTagMoney =
            moneyFormatAddUnitWithNoSpace((defLow.fetWithdraw() - curMoney).coerceAtLeast(0.0)) // 剩余体现的金额
        return Pair(curMoneyStr, needTagMoney)
    }

    fun moneyFormatAddUnit(double: Double): String { //        logError("moneyFormatAddUnit-->${resolveMoneyUnit()}")
        return "${resolveMoneyUnit()}\t${formatMoney(double)}"
    }

    fun moneyFormatAddUnitWithNoSpace(double: Double): String {
        return "${resolveMoneyUnit()}${formatMoney(double)}"
    }

    /** 顶部/其他处展示的“当前地区默认提现方式”图标 */
    fun fetchMoneyBankIcon(isBlack: Boolean = false): Int {
        return when {
            LauageTools.isIndonesia() -> if (isBlack) R.drawable.ic_daa_b else R.drawable.ic_daa_w
            LauageTools.isBrazil() -> if (isBlack) R.drawable.ic_pix_b else R.drawable.ic_pix_w
            LauageTools.isEnglish() -> if (isBlack) R.drawable.ic_paypal_b else R.drawable.ic_paypal_w
            else -> if (isBlack) R.drawable.ic_paypal_b else R.drawable.ic_paypal_w
        }
    }

    //格式化金钱数据
    fun formatMoney(value: Double): String {
        val locale = LauageTools.getAppLocale()
        val formatter = java.text.NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }
        return formatter.format(value)
    }

    fun resolveMoneyUnit(): String {
        return when {
            LauageTools.isIndonesia() -> IDR_UNIT
            LauageTools.isBrazil() -> BRL_UNIT
            else -> DEFAULT_UNIT
        }
    }

    fun fetchGetMoneyProgress(): Int {
        val pa = fetchCurMoneyAndGetMoneyMinValue()
        return ((pa.first / pa.second) * 100).toInt()
    }

    fun fetchCurMoneyAndGetMoneyMinValue(): Pair<Double, Double> {
        val mo = MoneyCacheHelper.fetchCurMoney()
        val total = defLow.fetWithdraw()
        return Pair(mo, total.toDouble())
    }

    fun fetchWithdrawMinMoney(): String {
        val total = defLow.fetWithdraw()
        return moneyFormatAddUnitWithNoSpace(total)
    }

    fun fetchWithdrawMinMoneyDouble(): Double {
        return defLow.fetWithdraw()
    }

    fun isCanWithdraw(): Boolean {
        if (fetchCurMoney() >= defLow.fetWithdraw()) {
            return true
        }
        return false
    }

    fun fetchWithdrawAmounts(): List<Double> {
        return tiers.map { it.fetWithdraw() }
    }

    data class WithdrawTier(val brl: Long, val usd: Long, val idr: Long) {
        fun fetWithdraw(): Double {
            return when {
                LauageTools.isIndonesia() -> idr.toDouble()
                LauageTools.isBrazil() -> brl.toDouble()
                else -> usd.toDouble()
            }
        }
    }


    fun brlToUsd(amountBrl: Long): Long = (amountBrl * USD_PER_BRL).toLong()

    fun brlToIdr(amountBrl: Long): Long = (amountBrl * IDR_PER_BRL).toLong()

    fun usdToBrl(amountUsd: Long): Long = (amountUsd / USD_PER_BRL).toLong()

    fun usdToIdr(amountUsd: Long): Long = brlToIdr(usdToBrl(amountUsd))

    fun idrToBrl(amountIdr: Long): Long = (amountIdr / IDR_PER_BRL).toLong()

    fun idrToUsd(amountIdr: Long): Long = brlToUsd(idrToBrl(amountIdr))
}
