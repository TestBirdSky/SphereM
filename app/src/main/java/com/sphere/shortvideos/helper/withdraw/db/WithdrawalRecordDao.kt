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

    @Query("SELECT * FROM withdrawal_record_table ORDER BY createdAt DESC")
    suspend fun queryAllOrderedByCreatedDesc(): List<WithdrawalRecordEntity>

    @Query("SELECT * FROM withdrawal_record_table WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): WithdrawalRecordEntity?
}
