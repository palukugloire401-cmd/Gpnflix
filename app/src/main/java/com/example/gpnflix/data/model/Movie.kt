package com.example.gpnflix.data.model

data class SubtitleTrack(
    val id: String,
    val label: String,
    val languageCode: String,
    val url: String? = null
)

object MovieSourceType {
    const val ARCHIVE_ORG = "ARCHIVE_ORG"
    const val TMDB = "TMDB"
}

object MediaContentType {
    const val FILM = "Film"
    const val SERIES = "Série"
    const val MUSIC = "Musique"
    const val CARTOON = "Dessin animé"
    const val THEATER = "Théâtre"

    val allTypes: List<String> = listOf(FILM, SERIES, MUSIC, CARTOON, THEATER)
}

enum class SpaceCompressionPreset(
    val id: String,
    val label: String,
    val ratio: Float,
    val savingsPercent: Int,
    val targetQualityLabel: String,
    val exampleText: String
) {
    ORIGINAL(
        id = "ORIGINAL",
        label = "Original HD (Sans compression)",
        ratio = 1.0f,
        savingsPercent = 0,
        targetQualityLabel = "1080p HD",
        exampleText = "100 Mo → 100 Mo"
    ),
    BALANCED(
        id = "BALANCED",
        label = "Équilibré HD (-35%)",
        ratio = 0.65f,
        savingsPercent = 35,
        targetQualityLabel = "720p HD",
        exampleText = "100 Mo → 65 Mo"
    ),
    SERIES_SAVER(
        id = "SERIES_SAVER",
        label = "Compresseur Série (-50%)",
        ratio = 0.50f,
        savingsPercent = 50,
        targetQualityLabel = "480p SD",
        exampleText = "100 Mo → 50 Mo"
    ),
    ULTRA_ECO(
        id = "ULTRA_ECO",
        label = "Ultra-Compact (-65%)",
        ratio = 0.35f,
        savingsPercent = 65,
        targetQualityLabel = "360p Éco",
        exampleText = "100 Mo → 35 Mo"
    );

    fun estimateCompressedMb(originalMb: Int): Int =
        (originalMb * ratio).toInt().coerceAtLeast(1)

    companion object {
        fun fromId(id: String?): SpaceCompressionPreset =
            entries.find { it.id == id } ?: SERIES_SAVER
    }
}

enum class ImageQualityMode(
    val id: String,
    val label: String,
    val tmdbPosterSize: String,
    val tmdbBackdropSize: String
) {
    HD(
        id = "HD",
        label = "Haute Définition (1080p / w780)",
        tmdbPosterSize = "w780",
        tmdbBackdropSize = "w1280"
    ),
    STANDARD(
        id = "STANDARD",
        label = "Standard équilibré (w500)",
        tmdbPosterSize = "w500",
        tmdbBackdropSize = "w780"
    ),
    DATA_SAVER(
        id = "DATA_SAVER",
        label = "Économie d'espace (-50% / w342)",
        tmdbPosterSize = "w342",
        tmdbBackdropSize = "w500"
    );

    companion object {
        fun fromId(id: String?): ImageQualityMode =
            entries.find { it.id == id } ?: HD
    }
}

object ImageAspectType {
    const val PORTRAIT_2_3 = "Affiche 2:3"
    const val LANDSCAPE_16_9 = "Paysage 16:9"
    const val SQUARE_1_1 = "Carré 1:1"
}

data class MediaGalleryImage(
    val url: String,
    val aspectType: String,
    val label: String
)

data class SeriesSeason(
    val seasonNumber: Int,
    val name: String,
    val episodeCount: Int,
    val posterUrl: String = ""
)

data class SeriesEpisode(
    val episodeNumber: Int,
    val seasonNumber: Int,
    val name: String,
    val overview: String,
    val runtimeMinutes: Int,
    val stillUrl: String,
    val airDate: String = ""
)

data class Movie(
    val id: String,
    val title: String,
    val originalTitle: String,
    val synopsis: String,
    val durationMinutes: Int,
    val releaseYear: Int,
    val rating: Float,
    val posterUrl: String,
    val backdropUrl: String,
    val videoUrl: String = "",
    val trailerUrl: String = "",
    val trailerYoutubeKey: String? = null,
    val sourceType: String = MovieSourceType.ARCHIVE_ORG,
    val tmdbId: Int? = null,
    val archiveIdentifier: String? = null,
    val category: String,
    val genres: List<String>,
    val director: String,
    val cast: List<String>,
    val language: String = "Français",
    val licenseType: String,
    val isDownloadable: Boolean = false,
    val isFeatured: Boolean = false,
    val isTrending: Boolean = false,
    val isAwardWinner: Boolean = false,
    val qualities: List<String> = listOf("Auto"),
    val subtitles: List<SubtitleTrack> = listOf(
        SubtitleTrack("off", "Désactivé", "none"),
        SubtitleTrack("fr", "Français", "fr"),
        SubtitleTrack("en", "English", "en")
    ),
    val mediaType: String = MediaContentType.FILM,
    val estimatedSizeMb: Int = 0,
    val isFamilyFriendly: Boolean = false,
    val seasons: List<SeriesSeason> = emptyList(),
    val galleryImages: List<MediaGalleryImage> = emptyList()
) {
    val hasDirectStream: Boolean
        get() = videoUrl.isNotBlank()

    val hasTrailer: Boolean
        get() = !trailerYoutubeKey.isNullOrBlank() || trailerUrl.isNotBlank()

    val isPlayable: Boolean
        get() = hasDirectStream || hasTrailer || sourceType == MovieSourceType.ARCHIVE_ORG

    val highestQualityBadge: String
        get() = when {
            qualities.any { it.contains("4K", ignoreCase = true) || it.contains("2160", ignoreCase = true) } -> "4K UHD"
            qualities.any { it.contains("1080", ignoreCase = true) } -> "Full HD"
            qualities.any { it.contains("720", ignoreCase = true) || it.contains("HD", ignoreCase = true) } -> "HD 720p"
            qualities.any { it.contains("480", ignoreCase = true) } -> "480p SD"
            else -> "HD"
        }

    val formattedDuration: String
        get() {
            if (durationMinutes <= 0) {
                return if (mediaType == MediaContentType.SERIES && seasons.isNotEmpty()) {
                    "${seasons.size} saison(s)"
                } else {
                    "Durée N/C"
                }
            }
            val hours = durationMinutes / 60
            val minutes = durationMinutes % 60
            return if (hours > 0) "${hours}h ${minutes}min" else "${minutes} min"
        }

    val legalAccessLabel: String
        get() = when {
            hasDirectStream -> "$mediaType complet"
            !trailerYoutubeKey.isNullOrBlank() -> "Bande-annonce • $mediaType"
            sourceType == MovieSourceType.ARCHIVE_ORG -> "$mediaType (Domaine Public)"
            else -> "$mediaType • TMDB"
        }
}

data class UpcomingMovie(
    val id: String,
    val title: String,
    val expectedReleaseDate: String,
    val posterUrl: String,
    val backdropUrl: String,
    val synopsis: String,
    val category: String,
    val licenseNotice: String,
    val trailerUrl: String = "",
    val trailerYoutubeKey: String? = null,
    val tmdbId: Int? = null,
    val archiveIdentifier: String? = null,
    val mediaType: String = MediaContentType.FILM
)
