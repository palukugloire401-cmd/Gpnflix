package com.example.gpnflix.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val movieId: String,
    val title: String,
    val posterUrl: String,
    val localFilePath: String,
    val fileSizeBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: String = "COMPLETED", // "QUEUED", "DOWNLOADING", "COMPLETED", "FAILED"
    val progressPercent: Int = 100,
    val downloadedTimestamp: Long = System.currentTimeMillis()
)
