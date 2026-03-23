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

