package com.sphere.shortvideos.helper.withdraw.db

import com.sphere.shortvideos.mApp
import com.sphere.shortvideos.helper.HelperRewardShow
import com.sphere.shortvideos.helper.mmkv.MMKVRepository
import com.sphere.shortvideos.helper.withdraw.WithdrawalActionHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class CutInBoostResult(
    val oldProgress: Double,
    val newProgress: Double,
    /** 本次实际提升的百分点（用于 “+X%” 展示，已考虑顶到 1.0 的截断） */
    val displayedDeltaPercent: Int,
)

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
            WithdrawalActionHelper.clearWithdrawalInfoAndAddTask()
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

    /** 用于「插队提现」等列表：最新记录在前 */
    suspend fun getAllRecordsOrdered(): List<WithdrawalRecordEntity> {
        return dao.queryAllOrderedByCreatedDesc().filter { it.progress < 1 }
    }

    /**
     * 读取插队列表前调用：每个自然日至多执行一次。
     * - 未完成记录若 **不是今天创建** 的，按与 Cut In 广告相同规则 [WithdrawalActionHelper.getCutInProgress] 加一次随机进度；
     * - **当天新入库** 的记录不参与（避免与创建时初始进度叠加）。
     */
    suspend fun applyDailyPassiveBoostIfNeeded() {
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (MMKVRepository.lastCutInDailyAutoBoostDay == day) return

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val startOfTomorrow = cal.timeInMillis

        val candidates = dao.queryAllOrderedByCreatedDesc().filter { it.progress < 1.0 - 1e-9 }
        for (entity in candidates) {
            if (entity.createdAt in startOfToday until startOfTomorrow) continue
            val deltaPercent = WithdrawalActionHelper.getCutInProgress()
            val old = entity.progress
            val newProgress = (old + deltaPercent / 100.0).coerceAtMost(1.0)
            if (newProgress > old) {
                updateProgress(entity.id, newProgress)
            }
        }
        MMKVRepository.lastCutInDailyAutoBoostDay = day
    }

    /** 历史记录：包含已完成，最新在前 */
    suspend fun getAllRecordsForHistory(): List<WithdrawalRecordEntity> {
        return dao.queryAllOrderedByCreatedDesc()
    }

    /**
     * 观看激励成功后：按 [WithdrawalActionHelper.getCutInProgress] 随机增量增加进度。
     * @return 失败时返回 null（进度已满、记录不存在等）
     */
    suspend fun tryApplyCutInBoost(recordId: Long): CutInBoostResult? {
        val entity = dao.getById(recordId) ?: return null
        if (entity.progress >= 1.0 - 1e-9) return null

        val deltaPercent = WithdrawalActionHelper.getCutInProgress()
        val old = entity.progress
        val newProgress = (old + deltaPercent / 100.0).coerceAtMost(1.0)
        if (newProgress <= old) return null
        val displayedDelta = ((newProgress - old) * 100.0).roundToInt().coerceAtLeast(1)

        if (!updateProgress(recordId, newProgress)) return null
        return CutInBoostResult(
            oldProgress = old,
            newProgress = newProgress,
            displayedDeltaPercent = displayedDelta,
        )
    }
}
