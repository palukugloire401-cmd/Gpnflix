package com.example.gpnflix.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history ORDER BY lastWatchedTimestamp DESC")
    fun getAllHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE isCompleted = 0 AND progressMs > 3000 ORDER BY lastWatchedTimestamp DESC")
    fun getContinueWatching(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE movieId = :movieId LIMIT 1")
    suspend fun getProgressForMovie(movieId: String): WatchHistoryEntity?

    @Query("SELECT * FROM watch_history WHERE movieId = :movieId LIMIT 1")
    fun observeProgressForMovie(movieId: String): Flow<WatchHistoryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(entity: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE movieId = :movieId")
    suspend fun deleteHistory(movieId: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearAllHistory()
}
