package com.example

import com.example.gpnflix.data.local.VideoSpaceCompressor
import com.example.gpnflix.data.model.ImageAspectType
import com.example.gpnflix.data.model.MediaContentType
import com.example.gpnflix.data.model.Movie
import com.example.gpnflix.data.model.MovieSourceType
import com.example.gpnflix.data.model.RegionLocaleConfig
import com.example.gpnflix.data.model.SpaceCompressionPreset
import com.example.gpnflix.data.remote.ApiMappers
import com.example.gpnflix.data.remote.ApiMappers.enrichMovie
import com.example.gpnflix.data.remote.ApiMappers.toEpisodes
import com.example.gpnflix.data.remote.ApiMappers.toGalleryImages
import com.example.gpnflix.data.remote.ApiMappers.toMovie
import com.example.gpnflix.data.remote.ArchiveDocDto
import com.example.gpnflix.data.remote.ArchiveFileDto
import com.example.gpnflix.data.remote.ArchiveItemMetadataDto
import com.example.gpnflix.data.remote.ArchiveItemMetadataResponseDto
import com.example.gpnflix.data.remote.TmdbCastDto
import com.example.gpnflix.data.remote.TmdbCreditsDto
import com.example.gpnflix.data.remote.TmdbCrewDto
import com.example.gpnflix.data.remote.TmdbEpisodeDto
import com.example.gpnflix.data.remote.TmdbGenreDto
import com.example.gpnflix.data.remote.TmdbImageDto
import com.example.gpnflix.data.remote.TmdbImagesResponseDto
import com.example.gpnflix.data.remote.TmdbMovieDetailsDto
import com.example.gpnflix.data.remote.TmdbMovieDto
import com.example.gpnflix.data.remote.TmdbSeasonDetailsDto
import com.example.gpnflix.data.remote.TmdbTvDto
import com.example.gpnflix.data.remote.TmdbVideoDto
import com.example.gpnflix.data.remote.TmdbVideoListDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GpnFlixUnitTest {

    private val sampleMovie = Movie(
        id = "arch_charade_1963",
        title = "Charade",
        originalTitle = "Charade",
        synopsis = "Thriller parisien classique entré dans le domaine public.",
        durationMinutes = 113,
        releaseYear = 1963,
        rating = 4.6f,
        posterUrl = "https://archive.org/services/img/Charade1963",
        backdropUrl = "https://archive.org/services/img/Charade1963",
        videoUrl = "https://archive.org/download/Charade1963/Charade.mp4",
        sourceType = MovieSourceType.ARCHIVE_ORG,
        archiveIdentifier = "Charade1963",
        category = "Classiques",
        genres = listOf("Mystère", "Comédie"),
        director = "Stanley Donen",
        cast = listOf("Cary Grant", "Audrey Hepburn"),
        licenseType = "Domaine Public (Archive.org)",
        isDownloadable = true
    )

    @Test
    fun testMovieFormattedDurationAndUnknownRuntime() {
        assertEquals("1h 53min", sampleMovie.formattedDuration)

        val shortMovie = sampleMovie.copy(durationMinutes = 12)
        assertEquals("12 min", shortMovie.formattedDuration)

        val unknownDuration = sampleMovie.copy(durationMinutes = 0)
        assertEquals("Durée N/C", unknownDuration.formattedDuration)
    }

    @Test
    fun testMovieLegalAccessLabels() {
        assertEquals("Film complet", sampleMovie.legalAccessLabel)
        assertTrue(sampleMovie.isDownloadable)

        val tmdbMovie = sampleMovie.copy(
            sourceType = MovieSourceType.TMDB,
            videoUrl = "",
            trailerYoutubeKey = "dQw4w9WgXcQ",
            isDownloadable = false
        )
        assertEquals("Bande-annonce • Film", tmdbMovie.legalAccessLabel)
        assertFalse(tmdbMovie.isDownloadable)
    }

    @Test
    fun testRegionLocaleFrenchOnlyDefault() {
        val frOnlyConfig = RegionLocaleConfig.fromPreference("Français uniquement")
        assertEquals("fr", frOnlyConfig.languageCode)
        assertTrue(frOnlyConfig.frenchOnly)
        assertTrue(frOnlyConfig.tmdbLanguage.startsWith("fr-"))
        assertTrue(frOnlyConfig.archiveLanguageQuery.contains("fre"))

        val enConfig = RegionLocaleConfig.fromPreference("English")
        assertEquals("en", enConfig.languageCode)
        assertFalse(enConfig.frenchOnly)
        assertTrue(enConfig.tmdbLanguage.startsWith("en-"))
    }

    @Test
    fun testSeriesSpaceCompression100MbTo50Mb() {
        assertEquals(50, SpaceCompressionPreset.SERIES_SAVER.estimateCompressedMb(100))
        assertEquals(65, SpaceCompressionPreset.BALANCED.estimateCompressedMb(100))
        assertEquals(35, SpaceCompressionPreset.ULTRA_ECO.estimateCompressedMb(100))

        val urls = linkedMapOf(
            "1080p HD" to "https://archive.org/download/ep1/ep1_1080.mp4",
            "720p HD" to "https://archive.org/download/ep1/ep1_720.mp4",
            "480p SD" to "https://archive.org/download/ep1/ep1_480.mp4"
        )
        val sizes = linkedMapOf(
            "1080p HD" to 100L * 1024L * 1024L,
            "720p HD" to 72L * 1024L * 1024L,
            "480p SD" to 50L * 1024L * 1024L
        )
        val selected = VideoSpaceCompressor.selectVariant(
            fallbackUrl = urls["1080p HD"]!!,
            qualityUrls = urls,
            qualitySizesBytes = sizes,
            preset = SpaceCompressionPreset.SERIES_SAVER
        )
        assertEquals(100L * 1024L * 1024L, selected.originalHdBytes)
        assertEquals(50L * 1024L * 1024L, selected.targetVariantBytes)
        assertEquals("https://archive.org/download/ep1/ep1_480.mp4", selected.streamUrl)
    }

    @Test
    fun testTvSeriesEpisodesAndMultiFormatImagesMapping() {
        val frLocale = RegionLocaleConfig.fromPreference("Français uniquement")
        val tvDto = TmdbTvDto(
            id = 999,
            name = "Les Aventuriers",
            originalName = "Les Aventuriers",
            overview = "Série d'aventure en français.",
            posterPath = "/tvPoster.jpg",
            backdropPath = "/tvBackdrop.jpg",
            firstAirDate = "2024-05-10",
            voteAverage = 8.2,
            genreIds = listOf(16),
            originalLanguage = "fr"
        )
        val mappedTv = tvDto.toMovie(mapOf(16 to "Animation"), frLocale)
        assertEquals("tmdb_tv_999", mappedTv.id)
        assertEquals(MediaContentType.CARTOON, mappedTv.mediaType)
        assertEquals(100, mappedTv.estimatedSizeMb)
        assertTrue(mappedTv.isFamilyFriendly)

        val seasonDto = TmdbSeasonDetailsDto(
            seasonNumber = 1,
            episodes = listOf(
                TmdbEpisodeDto(episodeNumber = 1, seasonNumber = 1, name = "Pilote", overview = "Début", runtime = 45)
            )
        )
        val episodes = seasonDto.toEpisodes()
        assertEquals(1, episodes.size)
        assertEquals("Pilote", episodes[0].name)
        assertEquals(45, episodes[0].runtimeMinutes)

        val galleryDto = TmdbImagesResponseDto(
            backdrops = listOf(TmdbImageDto(filePath = "/bg1.jpg")),
            posters = listOf(TmdbImageDto(filePath = "/p1.jpg")),
            logos = listOf(TmdbImageDto(filePath = "/l1.png"))
        )
        val gallery = galleryDto.toGalleryImages()
        assertTrue(gallery.any { it.aspectType == ImageAspectType.LANDSCAPE_16_9 })
        assertTrue(gallery.any { it.aspectType == ImageAspectType.PORTRAIT_2_3 })
        assertTrue(gallery.any { it.aspectType == ImageAspectType.SQUARE_1_1 })
    }

    @Test
    fun testArchiveOrgPolymorphicMappingAndMp4Resolution() {
        val doc = ArchiveDocDto(
            identifier = "LeVoyageDansLaLune1902",
            title = listOf("Le Voyage dans la Lune"),
            description = "<p>Chef-d'œuvre de <b>Georges Méliès</b>.</p>",
            creator = "Georges Méliès",
            year = 1902,
            avgRating = 4.9,
            language = listOf("fre", "French"),
            subject = listOf("Science Fiction", "Silent Films"),
            licenseUrl = "https://creativecommons.org/publicdomain/zero/1.0/"
        )

        val frLocale = RegionLocaleConfig.fromPreference("Français uniquement")
        val mapped = doc.toMovie(frLocale)!!

        assertEquals("arch_LeVoyageDansLaLune1902", mapped.id)
        assertEquals("Le Voyage dans la Lune", mapped.title)
        assertEquals("Chef-d'œuvre de Georges Méliès.", mapped.synopsis)
        assertEquals(1902, mapped.releaseYear)
        assertEquals("Français", mapped.language)
        assertEquals("Science-Fiction", mapped.category)

        val metadataResp = ArchiveItemMetadataResponseDto(
            files = listOf(
                ArchiveFileDto(name = "voyage_512kb.mp4", format = "512Kb MPEG4", height = "360", size = "15000000"),
                ArchiveFileDto(name = "voyage_hd.mp4", format = "MPEG4", height = "720", length = "14:00", size = "30000000")
            ),
            metadata = ArchiveItemMetadataDto(
                identifier = "LeVoyageDansLaLune1902",
                runtime = "14:00"
            )
        )

        val enriched = metadataResp.enrichMovie(mapped)
        assertTrue(enriched.movie.isDownloadable)
        assertEquals(14, enriched.movie.durationMinutes)
        assertTrue(enriched.movie.videoUrl.endsWith("/LeVoyageDansLaLune1902/voyage_hd.mp4"))
        assertTrue(enriched.qualityUrls.containsKey("720p HD"))
        assertEquals(30000000L, enriched.qualitySizesBytes["720p HD"])
    }

    @Test
    fun testTmdbDtoMappingAndTrailerEnrichment() {
        val frLocale = RegionLocaleConfig.fromPreference("Français uniquement")
        val genreMap = mapOf(878 to "Science-Fiction", 12 to "Aventure")
        val dto = TmdbMovieDto(
            id = 42,
            title = "Film Test",
            originalTitle = "Test Film",
            overview = "Un résumé français.",
            posterPath = "/abc.jpg",
            backdropPath = "/bg.jpg",
            releaseDate = "2025-11-01",
            voteAverage = 8.4,
            genreIds = listOf(878, 12),
            originalLanguage = "fr"
        )

        val mapped = dto.toMovie(genreMap, frLocale, isFeatured = true)
        assertEquals("tmdb_42", mapped.id)
        assertEquals("Science-Fiction", mapped.category)
        assertEquals(2025, mapped.releaseYear)
        assertEquals(4.2f, mapped.rating)
        assertFalse(mapped.isDownloadable)

        val details = TmdbMovieDetailsDto(
            id = 42,
            runtime = 124,
            genres = listOf(TmdbGenreDto(878, "Science-Fiction")),
            videos = TmdbVideoListDto(
                results = listOf(
                    TmdbVideoDto(id = "v1", key = "ytKey123", site = "YouTube", type = "Trailer", official = true)
                )
            ),
            credits = TmdbCreditsDto(
                cast = listOf(TmdbCastDto(1, "Acteur Un", "Héros")),
                crew = listOf(TmdbCrewDto(2, "Réalisateur Test", "Director"))
            )
        )

        val enriched = details.enrichMovie(mapped)
        assertEquals(124, enriched.durationMinutes)
        assertEquals("ytKey123", enriched.trailerYoutubeKey)
        assertEquals("Réalisateur Test", enriched.director)
        assertEquals(listOf("Acteur Un"), enriched.cast)
        assertEquals("", ApiMappers.stripHtml("<br/>"))
    }

    @Test
    fun testDynamicTmdbApiKeyOverride() {
        val originalCustom = com.example.gpnflix.data.remote.NetworkModule.customTmdbApiKey
        try {
            com.example.gpnflix.data.remote.NetworkModule.customTmdbApiKey =
                "  \"Bearer eyJhbGciOiJIUzI1NiJ9.dummyPayload.dummySig\"  "
            assertTrue(com.example.gpnflix.data.remote.NetworkModule.hasValidTmdbKey())
            assertTrue(com.example.gpnflix.data.remote.NetworkModule.isV4BearerToken())
            assertEquals(
                "eyJhbGciOiJIUzI1NiJ9.dummyPayload.dummySig",
                com.example.gpnflix.data.remote.NetworkModule.activeTmdbKey()
            )

            com.example.gpnflix.data.remote.NetworkModule.customTmdbApiKey = "abcdef1234567890abcdef1234567890"
            assertTrue(com.example.gpnflix.data.remote.NetworkModule.hasValidTmdbKey())
            assertFalse(com.example.gpnflix.data.remote.NetworkModule.isV4BearerToken())

            com.example.gpnflix.data.remote.NetworkModule.customTmdbApiKey = "MY_TMDB_API_KEY"
            assertFalse(com.example.gpnflix.data.remote.NetworkModule.hasValidTmdbKey())
        } finally {
            com.example.gpnflix.data.remote.NetworkModule.customTmdbApiKey = originalCustom
        }
    }
}
