package com.sphere.shortvideos.bean

import com.google.gson.annotations.SerializedName

/**
 * 提现动作配置（Remote Config key: Withdrawal_action）
 */
data class WithdrawalActionConfig(
    @SerializedName("Withdrawal_process")
    val withdrawalProcess: Int,
    @SerializedName("user_maintenance")
    val userMaintenance: WithdrawalUserMaintenance,
    @SerializedName("withdrawal_task")
    val withdrawalTask: WithdrawalTaskConfig? = null,
    @SerializedName("cut_in")
    val cutIn: List<Int>,
) {
    fun isOpenWithdraw(): Boolean {
        return withdrawalProcess == 1
    }


}

data class WithdrawalUserMaintenance(
    @SerializedName("withdrawal_form")
    val withdrawalForm: Int,
    @SerializedName("premium_user")
    val premiumUser: Int,
)

data class WithdrawalTaskConfig(
    @SerializedName("task1")
    val task1: WithdrawalTaskRule? = null,
    @SerializedName("task2")
    val task2: WithdrawalTaskRule? = null,
    @SerializedName("task3")
    val task3: WithdrawalTaskRule? = null,
)

data class WithdrawalTaskRule(
    @SerializedName("drama")
    val drama: Int = 0,
    @SerializedName("bubble")
    val bubble: Int = 0,
    @SerializedName("ad")
    val ad: Int = 0,
)

