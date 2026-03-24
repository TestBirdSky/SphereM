package com.sphere.shortvideos.helper.withdraw.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 独立提现记录库：与既有 [com.sphere.shortvideos.database.DramaDatabase] 完全隔离。
 */
@Database(
    entities = [WithdrawalRecordEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class WithdrawalRecordDatabase : RoomDatabase() {
    abstract fun withdrawalRecordDao(): WithdrawalRecordDao

    companion object {
        private const val DB_NAME = "withdrawal_record_db_v1"

        @Volatile
        private var INSTANCE: WithdrawalRecordDatabase? = null

        fun buildInstance(context: Context): WithdrawalRecordDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WithdrawalRecordDatabase::class.java,
                    DB_NAME,
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
