package com.example.gpnflix.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

object DownloadStatus {
    const val QUEUED = "QUEUED"
    const val DOWNLOADING = "DOWNLOADING"
    const val PAUSED = "PAUSED"
    const val COMPLETED = "COMPLETED"
    const val FAILED = "FAILED"
}

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val movieId: String,
    val title: String,
    val posterUrl: String,
    val videoUrl: String = "",
    val localFilePath: String,
    val quality: String = "Auto",
    val fileSizeBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: String = DownloadStatus.QUEUED,
    val progressPercent: Int = 0,
    val errorMessage: String? = null,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val downloadedTimestamp: Long = System.currentTimeMillis()
) {
    val isCompleted: Boolean get() = status == DownloadStatus.COMPLETED
    val isDownloading: Boolean get() = status == DownloadStatus.DOWNLOADING
    val isQueued: Boolean get() = status == DownloadStatus.QUEUED
    val isPaused: Boolean get() = status == DownloadStatus.PAUSED
    val isFailed: Boolean get() = status == DownloadStatus.FAILED
    val isActive: Boolean get() = status == DownloadStatus.QUEUED || status == DownloadStatus.DOWNLOADING
}

data class VideoDownloadWithMetadata(
    @Embedded val download: DownloadEntity,
    @Relation(
        parentColumn = "movieId",
        entityColumn = "id"
    )
    val metadata: VideoMetadataEntity?
)
