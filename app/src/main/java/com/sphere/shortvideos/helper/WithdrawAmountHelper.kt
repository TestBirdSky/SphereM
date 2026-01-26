package com.sphere.shortvideos.helper

import com.sphere.shortvideos.R
import com.sphere.shortvideos.helper.MoneyCacheHelper.fetchCurMoney

/**
 * 提现金额档位与货币换算
 */
object WithdrawAmountHelper {
    private val defLow = WithdrawTier(brl = 240, usd = 48, idr = 720_000)
    private val tiers = listOf(defLow,
        WithdrawTier(brl = 360, usd = 72, idr = 1_080_000),
        WithdrawTier(brl = 480, usd = 96, idr = 1_440_000),
        WithdrawTier(brl = 600, usd = 120, idr = 1_800_000),
        WithdrawTier(brl = 720, usd = 144, idr = 2_160_000))
    const val BRL_UNIT = "R$"
    const val IDR_UNIT = "Rp"
    const val DEFAULT_UNIT = "$"
    const val USD_PER_BRL = 0.2
    const val IDR_PER_BRL = 3000.0
    const val BRL = 1.0

    fun fetchCurMoneyAndWithdrawNeedMoney(): Pair<String, String> {
        val curMoney = fetchCurMoney()
        val curMoneyStr = moneyFormatAddUnit(curMoney)
        val needTagMoney = moneyFormatAddUnit(defLow.fetWithdraw() - curMoney) // 剩余体现的金额
        return Pair(curMoneyStr, needTagMoney)
    }

    fun moneyFormatAddUnit(double: Double): String {
        return "${resolveMoneyUnit()}\t${formatMoney(double)}"
    }

    fun moneyFormatAddUnitWithNoSpace(double: Double): String {
        return "${resolveMoneyUnit()}\t${formatMoney(double)}"
    }

    fun fetchMoneyBankIcon(isBlack: Boolean=false): Int {
        if (LauageTools.isIndonesia()) {
            return if (isBlack) R.drawable.ic_ovo_b else R.drawable.ic_ovo_w
        } else {
            return if (isBlack) R.drawable.ic_pix_b else R.drawable.ic_pix_w
        }
    }

    private fun formatMoney(value: Double): String {
        val locale = if (LauageTools.isIndonesia()) { //99999 -> 99.999
            LauageTools.getLocaleByCountry(LauageTools.CountryCode.INDONESIA)
        } else { //2999.99 --> 2.999,99
            LauageTools.getLocaleByCountry(LauageTools.CountryCode.BRAZIL)
        }
        val formatter = java.text.NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }
        return formatter.format(value)
    }

    private fun resolveMoneyUnit(): String {
        val locale = if (LauageTools.isIndonesia()) {
            LauageTools.getLocaleByCountry(LauageTools.CountryCode.INDONESIA)
        } else {
            LauageTools.getLocaleByCountry(LauageTools.CountryCode.BRAZIL)
        }
        return runCatching {
            java.util.Currency.getInstance(locale).getSymbol(locale)
        }.getOrElse {
            if (LauageTools.isIndonesia()) IDR_UNIT else BRL_UNIT
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

    data class WithdrawTier(val brl: Long, val usd: Long, val idr: Long) {
        fun fetWithdraw(): Double {
            return (if (LauageTools.isIndonesia()) idr else brl).toDouble()
        }
    }


    fun brlToUsd(amountBrl: Long): Long = (amountBrl * USD_PER_BRL).toLong()

    fun brlToIdr(amountBrl: Long): Long = (amountBrl * IDR_PER_BRL).toLong()

    fun usdToBrl(amountUsd: Long): Long = (amountUsd / USD_PER_BRL).toLong()

    fun usdToIdr(amountUsd: Long): Long = brlToIdr(usdToBrl(amountUsd))

    fun idrToBrl(amountIdr: Long): Long = (amountIdr / IDR_PER_BRL).toLong()

    fun idrToUsd(amountIdr: Long): Long = brlToUsd(idrToBrl(amountIdr))
}
