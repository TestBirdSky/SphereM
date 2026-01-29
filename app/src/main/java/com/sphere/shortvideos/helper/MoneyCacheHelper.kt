package com.sphere.shortvideos.helper

import com.sphere.shortvideos.helper.mmkv.MMKVData
import com.sphere.shortvideos.helper.reward.RewardHelper
import com.sphere.shortvideos.logError

/**
 * Date：2026/1/22
 * Describe:
 */
object MoneyCacheHelper {
    var userFetchMoneyBr by MMKVData(0.0) //用户现在有多少钱默认存的是巴西币,需要去兑换


    fun fetchPushReward(): Pair<Double, String> {
        return RewardHelper.getConfigByLanguage().getNotificationRewardMoney(userFetchMoneyBr)
    }

    fun fetchRvAdReward(): Pair<Double, String> {
        return RewardHelper.getConfigByLanguage().getRvRewardMoney(userFetchMoneyBr)
    }

    fun fetchWatchVideoReward(): Double {
        return RewardHelper.getConfigByLanguage().getMoneyVideoIconReward(userFetchMoneyBr)
    }


    fun fetchCurMoney(): Double {
        var mo = userFetchMoneyBr
        if (LauageTools.isIndonesia()) {
            mo *= WithdrawAmountHelper.IDR_PER_BRL
        }
        return mo
    }

    fun fetchCurMoneyBr(): Double {
        return userFetchMoneyBr
    }

    // 存储是巴西币，进来后需要进行换算
    @Synchronized
    fun addMoney(value: Double) {
        userFetchMoneyBr += value
    }

    @Synchronized
    fun addNotExchangeMoney(value: Double) {
        logError("000>addNotExchangeMoney$value")
        if (LauageTools.isIndonesia()) {
            userFetchMoneyBr += value / WithdrawAmountHelper.IDR_PER_BRL
        } else {
            userFetchMoneyBr += value
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
            val mo = userFetchMoneyBr
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