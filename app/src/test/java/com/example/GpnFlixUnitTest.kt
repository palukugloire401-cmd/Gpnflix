package com.example

import com.example.gpnflix.data.model.Movie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GpnFlixUnitTest {

    private val sampleMovie = Movie(
        id = "test_bbb",
        title = "Big Buck Bunny",
        originalTitle = "Big Buck Bunny Open Project",
        synopsis = "Un lapin géant fait face à trois rongeurs espiègles.",
        durationMinutes = 75,
        releaseYear = 2008,
        rating = 4.8f,
        posterUrl = "https://example.com/poster.jpg",
        backdropUrl = "https://example.com/backdrop.jpg",
        videoUrl = "https://example.com/video.mp4",
        category = "Animation",
        genres = listOf("Animation", "Comédie"),
        director = "Sacha Goedegebure",
        cast = listOf("Jan Morgenstern"),
        licenseType = "Creative Commons BY 3.0"
    )

    @Test
    fun testMovieFormattedDuration() {
        assertEquals("1h 15min", sampleMovie.formattedDuration)

        val shortMovie = sampleMovie.copy(durationMinutes = 12)
        assertEquals("12 min", shortMovie.formattedDuration)
    }

    @Test
    fun testMovieLicenseIsLegal() {
        assertTrue(sampleMovie.licenseType.contains("Creative Commons") || sampleMovie.licenseType.contains("Domaine Public"))
        assertTrue(sampleMovie.isDownloadable)
    }

    @Test
    fun testMovieQualitiesAndSubtitles() {
        assertTrue(sampleMovie.qualities.isNotEmpty())
        assertTrue(sampleMovie.subtitles.any { it.label == "Français" })
    }
}
