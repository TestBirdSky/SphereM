package com.sphere.shortvideos.helper

import com.sphere.shortvideos.helper.mmkv.MMKVData
import com.sphere.shortvideos.helper.reward.RewardHelper
import com.sphere.shortvideos.logError

/**
 * Date：2026/1/22
 * Describe:
 */
object MoneyCacheHelper {
    private var userNowMoneyBr by MMKVData(0.0) //用户现在有多少钱默认存的是巴西币,需要去兑换
    var watchVideoTime by MMKVData(0L) // 观看时长

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

    fun addNewUserGift() {
        addMoney(RewardHelper.getConfigByLanguage().getNewUserGiftBr())
    }

    fun fetchPushReward(): Pair<Double, String> {
        return RewardHelper.getConfigByLanguage().getNotificationRewardMoney(userNowMoneyBr)
    }

    //    fun fetchPushRewardText(): String {
    //
    //    }

    // 存储是巴西币，进来后需要进行换算
    @Synchronized
    fun addMoney(value: Double) {
        userNowMoneyBr += value
    }

    @Synchronized
    fun addNotExchangeMoney(value: Double) {
        if (LauageTools.isIndonesia()) {
            userNowMoneyBr += value / WithdrawAmountHelper.IDR_PER_BRL
        } else {
            userNowMoneyBr += value
        }
    }
}