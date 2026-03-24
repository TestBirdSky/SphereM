package com.sphere.shortvideos.helper.withdraw.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WithdrawalRecordDao {
    @Insert
    suspend fun insert(record: WithdrawalRecordEntity): Long

    @Query("UPDATE withdrawal_record_table SET progress = :progress WHERE id = :id")
    suspend fun updateProgressById(id: Long, progress: Double): Int
}
