package com.example.gpnflix.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gpnflix.data.model.Movie
import com.example.gpnflix.ui.components.CategoryChip
import com.example.gpnflix.ui.components.GpnFlixTopBar
import com.example.gpnflix.ui.components.HeroBanner
import com.example.gpnflix.ui.components.HeroBannerCarousel
import com.example.gpnflix.ui.components.MovieCard
import com.example.gpnflix.ui.components.SectionHeader
import com.example.gpnflix.ui.viewmodel.ContinueWatchingItem
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
    modifier: Modifier = Modifier
) {
    val continueWatching by viewModel.continueWatching.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val allMovies = viewModel.allMovies
    val featuredMovie = viewModel.featuredMovies.firstOrNull() ?: allMovies.first()
    val trendingMovies = viewModel.trendingMovies
    val animationMovies = remember(allMovies) { allMovies.filter { it.category == "Animation" } }
    val classicMovies = remember(allMovies) { allMovies.filter { it.category == "Classiques" } }
    val documentaryMovies = remember(allMovies) { allMovies.filter { it.category == "Documentaire" } }

    var selectedFilterCategory by remember { mutableStateOf("Tous") }

    val displayedMovies = remember(selectedFilterCategory, allMovies) {
        if (selectedFilterCategory == "Tous") allMovies
        else allMovies.filter { it.category.equals(selectedFilterCategory, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            GpnFlixTopBar(
                onSearchClick = onNavigateSearch,
                onFavoritesClick = onNavigateFavorites,
                onDownloadsClick = onNavigateDownloads,
                onHistoryClick = onNavigateHistory
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("home_screen_scroll"),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Hero Banner Carousel
            item {
                HeroBannerCarousel(
                    featuredMovies = viewModel.featuredMovies,
                    favorites = favorites,
                    onPlayClick = { movie -> onPlayMovie(movie) },
                    onDetailsClick = { movie -> onMovieClick(movie) },
                    onToggleFavorite = { movie ->
                        val isFav = favorites.any { it.id == movie.id }
                        viewModel.toggleFavorite(movie.id, isFav)
                    }
                )
            }

            // Legal Guarantee Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("legal_guarantee_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Catalogue 100% Légal & Gratuit",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Blender Open Movies • Domaine Public • Creative Commons • NASA Open Data",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Continue Watching Section (if history exists)
            if (continueWatching.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Reprendre la lecture",
                        onSeeAllClick = onNavigateHistory
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(continueWatching, key = { it.history.movieId }) { item ->
                            ContinueWatchingCard(
                                item = item,
                                onClick = { onPlayMovie(item.movie) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Quick Category Filter Bar
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterChips = listOf("Tous", "Animation", "Classiques", "Science-Fiction", "Documentaire")
                    items(filterChips) { chip ->
                        CategoryChip(
                            label = chip,
                            isSelected = selectedFilterCategory == chip,
                            onClick = { selectedFilterCategory = chip }
                        )
                    }
                }
            }

            // Filtered row if a category is selected
            if (selectedFilterCategory != "Tous") {
                item {
                    SectionHeader(
                        title = "Sélection $selectedFilterCategory",
                        onSeeAllClick = onNavigateCategories
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(displayedMovies, key = { it.id }) { movie ->
                            MovieCard(
                                movie = movie,
                                onClick = { onMovieClick(movie) },
                                modifier = Modifier.width(135.dp)
                            )
                        }
                    }
                }
            }

            // Tendances du moment
            item {
                SectionHeader(
                    title = "Tendances du moment",
                    onSeeAllClick = onNavigateSearch
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(trendingMovies, key = { it.id }) { movie ->
                        MovieCard(
                            movie = movie,
                            onClick = { onMovieClick(movie) },
                            modifier = Modifier.width(135.dp)
                        )
                    }
                }
            }

            // Animation Open Source (Blender Foundation)
            if (animationMovies.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Animation & Projets Libres",
                        onSeeAllClick = onNavigateCategories
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(animationMovies, key = { it.id }) { movie ->
                            MovieCard(
                                movie = movie,
                                onClick = { onMovieClick(movie) },
                                modifier = Modifier.width(135.dp)
                            )
                        }
                    }
                }
            }

            // Classiques du cinéma (Domaine public)
            if (classicMovies.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Chefs-d'œuvre du Domaine Public",
                        onSeeAllClick = onNavigateCategories
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(classicMovies, key = { it.id }) { movie ->
                            MovieCard(
                                movie = movie,
                                onClick = { onMovieClick(movie) },
                                modifier = Modifier.width(135.dp)
                            )
                        }
                    }
                }
            }

            // Documentaires
            if (documentaryMovies.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Sciences & Documentaires",
                        onSeeAllClick = onNavigateCategories
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(documentaryMovies, key = { it.id }) { movie ->
                            MovieCard(
                                movie = movie,
                                onClick = { onMovieClick(movie) },
                                modifier = Modifier.width(135.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContinueWatchingCard(
    item: ContinueWatchingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MovieCard(
        movie = item.movie,
        onClick = onClick,
        watchProgressFraction = item.history.progressFraction,
        modifier = modifier.width(140.dp)
    )
}
