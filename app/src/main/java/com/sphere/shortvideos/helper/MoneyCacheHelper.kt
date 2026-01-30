package com.sphere.shortvideos.helper

import com.sphere.shortvideos.helper.mmkv.MMKVData
import com.sphere.shortvideos.helper.reward.RewardHelper
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

    fun fetchPushReward(): Pair<Double, String> {
        return RewardHelper.getConfigByLanguage().getNotificationRewardMoney(fetchCurMoney())
    }

    fun fetchRvAdReward(): Pair<Double, String> {
        return RewardHelper.getConfigByLanguage().getRvRewardMoney(fetchCurMoney())
    }

    fun fetchWatchVideoReward(): Double {
        return RewardHelper.getConfigByLanguage().getMoneyVideoIconReward(fetchCurMoney())
    }

    /** 当前余额（展示用）：英语=USD，巴西=BRL，印尼=IDR */
    fun fetchCurMoney(): Double {
        val usd = userFetchMoneyUSD
        return when {
            LauageTools.isIndonesia() -> usd * WithdrawAmountHelper.IDR_PER_USD
            LauageTools.isBrazil() -> usd * WithdrawAmountHelper.BRL_PER_USD
            else -> usd
        }
    }

    @Synchronized
    fun addNotExchangeMoney(value: Double) {
        logError("000>addNotExchangeMoney$value")
        NotificationHelper.showOrUpdateNotificationService(mApp)
        userFetchMoneyUSD += when {
            LauageTools.isIndonesia() -> value / WithdrawAmountHelper.IDR_PER_USD
            LauageTools.isBrazil() -> value / WithdrawAmountHelper.BRL_PER_USD
            else -> value
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
            watchVideoTime += (now - time)
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