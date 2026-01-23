package com.sphere.shortvideos.helper

/**
 * 提现金额档位与货币换算
 */
object WithdrawAmountHelper {
    var userType = 0 //巴西

    const val USD_PER_BRL = 0.2
    const val IDR_PER_BRL = 3000.0
    const val BRL = 1.0

    data class WithdrawTier(val brl: Long, val usd: Long, val idr: Long)

    private val tiers = listOf(WithdrawTier(brl = 240, usd = 48, idr = 720_000),
        WithdrawTier(brl = 360, usd = 72, idr = 1_080_000),
        WithdrawTier(brl = 480, usd = 96, idr = 1_440_000),
        WithdrawTier(brl = 600, usd = 120, idr = 1_800_000),
        WithdrawTier(brl = 720, usd = 144, idr = 2_160_000))

    fun brlToUsd(amountBrl: Long): Long = (amountBrl * USD_PER_BRL).toLong()

    fun brlToIdr(amountBrl: Long): Long = (amountBrl * IDR_PER_BRL).toLong()

    fun usdToBrl(amountUsd: Long): Long = (amountUsd / USD_PER_BRL).toLong()

    fun usdToIdr(amountUsd: Long): Long = brlToIdr(usdToBrl(amountUsd))

    fun idrToBrl(amountIdr: Long): Long = (amountIdr / IDR_PER_BRL).toLong()

    fun idrToUsd(amountIdr: Long): Long = brlToUsd(idrToBrl(amountIdr))
}
