package com.example.gpnflix.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gpnflix.data.model.MediaContentType
import com.example.gpnflix.data.model.Movie
import com.example.gpnflix.ui.components.ContinueWatchingNetflixCard
import com.example.gpnflix.ui.components.GpnFlixTopBar
import com.example.gpnflix.ui.components.HeroBannerCarousel
import com.example.gpnflix.ui.components.MovieCard
import com.example.gpnflix.ui.components.SectionHeader
import com.example.gpnflix.ui.components.TopTenMovieCard
import com.example.gpnflix.ui.viewmodel.GpnFlixViewModel

@Composable
fun HomeScreen(
    viewModel: GpnFlixViewModel,
    onMovieClick: (Movie) -> Unit,
    onPlayMovie: (Movie) -> Unit,
    onNavigateSearch: () -> Unit,
    onNavigateCategories: () -> Unit,
    onNavigateFavorites: () -> Unit,
    onNavigateDownloads: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val homeUiState by viewModel.homeUiState.collectAsStateWithLifecycle()
    val regionLocale by viewModel.regionLocale.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val continueWatching by viewModel.continueWatching.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val allMovies by viewModel.moviesState.collectAsStateWithLifecycle()

    var selectedFilterCategory by remember { mutableStateOf("Tout") }

    val availablePillLabels = remember {
        listOf(
            "Tout",
            MediaContentType.FILM,
            MediaContentType.SERIES,
            MediaContentType.MUSIC,
            MediaContentType.CARTOON,
            MediaContentType.THEATER
        )
    }

    val familyPredicate: (Movie) -> Boolean = remember(userSettings.familyKidsMode) {
        { movie ->
            if (!userSettings.familyKidsMode) true
            else movie.isFamilyFriendly ||
                movie.mediaType == MediaContentType.CARTOON ||
                movie.mediaType == MediaContentType.MUSIC ||
                movie.category.equals("Documentaire", ignoreCase = true) ||
                movie.category.equals("Animation", ignoreCase = true)
        }
    }

    val displayedFeatured = remember(homeUiState.featuredMovies, userSettings.familyKidsMode, selectedFilterCategory) {
        val base = homeUiState.featuredMovies.filter(familyPredicate)
        val typeFiltered = if (selectedFilterCategory == "Tout") base
        else base.filter { it.mediaType == selectedFilterCategory }
        typeFiltered.ifEmpty { base }.ifEmpty { homeUiState.featuredMovies.take(3) }
    }

    val displayedTrending = remember(homeUiState.trendingMovies, userSettings.familyKidsMode, selectedFilterCategory) {
        val base = homeUiState.trendingMovies.filter(familyPredicate)
        if (selectedFilterCategory == "Tout") base
        else base.filter { it.mediaType == selectedFilterCategory }.ifEmpty { base }
    }

    val filteredRowMovies = remember(selectedFilterCategory, allMovies, homeUiState.sections, homeUiState.trendingMovies, userSettings.familyKidsMode) {
        if (selectedFilterCategory == "Tout") {
            emptyList()
        } else {
            val pool = (homeUiState.featuredMovies +
                homeUiState.trendingMovies +
                homeUiState.sections.flatMap { it.movies } +
                allMovies).distinctBy { it.id }
            pool.filter {
                familyPredicate(it) && (
                    it.mediaType == selectedFilterCategory ||
                        it.category.equals(selectedFilterCategory, ignoreCase = true) ||
                        it.genres.any { g -> g.equals(selectedFilterCategory, ignoreCase = true) }
                    )
            }
        }
    }

    Scaffold(
        topBar = {
            GpnFlixTopBar(
                onSearchClick = onNavigateSearch,
                onFavoritesClick = onNavigateFavorites,
                onDownloadsClick = onNavigateDownloads,
                onHistoryClick = onNavigateHistory,
                regionLabel = regionLocale.displayLabel
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        when {
            homeUiState.isLoading && homeUiState.featuredMovies.isEmpty() && homeUiState.sections.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Chargement du catalogue (${regionLocale.displayLabel})...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            homeUiState.errorMessage != null && homeUiState.featuredMovies.isEmpty() && homeUiState.sections.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Connexion requise",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = homeUiState.errorMessage ?: "Impossible de récupérer les œuvres pour le moment.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(onClick = { viewModel.refreshHomeCatalog() }) {
                            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Réessayer")
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .testTag("home_screen_scroll"),
                    contentPadding = PaddingValues(bottom = 36.dp)
                ) {
                    if (homeUiState.isLoading) {
                        item {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Netflix-style Mobile Top Multi-Type Filter Pills Row
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(availablePillLabels) { pill ->
                                val isSelected = selectedFilterCategory == pill
                                NetflixPillChip(
                                    label = pill,
                                    isSelected = isSelected,
                                    onClick = { selectedFilterCategory = pill }
                                )
                            }

                            item {
                                val kidsActive = userSettings.familyKidsMode
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (kidsActive) Color(0xFF22C55E).copy(alpha = 0.22f) else Color.Transparent,
                                    border = BorderStroke(
                                        1.dp,
                                        if (kidsActive) Color(0xFF22C55E) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                                    ),
                                    modifier = Modifier
                                        .clickable { viewModel.toggleFamilyKidsMode(!kidsActive) }
                                        .testTag("netflix_family_mode_pill")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ChildCare,
                                            contentDescription = "Mode Famille",
                                            tint = if (kidsActive) Color(0xFF22C55E) else MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (kidsActive) "Famille ON" else "Famille",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (kidsActive) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 12.sp
                                            ),
                                            color = if (kidsActive) Color(0xFF22C55E) else MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                            }

                            item {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.Transparent,
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                                    ),
                                    modifier = Modifier
                                        .clickable(onClick = onNavigateCategories)
                                        .testTag("netflix_categories_pill")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Catégories",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            imageVector = Icons.Filled.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            if (!userSettings.isTmdbActive) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                        modifier = Modifier
                                            .clickable(onClick = onNavigateSettings)
                                            .testTag("home_configure_tmdb_pill")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Key,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "Clé TMDB v4",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Framed Netflix Hero Banner Carousel
                    if (displayedFeatured.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            HeroBannerCarousel(
                                featuredMovies = displayedFeatured,
                                favorites = favorites,
                                onPlayClick = { movie -> onPlayMovie(movie) },
                                onDetailsClick = { movie -> onMovieClick(movie) },
                                onToggleFavorite = { movie ->
                                    val isFav = favorites.any { it.id == movie.id }
                                    viewModel.toggleFavorite(movie, isFav)
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    // Selected Content Type Row (Films, Séries, Musique, Dessins animés, Théâtre)
                    if (selectedFilterCategory != "Tout") {
                        item {
                            SectionHeader(
                                title = "Sélection FR • $selectedFilterCategory (${filteredRowMovies.size})",
                                onSeeAllClick = onNavigateCategories
                            )
                            if (filteredRowMovies.isEmpty()) {
                                Text(
                                    text = "Aucun titre dans cette sélection rapide. Ouvrez Catégories pour explorer.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            } else {
                                val cardAspect = when (selectedFilterCategory) {
                                    MediaContentType.MUSIC -> 1.0f
                                    MediaContentType.SERIES -> 1.45f
                                    else -> 0.68f
                                }
                                val cardWidth = if (cardAspect > 1.1f) 188.dp else if (cardAspect == 1.0f) 134.dp else 120.dp
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filteredRowMovies, key = { "filtered_${it.id}" }) { movie ->
                                        MovieCard(
                                            movie = movie,
                                            onClick = { onMovieClick(movie) },
                                            showTitle = true,
                                            aspectRatio = cardAspect,
                                            modifier = Modifier.width(cardWidth)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // Netflix "Continue Watching" with Centered Play + Progress Bar + Info Footer
                    if (continueWatching.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Reprendre la lecture",
                                onSeeAllClick = onNavigateHistory
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(continueWatching, key = { it.history.movieId }) { item ->
                                    ContinueWatchingNetflixCard(
                                        movie = item.movie,
                                        progressFraction = item.history.progressFraction,
                                        onPlayClick = { onPlayMovie(item.movie) },
                                        onInfoClick = { onMovieClick(item.movie) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    // Netflix Top 10 Ranked Carousel
                    if (displayedTrending.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Top 10 sur GpnFlix (${regionLocale.displayLabel})",
                                onSeeAllClick = onNavigateSearch
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(
                                    items = displayedTrending.take(10),
                                    key = { _, movie -> "top10_${movie.id}" }
                                ) { index, movie ->
                                    TopTenMovieCard(
                                        rank = index + 1,
                                        movie = movie,
                                        onClick = { onMovieClick(movie) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    // Netflix Clean Multi-Format Carousels (Portrait 2:3, Square 1:1 Musique, Landscape 16:9 Séries)
                    homeUiState.sections.forEach { section ->
                        val sectionMovies = section.movies.filter(familyPredicate)
                        if (sectionMovies.isNotEmpty()) {
                            item(key = "section_${section.id}") {
                                val rowAspect = when (section.id) {
                                    "music_fr" -> 1.0f
                                    "theater_fr" -> 1.5f
                                    else -> 0.68f
                                }
                                val rowWidth = when (rowAspect) {
                                    1.0f -> 132.dp
                                    1.5f -> 190.dp
                                    else -> 120.dp
                                }
                                SectionHeader(
                                    title = section.title,
                                    onSeeAllClick = onNavigateCategories
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(sectionMovies, key = { "${section.id}_${it.id}" }) { movie ->
                                        MovieCard(
                                            movie = movie,
                                            onClick = { onMovieClick(movie) },
                                            showTitle = rowAspect >= 1.0f,
                                            aspectRatio = rowAspect,
                                            modifier = Modifier.width(rowWidth)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    // Compact Netflix-style Legal Source & Refresh Footer
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .testTag("legal_guarantee_card"),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VerifiedUser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = homeUiState.activeSourceLabel.ifBlank { "Français uniquement • Internet Archive" },
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = buildString {
                                            append(if (userSettings.isTmdbActive) "TMDB actif (${userSettings.maskedTmdbKey})" else "Touchez Clé API pour TMDB v4")
                                            append(" • Compression ${userSettings.compressionPreset.exampleText}")
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = onNavigateSettings) {
                                    Text("Clé API", fontSize = 12.sp)
                                }
                                IconButton(onClick = { viewModel.refreshHomeCatalog() }) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = "Actualiser le catalogue",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NetflixPillChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("category_chip_$label")
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            ),
            color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}
