package com.example.gpnflix.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gpnflix.data.local.DownloadEntity
import com.example.gpnflix.data.local.VideoDownloadWithMetadata
import com.example.gpnflix.data.local.WatchHistoryEntity
import com.example.gpnflix.data.model.CategoryItem
import com.example.gpnflix.data.model.Movie
import com.example.gpnflix.data.model.UpcomingMovie
import com.example.gpnflix.data.repository.MovieRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContinueWatchingItem(
    val movie: Movie,
    val history: WatchHistoryEntity
)

data class SearchFilterState(
    val query: String = "",
    val selectedCategory: String = "all",
    val selectedYear: Int? = null,
    val selectedLanguage: String = "Toutes"
)

data class UserSettingsState(
    val themeMode: String = "DARK", // "DARK", "LIGHT", "SYSTEM"
    val defaultQuality: String = "Auto (1080p)",
    val language: String = "Français",
    val autoPlayNext: Boolean = true,
    val downloadWifiOnly: Boolean = false
)

class GpnFlixViewModel(
    application: Application,
    private val repository: MovieRepository
) : AndroidViewModel(application) {

    // Synchronous catalogue snapshot + Room-backed reactive video metadata flow
    val allMovies: List<Movie> = repository.getAllMovies()

    val moviesState: StateFlow<List<Movie>> = repository.observeAllVideos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = allMovies
        )

    val featuredMovies: List<Movie> = repository.getFeaturedMovies()
    val trendingMovies: List<Movie> = repository.getTrendingMovies()
    val categories: List<CategoryItem> = repository.getCategories()
    val upcomingMovies: List<UpcomingMovie> = repository.getUpcomingMovies()

    // Search state
    private val _searchFilter = MutableStateFlow(SearchFilterState())
    val searchFilter: StateFlow<SearchFilterState> = _searchFilter.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Movie>>(allMovies)
    val searchResults: StateFlow<List<Movie>> = _searchResults.asStateFlow()

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

    // User settings
    private val _userSettings = MutableStateFlow(UserSettingsState())
    val userSettings: StateFlow<UserSettingsState> = _userSettings.asStateFlow()

    // Selected movie for details screen
    private val _selectedMovie = MutableStateFlow<Movie?>(allMovies.firstOrNull())
    val selectedMovie: StateFlow<Movie?> = _selectedMovie.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureCatalogueSeeded()
        }
        performSearch()
    }

    fun selectMovie(movie: Movie) {
        _selectedMovie.value = movie
    }

    fun selectMovieById(id: String) {
        _selectedMovie.value = moviesState.value.find { it.id == id } ?: repository.getMovieById(id)
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

    fun downloadMovie(movie: Movie) {
        viewModelScope.launch {
            val settings = _userSettings.value
            repository.startDownload(
                movie = movie,
                quality = settings.defaultQuality,
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
        performSearch()
    }

    fun updateSearchCategory(category: String) {
        _searchFilter.update { it.copy(selectedCategory = category) }
        performSearch()
    }

    fun updateSearchYear(year: Int?) {
        _searchFilter.update { it.copy(selectedYear = year) }
        performSearch()
    }

    fun updateSearchLanguage(language: String) {
        _searchFilter.update { it.copy(selectedLanguage = language) }
        performSearch()
    }

    fun resetFilters() {
        _searchFilter.value = SearchFilterState()
        performSearch()
    }

    private fun performSearch() {
        val filter = _searchFilter.value
        _searchResults.value = repository.searchMovies(
            query = filter.query,
            categoryFilter = filter.selectedCategory,
            yearFilter = filter.selectedYear,
            languageFilter = filter.selectedLanguage
        )
    }

    // Settings actions
    fun setThemeMode(mode: String) {
        _userSettings.update { it.copy(themeMode = mode) }
    }

    fun setDefaultQuality(quality: String) {
        _userSettings.update { it.copy(defaultQuality = quality) }
    }

    fun setLanguage(lang: String) {
        _userSettings.update { it.copy(language = lang) }
    }

    fun toggleAutoPlay(enabled: Boolean) {
        _userSettings.update { it.copy(autoPlayNext = enabled) }
    }

    fun toggleWifiOnly(enabled: Boolean) {
        _userSettings.update { it.copy(downloadWifiOnly = enabled) }
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
