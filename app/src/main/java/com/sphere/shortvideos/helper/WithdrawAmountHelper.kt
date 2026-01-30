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
        val needTagMoney = moneyFormatAddUnitWithNoSpace(defLow.fetWithdraw() - curMoney) // 剩余体现的金额
        return Pair(curMoneyStr, needTagMoney)
    }

    fun moneyFormatAddUnit(double: Double): String {
        return "${resolveMoneyUnit()}\t${formatMoney(double)}"
    }

    fun moneyFormatAddUnitWithNoSpace(double: Double): String {
        return "${resolveMoneyUnit()}${formatMoney(double)}"
    }

    fun fetchMoneyBankIcon(isBlack: Boolean = false): Int {
        return when {
            LauageTools.isIndonesia() -> if (isBlack) R.drawable.ic_ovo_b else R.drawable.ic_ovo_w
            LauageTools.isBrazil() -> if (isBlack) R.drawable.ic_pix_b else R.drawable.ic_pix_w
            else -> if (isBlack) R.drawable.ic_pix_b else R.drawable.ic_pix_w
        }
    }

    fun formatMoney(value: Double): String {
        val locale = LauageTools.getAppLocale()
        val formatter = java.text.NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }
        return formatter.format(value)
    }

    private fun resolveMoneyUnit(): String {
        val locale = LauageTools.getAppLocale()
        return runCatching {
            java.util.Currency.getInstance(locale).getSymbol(locale)
        }.getOrElse {
            when {
                LauageTools.isIndonesia() -> IDR_UNIT
                LauageTools.isBrazil() -> BRL_UNIT
                else -> DEFAULT_UNIT
            }
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
