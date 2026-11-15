package com.example.gpnflix.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.gpnflix.data.model.Movie
import com.example.gpnflix.data.model.SubtitleTrack

/**
 * Room Entity persisting complete video metadata locally for fast offline browsing,
 * reactive UI updates, and relational linking with offline downloads.
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
    val cachedAtTimestamp: Long = System.currentTimeMillis()
) {
    fun toMovie(): Movie {
        val genres = genresCsv.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        val cast = castCsv.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        val qualities = qualitiesCsv.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            .ifEmpty { listOf("Auto", "1080p HD", "720p", "480p") }

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
            category = category,
            genres = genres,
            director = director,
            cast = cast,
            language = language,
            licenseType = licenseType,
            isDownloadable = isDownloadable,
            isFeatured = isFeatured,
            isTrending = isTrending,
            isAwardWinner = isAwardWinner,
            qualities = qualities,
            subtitles = listOf(
                SubtitleTrack("off", "Désactivé", "none"),
                SubtitleTrack("fr", "Français", "fr"),
                SubtitleTrack("en", "English", "en")
            )
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
        category = category,
        genresCsv = genres.joinToString("|"),
        director = director,
        castCsv = cast.joinToString("|"),
        language = language,
        licenseType = licenseType,
        isDownloadable = isDownloadable,
        isFeatured = isFeatured,
        isTrending = isTrending,
        isAwardWinner = isAwardWinner,
        qualitiesCsv = qualities.joinToString("|")
    )
}
