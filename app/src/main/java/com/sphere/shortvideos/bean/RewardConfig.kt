package com.sphere.shortvideos.bean

import ads_mobile_sdk.ma0
import com.google.gson.annotations.SerializedName

/**
 * Date：2026/1/22
 * Describe: BR 奖励配置
 */
data class RewardConfig(
    @SerializedName("money_newuser_gift")
    val moneyNewuserGift: RewardValue,

    @SerializedName("money_video_icon")
    val moneyVideoIcon: List<RewardRange>,

    @SerializedName("money_push")
    val moneyPush: List<RewardRange>,

    @SerializedName("drama_time_1")
    val dramaTime1: List<RewardRange>,

    @SerializedName("drama_time_2")
    val dramaTime2: List<RewardRange>,

    @SerializedName("drama_time_3")
    val dramaTime3: List<RewardRange>,

    @SerializedName("drama_time_4")
    val dramaTime4: List<RewardRange>,

    @SerializedName("drama_time_5")
    val dramaTime5: List<RewardRange>,

    @SerializedName("drama_time_6")
    val dramaTime6: List<RewardRange>,

    @SerializedName("task_pop")
    val taskPop: List<RewardRange>,

    @SerializedName("sign_in")
    val signIn: List<RewardRange>,

    @SerializedName("rv_video")
    val rvVideo: List<RewardRange>,

    @SerializedName("ad_interval")
    val adInterval: List<Int>
) {

}

data class RewardValue(
    @SerializedName("reward")
    val reward: Double
)

data class RewardRange(
    @SerializedName("min")
    val min: Int,

    @SerializedName("max")
    val max: Int?,

    @SerializedName("reward")
    val reward: List<Double>
) {

    fun isInRange(num: Double): Boolean {
        if (num > min) {
            if (num < (max ?: Int.MAX_VALUE)) {
                return true
            }
        }
        return false
    }

}
