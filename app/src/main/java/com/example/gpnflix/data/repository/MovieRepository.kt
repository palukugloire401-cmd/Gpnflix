package com.example.gpnflix.data.repository

import android.content.Context
import android.net.Uri
import com.example.gpnflix.data.local.DownloadDao
import com.example.gpnflix.data.local.DownloadEntity
import com.example.gpnflix.data.local.DownloadStatus
import com.example.gpnflix.data.local.FavoriteDao
import com.example.gpnflix.data.local.FavoriteEntity
import com.example.gpnflix.data.local.GpnFlixDatabase
import com.example.gpnflix.data.local.OfflineDownloadManager
import com.example.gpnflix.data.local.VideoDownloadWithMetadata
import com.example.gpnflix.data.local.VideoMetadataDao
import com.example.gpnflix.data.local.VideoSpaceCompressor
import com.example.gpnflix.data.local.WatchHistoryDao
import com.example.gpnflix.data.local.WatchHistoryEntity
import com.example.gpnflix.data.local.toVideoMetadataEntity
import com.example.gpnflix.data.model.CategoryItem
import com.example.gpnflix.data.model.MediaContentType
import com.example.gpnflix.data.model.Movie
import com.example.gpnflix.data.model.MovieSourceType
import com.example.gpnflix.data.model.RegionLocaleConfig
import com.example.gpnflix.data.model.SeriesEpisode
import com.example.gpnflix.data.model.SpaceCompressionPreset
import com.example.gpnflix.data.model.UpcomingMovie
import com.example.gpnflix.data.remote.ApiMappers.enrichMovie
import com.example.gpnflix.data.remote.ApiMappers.toEpisodes
import com.example.gpnflix.data.remote.ApiMappers.toMovie
import com.example.gpnflix.data.remote.ApiMappers.toUpcomingMovie
import com.example.gpnflix.data.remote.ArchiveOrgApiService
import com.example.gpnflix.data.remote.EnrichedMovieResult
import com.example.gpnflix.data.remote.NetworkModule
import com.example.gpnflix.data.remote.TmdbApiService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.Locale

data class HomeSection(
    val id: String,
    val title: String,
    val movies: List<Movie>
)

data class HomeCatalogResult(
    val featuredMovies: List<Movie>,
    val trendingMovies: List<Movie>,
    val allLoadedMovies: List<Movie>,
    val sections: List<HomeSection>,
    val categories: List<CategoryItem>,
    val activeSourceLabel: String,
    val loadedFromCacheFallback: Boolean = false
)

class MovieRepository(
    private val videoMetadataDao: VideoMetadataDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val favoriteDao: FavoriteDao,
    private val downloadDao: DownloadDao,
    private val downloadManager: OfflineDownloadManager,
    private val tmdbApi: TmdbApiService = NetworkModule.tmdbApi,
    private val archiveOrgApi: ArchiveOrgApiService = NetworkModule.archiveOrgApi
) {
    private var cachedGenreMap: Map<Int, String> = emptyMap()
    private var cachedTvGenreMap: Map<Int, String> = emptyMap()

    suspend fun reconcileDownloadsOnStartup() {
        downloadManager.reconcileOnStartup()
    }

    fun observeAllVideos(): Flow<List<Movie>> =
        videoMetadataDao.observeAllVideos().map { list -> list.map { it.toMovie() } }

    fun observeVideoById(id: String): Flow<Movie?> =
        videoMetadataDao.observeVideoById(id).map { it?.toMovie() }

    suspend fun getCachedMovieById(id: String): Movie? =
        videoMetadataDao.getVideoById(id)?.toMovie()

    fun searchVideosInRoom(
        query: String,
        categoryFilter: String,
        yearFilter: Int?,
        languageFilter: String
    ): Flow<List<Movie>> =
        videoMetadataDao.searchVideos(
            query = query,
            category = categoryFilter,
            year = yearFilter,
            language = languageFilter
        ).map { list -> list.map { it.toMovie() } }

    private fun baseArchiveCategories(localeConfig: RegionLocaleConfig): List<CategoryItem> {
        val langSuffix = if (localeConfig.frenchOnly) " AND ${localeConfig.archiveLanguageQuery}" else ""
        return listOf(
            CategoryItem(
                id = "all",
                name = "Tout en Français",
                description = "Films, séries, musique, dessins animés et théâtre en français",
                archiveQuery = "mediatype:movies AND (collection:feature_films OR collection:animationandcartoons OR collection:opensource_movies)$langSuffix"
            ),
            CategoryItem(
                id = MediaContentType.FILM,
                name = "Films",
                description = "Longs-métrages et cinéma international en version française",
                archiveQuery = "mediatype:movies AND (collection:feature_films OR collection:opensource_movies)$langSuffix"
            ),
            CategoryItem(
                id = MediaContentType.SERIES,
                name = "Séries",
                description = "Séries TV populaires, saisons complètes et compression 100 Mo → 50 Mo",
                archiveQuery = "mediatype:movies AND (subject:television OR subject:series OR collection:classic_tv)$langSuffix"
            ),
            CategoryItem(
                id = MediaContentType.CARTOON,
                name = "Dessins animés",
                description = "Animation familiale, dessins animés classiques et courts-métrages ouverts",
                archiveQuery = "mediatype:movies AND collection:animationandcartoons$langSuffix"
            ),
            CategoryItem(
                id = MediaContentType.MUSIC,
                name = "Musique & Concerts",
                description = "Films musicaux, concerts live et captations musicales en français",
                archiveQuery = "mediatype:movies AND (subject:music OR subject:concert OR subject:chanson)$langSuffix"
            ),
            CategoryItem(
                id = MediaContentType.THEATER,
                name = "Théâtre & Scène",
                description = "Pièces de théâtre, classiques dramatiques et arts vivants",
                archiveQuery = "mediatype:movies AND (subject:theater OR subject:theatre OR subject:moliere OR subject:drama)$langSuffix"
            ),
            CategoryItem(
                id = "Français",
                name = "Cinéma Francophone",
                description = "Œuvres francophones intégrales (${localeConfig.displayLabel})",
                archiveQuery = "mediatype:movies AND (collection:feature_films OR collection:opensource_movies) AND ${localeConfig.archiveLanguageQuery}"
            ),
            CategoryItem(
                id = "Science-Fiction",
                name = "Sci-Fi & Fantastique",
                description = "Récits futuristes et cinéma fantastique",
                archiveQuery = "mediatype:movies AND (collection:SciFi_Horror OR subject:\"science fiction\")"
            ),
            CategoryItem(
                id = "Documentaire",
                name = "Documentaires & Sciences",
                description = "Exploration spatiale NASA, histoire et découvertes",
                archiveQuery = "mediatype:movies AND (collection:nasa OR subject:documentary)"
            )
        )
    }

    private suspend fun searchArchiveFrenchPrioritized(
        baseQuery: String,
        rows: Int,
        localeConfig: RegionLocaleConfig,
        overrideMediaType: String? = null,
        overrideCategory: String? = null
    ): List<Movie> {
        val strictFrenchDocs = runCatching {
            archiveOrgApi.searchMovies(
                query = "$baseQuery AND ${localeConfig.archiveLanguageQuery}",
                rows = rows
            ).response?.docs.orEmpty()
        }.getOrDefault(emptyList())

        val combinedDocs = if (strictFrenchDocs.size >= 4 || localeConfig.frenchOnly && strictFrenchDocs.isNotEmpty()) {
            strictFrenchDocs
        } else {
            val fallbackDocs = runCatching {
                archiveOrgApi.searchMovies(
                    query = baseQuery,
                    rows = rows
                ).response?.docs.orEmpty()
            }.getOrDefault(emptyList())
            (strictFrenchDocs + fallbackDocs).distinctBy { it.identifier }
        }

        return combinedDocs.mapNotNull { doc ->
            doc.toMovie(
                localeConfig = localeConfig,
                overrideMediaType = overrideMediaType
            )?.let { m ->
                val localizedCat = overrideCategory ?: m.category
                if (localeConfig.frenchOnly) {
                    m.copy(category = localizedCat, language = "Français")
                } else {
                    m.copy(category = localizedCat)
                }
            }
        }
    }

    suspend fun fetchHomeCatalog(localeConfig: RegionLocaleConfig): HomeCatalogResult = coroutineScope {
        val tmdbEnabled = NetworkModule.hasValidTmdbKey()

        val genreMapDeferred = async {
            if (!tmdbEnabled) emptyMap()
            else runCatching {
                tmdbApi.getGenres(localeConfig.tmdbLanguage).genres.associate { it.id to it.name }
            }.getOrDefault(emptyMap())
        }

        val tvGenreMapDeferred = async {
            if (!tmdbEnabled) emptyMap()
            else runCatching {
                tmdbApi.getTvGenres(localeConfig.tmdbLanguage).genres.associate { it.id to it.name }
            }.getOrDefault(emptyMap())
        }

        // Archive.org multi-type French-prioritized streams
        val localizedArchiveDeferred = async {
            searchArchiveFrenchPrioritized(
                baseQuery = "mediatype:movies AND (collection:feature_films OR collection:opensource_movies)",
                rows = 14,
                localeConfig = localeConfig,
                overrideMediaType = MediaContentType.FILM
            ).mapIndexed { idx, m ->
                m.copy(
                    isFeatured = !tmdbEnabled && idx < 3,
                    isTrending = !tmdbEnabled && idx < 6
                )
            }
        }

        val archiveSeriesDeferred = async {
            searchArchiveFrenchPrioritized(
                baseQuery = "mediatype:movies AND (subject:television OR subject:series OR collection:classic_tv)",
                rows = 10,
                localeConfig = localeConfig,
                overrideMediaType = MediaContentType.SERIES,
                overrideCategory = "Séries"
            )
        }

        val animationArchiveDeferred = async {
            searchArchiveFrenchPrioritized(
                baseQuery = "mediatype:movies AND collection:animationandcartoons",
                rows = 10,
                localeConfig = localeConfig,
                overrideMediaType = MediaContentType.CARTOON,
                overrideCategory = "Dessins animés"
            )
        }

        val musicArchiveDeferred = async {
            searchArchiveFrenchPrioritized(
                baseQuery = "mediatype:movies AND (subject:music OR subject:concert OR subject:chanson)",
                rows = 10,
                localeConfig = localeConfig,
                overrideMediaType = MediaContentType.MUSIC,
                overrideCategory = "Musique"
            )
        }

        val theaterArchiveDeferred = async {
            searchArchiveFrenchPrioritized(
                baseQuery = "mediatype:movies AND (subject:theater OR subject:theatre OR subject:moliere OR subject:stage)",
                rows = 10,
                localeConfig = localeConfig,
                overrideMediaType = MediaContentType.THEATER,
                overrideCategory = "Théâtre"
            )
        }

        val docsArchiveDeferred = async {
            searchArchiveFrenchPrioritized(
                baseQuery = "mediatype:movies AND (collection:nasa OR subject:documentary)",
                rows = 8,
                localeConfig = localeConfig,
                overrideMediaType = MediaContentType.FILM,
                overrideCategory = "Documentaire"
            )
        }

        val genreMap = genreMapDeferred.await().also { if (it.isNotEmpty()) cachedGenreMap = it }
        val tvGenreMap = tvGenreMapDeferred.await().also { if (it.isNotEmpty()) cachedTvGenreMap = it }

        val tmdbTrendingMoviesDeferred = async {
            if (!tmdbEnabled) emptyList()
            else runCatching {
                tmdbApi.getTrendingMovies(localeConfig.tmdbLanguage).results
                    .let { list ->
                        if (localeConfig.frenchOnly) {
                            list.filter { !it.overview.isNullOrBlank() }.ifEmpty { list }
                        } else list
                    }
                    .mapIndexed { idx, dto ->
                        dto.toMovie(
                            genreMap = genreMap,
                            localeConfig = localeConfig,
                            isFeatured = idx < 4,
                            isTrending = true
                        )
                    }
            }.getOrDefault(emptyList())
        }

        val tmdbNowPlayingDeferred = async {
            if (!tmdbEnabled) emptyList()
            else runCatching {
                tmdbApi.getNowPlayingMovies(localeConfig.tmdbLanguage, localeConfig.regionCode).results
                    .let { list ->
                        if (localeConfig.frenchOnly) {
                            list.filter { !it.overview.isNullOrBlank() }.ifEmpty { list }
                        } else list
                    }
                    .map { dto ->
                        dto.toMovie(
                            genreMap = genreMap,
                            localeConfig = localeConfig,
                            overrideMediaType = MediaContentType.FILM
                        )
                    }
            }.getOrDefault(emptyList())
        }

        val tmdbSeriesDeferred = async {
            if (!tmdbEnabled) emptyList()
            else runCatching {
                val trending = tmdbApi.getTrendingTv(localeConfig.tmdbLanguage).results
                val discover = tmdbApi.discoverTv(language = localeConfig.tmdbLanguage).results
                (trending + discover).distinctBy { it.id }
                    .let { list ->
                        if (localeConfig.frenchOnly) {
                            list.filter { !it.overview.isNullOrBlank() }.ifEmpty { list }
                        } else list
                    }
                    .mapIndexed { idx, dto ->
                        dto.toMovie(
                            genreMap = tvGenreMap.ifEmpty { genreMap },
                            localeConfig = localeConfig,
                            isFeatured = idx == 0,
                            isTrending = idx < 3,
                            overrideMediaType = MediaContentType.SERIES
                        )
                    }
            }.getOrDefault(emptyList())
        }

        val tmdbCartoonsDeferred = async {
            if (!tmdbEnabled) emptyList()
            else runCatching {
                val animeMovies = tmdbApi.discoverMovies(
                    language = localeConfig.tmdbLanguage,
                    region = localeConfig.regionCode,
                    genreIds = "16"
                ).results.map {
                    it.toMovie(genreMap, localeConfig, overrideMediaType = MediaContentType.CARTOON)
                        .copy(category = "Dessins animés", isFamilyFriendly = true)
                }
                val animeTv = tmdbApi.discoverTv(
                    language = localeConfig.tmdbLanguage,
                    genreIds = "16"
                ).results.take(8).map {
                    it.toMovie(tvGenreMap.ifEmpty { genreMap }, localeConfig, overrideMediaType = MediaContentType.CARTOON)
                        .copy(category = "Dessins animés", isFamilyFriendly = true)
                }
                (animeMovies + animeTv).distinctBy { it.id }
            }.getOrDefault(emptyList())
        }

        val tmdbMusicDeferred = async {
            if (!tmdbEnabled) emptyList()
            else runCatching {
                tmdbApi.discoverMovies(
                    language = localeConfig.tmdbLanguage,
                    region = localeConfig.regionCode,
                    genreIds = "10402"
                ).results.map {
                    it.toMovie(genreMap, localeConfig, overrideMediaType = MediaContentType.MUSIC)
                        .copy(category = "Musique", isFamilyFriendly = true)
                }
            }.getOrDefault(emptyList())
        }

        val tmdbTheaterDeferred = async {
            if (!tmdbEnabled) emptyList()
            else runCatching {
                tmdbApi.discoverMovies(
                    language = localeConfig.tmdbLanguage,
                    region = localeConfig.regionCode,
                    genreIds = "18",
                    keywords = "10181|4344"
                ).results.ifEmpty {
                    tmdbApi.discoverMovies(
                        language = localeConfig.tmdbLanguage,
                        region = localeConfig.regionCode,
                        genreIds = "18",
                        originalLanguage = "fr"
                    ).results
                }.map {
                    it.toMovie(genreMap, localeConfig, overrideMediaType = MediaContentType.THEATER)
                        .copy(category = "Théâtre")
                }
            }.getOrDefault(emptyList())
        }

        val localizedArchive = localizedArchiveDeferred.await()
        val archiveSeries = archiveSeriesDeferred.await()
        val animationArchive = animationArchiveDeferred.await()
        val musicArchive = musicArchiveDeferred.await()
        val theaterArchive = theaterArchiveDeferred.await()
        val docsArchive = docsArchiveDeferred.await()

        val tmdbTrendingMovies = tmdbTrendingMoviesDeferred.await()
        val tmdbNowPlaying = tmdbNowPlayingDeferred.await()
        val tmdbSeries = tmdbSeriesDeferred.await()
        val tmdbCartoons = tmdbCartoonsDeferred.await()
        val tmdbMusic = tmdbMusicDeferred.await()
        val tmdbTheater = tmdbTheaterDeferred.await()

        val allFilms = (tmdbTrendingMovies + tmdbNowPlaying + localizedArchive + docsArchive).distinctBy { it.id }
        val allSeries = (tmdbSeries + archiveSeries).distinctBy { it.id }
        val allCartoons = (tmdbCartoons + animationArchive).distinctBy { it.id }
        val allMusic = (tmdbMusic + musicArchive).distinctBy { it.id }
        val allTheater = (tmdbTheater + theaterArchive).distinctBy { it.id }

        val combinedList = (tmdbTrendingMovies + tmdbSeries + allFilms + allSeries + allCartoons + allMusic + allTheater)
            .distinctBy { it.id }

        if (combinedList.isEmpty()) {
            val cached = videoMetadataDao.observeAllVideos().first().map { it.toMovie() }
            val fallbackFeatured = cached.filter { it.isFeatured }.ifEmpty { cached.take(5) }
            val fallbackTrending = cached.filter { it.isTrending }.ifEmpty { cached.take(10) }
            val sections = buildHomeSections(
                localeConfig = localeConfig,
                films = cached.filter { it.mediaType == MediaContentType.FILM },
                series = cached.filter { it.mediaType == MediaContentType.SERIES },
                cartoons = cached.filter { it.mediaType == MediaContentType.CARTOON },
                music = cached.filter { it.mediaType == MediaContentType.MUSIC },
                theater = cached.filter { it.mediaType == MediaContentType.THEATER },
                localizedArchive = cached.filter { it.sourceType == MovieSourceType.ARCHIVE_ORG },
                docs = cached.filter { it.category == "Documentaire" }
            )
            return@coroutineScope HomeCatalogResult(
                featuredMovies = fallbackFeatured,
                trendingMovies = fallbackTrending,
                allLoadedMovies = cached,
                sections = sections,
                categories = enrichCategoriesWithBanners(baseArchiveCategories(localeConfig), cached, genreMap, localeConfig),
                activeSourceLabel = "Cache local hors-ligne (FR)",
                loadedFromCacheFallback = true
            )
        }

        // Preserve already-resolved direct video URLs & trailer keys in Room
        val existingById = videoMetadataDao.observeAllVideos().first().associateBy { it.id }
        val mergedForCache = combinedList.map { incoming ->
            val prev = existingById[incoming.id]
            if (prev != null && (prev.videoUrl.isNotBlank() || !prev.trailerYoutubeKey.isNullOrBlank())) {
                incoming.copy(
                    videoUrl = prev.videoUrl.ifBlank { incoming.videoUrl },
                    trailerUrl = prev.trailerUrl.ifBlank { incoming.trailerUrl },
                    trailerYoutubeKey = prev.trailerYoutubeKey ?: incoming.trailerYoutubeKey,
                    durationMinutes = if (prev.durationMinutes > 0) prev.durationMinutes else incoming.durationMinutes,
                    director = if (prev.director.isNotBlank() && prev.director != "Voir fiche technique") prev.director else incoming.director,
                    cast = prev.castCsv.split("|").filter { it.isNotBlank() }.ifEmpty { incoming.cast },
                    isDownloadable = prev.isDownloadable && prev.videoUrl.isNotBlank(),
                    qualities = prev.qualitiesCsv.split("|").filter { it.isNotBlank() }.ifEmpty { incoming.qualities }
                )
            } else {
                incoming
            }
        }
        videoMetadataDao.upsertAll(mergedForCache.map { it.toVideoMetadataEntity() })

        val featured = mergedForCache.filter { it.isFeatured }.ifEmpty { mergedForCache.take(5) }
        val trending = mergedForCache.filter { it.isTrending }.ifEmpty { mergedForCache.take(10) }

        val sections = buildHomeSections(
            localeConfig = localeConfig,
            films = (tmdbNowPlaying + tmdbTrendingMovies).distinctBy { it.id },
            series = allSeries,
            cartoons = allCartoons,
            music = allMusic,
            theater = allTheater,
            localizedArchive = localizedArchive,
            docs = docsArchive
        )

        val categories = enrichCategoriesWithBanners(
            baseCategories = baseArchiveCategories(localeConfig),
            movies = mergedForCache,
            genreMap = genreMap,
            localeConfig = localeConfig
        )

        val sourceLabel = if (tmdbEnabled && (tmdbTrendingMovies.isNotEmpty() || tmdbSeries.isNotEmpty())) {
            "Français uniquement (${localeConfig.tmdbLanguage}) • TMDB + Archive"
        } else {
            "Français uniquement • Internet Archive (${localeConfig.displayLabel})"
        }

        HomeCatalogResult(
            featuredMovies = featured,
            trendingMovies = trending,
            allLoadedMovies = mergedForCache,
            sections = sections,
            categories = categories,
            activeSourceLabel = sourceLabel,
            loadedFromCacheFallback = false
        )
    }

    private fun buildHomeSections(
        localeConfig: RegionLocaleConfig,
        films: List<Movie>,
        series: List<Movie>,
        cartoons: List<Movie>,
        music: List<Movie>,
        theater: List<Movie>,
        localizedArchive: List<Movie>,
        docs: List<Movie>
    ): List<HomeSection> {
        val list = mutableListOf<HomeSection>()
        if (films.isNotEmpty()) {
            list.add(HomeSection("films_fr", "Films en Français (${localeConfig.displayLabel})", films))
        }
        if (series.isNotEmpty()) {
            list.add(HomeSection("series_fr", "Séries TV & Feuilletons (FR)", series))
        }
        if (cartoons.isNotEmpty()) {
            list.add(HomeSection("cartoons_fr", "Dessins animés & Animation", cartoons))
        }
        if (music.isNotEmpty()) {
            list.add(HomeSection("music_fr", "Musique, Concerts & Clips", music))
        }
        if (theater.isNotEmpty()) {
            list.add(HomeSection("theater_fr", "Théâtre & Arts de la scène", theater))
        }
        if (localizedArchive.isNotEmpty()) {
            list.add(HomeSection("localized_archive", "Films complets en Français (Archive.org)", localizedArchive))
        }
        if (docs.isNotEmpty()) {
            list.add(HomeSection("docs_fr", "Documentaires & Sciences", docs))
        }
        return list
    }

    private fun enrichCategoriesWithBanners(
        baseCategories: List<CategoryItem>,
        movies: List<Movie>,
        genreMap: Map<Int, String>,
        localeConfig: RegionLocaleConfig
    ): List<CategoryItem> {
        val tmdbGenreCategories = genreMap.entries.take(8).map { (genreId, genreName) ->
            val sampleBanner = movies.firstOrNull { m ->
                m.genres.any { it.equals(genreName, ignoreCase = true) } && m.backdropUrl.isNotBlank()
            }?.backdropUrl
                ?: movies.firstOrNull { it.backdropUrl.isNotBlank() }?.backdropUrl.orEmpty()
            CategoryItem(
                id = "tmdb_genre_$genreId",
                name = genreName,
                description = "Titres populaires dans le genre $genreName (${localeConfig.tmdbLanguage})",
                bannerUrl = sampleBanner,
                tmdbGenreId = genreId
            )
        }

        val enrichedBase = baseCategories.map { cat ->
            val banner = when (cat.id) {
                "all" -> movies.firstOrNull { it.backdropUrl.isNotBlank() }?.backdropUrl.orEmpty()
                MediaContentType.FILM,
                MediaContentType.SERIES,
                MediaContentType.MUSIC,
                MediaContentType.CARTOON,
                MediaContentType.THEATER -> movies.firstOrNull {
                    it.mediaType == cat.id && it.backdropUrl.isNotBlank()
                }?.backdropUrl ?: movies.firstOrNull { it.backdropUrl.isNotBlank() }?.backdropUrl.orEmpty()
                "Français" -> movies.firstOrNull {
                    it.language.equals("Français", ignoreCase = true) && it.backdropUrl.isNotBlank()
                }?.backdropUrl ?: movies.firstOrNull { it.backdropUrl.isNotBlank() }?.backdropUrl.orEmpty()
                else -> movies.firstOrNull {
                    it.category.equals(cat.id, ignoreCase = true) && it.backdropUrl.isNotBlank()
                }?.backdropUrl ?: movies.firstOrNull { it.backdropUrl.isNotBlank() }?.backdropUrl.orEmpty()
            }
            cat.copy(bannerUrl = banner)
        }

        return (enrichedBase + tmdbGenreCategories).distinctBy { it.name.lowercase(Locale.ROOT) }
    }

    suspend fun fetchCategoryMovies(
        category: CategoryItem,
        localeConfig: RegionLocaleConfig
    ): List<Movie> = coroutineScope {
        val tmdbEnabled = NetworkModule.hasValidTmdbKey()
        val gMap = cachedGenreMap
        val tvMap = cachedTvGenreMap.ifEmpty { cachedGenreMap }

        val tmdbDeferred = async {
            if (!tmdbEnabled) return@async emptyList()
            runCatching {
                when {
                    category.tmdbGenreId != null -> tmdbApi.discoverMovies(
                        language = localeConfig.tmdbLanguage,
                        region = localeConfig.regionCode,
                        genreIds = category.tmdbGenreId.toString()
                    ).results.map { it.toMovie(gMap, localeConfig).copy(category = category.name) }

                    category.id == MediaContentType.SERIES -> tmdbApi.discoverTv(
                        language = localeConfig.tmdbLanguage
                    ).results.map { it.toMovie(tvMap, localeConfig, overrideMediaType = MediaContentType.SERIES) }

                    category.id == MediaContentType.CARTOON -> {
                        val m = tmdbApi.discoverMovies(
                            language = localeConfig.tmdbLanguage,
                            region = localeConfig.regionCode,
                            genreIds = "16"
                        ).results.map { it.toMovie(gMap, localeConfig, overrideMediaType = MediaContentType.CARTOON) }
                        val t = tmdbApi.discoverTv(
                            language = localeConfig.tmdbLanguage,
                            genreIds = "16"
                        ).results.map { it.toMovie(tvMap, localeConfig, overrideMediaType = MediaContentType.CARTOON) }
                        (m + t).distinctBy { it.id }
                    }

                    category.id == MediaContentType.MUSIC -> tmdbApi.discoverMovies(
                        language = localeConfig.tmdbLanguage,
                        region = localeConfig.regionCode,
                        genreIds = "10402"
                    ).results.map { it.toMovie(gMap, localeConfig, overrideMediaType = MediaContentType.MUSIC) }

                    category.id == MediaContentType.THEATER -> tmdbApi.discoverMovies(
                        language = localeConfig.tmdbLanguage,
                        region = localeConfig.regionCode,
                        genreIds = "18",
                        keywords = "10181|4344"
                    ).results.ifEmpty {
                        tmdbApi.discoverMovies(
                            language = localeConfig.tmdbLanguage,
                            region = localeConfig.regionCode,
                            genreIds = "18",
                            originalLanguage = "fr"
                        ).results
                    }.map { it.toMovie(gMap, localeConfig, overrideMediaType = MediaContentType.THEATER) }

                    category.id == MediaContentType.FILM || category.id == "Français" -> tmdbApi.discoverMovies(
                        language = localeConfig.tmdbLanguage,
                        region = localeConfig.regionCode,
                        originalLanguage = if (category.id == "Français") "fr" else null
                    ).results.map { it.toMovie(gMap, localeConfig, overrideMediaType = MediaContentType.FILM) }

                    else -> emptyList()
                }
            }.getOrDefault(emptyList())
        }

        val archiveDeferred = async {
            if (category.archiveQuery.isBlank()) return@async emptyList()
            val overrideType = when (category.id) {
                MediaContentType.FILM,
                MediaContentType.SERIES,
                MediaContentType.MUSIC,
                MediaContentType.CARTOON,
                MediaContentType.THEATER -> category.id
                else -> null
            }
            searchArchiveFrenchPrioritized(
                baseQuery = category.archiveQuery.substringBefore(" AND (language:"),
                rows = 24,
                localeConfig = localeConfig,
                overrideMediaType = overrideType
            )
        }

        val merged = (tmdbDeferred.await() + archiveDeferred.await()).distinctBy { it.id }
        if (merged.isNotEmpty()) {
            videoMetadataDao.upsertAll(merged.map { it.toVideoMetadataEntity() })
            merged
        } else {
            val cached = videoMetadataDao.observeAllVideos().first().map { it.toMovie() }
            if (category.id == "all") cached
            else cached.filter {
                it.mediaType == category.id ||
                    it.category.equals(category.id, ignoreCase = true) ||
                    it.category.equals(category.name, ignoreCase = true) ||
                    it.genres.any { g -> g.equals(category.name, ignoreCase = true) }
            }
        }
    }

    suspend fun searchMoviesOnline(
        query: String,
        categoryFilter: String,
        yearFilter: Int?,
        languageFilter: String,
        localeConfig: RegionLocaleConfig
    ): List<Movie> = coroutineScope {
        val trimmed = query.trim()
        val hasQuery = trimmed.isNotEmpty()
        val tmdbEnabled = NetworkModule.hasValidTmdbKey()
        val isMediaTypeFilter = categoryFilter in MediaContentType.allTypes

        if (!hasQuery && categoryFilter == "all" && yearFilter == null && languageFilter == "Toutes") {
            return@coroutineScope videoMetadataDao.observeAllVideos().first().map { it.toMovie() }
        }

        val safeTerm = trimmed.replace("\"", "").replace("(", "").replace(")", "")
        val archiveQueryParts = mutableListOf("mediatype:movies")
        if (hasQuery) {
            archiveQueryParts.add("(title:($safeTerm) OR creator:($safeTerm) OR subject:($safeTerm) OR description:($safeTerm))")
        } else {
            archiveQueryParts.add("(collection:feature_films OR collection:animationandcartoons OR collection:opensource_movies OR collection:SciFi_Horror)")
        }
        when (categoryFilter) {
            MediaContentType.CARTOON, "Animation" -> archiveQueryParts.add("collection:animationandcartoons")
            MediaContentType.SERIES -> archiveQueryParts.add("(subject:television OR subject:series OR collection:classic_tv)")
            MediaContentType.MUSIC -> archiveQueryParts.add("(subject:music OR subject:concert OR subject:chanson)")
            MediaContentType.THEATER -> archiveQueryParts.add("(subject:theater OR subject:theatre OR subject:moliere)")
            "Classiques" -> archiveQueryParts.add("collection:feature_films")
            "Science-Fiction" -> archiveQueryParts.add("(collection:SciFi_Horror OR subject:\"science fiction\")")
            "Documentaire" -> archiveQueryParts.add("(collection:nasa OR subject:documentary)")
        }
        if (yearFilter != null) {
            archiveQueryParts.add("year:$yearFilter")
        }
        when {
            languageFilter == "Français" || localeConfig.frenchOnly ->
                archiveQueryParts.add("(language:fre OR language:French OR language:fra)")
            languageFilter == "Anglais" -> archiveQueryParts.add("(language:eng OR language:English)")
            languageFilter == "Espagnol" -> archiveQueryParts.add("(language:spa OR language:Spanish)")
        }

        val archiveDeferred = async {
            val results = runCatching {
                archiveOrgApi.searchMovies(
                    query = archiveQueryParts.joinToString(" AND "),
                    rows = 24
                ).response?.docs.orEmpty().mapNotNull { doc ->
                    doc.toMovie(
                        localeConfig = localeConfig,
                        overrideMediaType = if (isMediaTypeFilter) categoryFilter else null
                    )
                }
            }.getOrDefault(emptyList())
            if (localeConfig.frenchOnly) results.map { it.copy(language = "Français") } else results
        }

        val tmdbMoviesDeferred = async {
            if (!tmdbEnabled || categoryFilter == MediaContentType.SERIES) emptyList()
            else runCatching {
                val results = if (hasQuery) {
                    tmdbApi.searchMovies(
                        query = trimmed,
                        language = localeConfig.tmdbLanguage,
                        region = localeConfig.regionCode,
                        year = yearFilter
                    ).results
                } else {
                    val genreId = when (categoryFilter) {
                        MediaContentType.CARTOON -> "16"
                        MediaContentType.MUSIC -> "10402"
                        MediaContentType.THEATER -> "18"
                        else -> cachedGenreMap.entries.firstOrNull {
                            it.value.equals(categoryFilter, ignoreCase = true)
                        }?.key?.toString()
                    }
                    val origLang = when (languageFilter) {
                        "Français" -> "fr"
                        "Anglais" -> "en"
                        "Espagnol" -> "es"
                        else -> null
                    }
                    tmdbApi.discoverMovies(
                        language = localeConfig.tmdbLanguage,
                        region = localeConfig.regionCode,
                        genreIds = genreId,
                        keywords = if (categoryFilter == MediaContentType.THEATER) "10181|4344" else null,
                        year = yearFilter,
                        originalLanguage = origLang
                    ).results
                }
                results.map {
                    it.toMovie(
                        genreMap = cachedGenreMap,
                        localeConfig = localeConfig,
                        overrideMediaType = if (isMediaTypeFilter) categoryFilter else null
                    )
                }
            }.getOrDefault(emptyList())
        }

        val tmdbTvDeferred = async {
            if (!tmdbEnabled || (isMediaTypeFilter && categoryFilter != MediaContentType.SERIES && categoryFilter != MediaContentType.CARTOON)) {
                emptyList()
            } else runCatching {
                val tvResults = if (hasQuery) {
                    tmdbApi.searchTv(
                        query = trimmed,
                        language = localeConfig.tmdbLanguage,
                        year = yearFilter
                    ).results
                } else {
                    tmdbApi.discoverTv(
                        language = localeConfig.tmdbLanguage,
                        genreIds = if (categoryFilter == MediaContentType.CARTOON) "16" else null,
                        year = yearFilter
                    ).results
                }
                tvResults.map {
                    it.toMovie(
                        genreMap = cachedTvGenreMap.ifEmpty { cachedGenreMap },
                        localeConfig = localeConfig,
                        overrideMediaType = if (categoryFilter == MediaContentType.CARTOON) {
                            MediaContentType.CARTOON
                        } else {
                            MediaContentType.SERIES
                        }
                    )
                }
            }.getOrDefault(emptyList())
        }

        val networkResults = (tmdbMoviesDeferred.await() + tmdbTvDeferred.await() + archiveDeferred.await())
            .distinctBy { it.id }

        if (networkResults.isNotEmpty()) {
            videoMetadataDao.upsertAll(networkResults.map { it.toVideoMetadataEntity() })
            return@coroutineScope networkResults.filter { movie ->
                val matchesYear = yearFilter == null || movie.releaseYear == yearFilter
                val matchesLanguage = languageFilter == "Toutes" || movie.language.contains(languageFilter, ignoreCase = true)
                val matchesCat = categoryFilter == "all" ||
                    movie.mediaType == categoryFilter ||
                    movie.category.equals(categoryFilter, ignoreCase = true) ||
                    movie.genres.any { it.equals(categoryFilter, ignoreCase = true) }
                matchesYear && matchesLanguage && matchesCat
            }
        }

        // Local Room fallback
        val cached = videoMetadataDao.searchVideos(
            query = trimmed,
            category = if (isMediaTypeFilter) "all" else categoryFilter,
            year = yearFilter,
            language = languageFilter
        ).first().map { it.toMovie() }
        if (isMediaTypeFilter) cached.filter { it.mediaType == categoryFilter } else cached
    }

    suspend fun fetchMovieDetails(
        movie: Movie,
        localeConfig: RegionLocaleConfig
    ): EnrichedMovieResult {
        val isTmdbTv = movie.id.startsWith("tmdb_tv_") ||
            (movie.sourceType == MovieSourceType.TMDB && movie.mediaType == MediaContentType.SERIES)

        if (isTmdbTv && movie.tmdbId != null && NetworkModule.hasValidTmdbKey()) {
            val details = runCatching {
                tmdbApi.getTvDetails(
                    tvId = movie.tmdbId,
                    language = localeConfig.tmdbLanguage,
                    includeVideoLanguage = localeConfig.includeVideoLanguage,
                    includeImageLanguage = localeConfig.includeImageLanguage
                )
            }.getOrNull()

            if (details != null) {
                val enFallback = if (!localeConfig.frenchOnly &&
                    (details.overview.isNullOrBlank() || details.videos?.results.isNullOrEmpty()) &&
                    localeConfig.languageCode != "en"
                ) {
                    runCatching {
                        tmdbApi.getTvDetails(
                            tvId = movie.tmdbId,
                            language = "en-US",
                            includeVideoLanguage = "en,null",
                            includeImageLanguage = "en,null"
                        )
                    }.getOrNull()
                } else if (localeConfig.frenchOnly && details.videos?.results.isNullOrEmpty()) {
                    // Only grab official trailer video key if French trailer is absent, never replace French synopsis
                    runCatching {
                        tmdbApi.getTvDetails(
                            tvId = movie.tmdbId,
                            language = localeConfig.tmdbLanguage,
                            includeVideoLanguage = "fr,en,null",
                            includeImageLanguage = localeConfig.includeImageLanguage
                        )
                    }.getOrNull()?.let { vidDto -> vidDto.copy(overview = details.overview) }
                } else {
                    null
                }
                val enriched = details.enrichMovie(movie, enFallback)
                videoMetadataDao.upsertVideo(enriched.toVideoMetadataEntity())
                return EnrichedMovieResult(movie = enriched)
            }
        }

        if (movie.sourceType == MovieSourceType.TMDB && movie.tmdbId != null && NetworkModule.hasValidTmdbKey()) {
            val details = runCatching {
                tmdbApi.getMovieDetails(
                    movieId = movie.tmdbId,
                    language = localeConfig.tmdbLanguage,
                    includeVideoLanguage = localeConfig.includeVideoLanguage,
                    includeImageLanguage = localeConfig.includeImageLanguage
                )
            }.getOrNull()

            if (details != null) {
                val enFallback = if (!localeConfig.frenchOnly &&
                    (details.overview.isNullOrBlank() || details.videos?.results.isNullOrEmpty()) &&
                    localeConfig.languageCode != "en"
                ) {
                    runCatching {
                        tmdbApi.getMovieDetails(
                            movieId = movie.tmdbId,
                            language = "en-US",
                            includeVideoLanguage = "en,null",
                            includeImageLanguage = "en,null"
                        )
                    }.getOrNull()
                } else if (localeConfig.frenchOnly && details.videos?.results.isNullOrEmpty()) {
                    runCatching {
                        tmdbApi.getMovieDetails(
                            movieId = movie.tmdbId,
                            language = localeConfig.tmdbLanguage,
                            includeVideoLanguage = "fr,en,null",
                            includeImageLanguage = localeConfig.includeImageLanguage
                        )
                    }.getOrNull()?.let { vidDto -> vidDto.copy(overview = details.overview) }
                } else {
                    null
                }
                val enriched = details.enrichMovie(movie, enFallback)
                videoMetadataDao.upsertVideo(enriched.toVideoMetadataEntity())
                return EnrichedMovieResult(movie = enriched)
            }
        }

        val archiveId = movie.archiveIdentifier ?: movie.id.takeIf { it.startsWith("arch_") }?.removePrefix("arch_")
        if (!archiveId.isNullOrBlank()) {
            val metadataResp = runCatching { archiveOrgApi.getItemMetadata(archiveId) }.getOrNull()
            if (metadataResp != null) {
                val result = metadataResp.enrichMovie(movie)
                videoMetadataDao.upsertVideo(result.movie.toVideoMetadataEntity())
                return result
            }
        }

        return EnrichedMovieResult(movie = movie)
    }

    suspend fun fetchSeriesSeasonEpisodes(
        tvId: Int,
        seasonNumber: Int,
        localeConfig: RegionLocaleConfig
    ): List<SeriesEpisode> {
        if (!NetworkModule.hasValidTmdbKey()) return emptyList()
        return runCatching {
            tmdbApi.getTvSeasonDetails(
                tvId = tvId,
                seasonNumber = seasonNumber,
                language = localeConfig.tmdbLanguage
            ).toEpisodes()
        }.getOrDefault(emptyList())
    }

    suspend fun fetchUpcomingReleases(localeConfig: RegionLocaleConfig): List<UpcomingMovie> {
        if (NetworkModule.hasValidTmdbKey()) {
            val tmdbUpcoming = runCatching {
                if (cachedGenreMap.isEmpty()) {
                    cachedGenreMap = runCatching {
                        tmdbApi.getGenres(localeConfig.tmdbLanguage).genres.associate { it.id to it.name }
                    }.getOrDefault(emptyMap())
                }
                tmdbApi.getUpcomingMovies(
                    language = localeConfig.tmdbLanguage,
                    region = localeConfig.regionCode
                ).results
                    .let { list ->
                        if (localeConfig.frenchOnly) list.filter { !it.overview.isNullOrBlank() }.ifEmpty { list }
                        else list
                    }
                    .map { it.toUpcomingMovie(cachedGenreMap) }
            }.getOrDefault(emptyList())

            if (tmdbUpcoming.isNotEmpty()) return tmdbUpcoming
        }

        return runCatching {
            archiveOrgApi.searchMovies(
                query = "mediatype:movies AND (collection:feature_films OR collection:opensource_movies) AND ${localeConfig.archiveLanguageQuery}",
                sort = "addeddate desc",
                rows = 12
            ).response?.docs.orEmpty().mapNotNull { it.toUpcomingMovie(localeConfig) }
        }.getOrDefault(emptyList())
    }

    suspend fun verifyTmdbConnection(localeConfig: RegionLocaleConfig): Result<String> {
        if (!NetworkModule.hasValidTmdbKey()) {
            return Result.failure(IllegalStateException("Aucune clé API TMDB active détectée."))
        }
        return runCatching {
            val genres = tmdbApi.getGenres(localeConfig.tmdbLanguage).genres
            val trending = tmdbApi.getTrendingMovies(localeConfig.tmdbLanguage).results
            val mode = if (NetworkModule.isV4BearerToken()) "Jeton v4 Bearer" else "Clé v3"
            "Connexion TMDB réussie ($mode) • ${genres.size} genres et ${trending.size} tendances en français (${localeConfig.tmdbLanguage})"
        }
    }

    // Room Watch History delegates
    fun getAllHistory(): Flow<List<WatchHistoryEntity>> = watchHistoryDao.getAllHistory()
    fun getContinueWatching(): Flow<List<WatchHistoryEntity>> = watchHistoryDao.getContinueWatching()
    fun observeProgressForMovie(movieId: String): Flow<WatchHistoryEntity?> = watchHistoryDao.observeProgressForMovie(movieId)
    suspend fun getProgressForMovie(movieId: String): WatchHistoryEntity? = watchHistoryDao.getProgressForMovie(movieId)

    suspend fun saveProgress(movieId: String, progressMs: Long, durationMs: Long) {
        val isCompleted = durationMs > 0 && progressMs >= (durationMs * 0.95)
        watchHistoryDao.saveProgress(
            WatchHistoryEntity(
                movieId = movieId,
                progressMs = progressMs,
                durationMs = durationMs,
                lastWatchedTimestamp = System.currentTimeMillis(),
                isCompleted = isCompleted
            )
        )
    }

    suspend fun deleteHistory(movieId: String) = watchHistoryDao.deleteHistory(movieId)
    suspend fun clearAllHistory() = watchHistoryDao.clearAllHistory()

    // Room Favorites delegates
    fun getAllFavorites(): Flow<List<FavoriteEntity>> = favoriteDao.getAllFavorites()
    fun isFavorite(movieId: String): Flow<Boolean> = favoriteDao.isFavorite(movieId)

    suspend fun toggleFavorite(movieId: String, currentIsFav: Boolean) {
        if (currentIsFav) {
            favoriteDao.removeFavorite(movieId)
        } else {
            favoriteDao.addFavorite(FavoriteEntity(movieId = movieId))
        }
    }

    // Room Downloads & Offline Storage delegates
    fun getAllDownloads(): Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()
    fun observeDownloadsWithMetadata(): Flow<List<VideoDownloadWithMetadata>> = downloadDao.observeDownloadsWithMetadata()
    fun observeTotalDownloadedBytes(): Flow<Long> = downloadDao.observeTotalDownloadedBytes()
    fun observeTotalOriginalBytes(): Flow<Long> = downloadDao.observeTotalOriginalBytes()
    fun observeDownload(movieId: String): Flow<DownloadEntity?> = downloadDao.observeDownload(movieId)

    suspend fun startDownload(
        movie: Movie,
        quality: String = "Auto",
        compressionPreset: SpaceCompressionPreset = SpaceCompressionPreset.SERIES_SAVER,
        wifiOnly: Boolean = false
    ) {
        val enrichedResult = if (movie.sourceType == MovieSourceType.ARCHIVE_ORG) {
            fetchMovieDetails(movie, RegionLocaleConfig.fromPreference("Français uniquement"))
        } else {
            EnrichedMovieResult(movie = movie)
        }
        val resolvedMovie = enrichedResult.movie
        if (!resolvedMovie.isDownloadable || resolvedMovie.videoUrl.isBlank()) return

        val selectedVariant = VideoSpaceCompressor.selectVariant(
            fallbackUrl = resolvedMovie.videoUrl,
            qualityUrls = enrichedResult.qualityUrls,
            qualitySizesBytes = enrichedResult.qualitySizesBytes,
            preset = compressionPreset
        )

        videoMetadataDao.upsertVideo(resolvedMovie.toVideoMetadataEntity())
        downloadManager.startOrResumeDownload(
            movie = resolvedMovie,
            quality = if (selectedVariant.qualityLabel.isNotBlank()) selectedVariant.qualityLabel else quality,
            wifiOnly = wifiOnly,
            overrideStreamUrl = selectedVariant.streamUrl,
            originalHdBytes = selectedVariant.originalHdBytes,
            compressionMode = compressionPreset.id
        )
    }

    suspend fun pauseDownload(movieId: String) {
        downloadManager.pauseDownload(movieId)
    }

    suspend fun resumeDownload(movieId: String, wifiOnly: Boolean = false) {
        val movie = videoMetadataDao.getVideoById(movieId)?.toMovie() ?: return
        val existing = downloadDao.getDownload(movieId)
        downloadManager.startOrResumeDownload(
            movie = movie,
            quality = existing?.quality ?: "Auto",
            wifiOnly = wifiOnly,
            overrideStreamUrl = existing?.videoUrl?.takeIf { it.isNotBlank() },
            originalHdBytes = existing?.originalSizeBytes ?: 0L,
            compressionMode = existing?.compressionMode ?: SpaceCompressionPreset.SERIES_SAVER.id
        )
    }

    suspend fun deleteDownload(movieId: String) {
        downloadManager.deleteDownload(movieId)
    }

    suspend fun clearAllDownloads() {
        downloadManager.clearAllDownloads()
    }

    suspend fun resolveOfflineUri(movieId: String): Uri? {
        val entity = downloadDao.getDownload(movieId) ?: return null
        if (entity.status != DownloadStatus.COMPLETED) return null
        val file = File(entity.localFilePath)
        return if (file.exists() && file.length() > 0L) Uri.fromFile(file) else null
    }

    companion object {
        @Volatile
        private var INSTANCE: MovieRepository? = null

        fun getInstance(context: Context): MovieRepository {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val db = GpnFlixDatabase.getDatabase(appContext)
                val downloadManager = OfflineDownloadManager(
                    context = appContext,
                    downloadDao = db.downloadDao()
                )
                val instance = MovieRepository(
                    videoMetadataDao = db.videoMetadataDao(),
                    watchHistoryDao = db.watchHistoryDao(),
                    favoriteDao = db.favoriteDao(),
                    downloadDao = db.downloadDao(),
                    downloadManager = downloadManager
                )
                INSTANCE = instance
                instance
            }
        }
    }
}
