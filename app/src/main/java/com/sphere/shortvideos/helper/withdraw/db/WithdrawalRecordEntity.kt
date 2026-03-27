package com.sphere.shortvideos.helper.withdraw.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "withdrawal_record_table")
data class WithdrawalRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val account: String,
    val withdrawalMethodId: String,
    /** 进度区间：[0.1, 1.0] */
    val progress: Double = 0.1,
    /** 提现金额，美元(USD)，与 [MoneyCacheHelper] 一致；展示时用 [MoneyCacheHelper.usdToShowMoneyD] */
    val withdrawalAmount: Double,
)
