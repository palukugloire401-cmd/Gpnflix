package com.example.gpnflix.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gpnflix.data.local.DownloadEntity
import com.example.gpnflix.data.local.VideoDownloadWithMetadata
import com.example.gpnflix.data.local.WatchHistoryEntity
import com.example.gpnflix.data.model.CategoryItem
import com.example.gpnflix.data.model.ImageQualityMode
import com.example.gpnflix.data.model.Movie
import com.example.gpnflix.data.model.RegionLocaleConfig
import com.example.gpnflix.data.model.SeriesEpisode
import com.example.gpnflix.data.model.SpaceCompressionPreset
import com.example.gpnflix.data.model.UpcomingMovie
import com.example.gpnflix.data.remote.NetworkModule
import com.example.gpnflix.data.repository.HomeSection
import com.example.gpnflix.data.repository.MovieRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContinueWatchingItem(
    val movie: Movie,
    val history: WatchHistoryEntity
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val featuredMovies: List<Movie> = emptyList(),
    val trendingMovies: List<Movie> = emptyList(),
    val sections: List<HomeSection> = emptyList(),
    val activeSourceLabel: String = "",
    val loadedFromCacheFallback: Boolean = false
)

data class SearchFilterState(
    val query: String = "",
    val selectedCategory: String = "all",
    val selectedYear: Int? = null,
    val selectedLanguage: String = "Français"
)

data class UserSettingsState(
    val themeMode: String = "DARK", // "DARK", "LIGHT", "SYSTEM"
    val defaultQuality: String = "Auto (1080p HD)",
    val language: String = "Français uniquement",
    val compressionPreset: SpaceCompressionPreset = SpaceCompressionPreset.SERIES_SAVER,
    val imageQualityMode: ImageQualityMode = ImageQualityMode.HD,
    val familyKidsMode: Boolean = false,
    val autoPlayNext: Boolean = true,
    val downloadWifiOnly: Boolean = false,
    val isTmdbActive: Boolean = false,
    val isTmdbV4Token: Boolean = false,
    val hasCustomTmdbKey: Boolean = false,
    val maskedTmdbKey: String = ""
)

data class TmdbConnectionTestState(
    val isTesting: Boolean = false,
    val isSuccess: Boolean? = null,
    val message: String = ""
)

data class PersonalReview(
    val ratingStars: Int = 0,
    val note: String = ""
)

data class PipPlaybackState(
    val isPlayerActive: Boolean = false,
    val isPlaying: Boolean = false,
    val isInPipMode: Boolean = false,
    val title: String = ""
)

class GpnFlixViewModel(
    application: Application,
    private val repository: MovieRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // User settings
    private val _userSettings = MutableStateFlow(UserSettingsState())
    val userSettings: StateFlow<UserSettingsState> = _userSettings.asStateFlow()

    private val _tmdbTestState = MutableStateFlow(TmdbConnectionTestState())
    val tmdbTestState: StateFlow<TmdbConnectionTestState> = _tmdbTestState.asStateFlow()

    val regionLocale: StateFlow<RegionLocaleConfig> = _userSettings
        .map { RegionLocaleConfig.fromPreference(it.language) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = RegionLocaleConfig.fromPreference("Français uniquement")
        )

    // Picture-in-Picture live playback state for MainActivity coordination
    private val _pipPlaybackState = MutableStateFlow(PipPlaybackState())
    val pipPlaybackState: StateFlow<PipPlaybackState> = _pipPlaybackState.asStateFlow()

    // Reactive Room video metadata stream
    val moviesState: StateFlow<List<Movie>> = repository.observeAllVideos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Home UI State
    private val _homeUiState = MutableStateFlow(HomeUiState(isLoading = true))
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()

    // Categories state
    private val _categoriesState = MutableStateFlow<List<CategoryItem>>(emptyList())
    val categoriesState: StateFlow<List<CategoryItem>> = _categoriesState.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow("all")
    val selectedCategoryId: StateFlow<String> = _selectedCategoryId.asStateFlow()

    private val _categoryMoviesState = MutableStateFlow<List<Movie>>(emptyList())
    val categoryMoviesState: StateFlow<List<Movie>> = _categoryMoviesState.asStateFlow()

    private val _isLoadingCategory = MutableStateFlow(false)
    val isLoadingCategory: StateFlow<Boolean> = _isLoadingCategory.asStateFlow()

    // Upcoming releases state
    private val _upcomingMoviesState = MutableStateFlow<List<UpcomingMovie>>(emptyList())
    val upcomingMoviesState: StateFlow<List<UpcomingMovie>> = _upcomingMoviesState.asStateFlow()

    private val _isLoadingUpcoming = MutableStateFlow(false)
    val isLoadingUpcoming: StateFlow<Boolean> = _isLoadingUpcoming.asStateFlow()

    // Search state
    private val _searchFilter = MutableStateFlow(SearchFilterState())
    val searchFilter: StateFlow<SearchFilterState> = _searchFilter.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Movie>>(emptyList())
    val searchResults: StateFlow<List<Movie>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Selected movie, details & series season episodes
    private val _selectedMovie = MutableStateFlow<Movie?>(null)
    val selectedMovie: StateFlow<Movie?> = _selectedMovie.asStateFlow()

    private val _isLoadingDetails = MutableStateFlow(false)
    val isLoadingDetails: StateFlow<Boolean> = _isLoadingDetails.asStateFlow()

    private val _movieQualityUrls = MutableStateFlow<Map<String, String>>(emptyMap())
    val movieQualityUrls: StateFlow<Map<String, String>> = _movieQualityUrls.asStateFlow()

    private val _selectedSeasonNumber = MutableStateFlow(1)
    val selectedSeasonNumber: StateFlow<Int> = _selectedSeasonNumber.asStateFlow()

    private val _seriesEpisodes = MutableStateFlow<List<SeriesEpisode>>(emptyList())
    val seriesEpisodes: StateFlow<List<SeriesEpisode>> = _seriesEpisodes.asStateFlow()

    private val _isLoadingEpisodes = MutableStateFlow(false)
    val isLoadingEpisodes: StateFlow<Boolean> = _isLoadingEpisodes.asStateFlow()

    // Personal reviews / user ratings persisted locally
    private val _personalReviews = MutableStateFlow<Map<String, PersonalReview>>(emptyMap())
    val personalReviews: StateFlow<Map<String, PersonalReview>> = _personalReviews.asStateFlow()

    // Continue watching flow (Room WatchHistory joined with Room VideoMetadata)
    val continueWatching: StateFlow<List<ContinueWatchingItem>> = repository.getContinueWatching()
        .combine(moviesState) { historyList, movies ->
            historyList.mapNotNull { hist ->
                val movie = movies.find { it.id == hist.movieId }
                movie?.let { ContinueWatchingItem(it, hist) }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Full Watch History flow
    val watchHistory: StateFlow<List<ContinueWatchingItem>> = repository.getAllHistory()
        .combine(moviesState) { historyList, movies ->
            historyList.mapNotNull { hist ->
                val movie = movies.find { it.id == hist.movieId }
                movie?.let { ContinueWatchingItem(it, hist) }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Favorites flow
    val favorites: StateFlow<List<Movie>> = repository.getAllFavorites()
        .combine(moviesState) { favList, movies ->
            favList.mapNotNull { fav ->
                movies.find { it.id == fav.movieId }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Downloads flows backed by Room
    val downloads: StateFlow<List<DownloadEntity>> = repository.getAllDownloads()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val downloadsWithMetadata: StateFlow<List<VideoDownloadWithMetadata>> =
        repository.observeDownloadsWithMetadata()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val totalDownloadedBytes: StateFlow<Long> = repository.observeTotalDownloadedBytes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    val totalOriginalBytes: StateFlow<Long> = repository.observeTotalOriginalBytes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    private var searchJob: Job? = null
    private var categoryJob: Job? = null
    private var detailsJob: Job? = null
    private var seasonJob: Job? = null

    init {
        val savedCustomKey = prefs.getString(KEY_CUSTOM_TMDB_TOKEN, null)?.trim()
        if (!savedCustomKey.isNullOrEmpty()) {
            NetworkModule.customTmdbApiKey = savedCustomKey
        }
        val savedCompression = SpaceCompressionPreset.fromId(prefs.getString(KEY_COMPRESSION_PRESET, null))
        val savedImageQuality = ImageQualityMode.fromId(prefs.getString(KEY_IMAGE_QUALITY, null))
        val savedLang = prefs.getString(KEY_CATALOG_LANGUAGE, "Français uniquement") ?: "Français uniquement"
        val savedFamily = prefs.getBoolean(KEY_FAMILY_MODE, false)
        val savedQuality = prefs.getString(KEY_DEFAULT_QUALITY, "Auto (1080p HD)") ?: "Auto (1080p HD)"

        NetworkModule.currentImageQuality = savedImageQuality
        _userSettings.update {
            it.copy(
                language = savedLang,
                compressionPreset = savedCompression,
                imageQualityMode = savedImageQuality,
                familyKidsMode = savedFamily,
                defaultQuality = savedQuality
            )
        }
        if (savedLang == "Français uniquement") {
            _searchFilter.update { it.copy(selectedLanguage = "Français") }
        }

        syncTmdbKeyState()

        viewModelScope.launch {
            repository.reconcileDownloadsOnStartup()
        }
        refreshHomeCatalog()
        loadUpcomingMovies()
    }

    private fun syncTmdbKeyState() {
        val custom = NetworkModule.normalizeTmdbKey(NetworkModule.customTmdbApiKey)
        val active = NetworkModule.activeTmdbKey()
        val valid = NetworkModule.hasValidTmdbKey()
        val masked = if (valid && active.length >= 8) {
            "${active.take(4)}••••••${active.takeLast(4)}"
        } else {
            ""
        }
        _userSettings.update {
            it.copy(
                isTmdbActive = valid,
                isTmdbV4Token = valid && NetworkModule.isV4BearerToken(active),
                hasCustomTmdbKey = custom.isNotEmpty(),
                maskedTmdbKey = masked
            )
        }
    }

    fun saveCustomTmdbKey(rawKey: String) {
        val cleaned = NetworkModule.normalizeTmdbKey(rawKey)
        if (cleaned.isEmpty()) return
        prefs.edit().putString(KEY_CUSTOM_TMDB_TOKEN, cleaned).apply()
        NetworkModule.customTmdbApiKey = cleaned
        _tmdbTestState.value = TmdbConnectionTestState()
        syncTmdbKeyState()
        refreshHomeCatalog()
        loadUpcomingMovies()
        performSearch(debounceMs = 0L)
    }

    fun clearCustomTmdbKey() {
        prefs.edit().remove(KEY_CUSTOM_TMDB_TOKEN).apply()
        NetworkModule.customTmdbApiKey = null
        _tmdbTestState.value = TmdbConnectionTestState()
        syncTmdbKeyState()
        refreshHomeCatalog()
        loadUpcomingMovies()
        performSearch(debounceMs = 0L)
    }

    fun testTmdbConnection() {
        val locale = regionLocale.value
        viewModelScope.launch {
            _tmdbTestState.value = TmdbConnectionTestState(
                isTesting = true,
                isSuccess = null,
                message = "Test de connexion TMDB en cours..."
            )
            repository.verifyTmdbConnection(locale)
                .onSuccess { msg ->
                    _tmdbTestState.value = TmdbConnectionTestState(
                        isTesting = false,
                        isSuccess = true,
                        message = msg
                    )
                }
                .onFailure { err ->
                    _tmdbTestState.value = TmdbConnectionTestState(
                        isTesting = false,
                        isSuccess = false,
                        message = err.localizedMessage ?: "Échec de connexion à TMDB."
                    )
                }
        }
    }

    fun refreshHomeCatalog() {
        val locale = regionLocale.value
        viewModelScope.launch {
            _homeUiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                repository.fetchHomeCatalog(locale)
            }.onSuccess { result ->
                _homeUiState.value = HomeUiState(
                    isLoading = false,
                    errorMessage = if (result.allLoadedMovies.isEmpty()) {
                        "Impossible de charger le catalogue. Vérifiez votre connexion Internet."
                    } else null,
                    featuredMovies = result.featuredMovies,
                    trendingMovies = result.trendingMovies,
                    sections = result.sections,
                    activeSourceLabel = result.activeSourceLabel,
                    loadedFromCacheFallback = result.loadedFromCacheFallback
                )
                _categoriesState.value = result.categories
                if (_categoryMoviesState.value.isEmpty() || _selectedCategoryId.value == "all") {
                    _categoryMoviesState.value = result.allLoadedMovies
                }
                if (_searchResults.value.isEmpty() || _searchFilter.value.query.isBlank()) {
                    _searchResults.value = result.allLoadedMovies
                }
            }.onFailure { err ->
                _homeUiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.localizedMessage ?: "Erreur réseau lors du chargement du catalogue."
                    )
                }
            }
        }
    }

    fun selectCategory(category: CategoryItem) {
        _selectedCategoryId.value = category.id
        categoryJob?.cancel()
        categoryJob = viewModelScope.launch {
            _isLoadingCategory.value = true
            val results = repository.fetchCategoryMovies(category, regionLocale.value)
            _categoryMoviesState.value = results
            _isLoadingCategory.value = false
        }
    }

    fun loadUpcomingMovies() {
        viewModelScope.launch {
            _isLoadingUpcoming.value = true
            val list = repository.fetchUpcomingReleases(regionLocale.value)
            _upcomingMoviesState.value = list
            _isLoadingUpcoming.value = false
        }
    }

    fun selectMovie(movie: Movie) {
        _selectedMovie.value = movie
        _movieQualityUrls.value = emptyMap()
        _seriesEpisodes.value = emptyList()
        _selectedSeasonNumber.value = 1
        loadPersonalReviewForMovie(movie.id)
    }

    fun resolveMovieById(id: String): Movie? {
        return _selectedMovie.value?.takeIf { it.id == id }
            ?: _homeUiState.value.featuredMovies.find { it.id == id }
            ?: _homeUiState.value.trendingMovies.find { it.id == id }
            ?: _homeUiState.value.sections.asSequence().flatMap { it.movies.asSequence() }.find { it.id == id }
            ?: _searchResults.value.find { it.id == id }
            ?: _categoryMoviesState.value.find { it.id == id }
            ?: moviesState.value.find { it.id == id }
            ?: favorites.value.find { it.id == id }
    }

    fun loadMovieDetails(movie: Movie) {
        detailsJob?.cancel()
        detailsJob = viewModelScope.launch {
            _isLoadingDetails.value = true
            loadPersonalReviewForMovie(movie.id)
            val result = repository.fetchMovieDetails(movie, regionLocale.value)
            _selectedMovie.value = result.movie
            _movieQualityUrls.value = result.qualityUrls
            _isLoadingDetails.value = false

            if (result.movie.tmdbId != null && result.movie.seasons.isNotEmpty()) {
                val defaultSeason = result.movie.seasons.first().seasonNumber
                loadSeriesSeason(result.movie.tmdbId, defaultSeason)
            }
        }
    }

    fun loadSeriesSeason(tvId: Int, seasonNumber: Int) {
        _selectedSeasonNumber.value = seasonNumber
        seasonJob?.cancel()
        seasonJob = viewModelScope.launch {
            _isLoadingEpisodes.value = true
            val episodes = repository.fetchSeriesSeasonEpisodes(tvId, seasonNumber, regionLocale.value)
            _seriesEpisodes.value = episodes
            _isLoadingEpisodes.value = false
        }
    }

    fun ensureMovieLoaded(movieId: String) {
        val current = resolveMovieById(movieId)
        if (current != null) {
            if (_selectedMovie.value?.id != current.id) {
                _selectedMovie.value = current
            }
            loadMovieDetails(current)
            return
        }
        viewModelScope.launch {
            val cached = repository.getCachedMovieById(movieId)
            if (cached != null) {
                _selectedMovie.value = cached
                loadMovieDetails(cached)
            }
        }
    }

    fun observeDownload(movieId: String): Flow<DownloadEntity?> =
        repository.observeDownload(movieId)

    fun isFavorite(movieId: String): StateFlow<Boolean> {
        return repository.isFavorite(movieId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    }

    fun toggleFavorite(movie: Movie, currentIsFav: Boolean) {
        viewModelScope.launch {
            if (!currentIsFav) {
                repository.fetchMovieDetails(movie, regionLocale.value)
            }
            repository.toggleFavorite(movie.id, currentIsFav)
        }
    }

    fun toggleFavorite(movieId: String, currentIsFav: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(movieId, currentIsFav)
        }
    }

    fun saveWatchProgress(movieId: String, progressMs: Long, durationMs: Long) {
        viewModelScope.launch {
            repository.saveProgress(movieId, progressMs, durationMs)
        }
    }

    suspend fun getSavedProgress(movieId: String): Long {
        return repository.getProgressForMovie(movieId)?.progressMs ?: 0L
    }

    fun deleteHistoryItem(movieId: String) {
        viewModelScope.launch {
            repository.deleteHistory(movieId)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }

    fun downloadMovie(
        movie: Movie,
        overridePreset: SpaceCompressionPreset? = null
    ) {
        viewModelScope.launch {
            val settings = _userSettings.value
            val resolved = if (movie.videoUrl.isBlank()) {
                repository.fetchMovieDetails(movie, regionLocale.value).movie
            } else {
                movie
            }
            repository.startDownload(
                movie = resolved,
                quality = settings.defaultQuality,
                compressionPreset = overridePreset ?: settings.compressionPreset,
                wifiOnly = settings.downloadWifiOnly
            )
        }
    }

    fun pauseDownload(movieId: String) {
        viewModelScope.launch {
            repository.pauseDownload(movieId)
        }
    }

    fun resumeDownload(movieId: String) {
        viewModelScope.launch {
            repository.resumeDownload(
                movieId = movieId,
                wifiOnly = _userSettings.value.downloadWifiOnly
            )
        }
    }

    fun deleteDownload(movieId: String) {
        viewModelScope.launch {
            repository.deleteDownload(movieId)
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            repository.clearAllDownloads()
        }
    }

    // Search query & filters
    fun updateSearchQuery(query: String) {
        _searchFilter.update { it.copy(query = query) }
        performSearch(debounceMs = 350L)
    }

    fun updateSearchCategory(category: String) {
        _searchFilter.update { it.copy(selectedCategory = category) }
        performSearch(debounceMs = 0L)
    }

    fun updateSearchYear(year: Int?) {
        _searchFilter.update { it.copy(selectedYear = year) }
        performSearch(debounceMs = 0L)
    }

    fun updateSearchLanguage(language: String) {
        _searchFilter.update { it.copy(selectedLanguage = language) }
        performSearch(debounceMs = 0L)
    }

    fun resetFilters() {
        _searchFilter.value = SearchFilterState(
            selectedLanguage = if (regionLocale.value.frenchOnly) "Français" else "Toutes"
        )
        performSearch(debounceMs = 0L)
    }

    private fun performSearch(debounceMs: Long = 0L) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounceMs > 0L) delay(debounceMs)
            _isSearching.value = true
            val filter = _searchFilter.value
            val results = repository.searchMoviesOnline(
                query = filter.query,
                categoryFilter = filter.selectedCategory,
                yearFilter = filter.selectedYear,
                languageFilter = filter.selectedLanguage,
                localeConfig = regionLocale.value
            )
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    // Personal Ratings & Notes (Persisted locally per title)
    private fun loadPersonalReviewForMovie(movieId: String) {
        if (_personalReviews.value.containsKey(movieId)) return
        val stars = prefs.getInt("review_stars_$movieId", 0)
        val note = prefs.getString("review_note_$movieId", "") ?: ""
        if (stars > 0 || note.isNotBlank()) {
            _personalReviews.update { it + (movieId to PersonalReview(stars, note)) }
        }
    }

    fun savePersonalReview(movieId: String, stars: Int, note: String) {
        val cleanStars = stars.coerceIn(0, 5)
        val cleanNote = note.trim()
        prefs.edit()
            .putInt("review_stars_$movieId", cleanStars)
            .putString("review_note_$movieId", cleanNote)
            .apply()
        _personalReviews.update { it + (movieId to PersonalReview(cleanStars, cleanNote)) }
    }

    // PiP Lifecycle Coordination
    fun updatePipPlaybackState(
        isPlayerActive: Boolean,
        isPlaying: Boolean,
        title: String = ""
    ) {
        _pipPlaybackState.update {
            it.copy(
                isPlayerActive = isPlayerActive,
                isPlaying = isPlaying,
                title = title
            )
        }
    }

    fun setInPictureInPictureMode(inPip: Boolean) {
        _pipPlaybackState.update { it.copy(isInPipMode = inPip) }
    }

    // Settings actions
    fun setThemeMode(mode: String) {
        _userSettings.update { it.copy(themeMode = mode) }
    }

    fun setDefaultQuality(quality: String) {
        prefs.edit().putString(KEY_DEFAULT_QUALITY, quality).apply()
        _userSettings.update { it.copy(defaultQuality = quality) }
    }

    fun setLanguage(lang: String) {
        val changed = _userSettings.value.language != lang
        prefs.edit().putString(KEY_CATALOG_LANGUAGE, lang).apply()
        _userSettings.update { it.copy(language = lang) }
        if (lang == "Français uniquement") {
            _searchFilter.update { it.copy(selectedLanguage = "Français") }
        }
        if (changed) {
            refreshHomeCatalog()
            loadUpcomingMovies()
            performSearch(debounceMs = 0L)
        }
    }

    fun setCompressionPreset(preset: SpaceCompressionPreset) {
        prefs.edit().putString(KEY_COMPRESSION_PRESET, preset.id).apply()
        _userSettings.update { it.copy(compressionPreset = preset) }
    }

    fun setImageQualityMode(mode: ImageQualityMode) {
        val changed = _userSettings.value.imageQualityMode != mode
        prefs.edit().putString(KEY_IMAGE_QUALITY, mode.id).apply()
        NetworkModule.currentImageQuality = mode
        _userSettings.update { it.copy(imageQualityMode = mode) }
        if (changed) {
            refreshHomeCatalog()
        }
    }

    fun toggleFamilyKidsMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FAMILY_MODE, enabled).apply()
        _userSettings.update { it.copy(familyKidsMode = enabled) }
    }

    fun toggleAutoPlay(enabled: Boolean) {
        _userSettings.update { it.copy(autoPlayNext = enabled) }
    }

    fun toggleWifiOnly(enabled: Boolean) {
        _userSettings.update { it.copy(downloadWifiOnly = enabled) }
    }

    companion object {
        private const val PREFS_NAME = "gpnflix_user_prefs"
        private const val KEY_CUSTOM_TMDB_TOKEN = "custom_tmdb_v4_key"
        private const val KEY_COMPRESSION_PRESET = "space_compression_preset"
        private const val KEY_IMAGE_QUALITY = "image_quality_mode"
        private const val KEY_CATALOG_LANGUAGE = "catalog_language_mode"
        private const val KEY_FAMILY_MODE = "family_kids_mode"
        private const val KEY_DEFAULT_QUALITY = "default_video_quality"
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GpnFlixViewModel::class.java)) {
                val repo = MovieRepository.getInstance(application)
                return GpnFlixViewModel(application, repo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
