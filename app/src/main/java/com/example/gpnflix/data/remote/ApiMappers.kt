package com.example.gpnflix.data.remote

import com.example.gpnflix.data.model.ImageAspectType
import com.example.gpnflix.data.model.MediaContentType
import com.example.gpnflix.data.model.MediaGalleryImage
import com.example.gpnflix.data.model.Movie
import com.example.gpnflix.data.model.MovieSourceType
import com.example.gpnflix.data.model.RegionLocaleConfig
import com.example.gpnflix.data.model.SeriesEpisode
import com.example.gpnflix.data.model.SeriesSeason
import com.example.gpnflix.data.model.UpcomingMovie
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.math.roundToInt

data class EnrichedMovieResult(
    val movie: Movie,
    val qualityUrls: Map<String, String> = emptyMap(),
    val qualitySizesBytes: Map<String, Long> = emptyMap()
)

object ApiMappers {

    private val htmlTagRegex = Regex("<[^>]+>")
    private val whitespaceRegex = Regex("\\s+")
    private val yearRegex = Regex("\\b(18\\d{2}|19\\d{2}|20\\d{2})\\b")

    fun extractString(value: Any?): String {
        return when (value) {
            null -> ""
            is String -> value.trim()
            is List<*> -> value.firstOrNull()?.toString()?.trim().orEmpty()
            is Number -> value.toString()
            else -> value.toString().trim()
        }
    }

    fun extractStringList(value: Any?): List<String> {
        return when (value) {
            null -> emptyList()
            is String -> value.split(",", ";").map { it.trim() }.filter { it.isNotEmpty() }
            is List<*> -> value.mapNotNull { it?.toString()?.trim() }.filter { it.isNotEmpty() }
            else -> listOf(value.toString().trim()).filter { it.isNotEmpty() }
        }
    }

    fun stripHtml(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .replace(htmlTagRegex, " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace(whitespaceRegex, " ")
            .replace(Regex("\\s+([.,;!?])"), "$1")
            .trim()
    }

    fun extractYear(value: Any?): Int {
        val str = extractString(value)
        val match = yearRegex.find(str)
        return match?.value?.toIntOrNull() ?: 0
    }

    fun extractArchiveRating(value: Any?): Float {
        val raw = when (value) {
            is Number -> value.toFloat()
            is String -> value.toFloatOrNull() ?: 0f
            is List<*> -> value.firstOrNull()?.toString()?.toFloatOrNull() ?: 0f
            else -> 0f
        }
        return ((raw.coerceIn(0f, 5f)) * 10f).roundToInt() / 10f
    }

    fun parseRuntimeMinutes(raw: String): Int {
        if (raw.isBlank()) return 0
        val clean = raw.trim()
        if (clean.contains(":")) {
            val parts = clean.split(":").mapNotNull { it.trim().toDoubleOrNull()?.toInt() }
            return when (parts.size) {
                3 -> parts[0] * 60 + parts[1]
                2 -> parts[0]
                else -> 0
            }
        }
        val num = clean.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: return 0
        return if (num > 300) (num / 60.0).roundToInt() else num.roundToInt()
    }

    fun resolveLanguageLabel(rawLanguages: List<String>, fallbackCode: String): String {
        val joined = rawLanguages.joinToString(" ").lowercase(Locale.ROOT)
        return when {
            joined.contains("fre") || joined.contains("fra") || joined.contains("french") || joined == "fr" -> "Français"
            joined.contains("eng") || joined.contains("english") || joined == "en" -> "Anglais"
            joined.contains("spa") || joined.contains("spanish") || joined == "es" -> "Espagnol"
            joined.contains("ger") || joined.contains("deu") || joined.contains("german") || joined == "de" -> "Allemand"
            joined.contains("ita") || joined.contains("italian") || joined == "it" -> "Italien"
            joined.contains("por") || joined.contains("portuguese") || joined == "pt" -> "Portugais"
            joined.contains("jpn") || joined.contains("japanese") || joined == "ja" -> "Japonais"
            joined.contains("silent") || joined.contains("none") -> "Muet"
            rawLanguages.isNotEmpty() -> rawLanguages.first().replaceFirstChar { it.uppercaseChar() }
            else -> when (fallbackCode) {
                "fr" -> "Français"
                "es" -> "Espagnol"
                "de" -> "Allemand"
                "it" -> "Italien"
                "pt" -> "Portugais"
                else -> "Anglais"
            }
        }
    }

    fun resolveArchiveCategory(subjects: List<String>, collections: List<String>): String {
        val haystack = (subjects + collections).joinToString(" ").lowercase(Locale.ROOT)
        return when {
            haystack.contains("animation") || haystack.contains("cartoon") || haystack.contains("dessin") -> "Animation"
            haystack.contains("music") || haystack.contains("concert") || haystack.contains("chanson") || haystack.contains("symphony") -> "Musique"
            haystack.contains("theater") || haystack.contains("theatre") || haystack.contains("théâtre") || haystack.contains("moliere") || haystack.contains("stage") -> "Théâtre"
            haystack.contains("television") || haystack.contains("tv") || haystack.contains("series") || haystack.contains("episode") -> "Séries"
            haystack.contains("sci-fi") || haystack.contains("science fiction") || haystack.contains("scifi") -> "Science-Fiction"
            haystack.contains("documentary") || haystack.contains("nasa") || haystack.contains("education") -> "Documentaire"
            haystack.contains("horror") -> "Horreur"
            haystack.contains("comedy") -> "Comédie"
            haystack.contains("action") || haystack.contains("adventure") || haystack.contains("western") -> "Aventure"
            haystack.contains("drama") -> "Drame"
            else -> "Classiques"
        }
    }

    fun resolveArchiveMediaType(subjects: List<String>, collections: List<String>, category: String): String {
        val haystack = (subjects + collections).joinToString(" ").lowercase(Locale.ROOT)
        return when {
            category == "Animation" || haystack.contains("animation") || haystack.contains("cartoon") -> MediaContentType.CARTOON
            category == "Musique" || haystack.contains("music") || haystack.contains("concert") -> MediaContentType.MUSIC
            category == "Théâtre" || haystack.contains("theater") || haystack.contains("theatre") || haystack.contains("théâtre") -> MediaContentType.THEATER
            category == "Séries" || haystack.contains("television") || haystack.contains("series") || haystack.contains("episode") -> MediaContentType.SERIES
            else -> MediaContentType.FILM
        }
    }

    fun ArchiveDocDto.toMovie(
        localeConfig: RegionLocaleConfig,
        isFeatured: Boolean = false,
        isTrending: Boolean = false,
        overrideMediaType: String? = null
    ): Movie? {
        if (identifier.isBlank()) return null
        val parsedTitle = stripHtml(extractString(title)).ifBlank { identifier }
        val parsedDesc = stripHtml(extractString(description))
            .ifBlank { "Œuvre accessible légalement en version française ou libre d'accès sur Internet Archive." }
        val parsedCreator = extractStringList(creator).joinToString(", ")
            .ifBlank { "Archive Publique" }
        val subjects = extractStringList(subject)
        val collections = extractStringList(collection)
        val rawLangs = extractStringList(language)
        val category = resolveArchiveCategory(subjects, collections)
        val mediaType = overrideMediaType ?: resolveArchiveMediaType(subjects, collections, category)
        val licenseUrlStr = extractString(licenseUrl).lowercase(Locale.ROOT)
        val licenseLabel = when {
            licenseUrlStr.contains("creativecommons.org/publicdomain/zero") -> "CC0 Domaine Public (Archive.org)"
            licenseUrlStr.contains("creativecommons.org") -> "Creative Commons (Archive.org)"
            else -> "Domaine Public / Archive.org"
        }
        val encodedId = encodeUrlSegment(identifier)
        val imageUrl = "https://archive.org/services/img/$encodedId"
        val genres = (subjects.take(3).ifEmpty { listOf(category) })
            .map { it.replaceFirstChar { c -> c.uppercaseChar() } }
            .distinct()

        val estimatedSizeMb = when (mediaType) {
            MediaContentType.SERIES -> 100
            MediaContentType.CARTOON -> 85
            MediaContentType.MUSIC -> 65
            MediaContentType.THEATER -> 140
            else -> 180
        }

        return Movie(
            id = "arch_$identifier",
            title = parsedTitle,
            originalTitle = parsedTitle,
            synopsis = parsedDesc,
            durationMinutes = 0,
            releaseYear = extractYear(year),
            rating = extractArchiveRating(avgRating),
            posterUrl = imageUrl,
            backdropUrl = imageUrl,
            videoUrl = "",
            trailerUrl = "",
            trailerYoutubeKey = null,
            sourceType = MovieSourceType.ARCHIVE_ORG,
            tmdbId = null,
            archiveIdentifier = identifier,
            category = category,
            genres = genres,
            director = parsedCreator,
            cast = emptyList(),
            language = resolveLanguageLabel(rawLangs, localeConfig.languageCode),
            licenseType = licenseLabel,
            isDownloadable = false,
            isFeatured = isFeatured,
            isTrending = isTrending,
            isAwardWinner = extractArchiveRating(avgRating) >= 4.4f,
            qualities = listOf("Auto", "720p HD", "480p SD", "360p Éco"),
            mediaType = mediaType,
            estimatedSizeMb = estimatedSizeMb,
            isFamilyFriendly = mediaType == MediaContentType.CARTOON || mediaType == MediaContentType.MUSIC
        )
    }

    fun ArchiveItemMetadataResponseDto.enrichMovie(existing: Movie): EnrichedMovieResult {
        val identifier = existing.archiveIdentifier
            ?: metadata?.identifier
            ?: existing.id.removePrefix("arch_")

        val mp4Files = files
            .filter { file ->
                val lowerName = file.name.lowercase(Locale.ROOT)
                val lowerFormat = file.format?.lowercase(Locale.ROOT).orEmpty()
                lowerName.endsWith(".mp4") &&
                    !lowerName.contains("_512kb") ||
                    (lowerName.endsWith(".mp4") && lowerFormat.contains("mpeg4"))
            }
            .sortedWith(
                compareByDescending<ArchiveFileDto> { it.height?.toIntOrNull() ?: 0 }
                    .thenByDescending { it.size?.toLongOrNull() ?: 0L }
                    .thenByDescending { it.format?.contains("MPEG4", ignoreCase = true) == true }
                    .thenBy { it.name.contains("_512kb", ignoreCase = true) }
            )

        val qualityUrls = linkedMapOf<String, String>()
        val qualitySizesBytes = linkedMapOf<String, Long>()
        val encodedIdentifier = encodeUrlSegment(identifier)
        mp4Files.forEach { file ->
            val encodedPath = file.name.split("/").joinToString("/") { encodeUrlSegment(it) }
            val fullUrl = "https://archive.org/download/$encodedIdentifier/$encodedPath"
            val heightVal = file.height?.toIntOrNull()
            val label = when {
                heightVal != null && heightVal >= 2160 -> "4K UHD"
                heightVal != null && heightVal >= 1080 -> "1080p HD"
                heightVal != null && heightVal >= 720 -> "720p HD"
                heightVal != null && heightVal >= 480 -> "480p SD"
                file.name.contains("512kb", ignoreCase = true) -> "360p Éco"
                else -> "MP4 Standard"
            }
            if (!qualityUrls.containsKey(label)) {
                qualityUrls[label] = fullUrl
                file.size?.toLongOrNull()?.takeIf { it > 0L }?.let { bytes ->
                    qualitySizesBytes[label] = bytes
                }
            }
        }

        val bestUrl = qualityUrls.values.firstOrNull().orEmpty()
        val parsedRuntime = extractString(metadata?.runtime)
            .let { parseRuntimeMinutes(it) }
            .takeIf { it > 0 }
            ?: files.firstNotNullOfOrNull { f ->
                f.length?.let { parseRuntimeMinutes(it) }?.takeIf { it > 0 }
            }
            ?: existing.durationMinutes

        val highestSizeMb = qualitySizesBytes.values.maxOrNull()
            ?.let { (it / (1024L * 1024L)).toInt().coerceAtLeast(1) }
            ?: existing.estimatedSizeMb

        val updatedTitle = stripHtml(extractString(metadata?.title)).ifBlank { existing.title }
        val updatedSynopsis = stripHtml(extractString(metadata?.description)).ifBlank { existing.synopsis }
        val updatedDirector = extractStringList(metadata?.creator).joinToString(", ").ifBlank { existing.director }
        val updatedQualities = if (qualityUrls.isNotEmpty()) {
            listOf("Auto") + qualityUrls.keys.toList()
        } else {
            existing.qualities
        }

        val enriched = existing.copy(
            title = updatedTitle,
            synopsis = updatedSynopsis,
            durationMinutes = parsedRuntime,
            director = updatedDirector,
            videoUrl = bestUrl,
            isDownloadable = bestUrl.isNotBlank(),
            qualities = updatedQualities,
            estimatedSizeMb = highestSizeMb
        )
        return EnrichedMovieResult(
            movie = enriched,
            qualityUrls = qualityUrls,
            qualitySizesBytes = qualitySizesBytes
        )
    }

    fun TmdbMovieDto.toMovie(
        genreMap: Map<Int, String>,
        localeConfig: RegionLocaleConfig,
        isFeatured: Boolean = false,
        isTrending: Boolean = false,
        overrideMediaType: String? = null
    ): Movie {
        val genres = genreIds.mapNotNull { genreMap[it] }.ifEmpty { listOf("Cinéma") }
        val primaryCategory = genres.first()
        val inferredType = overrideMediaType ?: when {
            genreIds.contains(16) -> MediaContentType.CARTOON
            genreIds.contains(10402) -> MediaContentType.MUSIC
            else -> MediaContentType.FILM
        }
        val poster = NetworkModule.buildTmdbPosterUrl(posterPath)
        val backdrop = NetworkModule.buildTmdbBackdropUrl(backdropPath ?: posterPath)
        val releaseYr = releaseDate?.take(4)?.toIntOrNull() ?: 0
        val starRating = voteAverage?.let { (((it / 2.0) * 10.0).roundToInt() / 10.0).toFloat() } ?: 0f

        return Movie(
            id = "tmdb_$id",
            title = title?.takeIf { it.isNotBlank() } ?: originalTitle ?: "Titre inconnu",
            originalTitle = originalTitle.orEmpty(),
            synopsis = overview?.takeIf { it.isNotBlank() } ?: "Synopsis en français disponible sur fiche détaillée.",
            durationMinutes = 0,
            releaseYear = releaseYr,
            rating = starRating,
            posterUrl = poster,
            backdropUrl = backdrop,
            videoUrl = "",
            trailerUrl = "",
            trailerYoutubeKey = null,
            sourceType = MovieSourceType.TMDB,
            tmdbId = id,
            archiveIdentifier = null,
            category = primaryCategory,
            genres = genres,
            director = "Voir fiche technique",
            cast = emptyList(),
            language = if (localeConfig.frenchOnly) "Français" else resolveLanguageLabel(listOfNotNull(originalLanguage), localeConfig.languageCode),
            licenseType = "Base officielle TMDB • Bande-annonce légale",
            isDownloadable = false,
            isFeatured = isFeatured,
            isTrending = isTrending,
            isAwardWinner = starRating >= 4.0f,
            qualities = listOf("4K UHD", "1080p Full HD", "720p HD"),
            mediaType = inferredType,
            estimatedSizeMb = when (inferredType) {
                MediaContentType.CARTOON -> 120
                MediaContentType.MUSIC -> 80
                else -> 240
            },
            isFamilyFriendly = genreIds.contains(16) || genreIds.contains(10751) || genreIds.contains(10402)
        )
    }

    fun TmdbTvDto.toMovie(
        genreMap: Map<Int, String>,
        localeConfig: RegionLocaleConfig,
        isFeatured: Boolean = false,
        isTrending: Boolean = false,
        overrideMediaType: String? = null
    ): Movie {
        val genres = genreIds.mapNotNull { genreMap[it] }.ifEmpty { listOf("Séries") }
        val primaryCategory = genres.first()
        val inferredType = overrideMediaType ?: when {
            genreIds.contains(16) -> MediaContentType.CARTOON
            else -> MediaContentType.SERIES
        }
        val poster = NetworkModule.buildTmdbPosterUrl(posterPath)
        val backdrop = NetworkModule.buildTmdbBackdropUrl(backdropPath ?: posterPath)
        val releaseYr = firstAirDate?.take(4)?.toIntOrNull() ?: 0
        val starRating = voteAverage?.let { (((it / 2.0) * 10.0).roundToInt() / 10.0).toFloat() } ?: 0f

        return Movie(
            id = "tmdb_tv_$id",
            title = name?.takeIf { it.isNotBlank() } ?: originalName ?: "Série inconnue",
            originalTitle = originalName.orEmpty(),
            synopsis = overview?.takeIf { it.isNotBlank() } ?: "Synopsis français de la série sur fiche détaillée.",
            durationMinutes = 45,
            releaseYear = releaseYr,
            rating = starRating,
            posterUrl = poster,
            backdropUrl = backdrop,
            videoUrl = "",
            trailerUrl = "",
            trailerYoutubeKey = null,
            sourceType = MovieSourceType.TMDB,
            tmdbId = id,
            archiveIdentifier = null,
            category = primaryCategory,
            genres = genres,
            director = "Série TV",
            cast = emptyList(),
            language = if (localeConfig.frenchOnly) "Français" else resolveLanguageLabel(listOfNotNull(originalLanguage), localeConfig.languageCode),
            licenseType = "Base officielle TMDB Séries • Bande-annonce légale",
            isDownloadable = false,
            isFeatured = isFeatured,
            isTrending = isTrending,
            isAwardWinner = starRating >= 4.0f,
            qualities = listOf("1080p Full HD", "720p HD", "480p SD (-50%)"),
            mediaType = inferredType,
            estimatedSizeMb = 100, // Episode typical HD footprint (100 Mo -> 50 Mo compressed)
            isFamilyFriendly = genreIds.contains(16) || genreIds.contains(10751) || genreIds.contains(10762)
        )
    }

    fun TmdbMovieDetailsDto.enrichMovie(
        existing: Movie,
        englishFallback: TmdbMovieDetailsDto? = null
    ): Movie {
        val localizedOverview = overview?.takeIf { it.isNotBlank() }
        val fallbackOverview = englishFallback?.overview?.takeIf { it.isNotBlank() }
        val resolvedSynopsis = localizedOverview ?: fallbackOverview ?: existing.synopsis

        val directorName = credits?.crew
            ?.firstOrNull { it.job.equals("Director", ignoreCase = true) }
            ?.name
            ?.takeIf { it.isNotBlank() }
            ?: existing.director

        val castNames = credits?.cast
            ?.take(8)
            ?.map { it.name }
            ?.filter { it.isNotBlank() }
            ?: existing.cast

        val allVideos = ((videos?.results.orEmpty()) + (englishFallback?.videos?.results.orEmpty()))
            .filter { it.site.equals("YouTube", ignoreCase = true) && it.key.isNotBlank() }

        val bestTrailer = allVideos.firstOrNull {
            it.type.equals("Trailer", ignoreCase = true) && it.official
        } ?: allVideos.firstOrNull {
            it.type.equals("Trailer", ignoreCase = true)
        } ?: allVideos.firstOrNull {
            it.type.equals("Teaser", ignoreCase = true)
        } ?: allVideos.firstOrNull()

        val genreNames = genres.map { it.name }.ifEmpty { existing.genres }
        val poster = posterPath?.let { NetworkModule.buildTmdbPosterUrl(it) } ?: existing.posterUrl
        val backdrop = (backdropPath ?: posterPath)?.let { NetworkModule.buildTmdbBackdropUrl(it) } ?: existing.backdropUrl
        val starRating = voteAverage?.let { (((it / 2.0) * 10.0).roundToInt() / 10.0).toFloat() } ?: existing.rating
        val gallery = images?.toGalleryImages(poster, backdrop).orEmpty()

        return existing.copy(
            title = title?.takeIf { it.isNotBlank() } ?: existing.title,
            originalTitle = originalTitle?.takeIf { it.isNotBlank() } ?: existing.originalTitle,
            synopsis = resolvedSynopsis,
            durationMinutes = runtime ?: existing.durationMinutes,
            releaseYear = releaseDate?.take(4)?.toIntOrNull() ?: existing.releaseYear,
            rating = starRating,
            posterUrl = poster,
            backdropUrl = backdrop,
            trailerYoutubeKey = bestTrailer?.key ?: existing.trailerYoutubeKey,
            trailerUrl = bestTrailer?.key?.let { "https://www.youtube.com/watch?v=$it" } ?: existing.trailerUrl,
            category = genreNames.firstOrNull() ?: existing.category,
            genres = genreNames,
            director = directorName,
            cast = castNames,
            galleryImages = gallery
        )
    }

    fun TmdbTvDetailsDto.enrichMovie(
        existing: Movie,
        englishFallback: TmdbTvDetailsDto? = null
    ): Movie {
        val localizedOverview = overview?.takeIf { it.isNotBlank() }
        val fallbackOverview = englishFallback?.overview?.takeIf { it.isNotBlank() }
        val resolvedSynopsis = localizedOverview ?: fallbackOverview ?: existing.synopsis

        val creatorName = createdBy.firstOrNull()?.name?.takeIf { it.isNotBlank() }
            ?: credits?.crew?.firstOrNull { it.job.equals("Executive Producer", ignoreCase = true) }?.name
            ?: existing.director

        val castNames = credits?.cast
            ?.take(8)
            ?.map { it.name }
            ?.filter { it.isNotBlank() }
            ?: existing.cast

        val allVideos = ((videos?.results.orEmpty()) + (englishFallback?.videos?.results.orEmpty()))
            .filter { it.site.equals("YouTube", ignoreCase = true) && it.key.isNotBlank() }

        val bestTrailer = allVideos.firstOrNull {
            it.type.equals("Trailer", ignoreCase = true) && it.official
        } ?: allVideos.firstOrNull {
            it.type.equals("Trailer", ignoreCase = true)
        } ?: allVideos.firstOrNull()

        val genreNames = genres.map { it.name }.ifEmpty { existing.genres }
        val poster = posterPath?.let { NetworkModule.buildTmdbPosterUrl(it) } ?: existing.posterUrl
        val backdrop = (backdropPath ?: posterPath)?.let { NetworkModule.buildTmdbBackdropUrl(it) } ?: existing.backdropUrl
        val starRating = voteAverage?.let { (((it / 2.0) * 10.0).roundToInt() / 10.0).toFloat() } ?: existing.rating
        val seasonList = seasons
            .filter { it.seasonNumber > 0 || seasons.size == 1 }
            .map { s ->
                SeriesSeason(
                    seasonNumber = s.seasonNumber,
                    name = s.name.ifBlank { "Saison ${s.seasonNumber}" },
                    episodeCount = s.episodeCount,
                    posterUrl = NetworkModule.buildTmdbPosterUrl(s.posterPath)
                )
            }
        val gallery = images?.toGalleryImages(poster, backdrop).orEmpty()

        return existing.copy(
            title = name?.takeIf { it.isNotBlank() } ?: existing.title,
            originalTitle = originalName?.takeIf { it.isNotBlank() } ?: existing.originalTitle,
            synopsis = resolvedSynopsis,
            durationMinutes = episodeRunTime.firstOrNull() ?: existing.durationMinutes,
            releaseYear = firstAirDate?.take(4)?.toIntOrNull() ?: existing.releaseYear,
            rating = starRating,
            posterUrl = poster,
            backdropUrl = backdrop,
            trailerYoutubeKey = bestTrailer?.key ?: existing.trailerYoutubeKey,
            trailerUrl = bestTrailer?.key?.let { "https://www.youtube.com/watch?v=$it" } ?: existing.trailerUrl,
            category = genreNames.firstOrNull() ?: existing.category,
            genres = genreNames,
            director = creatorName,
            cast = castNames,
            seasons = seasonList,
            galleryImages = gallery,
            estimatedSizeMb = 100
        )
    }

    fun TmdbSeasonDetailsDto.toEpisodes(): List<SeriesEpisode> {
        return episodes.map { ep ->
            SeriesEpisode(
                episodeNumber = ep.episodeNumber,
                seasonNumber = ep.seasonNumber,
                name = ep.name?.takeIf { it.isNotBlank() } ?: "Épisode ${ep.episodeNumber}",
                overview = ep.overview?.takeIf { it.isNotBlank() } ?: "Résumé de l'épisode en français.",
                runtimeMinutes = ep.runtime ?: 42,
                stillUrl = NetworkModule.buildTmdbBackdropUrl(ep.stillPath),
                airDate = ep.airDate.orEmpty()
            )
        }
    }

    fun TmdbImagesResponseDto.toGalleryImages(
        fallbackPoster: String = "",
        fallbackBackdrop: String = ""
    ): List<MediaGalleryImage> {
        val list = mutableListOf<MediaGalleryImage>()
        backdrops.take(6).forEachIndexed { idx, img ->
            val url = NetworkModule.buildTmdbBackdropUrl(img.filePath)
            if (url.isNotBlank()) {
                list.add(MediaGalleryImage(url = url, aspectType = ImageAspectType.LANDSCAPE_16_9, label = "Capture HD #${idx + 1}"))
            }
        }
        posters.take(6).forEachIndexed { idx, img ->
            val url = NetworkModule.buildTmdbPosterUrl(img.filePath)
            if (url.isNotBlank()) {
                list.add(MediaGalleryImage(url = url, aspectType = ImageAspectType.PORTRAIT_2_3, label = "Affiche #${idx + 1}"))
            }
        }
        logos.take(3).forEachIndexed { idx, img ->
            val url = NetworkModule.buildTmdbPosterUrl(img.filePath)
            if (url.isNotBlank()) {
                list.add(MediaGalleryImage(url = url, aspectType = ImageAspectType.SQUARE_1_1, label = "Vignette #${idx + 1}"))
            }
        }
        if (list.isEmpty()) {
            if (fallbackBackdrop.isNotBlank()) {
                list.add(MediaGalleryImage(fallbackBackdrop, ImageAspectType.LANDSCAPE_16_9, "Bannière 16:9"))
            }
            if (fallbackPoster.isNotBlank()) {
                list.add(MediaGalleryImage(fallbackPoster, ImageAspectType.PORTRAIT_2_3, "Affiche 2:3"))
                list.add(MediaGalleryImage(fallbackPoster, ImageAspectType.SQUARE_1_1, "Vignette 1:1"))
            }
        }
        return list.distinctBy { "${it.aspectType}_${it.url}" }
    }

    fun TmdbMovieDto.toUpcomingMovie(genreMap: Map<Int, String>): UpcomingMovie {
        val genreName = genreIds.firstOrNull()?.let { genreMap[it] } ?: "Cinéma"
        val poster = NetworkModule.buildTmdbPosterUrl(posterPath)
        val backdrop = NetworkModule.buildTmdbBackdropUrl(backdropPath ?: posterPath)
        return UpcomingMovie(
            id = "tmdb_$id",
            title = title?.takeIf { it.isNotBlank() } ?: originalTitle ?: "Prochaine sortie",
            expectedReleaseDate = releaseDate?.takeIf { it.isNotBlank() } ?: "Prochainement",
            posterUrl = poster,
            backdropUrl = backdrop,
            synopsis = overview?.takeIf { it.isNotBlank() } ?: "Sortie cinéma à venir référencée en français sur TMDB.",
            category = genreName,
            licenseNotice = "TMDB • Sortie officielle à venir",
            tmdbId = id,
            mediaType = if (genreIds.contains(16)) MediaContentType.CARTOON else MediaContentType.FILM
        )
    }

    fun ArchiveDocDto.toUpcomingMovie(localeConfig: RegionLocaleConfig): UpcomingMovie? {
        val movie = toMovie(localeConfig) ?: return null
        val releaseYr = if (movie.releaseYear > 0) movie.releaseYear.toString() else "Nouvel ajout"
        return UpcomingMovie(
            id = movie.id,
            title = movie.title,
            expectedReleaseDate = releaseYr,
            posterUrl = movie.posterUrl,
            backdropUrl = movie.backdropUrl,
            synopsis = movie.synopsis,
            category = movie.category,
            licenseNotice = movie.licenseType,
            archiveIdentifier = movie.archiveIdentifier,
            mediaType = movie.mediaType
        )
    }

    private fun encodeUrlSegment(segment: String): String {
        return URLEncoder.encode(segment, StandardCharsets.UTF_8.name()).replace("+", "%20")
    }
}
