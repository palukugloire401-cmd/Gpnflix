package com.example.gpnflix.data.model

data class SubtitleTrack(
    val id: String,
    val label: String,
    val languageCode: String,
    val url: String? = null
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
    val videoUrl: String,
    val trailerUrl: String = videoUrl,
    val category: String,
    val genres: List<String>,
    val director: String,
    val cast: List<String>,
    val language: String = "Anglais",
    val licenseType: String,
    val isDownloadable: Boolean = true,
    val isFeatured: Boolean = false,
    val isTrending: Boolean = false,
    val isAwardWinner: Boolean = false,
    val qualities: List<String> = listOf("Auto", "1080p HD", "720p", "480p"),
    val subtitles: List<SubtitleTrack> = listOf(
        SubtitleTrack("off", "Désactivé", "none"),
        SubtitleTrack("fr", "Français", "fr"),
        SubtitleTrack("en", "English", "en")
    )
) {
    val formattedDuration: String
        get() {
            val hours = durationMinutes / 60
            val minutes = durationMinutes % 60
            return if (hours > 0) "${hours}h ${minutes}min" else "${minutes} min"
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
    val trailerUrl: String
)
