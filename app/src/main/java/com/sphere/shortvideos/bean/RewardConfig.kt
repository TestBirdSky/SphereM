package com.sphere.shortvideos.bean

import com.google.gson.annotations.SerializedName
import com.sphere.shortvideos.logError

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
    var moneyShowUnit = "R$"

    fun getRewardNewUser(): Pair<Double, String> {
        return Pair(moneyNewuserGift.reward, "+$moneyShowUnit\t${moneyNewuserGift.reward}")
    }

    // 传进来的是巴西币
    fun getNotificationRewardMoney(moneyBr: Double): Pair<Double, String> {
        val m = moneyPush.firstOrNull {
            it.isInRange(moneyBr * moneyRate)
        }?.reward?.random() ?: 0.0
        return Pair(m, "$moneyShowUnit${m}")
    }

    // getVideo
    fun getRvRewardMoney(moneyBr: Double): Pair<Double, String> {
        val m = rvVideo.firstOrNull {
            it.isInRange(moneyBr * moneyRate)
        }?.reward?.random() ?: 0.0
        return Pair(m, "+$moneyShowUnit\t${m}")
    }

    fun getWatchTime1Money(): Double {

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
