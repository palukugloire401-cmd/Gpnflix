package com.example.gpnflix.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.gpnflix.data.model.MediaContentType
import com.example.gpnflix.data.model.Movie
import com.example.gpnflix.data.model.MovieSourceType
import com.example.gpnflix.data.model.SubtitleTrack

/**
 * Room Entity persisting complete video metadata locally for fast offline browsing,
 * reactive UI updates, and relational linking with offline downloads, favorites, and history.
 */
@Entity(tableName = "video_metadata")
data class VideoMetadataEntity(
    @PrimaryKey val id: String,
    val title: String,
    val originalTitle: String,
    val synopsis: String,
    val durationMinutes: Int,
    val releaseYear: Int,
    val rating: Float,
    val posterUrl: String,
    val backdropUrl: String,
    val videoUrl: String,
    val trailerUrl: String,
    val trailerYoutubeKey: String? = null,
    val sourceType: String = MovieSourceType.ARCHIVE_ORG,
    val tmdbId: Int? = null,
    val archiveIdentifier: String? = null,
    val category: String,
    val genresCsv: String,
    val director: String,
    val castCsv: String,
    val language: String,
    val licenseType: String,
    val isDownloadable: Boolean,
    val isFeatured: Boolean,
    val isTrending: Boolean,
    val isAwardWinner: Boolean,
    val qualitiesCsv: String,
    val mediaType: String = MediaContentType.FILM,
    val estimatedSizeMb: Int = 0,
    val isFamilyFriendly: Boolean = false,
    val cachedAtTimestamp: Long = System.currentTimeMillis()
) {
    fun toMovie(): Movie {
        val genres = genresCsv.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        val cast = castCsv.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        val qualities = qualitiesCsv.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            .ifEmpty { listOf("Auto") }

        return Movie(
            id = id,
            title = title,
            originalTitle = originalTitle,
            synopsis = synopsis,
            durationMinutes = durationMinutes,
            releaseYear = releaseYear,
            rating = rating,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            videoUrl = videoUrl,
            trailerUrl = trailerUrl,
            trailerYoutubeKey = trailerYoutubeKey,
            sourceType = sourceType,
            tmdbId = tmdbId,
            archiveIdentifier = archiveIdentifier,
            category = category,
            genres = genres,
            director = director,
            cast = cast,
            language = language,
            licenseType = licenseType,
            isDownloadable = isDownloadable && videoUrl.isNotBlank(),
            isFeatured = isFeatured,
            isTrending = isTrending,
            isAwardWinner = isAwardWinner,
            qualities = qualities,
            subtitles = listOf(SubtitleTrack("off", "Désactivé", "none")),
            mediaType = mediaType,
            estimatedSizeMb = estimatedSizeMb,
            isFamilyFriendly = isFamilyFriendly
        )
    }
}

fun Movie.toVideoMetadataEntity(): VideoMetadataEntity {
    return VideoMetadataEntity(
        id = id,
        title = title,
        originalTitle = originalTitle,
        synopsis = synopsis,
        durationMinutes = durationMinutes,
        releaseYear = releaseYear,
        rating = rating,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        videoUrl = videoUrl,
        trailerUrl = trailerUrl,
        trailerYoutubeKey = trailerYoutubeKey,
        sourceType = sourceType,
        tmdbId = tmdbId,
        archiveIdentifier = archiveIdentifier,
        category = category,
        genresCsv = genres.joinToString("|"),
        director = director,
        castCsv = cast.joinToString("|"),
        language = language,
        licenseType = licenseType,
        isDownloadable = isDownloadable && videoUrl.isNotBlank(),
        isFeatured = isFeatured,
        isTrending = isTrending,
        isAwardWinner = isAwardWinner,
        qualitiesCsv = qualities.joinToString("|"),
        mediaType = mediaType,
        estimatedSizeMb = estimatedSizeMb,
        isFamilyFriendly = isFamilyFriendly
    )
}
