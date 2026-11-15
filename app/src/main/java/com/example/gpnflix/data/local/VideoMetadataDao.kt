package com.example.gpnflix.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoMetadataDao {
    @Query("SELECT * FROM video_metadata ORDER BY isFeatured DESC, releaseYear DESC")
    fun observeAllVideos(): Flow<List<VideoMetadataEntity>>

    @Query("SELECT * FROM video_metadata WHERE isFeatured = 1 ORDER BY releaseYear DESC")
    fun observeFeaturedVideos(): Flow<List<VideoMetadataEntity>>

    @Query("SELECT * FROM video_metadata WHERE isTrending = 1 ORDER BY rating DESC")
    fun observeTrendingVideos(): Flow<List<VideoMetadataEntity>>

    @Query("SELECT * FROM video_metadata WHERE LOWER(category) = LOWER(:category) ORDER BY releaseYear DESC")
    fun observeVideosByCategory(category: String): Flow<List<VideoMetadataEntity>>

    @Query("SELECT * FROM video_metadata WHERE id = :id LIMIT 1")
    fun observeVideoById(id: String): Flow<VideoMetadataEntity?>

    @Query("SELECT * FROM video_metadata WHERE id = :id LIMIT 1")
    suspend fun getVideoById(id: String): VideoMetadataEntity?

    @Query("SELECT COUNT(*) FROM video_metadata")
    suspend fun getVideoCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(videos: List<VideoMetadataEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVideo(video: VideoMetadataEntity)

    @Query(
        """
        SELECT * FROM video_metadata
        WHERE (:query = '' OR title LIKE '%' || :query || '%' OR originalTitle LIKE '%' || :query || '%' OR synopsis LIKE '%' || :query || '%' OR director LIKE '%' || :query || '%' OR castCsv LIKE '%' || :query || '%')
          AND (:category = '' OR LOWER(:category) = 'all' OR LOWER(category) = LOWER(:category))
          AND (:year IS NULL OR releaseYear = :year)
          AND (:language = '' OR LOWER(:language) = 'toutes' OR language LIKE '%' || :language || '%')
        ORDER BY isFeatured DESC, rating DESC
        """
    )
    fun searchVideos(
        query: String,
        category: String,
        year: Int?,
        language: String
    ): Flow<List<VideoMetadataEntity>>
}
