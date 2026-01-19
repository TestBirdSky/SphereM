package com.sphere.shortvideos.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DramaHistoryDao {
    @Upsert
    suspend fun upsert(data: DramaHistoryEntity): Long

    @Query("SELECT * FROM drama_history_table WHERE dramaId = :id LIMIT 1")
    suspend fun getItemById(id: String): DramaHistoryEntity?

    @Query("SELECT * FROM drama_history_table ORDER BY lastWatchTime DESC LIMIT 1")
    suspend fun getLastHistory(): DramaHistoryEntity?

    @Query("SELECT * FROM drama_history_table ORDER BY lastWatchTime DESC")
    fun getAll(): Flow<List<DramaHistoryEntity>>

    @Delete
    suspend fun delete(data: DramaHistoryEntity)
}

@Dao
interface DramaCollectDao {

    @Upsert
    suspend fun upsert(data: DramaCollectEntity): Long

    @Query("SELECT * FROM drama_collect_table WHERE dramaId = :id LIMIT 1")
    suspend fun getItemById(id: String): DramaCollectEntity?

    @Query("SELECT * FROM drama_collect_table ORDER BY uid DESC")
    fun getAll(): Flow<List<DramaCollectEntity>>

    @Delete
    suspend fun delete(data: DramaCollectEntity)
}

@Dao
interface DramaEpisodeDao {

    @Upsert
    suspend fun upsert(unlock: DramaEpisodeEntity): Long

    @Query("UPDATE drama_eps_table SET numbers = :indexList WHERE dramaId = :id")
    suspend fun updateItemById(id: String, indexList: List<Int>)

    @Query("SELECT * FROM drama_eps_table WHERE dramaId = :id LIMIT 1")
    suspend fun getItemById(id: String): DramaEpisodeEntity?

}