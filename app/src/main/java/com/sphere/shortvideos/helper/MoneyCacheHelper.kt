package com.sphere.shortvideos.helper

import com.sphere.shortvideos.helper.mmkv.MMKVData
import com.sphere.shortvideos.helper.reward.RewardHelper
import com.sphere.shortvideos.helper.withdraw.WithdrawalActionHelper
import com.sphere.shortvideos.isDebugMode
import com.sphere.shortvideos.logError
import com.sphere.shortvideos.mApp
import com.sphere.shortvideos.notification.NotificationHelper

/**
 * Date：2026/1/22
 * Describe: 沿用原 key 存金额，现为美元(USD)，巴西/印尼按汇率换算
 */
object MoneyCacheHelper {
    /** 当前余额，沿用原 key，存的是美元(USD) */
    private var userFetchMoneyUSD by MMKVData(0.0)
    private var historyMoneyUSD by MMKVData(0.0)


    fun fetchPushReward(): Pair<Double, String> {
        return RewardHelper.getConfigByLanguage().getNotificationRewardMoney(fetchCurMoney())
    }

    fun fetchRvAdReward(): Pair<Double, String> {
        return RewardHelper.getConfigByLanguage().getRvRewardMoney(fetchCurMoney())
    }

    fun fetchWatchVideoReward(): Double {
        if (isDebugMode) { // todo remove
            return WithdrawAmountHelper.fetchWithdrawMinMoneyDouble() / 5
        }
        return RewardHelper.getConfigByLanguage().getMoneyVideoIconReward(fetchCurMoney())
    }

    /** 当前余额（展示用）：英语=USD，巴西=BRL，印尼=IDR */
    fun fetchCurMoney(): Double {
        val usd = userFetchMoneyUSD
        return usdToShowMoneyD(usd)
    }

    @Synchronized
    fun addNotExchangeMoney(value: Double) {
        logError("000>addNotExchangeMoney$value")
        val v = when {
            LauageTools.isIndonesia() -> value / WithdrawAmountHelper.IDR_PER_USD
            LauageTools.isBrazil() -> value / WithdrawAmountHelper.BRL_PER_USD
            else -> value
        }
        userFetchMoneyUSD += v
        historyMoneyUSD += v
        NotificationHelper.showOrUpdateNotificationService(mApp)
    }

    @Synchronized
    fun reduceNotExchangeMoney(value: Double) {
        if (value <= 0) return
        val reduceUsd = showMoneyToUsdMoney(value)
        userFetchMoneyUSD = (userFetchMoneyUSD - reduceUsd).coerceAtLeast(0.0)
        NotificationHelper.showOrUpdateNotificationService(mApp)
    }

    fun showMoneyToUsdMoney(value: Double): Double {
        return when {
            LauageTools.isIndonesia() -> value / WithdrawAmountHelper.IDR_PER_USD
            LauageTools.isBrazil() -> value / WithdrawAmountHelper.BRL_PER_USD
            else -> value
        }
    }

    fun usdToShowMoneyD(usd: Double): Double {
        return when {
            LauageTools.isIndonesia() -> usd * WithdrawAmountHelper.IDR_PER_USD
            LauageTools.isBrazil() -> usd * WithdrawAmountHelper.BRL_PER_USD
            else -> usd
        }
    }

    var watchVideoTime by MMKVData(0L) // 观看时长
        private set

    private var time = 0L
    fun startWatchVideo() {
        logError("startWatchVideo-->$watchVideoTime")
        time = System.currentTimeMillis()
    }

    fun stopWatchVideo() {
        if (time <= 0L) return
        val now = System.currentTimeMillis()
        if (now > time) {
            val time = (now - time)
            WithdrawalActionHelper.addTask1WatchTime((time / 1000).toInt())
            watchVideoTime += time
        }
        logError("stopWatchVideo-->$watchVideoTime")
        time = 0L
    }

    fun addWatchVideoTime(durationMs: Long) {
        if (durationMs <= 0L) return
        watchVideoTime += durationMs
    }

    private val listWatchValue = ArrayList<Pair<Double, String>>()

    fun fetchAllWatchReword(): List<Pair<Double, String>> {
        if (listWatchValue.isEmpty()) {
            val mo = fetchCurMoney()
            val conf = RewardHelper.getConfigByLanguage()
            listWatchValue.add(conf.getDramaTime1Reward(mo))
            listWatchValue.add(conf.getDramaTime2Reward(mo))
            listWatchValue.add(conf.getDramaTime3Reward(mo))
            listWatchValue.add(conf.getDramaTime4Reward(mo))
            listWatchValue.add(conf.getDramaTime5Reward(mo))
            listWatchValue.add(conf.getDramaTime6Reward(mo))
        }
        return listWatchValue
    }

}