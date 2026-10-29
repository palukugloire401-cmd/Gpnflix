package com.example.gpnflix.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val movieId: String,
    val addedTimestamp: Long = System.currentTimeMillis()
)
