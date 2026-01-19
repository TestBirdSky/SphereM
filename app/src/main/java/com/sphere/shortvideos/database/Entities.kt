package com.sphere.shortvideos.database

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "drama_history_table")
data class DramaHistoryEntity(
    var dataJson: String,
    var dramaId: String,
    var currentIndex: Int = 1,
    var currentProgress: Int = 0,
    var maxProgress: Int = 100,
    var lastWatchTime: Long = System.currentTimeMillis(),
    var isPangle: Boolean = true,
    @PrimaryKey(autoGenerate = true) var uid: Long = 0,
) : Parcelable

@Entity(tableName = "drama_collect_table")
data class DramaCollectEntity(
    var dataJson: String,
    var dramaId: String,
    var isPangle: Boolean = true,
    @PrimaryKey(autoGenerate = true) var uid: Long = 0,
)

@Entity(tableName = "drama_eps_table")
data class DramaEpisodeEntity(
    var dramaId: String,
    var numbers: List<Int> = listOf(1, 2),
    @PrimaryKey(autoGenerate = true) var uid: Long = 0L,
)