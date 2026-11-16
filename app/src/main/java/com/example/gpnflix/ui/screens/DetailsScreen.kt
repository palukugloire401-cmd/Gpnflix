package com.example.gpnflix.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.gpnflix.data.local.DownloadStatus
import com.example.gpnflix.data.local.WatchHistoryEntity
import com.example.gpnflix.data.model.ImageAspectType
import com.example.gpnflix.data.model.MediaGalleryImage
import com.example.gpnflix.data.model.Movie
import com.example.gpnflix.data.model.MovieSourceType
import com.example.gpnflix.data.model.SpaceCompressionPreset
import com.example.gpnflix.ui.components.MovieCard
import com.example.gpnflix.ui.components.SectionHeader
import com.example.gpnflix.ui.viewmodel.GpnFlixViewModel
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailsScreen(
    movie: Movie,
    viewModel: GpnFlixViewModel,
    onBackClick: () -> Unit,
    onPlayClick: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackClick() }

    val context = LocalContext.current
    LaunchedEffect(movie.id) {
        viewModel.loadMovieDetails(movie)
    }

    val selectedMovie by viewModel.selectedMovie.collectAsStateWithLifecycle()
    val isLoadingDetails by viewModel.isLoadingDetails.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val selectedSeasonNumber by viewModel.selectedSeasonNumber.collectAsStateWithLifecycle()
    val seriesEpisodes by viewModel.seriesEpisodes.collectAsStateWithLifecycle()
    val isLoadingEpisodes by viewModel.isLoadingEpisodes.collectAsStateWithLifecycle()
    val personalReviews by viewModel.personalReviews.collectAsStateWithLifecycle()

    val displayMovie = remember(movie, selectedMovie) {
        selectedMovie?.takeIf { it.id == movie.id } ?: movie
    }

    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val isFav = favorites.any { it.id == displayMovie.id }
    val downloadState by viewModel.observeDownload(displayMovie.id).collectAsStateWithLifecycle(initialValue = null)

    var selectedGalleryFilter by remember { mutableStateOf("Tout") }
    var zoomedGalleryImage by remember { mutableStateOf<MediaGalleryImage?>(null) }

    val existingReview = personalReviews[displayMovie.id]
    var reviewStars by remember(displayMovie.id, existingReview) {
        mutableIntStateOf(existingReview?.ratingStars ?: 0)
    }
    var reviewNote by remember(displayMovie.id, existingReview) {
        mutableStateOf(existingReview?.note.orEmpty())
    }
    var reviewSavedFeedback by remember { mutableStateOf(false) }

    val savedHistory by produceState<WatchHistoryEntity?>(initialValue = null, key1 = displayMovie.id) {
        value = viewModel.getSavedProgress(displayMovie.id).let { pos ->
            if (pos > 0) WatchHistoryEntity(displayMovie.id, pos, (displayMovie.durationMinutes * 60 * 1000L)) else null
        }
    }

    val allMovies by viewModel.moviesState.collectAsStateWithLifecycle()
    val similarMovies = remember(displayMovie, allMovies) {
        allMovies
            .filter {
                it.id != displayMovie.id &&
                    (it.mediaType == displayMovie.mediaType ||
                        it.category == displayMovie.category ||
                        it.genres.any { g -> displayMovie.genres.contains(g) })
            }
            .take(6)
    }

    val galleryImages = remember(displayMovie.galleryImages, displayMovie.backdropUrl, displayMovie.posterUrl) {
        if (displayMovie.galleryImages.isNotEmpty()) {
            displayMovie.galleryImages
        } else {
            buildList {
                if (displayMovie.backdropUrl.isNotBlank()) {
                    add(MediaGalleryImage(displayMovie.backdropUrl, ImageAspectType.LANDSCAPE_16_9, "Paysage 16:9"))
                }
                if (displayMovie.posterUrl.isNotBlank()) {
                    add(MediaGalleryImage(displayMovie.posterUrl, ImageAspectType.PORTRAIT_2_3, "Affiche 2:3"))
                    add(MediaGalleryImage(displayMovie.posterUrl, ImageAspectType.SQUARE_1_1, "Vignette 1:1"))
                }
            }
        }
    }

    val filteredGallery = remember(galleryImages, selectedGalleryFilter) {
        if (selectedGalleryFilter == "Tout") galleryImages
        else galleryImages.filter { it.aspectType == selectedGalleryFilter }
    }

    val canPlay = downloadState?.isCompleted == true ||
        displayMovie.hasDirectStream ||
        !displayMovie.trailerYoutubeKey.isNullOrBlank() ||
        displayMovie.sourceType == MovieSourceType.ARCHIVE_ORG

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("details_scroll_view"),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val headerImage = displayMovie.backdropUrl.ifBlank { displayMovie.posterUrl }
                    if (headerImage.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(headerImage)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Movie,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.55f),
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .testTag("details_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Retour",
                                tint = Color.White
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = {
                                    val shareText = "Découvrez \"${displayMovie.title}\" (${displayMovie.mediaType}) en français sur GpnFlix !\n${displayMovie.trailerUrl.ifBlank { displayMovie.videoUrl }}"
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, displayMovie.title)
                                        putExtra(Intent.EXTRA_TEXT, shareText.trim())
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Partager ${displayMovie.title}"))
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .testTag("details_share_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Partager",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = { viewModel.toggleFavorite(displayMovie, isFav) },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .testTag("details_favorite_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isFav) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                    contentDescription = "Favoris",
                                    tint = if (isFav) MaterialTheme.colorScheme.secondary else Color.White
                                )
                            }
                        }
                    }
                }
            }

            if (isLoadingDetails) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = displayMovie.title,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (displayMovie.originalTitle.isNotBlank() && displayMovie.originalTitle != displayMovie.title) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = displayMovie.originalTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (displayMovie.rating > 0f) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = String.format("%.1f", displayMovie.rating),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.HighQuality,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = displayMovie.highestQualityBadge,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (displayMovie.releaseYear > 0) {
                            Text(
                                text = "${displayMovie.releaseYear} • ${displayMovie.formattedDuration}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        } else {
                            Text(
                                text = displayMovie.formattedDuration,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = displayMovie.legalAccessLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (displayMovie.genres.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            displayMovie.genres.forEach { genre ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = genre,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (savedHistory != null && savedHistory!!.progressMs > 5000L) {
                item {
                    val minutesWatched = savedHistory!!.progressMs / 60000
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Replay,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Reprendre la lecture",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Dernière position : environ $minutesWatched min",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Button(
                        onClick = { onPlayClick(displayMovie) },
                        enabled = canPlay && !isLoadingDetails || displayMovie.hasDirectStream || !displayMovie.trailerYoutubeKey.isNullOrBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("details_play_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                downloadState?.isCompleted == true -> "Lire hors-ligne"
                                isLoadingDetails -> "Vérification des sources..."
                                displayMovie.hasDirectStream && savedHistory != null && savedHistory!!.progressMs > 5000L -> "Reprendre la lecture"
                                displayMovie.hasDirectStream -> "Regarder en streaming"
                                !displayMovie.trailerYoutubeKey.isNullOrBlank() -> "Voir la bande-annonce (FR)"
                                displayMovie.sourceType == MovieSourceType.ARCHIVE_ORG -> "Regarder"
                                else -> "Bande-annonce indisponible"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Space Compressor Card before Downloading (e.g., 100 Mo -> 50 Mo)
                    if (displayMovie.isDownloadable && displayMovie.videoUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val baseMb = displayMovie.estimatedSizeMb.takeIf { it > 0 } ?: 100
                        val activePreset = userSettings.compressionPreset
                        val targetMb = activePreset.estimateCompressedMb(baseMb)

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("details_compressor_card")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Compress,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Compresseur d'espace : $baseMb Mo → $targetMb Mo (-${activePreset.savingsPercent}%)",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(SpaceCompressionPreset.entries) { preset ->
                                        FilterChip(
                                            selected = userSettings.compressionPreset == preset,
                                            onClick = { viewModel.setCompressionPreset(preset) },
                                            label = {
                                                Text(
                                                    text = if (preset == SpaceCompressionPreset.SERIES_SAVER) {
                                                        "Série -50%"
                                                    } else {
                                                        preset.targetQualityLabel
                                                    },
                                                    fontSize = 11.sp
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (displayMovie.isDownloadable && displayMovie.videoUrl.isNotBlank()) {
                            val dl = downloadState
                            FilledTonalButton(
                                onClick = {
                                    when (dl?.status) {
                                        null -> viewModel.downloadMovie(displayMovie)
                                        DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING -> viewModel.pauseDownload(displayMovie.id)
                                        DownloadStatus.PAUSED, DownloadStatus.FAILED -> viewModel.resumeDownload(displayMovie.id)
                                        DownloadStatus.COMPLETED -> viewModel.deleteDownload(displayMovie.id)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("details_download_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = when (dl?.status) {
                                        DownloadStatus.COMPLETED -> Icons.Filled.DownloadDone
                                        DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING -> Icons.Filled.Pause
                                        DownloadStatus.FAILED -> Icons.Filled.Refresh
                                        DownloadStatus.PAUSED -> Icons.Filled.PlayArrow
                                        else -> Icons.Filled.Download
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (dl?.status) {
                                        DownloadStatus.QUEUED -> "En file..."
                                        DownloadStatus.DOWNLOADING -> "Pause (${dl.progressPercent}%)"
                                        DownloadStatus.PAUSED -> "Reprendre (${dl.progressPercent}%)"
                                        DownloadStatus.FAILED -> "Réessayer"
                                        DownloadStatus.COMPLETED -> "Hors-ligne ✓"
                                        else -> "Télécharger (-${userSettings.compressionPreset.savingsPercent}%)"
                                    },
                                    maxLines = 1
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.toggleFavorite(displayMovie, isFav) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("details_favorite_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isFav) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = null,
                                tint = if (isFav) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isFav) "Dans ma liste" else "Ma Liste")
                        }
                    }

                    downloadState?.takeIf { !it.isCompleted }?.let { dl ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("details_download_progress_card"),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (dl.isFailed) {
                                            Icon(
                                                imageVector = Icons.Filled.ErrorOutline,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = when (dl.status) {
                                                DownloadStatus.QUEUED -> "Préparation du téléchargement..."
                                                DownloadStatus.DOWNLOADING -> "Téléchargement • ${dl.progressPercent}%"
                                                DownloadStatus.PAUSED -> "En pause • ${dl.progressPercent}%"
                                                DownloadStatus.FAILED -> dl.errorMessage ?: "Échec du téléchargement"
                                                else -> ""
                                            },
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (dl.isFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteDownload(displayMovie.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Annuler le téléchargement",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (dl.fileSizeBytes > 0L) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${formatDetailsBytes(dl.downloadedBytes)} / ${formatDetailsBytes(dl.fileSizeBytes)} • ${dl.quality}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (dl.isDownloading || dl.isQueued || dl.isPaused) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { (dl.progressPercent / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Series Seasons & Episodes Section
            if (displayMovie.seasons.isNotEmpty() && displayMovie.tmdbId != null) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = "Saisons & Épisodes (${displayMovie.seasons.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(displayMovie.seasons, key = { it.seasonNumber }) { season ->
                                FilterChip(
                                    selected = selectedSeasonNumber == season.seasonNumber,
                                    onClick = { viewModel.loadSeriesSeason(displayMovie.tmdbId, season.seasonNumber) },
                                    label = {
                                        Text("${season.name} (${season.episodeCount} ép.)")
                                    }
                                )
                            }
                        }

                        if (isLoadingEpisodes) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        } else if (seriesEpisodes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            seriesEpisodes.take(12).forEach { ep ->
                                val rawEpMb = (ep.runtimeMinutes * 2.2f).toInt().coerceIn(70, 140)
                                val compEpMb = userSettings.compressionPreset.estimateCompressedMb(rawEpMb)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { onPlayClick(displayMovie) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(108.dp)
                                                .aspectRatio(16f / 9f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                        ) {
                                            if (ep.stillUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = ep.stillUrl,
                                                    contentDescription = ep.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Filled.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                                    .size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Ép. ${ep.episodeNumber} • ${ep.name}",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${ep.runtimeMinutes} min • $rawEpMb Mo → $compEpMb Mo",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = ep.overview,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Multi-Format Image Gallery Section (Portrait 2:3, Landscape 16:9, Square 1:1)
            if (galleryImages.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Galerie & Types d'images",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(listOf("Tout", ImageAspectType.LANDSCAPE_16_9, ImageAspectType.PORTRAIT_2_3, ImageAspectType.SQUARE_1_1)) { aspect ->
                                FilterChip(
                                    selected = selectedGalleryFilter == aspect,
                                    onClick = { selectedGalleryFilter = aspect },
                                    label = { Text(aspect, fontSize = 11.sp) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredGallery, key = { "${it.aspectType}_${it.url}" }) { item ->
                                val ratio = when (item.aspectType) {
                                    ImageAspectType.LANDSCAPE_16_9 -> 16f / 9f
                                    ImageAspectType.SQUARE_1_1 -> 1f
                                    else -> 2f / 3f
                                }
                                Card(
                                    modifier = Modifier
                                        .height(126.dp)
                                        .aspectRatio(ratio)
                                        .clickable { zoomedGalleryImage = item },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        AsyncImage(
                                            model = item.url,
                                            contentDescription = item.label,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.7f),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(6.dp)
                                        ) {
                                            Text(
                                                text = item.aspectType,
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("details_license_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Origine & Droits de Diffusion",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = displayMovie.licenseType,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (displayMovie.sourceType == MovieSourceType.TMDB) {
                                "Fiche officielle TMDB en français : lecture limitée à la bande-annonce légale officielle."
                            } else {
                                "Contenu légal accessible directement depuis Internet Archive (Domaine Public / Creative Commons)."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = "Synopsis",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = displayMovie.synopsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }
            }

            // Personal Star Rating + Review Memo Card (Feature #5)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("details_personal_review_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Votre note personnelle & mémo",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..5).forEach { star ->
                                IconButton(
                                    onClick = {
                                        reviewStars = star
                                        viewModel.savePersonalReview(displayMovie.id, star, reviewNote)
                                        reviewSavedFeedback = true
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (star <= reviewStars) Icons.Filled.Star else Icons.Filled.StarBorder,
                                        contentDescription = "$star étoiles",
                                        tint = if (star <= reviewStars) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = reviewNote,
                            onValueChange = {
                                reviewNote = it
                                reviewSavedFeedback = false
                            },
                            label = { Text("Avis rapide ou moment favori...") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (reviewSavedFeedback) {
                                Text(
                                    text = "Enregistré localement ✓",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF22C55E)
                                )
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }
                            FilledTonalButton(
                                onClick = {
                                    viewModel.savePersonalReview(displayMovie.id, reviewStars, reviewNote)
                                    reviewSavedFeedback = true
                                }
                            ) {
                                Text("Enregistrer ma note")
                            }
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = "Fiche technique",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    MetadataRow(label = "Type", value = displayMovie.mediaType)
                    if (displayMovie.director.isNotBlank()) {
                        MetadataRow(label = "Réalisation", value = displayMovie.director)
                    }
                    if (displayMovie.cast.isNotEmpty()) {
                        MetadataRow(label = "Distribution", value = displayMovie.cast.joinToString(", "))
                    }
                    if (displayMovie.language.isNotBlank()) {
                        MetadataRow(label = "Langue", value = displayMovie.language)
                    }
                    if (displayMovie.qualities.isNotEmpty()) {
                        MetadataRow(label = "Formats / Qualités", value = displayMovie.qualities.joinToString(", "))
                    }
                }
            }

            if (similarMovies.isNotEmpty()) {
                item {
                    SectionHeader(title = "Titres similaires")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(similarMovies, key = { it.id }) { simMovie ->
                            MovieCard(
                                movie = simMovie,
                                onClick = { onMovieClick(simMovie) },
                                modifier = Modifier.width(130.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    zoomedGalleryImage?.let { img ->
        Dialog(onDismissRequest = { zoomedGalleryImage = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    AsyncImage(
                        model = img.url,
                        contentDescription = img.label,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${img.label} • ${img.aspectType}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                        IconButton(onClick = { zoomedGalleryImage = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label : ",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(130.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatDetailsBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 Mo"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        String.format(Locale.FRANCE, "%.2f Go", mb / 1024.0)
    } else {
        String.format(Locale.FRANCE, "%.1f Mo", mb)
    }
}
