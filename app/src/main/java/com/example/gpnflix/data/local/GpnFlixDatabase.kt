package com.example.gpnflix.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        VideoMetadataEntity::class,
        WatchHistoryEntity::class,
        FavoriteEntity::class,
        DownloadEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class GpnFlixDatabase : RoomDatabase() {
    abstract fun videoMetadataDao(): VideoMetadataDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile
        private var INSTANCE: GpnFlixDatabase? = null

        fun getDatabase(context: Context): GpnFlixDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GpnFlixDatabase::class.java,
                    "gpnflix_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
