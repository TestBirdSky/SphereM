package com.sphere.shortvideos.helper.withdraw.db

import com.sphere.shortvideos.helper.MoneyCacheHelper
import com.sphere.shortvideos.mApp
import com.sphere.shortvideos.helper.HelperRewardShow
import com.sphere.shortvideos.helper.withdraw.WithdrawalActionHelper

object WithdrawalRecordStore {

    private val db by lazy { WithdrawalRecordDatabase.buildInstance(mApp) }
    private val dao by lazy { db.withdrawalRecordDao() }

    /**
     * 将当前 [WithdrawalActionHelper] 缓存落库。
     * 成功后清空 accountWithdrawal / withdrawalMethodId / withdrawalValue。
     */
    suspend fun createRecordFromCache(initialProgress: Double = 0.1): Long? {
        val account = WithdrawalActionHelper.accountWithdrawal.trim()
        val methodId = WithdrawalActionHelper.withdrawalMethodId.trim()
        val amount = WithdrawalActionHelper.withdrawalValue
        if (account.isBlank() || methodId.isBlank()) return null

        val recordId = dao.insert(
            WithdrawalRecordEntity(
                account = account,
                withdrawalMethodId = methodId,
                progress = normalizeProgress(initialProgress),
                withdrawalAmount = amount,
            )
        )

        if (recordId > 0) {
            HelperRewardShow.reduceMoney(amount)
            WithdrawalActionHelper.accountWithdrawal = ""
            WithdrawalActionHelper.withdrawalMethodId = ""
            WithdrawalActionHelper.withdrawalValue = 0.0
            return recordId
        }
        return null
    }

    /** 进度范围：[0.1, 1.0] */
    suspend fun updateProgress(recordId: Long, progress: Double): Boolean {
        val affected = dao.updateProgressById(recordId, normalizeProgress(progress))
        return affected > 0
    }

    private fun normalizeProgress(progress: Double): Double {
        return progress.coerceIn(0.1, 1.0)
    }
}
