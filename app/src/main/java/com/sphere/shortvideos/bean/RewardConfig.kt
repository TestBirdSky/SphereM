package com.sphere.shortvideos.bean

import com.google.gson.annotations.SerializedName
import com.sphere.shortvideos.helper.WithdrawAmountHelper

/**
 * Date：2026/1/22
 * Describe: BR 奖励配置
 */
data class RewardConfig(@SerializedName("money_newuser_gift") val moneyNewuserGift: RewardValue,

                        @SerializedName("money_video_icon") val moneyVideoIcon: List<RewardRange>,

                        @SerializedName("money_push") val moneyPush: List<RewardRange>,

                        @SerializedName("drama_time_1") val dramaTime1: List<RewardRange>,

                        @SerializedName("drama_time_2") val dramaTime2: List<RewardRange>,

                        @SerializedName("drama_time_3") val dramaTime3: List<RewardRange>,

                        @SerializedName("drama_time_4") val dramaTime4: List<RewardRange>,

                        @SerializedName("drama_time_5") val dramaTime5: List<RewardRange>,

                        @SerializedName("drama_time_6") val dramaTime6: List<RewardRange>,

                        @SerializedName("task_pop") val taskPop: List<RewardRange>,

                        @SerializedName("sign_in") val signIn: List<RewardRange>,

                        @SerializedName("rv_video") val rvVideo: List<RewardRange>,

                        @SerializedName("ad_interval") val adInterval: List<Int>) {
    var moneyRate = 1.0 //价钱换算默认巴西币

    fun getRewardNewUser(): Pair<Double, String> {
        return Pair(moneyNewuserGift.reward, "+${WithdrawAmountHelper.moneyFormatAddUnit(moneyNewuserGift.reward)}")
    }

    // 传进来的是巴西币
    fun getNotificationRewardMoney(moneyBr: Double): Pair<Double, String> {
        val m = moneyPush.firstOrNull {
            it.isInRange(moneyBr * moneyRate)
        }?.reward?.random() ?: 0.0
        return Pair(m, WithdrawAmountHelper.moneyFormatAddUnit(m))
    }

    // getVideo
    fun getRvRewardMoney(moneyBr: Double): Pair<Double, String> {
        return buildReward(rvVideo, moneyBr, true)
    }

    fun getMoneyVideoIconReward(moneyBr: Double, withPlus: Boolean = true): Pair<Double, String> {
        return buildReward(moneyVideoIcon, moneyBr, withPlus)
    }

    fun getDramaTime1Reward(moneyBr: Double): Pair<Double, String> {
        return buildReward(dramaTime1, moneyBr, false, false)
    }

    fun getDramaTime2Reward(moneyBr: Double): Pair<Double, String> {
        return buildReward(dramaTime2, moneyBr, false, false)
    }

    fun getDramaTime3Reward(moneyBr: Double): Pair<Double, String> {
        return buildReward(dramaTime3, moneyBr, false, false)
    }

    fun getDramaTime4Reward(moneyBr: Double): Pair<Double, String> {
        return buildReward(dramaTime4, moneyBr, false, false)
    }

    fun getDramaTime5Reward(moneyBr: Double): Pair<Double, String> {
        return buildReward(dramaTime5, moneyBr, false, false)
    }

    fun getDramaTime6Reward(moneyBr: Double): Pair<Double, String> {
        return buildReward(dramaTime6, moneyBr, false, false)
    }

    fun getTaskPopReward(moneyBr: Double, withPlus: Boolean = true): Pair<Double, String> {
        return buildReward(taskPop, moneyBr, withPlus)
    }

    fun getSignInReward(moneyBr: Double, withPlus: Boolean = true): Pair<Double, String> {
        return buildReward(signIn, moneyBr, withPlus)
    }

    private fun buildReward(
        list: List<RewardRange>,
        moneyBr: Double,
        withPlus: Boolean,
        withSpace: Boolean = true
    ): Pair<Double, String> {
        val m = list.firstOrNull {
            it.isInRange(moneyBr * moneyRate)
        }?.reward?.random() ?: 0.0
        val formatted = if (withSpace) {
            WithdrawAmountHelper.moneyFormatAddUnit(m)
        } else {
            WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(m)
        }
        return if (withPlus) Pair(m, "+$formatted") else Pair(m, formatted)
    }
}

data class RewardValue(@SerializedName("reward") val reward: Double)

data class RewardRange(@SerializedName("min") val min: Int,

                       @SerializedName("max") val max: Int?,

                       @SerializedName("reward") val reward: List<Double>) {

    fun isInRange(num: Double): Boolean {
        if (num >= min) {
            if (num < (max ?: Int.MAX_VALUE)) {
                return true
            }
        }
        return false
    }

}
