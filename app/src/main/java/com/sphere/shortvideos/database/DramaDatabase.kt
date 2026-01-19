package com.sphere.shortvideos.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [DramaHistoryEntity::class, DramaCollectEntity::class, DramaEpisodeEntity::class], version = 1, exportSchema = false)
@TypeConverters(IntListConverters::class)
abstract class DramaDatabase : RoomDatabase() {

    abstract fun historyDao(): DramaHistoryDao
    abstract fun collectDao(): DramaCollectDao
    abstract fun episodeDao(): DramaEpisodeDao

    companion object {
        @Volatile
        private var INSTANCE: DramaDatabase? = null

        fun buildInstance(context: Context): DramaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(context.applicationContext, DramaDatabase::class.java, "drama_database").build()
                INSTANCE = instance
                instance
            }
        }

    }

}