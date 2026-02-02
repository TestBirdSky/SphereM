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
                        @SerializedName("exit_reward") val exitReward: List<RewardRange>,

                        @SerializedName("ad_interval") val adInterval: List<Int>) {
    fun getRewardNewUser(): Pair<Double, String> {
        return Pair(moneyNewuserGift.reward, "+${WithdrawAmountHelper.moneyFormatAddUnit(moneyNewuserGift.reward)}")
    }

    // 传进来的是换算后的钱；money 为 0 时使用列表中 min 最小的区间
    fun getNotificationRewardMoney(money: Double): Pair<Double, String> {
        val m = pickRewardFromRanges(moneyPush, money)
        return Pair(m, WithdrawAmountHelper.moneyFormatAddUnit(m))
    }

    // getVideo
    fun getRvRewardMoney(money: Double, withPlus: Boolean = true): Pair<Double, String> {
        return buildReward(rvVideo, money, withPlus)
    }

    // 网赚纸钞挂件：每两次现金加载自动累加；money 为 0 时使用 min 最小的区间
    fun getMoneyVideoIconReward(money: Double): Double {
        return pickRewardFromRanges(moneyVideoIcon, money)
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

    fun getTaskPopReward(moneyBr: Double): Pair<Double, String> {
        val m = pickRewardFromRanges(taskPop, moneyBr)
        return Pair(m, "+${WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(m)}")
    }

    fun getExitReward(moneyBr: Double): Pair<Double, String> {
        val m = pickRewardFromRanges(exitReward, moneyBr)
        return Pair(m, WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(m))
    }

    private fun buildReward(list: List<RewardRange>,
                            moneyBr: Double,
                            withPlus: Boolean,
                            withSpace: Boolean = true): Pair<Double, String> {
        val m = pickRewardFromRanges(list, moneyBr)
        val formatted = if (withSpace) {
            WithdrawAmountHelper.moneyFormatAddUnit(m)
        } else {
            WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(m)
        }
        return if (withPlus) Pair(m, "+$formatted") else Pair(m, formatted)
    }
}

data class RewardValue(@SerializedName("reward") val reward: Double)

data class RewardRange(
    @SerializedName("min") val min: Double,
    @SerializedName("max") val max: Double?,
    @SerializedName("reward") val reward: List<Double>
) {
    fun isInRange(num: Double): Boolean {
        if (num < min) return false
        val maxVal = max ?: Double.MAX_VALUE
        return num < maxVal
    }
}

/** 从区间列表中选取奖励：money 为 0 时取 min 最小的区间，否则取包含 money 的区间 */
private fun pickRewardFromRanges(list: List<RewardRange>, money: Double): Double {
    val range = if (money == 0.0) list.minByOrNull { it.min } else list.firstOrNull { it.isInRange(money) }
    return range?.reward?.random() ?: 0.0
}
