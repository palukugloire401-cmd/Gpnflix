package com.example.gpnflix.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY downloadedTimestamp DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE movieId = :movieId LIMIT 1")
    fun observeDownload(movieId: String): Flow<DownloadEntity?>

    @Query("SELECT * FROM downloads WHERE movieId = :movieId LIMIT 1")
    suspend fun getDownload(movieId: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDownload(entity: DownloadEntity)

    @Query("DELETE FROM downloads WHERE movieId = :movieId")
    suspend fun deleteDownload(movieId: String)

    @Query("DELETE FROM downloads")
    suspend fun clearAllDownloads()
}
