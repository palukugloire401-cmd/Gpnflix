package com.example.gpnflix.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY downloadedTimestamp DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Transaction
    @Query("SELECT * FROM downloads ORDER BY downloadedTimestamp DESC")
    fun observeDownloadsWithMetadata(): Flow<List<VideoDownloadWithMetadata>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' ORDER BY downloadedTimestamp DESC")
    fun observeCompletedDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE movieId = :movieId LIMIT 1")
    fun observeDownload(movieId: String): Flow<DownloadEntity?>

    @Query("SELECT * FROM downloads WHERE movieId = :movieId LIMIT 1")
    suspend fun getDownload(movieId: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED', 'DOWNLOADING')")
    suspend fun getInterruptedDownloads(): List<DownloadEntity>

    @Query("SELECT COALESCE(SUM(downloadedBytes), 0) FROM downloads")
    fun observeTotalDownloadedBytes(): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDownload(entity: DownloadEntity)

    @Query(
        """
        UPDATE downloads
        SET downloadedBytes = :downloadedBytes,
            fileSizeBytes = :fileSizeBytes,
            progressPercent = :progressPercent,
            status = :status,
            errorMessage = NULL,
            downloadedTimestamp = :timestamp
        WHERE movieId = :movieId
        """
    )
    suspend fun updateProgress(
        movieId: String,
        downloadedBytes: Long,
        fileSizeBytes: Long,
        progressPercent: Int,
        status: String,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE downloads
        SET status = :status,
            errorMessage = :errorMessage,
            downloadedTimestamp = :timestamp
        WHERE movieId = :movieId
        """
    )
    suspend fun updateStatus(
        movieId: String,
        status: String,
        errorMessage: String? = null,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM downloads WHERE movieId = :movieId")
    suspend fun deleteDownload(movieId: String)

    @Query("DELETE FROM downloads")
    suspend fun clearAllDownloads()
}
