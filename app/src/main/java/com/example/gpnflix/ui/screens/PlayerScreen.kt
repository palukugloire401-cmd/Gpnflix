package com.example.gpnflix.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.PipActions
import com.example.gpnflix.data.local.DownloadStatus
import com.example.gpnflix.data.model.Movie
import com.example.gpnflix.ui.viewmodel.GpnFlixViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    movie: Movie,
    viewModel: GpnFlixViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(movie.id) {
        viewModel.ensureMovieLoaded(movie.id)
    }

    val selectedMovie by viewModel.selectedMovie.collectAsStateWithLifecycle()
    val isLoadingDetails by viewModel.isLoadingDetails.collectAsStateWithLifecycle()
    val qualityUrls by viewModel.movieQualityUrls.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()

    val activeMovie = remember(movie, selectedMovie) {
        selectedMovie?.takeIf { it.id == movie.id } ?: movie
    }

    val downloadedItem = remember(downloads, activeMovie.id) {
        downloads.find { it.movieId == activeMovie.id && it.status == DownloadStatus.COMPLETED }
    }
    val localOfflineFile = remember(downloadedItem) {
        downloadedItem?.localFilePath?.let { File(it) }?.takeIf { it.exists() && it.length() > 0L }
    }

    var selectedQuality by remember(activeMovie.id) {
        mutableStateOf(activeMovie.qualities.firstOrNull() ?: "Auto")
    }

    val resolvedStreamUrl = remember(localOfflineFile, activeMovie.videoUrl, selectedQuality, qualityUrls) {
        when {
            localOfflineFile != null -> Uri.fromFile(localOfflineFile).toString()
            selectedQuality != "Auto" && qualityUrls.containsKey(selectedQuality) -> qualityUrls[selectedQuality].orEmpty()
            else -> activeMovie.videoUrl
        }
    }

    val youtubeKey = activeMovie.trailerYoutubeKey

    when {
        resolvedStreamUrl.isNotBlank() -> {
            DirectStreamExoPlayerView(
                movie = activeMovie,
                streamUrl = resolvedStreamUrl,
                isOffline = localOfflineFile != null,
                selectedQuality = selectedQuality,
                onSelectQuality = { selectedQuality = it },
                viewModel = viewModel,
                onBackClick = onBackClick,
                modifier = modifier
            )
        }

        !youtubeKey.isNullOrBlank() -> {
            OfficialTrailerPlayerView(
                movie = activeMovie,
                youtubeKey = youtubeKey,
                viewModel = viewModel,
                onBackClick = onBackClick,
                modifier = modifier
            )
        }

        isLoadingDetails -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                BackHandler { onBackClick() }
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = Color.White
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Préparation du lecteur GpnFlix...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        else -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                BackHandler { onBackClick() }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Aucune source vidéo ou bande-annonce officielle disponible pour ce titre.",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(onClick = onBackClick) {
                        Text("Retour")
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun DirectStreamExoPlayerView(
    movie: Movie,
    streamUrl: String,
    isOffline: Boolean,
    selectedQuality: String,
    onSelectQuality: (String) -> Unit,
    viewModel: GpnFlixViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    val pipState by viewModel.pipPlaybackState.collectAsStateWithLifecycle()
    val seriesEpisodes by viewModel.seriesEpisodes.collectAsStateWithLifecycle()
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }
    var areControlsVisible by remember { mutableStateOf(true) }
    var isScreenLocked by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }

    var showQualityDialog by remember { mutableStateOf(false) }
    var selectedSubtitle by remember { mutableStateOf("Désactivé") }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    // Extra Feature #1: Sleep Timer (Minuterie de sommeil)
    var sleepTimerRemainingSec by remember { mutableStateOf<Int?>(null) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    // Extra Feature #2: Vertical Brightness & Volume Gestures
    var brightnessLevel by remember {
        mutableFloatStateOf(
            activity?.window?.attributes?.screenBrightness?.takeIf { it > 0f } ?: 0.6f
        )
    }
    var volumeFraction by remember {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        mutableFloatStateOf(curVol.toFloat() / maxVol.toFloat())
    }
    var gestureHudLabel by remember { mutableStateOf<String?>(null) }
    var gestureHudProgress by remember { mutableFloatStateOf(0f) }
    var gestureHudIsBrightness by remember { mutableStateOf(true) }

    // Extra Feature #3: Series Episodes Quick Switcher
    var showEpisodesDialog by remember { mutableStateOf(false) }
    var activeEpisodeLabel by remember { mutableStateOf<String?>(null) }

    var showLeftSeekFeedback by remember { mutableStateOf(false) }
    var showRightSeekFeedback by remember { mutableStateOf(false) }

    val exoPlayer = remember(movie.id) {
        ExoPlayer.Builder(context).build()
    }

    // Sync PiP active playback status to MainActivity
    LaunchedEffect(movie.id, isPlaying) {
        viewModel.updatePipPlaybackState(
            isPlayerActive = true,
            isPlaying = isPlaying,
            title = movie.title
        )
    }

    // Register PiP RemoteAction BroadcastReceiver (-10s / Play-Pause / +10s inside PiP mini-window)
    DisposableEffect(exoPlayer) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != PipActions.ACTION_PIP_CONTROL) return
                when (intent.getIntExtra(PipActions.EXTRA_CONTROL_TYPE, 0)) {
                    PipActions.CONTROL_TYPE_REWIND -> {
                        exoPlayer.seekTo((exoPlayer.currentPosition - 10000L).coerceAtLeast(0L))
                    }
                    PipActions.CONTROL_TYPE_PLAY_PAUSE -> {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                    }
                    PipActions.CONTROL_TYPE_FORWARD -> {
                        val cap = exoPlayer.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                        exoPlayer.seekTo((exoPlayer.currentPosition + 10000L).coerceAtMost(cap))
                    }
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(PipActions.ACTION_PIP_CONTROL),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    // Sleep Timer countdown coroutine
    LaunchedEffect(sleepTimerRemainingSec) {
        val remaining = sleepTimerRemainingSec ?: return@LaunchedEffect
        if (remaining <= 0) {
            exoPlayer.pause()
            sleepTimerRemainingSec = null
            Toast.makeText(context, "Minuterie terminée : lecture mise en pause", Toast.LENGTH_SHORT).show()
            return@LaunchedEffect
        }
        delay(1000L)
        sleepTimerRemainingSec = remaining - 1
    }

    // Immersive Netflix fullscreen system-bar hiding
    DisposableEffect(view, activity) {
        val window = activity?.window
        val insetsController = if (window != null) {
            WindowCompat.getInsetsController(window, view).apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
        } else null

        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Update MediaItem smoothly whenever streamUrl changes while preserving position
    LaunchedEffect(streamUrl) {
        val previousPos = exoPlayer.currentPosition.coerceAtLeast(0L)
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(streamUrl)))
        exoPlayer.prepare()
        if (previousPos > 1000L) {
            exoPlayer.seekTo(previousPos)
        } else {
            val savedPos = viewModel.getSavedProgress(movie.id)
            if (savedPos > 3000L) {
                exoPlayer.seekTo(savedPos)
            }
        }
        exoPlayer.playWhenReady = true
    }

    LaunchedEffect(exoPlayer) {
        while (isActive) {
            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
            isPlaying = exoPlayer.isPlaying
            isBuffering = exoPlayer.playbackState == Player.STATE_BUFFERING

            if (currentPositionMs > 1000L && totalDurationMs > 0L) {
                viewModel.saveWatchProgress(movie.id, currentPositionMs, totalDurationMs)
            }
            delay(500)
        }
    }

    LaunchedEffect(areControlsVisible, isPlaying) {
        if (areControlsVisible && isPlaying && !isScreenLocked) {
            delay(4000)
            areControlsVisible = false
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            viewModel.updatePipPlaybackState(isPlayerActive = false, isPlaying = false)
            viewModel.saveWatchProgress(
                movie.id,
                exoPlayer.currentPosition.coerceAtLeast(0L),
                exoPlayer.duration.coerceAtLeast(0L)
            )
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            exoPlayer.release()
        }
    }

    BackHandler {
        if (isScreenLocked) {
            isScreenLocked = false
        } else if (isFullscreen) {
            isFullscreen = false
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            onBackClick()
        }
    }

    val enterPip: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
            runCatching {
                val params = PipActions.buildPipParams(
                    context = context,
                    isPlaying = isPlaying,
                    autoEnter = true
                )
                activity.enterPictureInPictureMode(params)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("video_player_container")
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Left & Right Tap/Double-Tap + Vertical Brightness/Volume Drag Zones
        if (!pipState.isInPipMode) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(isScreenLocked) {
                            if (!isScreenLocked) {
                                detectTapGestures(
                                    onTap = { areControlsVisible = !areControlsVisible },
                                    onDoubleTap = {
                                        val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                        exoPlayer.seekTo(newPos)
                                        coroutineScope.launch {
                                            showLeftSeekFeedback = true
                                            delay(800)
                                            showLeftSeekFeedback = false
                                        }
                                    }
                                )
                            }
                        }
                        .pointerInput(isScreenLocked) {
                            if (!isScreenLocked) {
                                detectVerticalDragGestures(
                                    onDragEnd = { gestureHudLabel = null },
                                    onDragCancel = { gestureHudLabel = null }
                                ) { _, dragAmount ->
                                    val delta = (-dragAmount / 480f)
                                    val updated = (brightnessLevel + delta).coerceIn(0.05f, 1.0f)
                                    brightnessLevel = updated
                                    activity?.window?.let { win ->
                                        val lp = win.attributes
                                        lp.screenBrightness = updated
                                        win.attributes = lp
                                    }
                                    gestureHudIsBrightness = true
                                    gestureHudProgress = updated
                                    gestureHudLabel = "Luminosité ${(updated * 100).toInt()}%"
                                }
                            }
                        }
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(isScreenLocked) {
                            if (!isScreenLocked) {
                                detectTapGestures(
                                    onTap = { areControlsVisible = !areControlsVisible },
                                    onDoubleTap = {
                                        val durationCap = exoPlayer.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                                        val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(durationCap)
                                        exoPlayer.seekTo(newPos)
                                        coroutineScope.launch {
                                            showRightSeekFeedback = true
                                            delay(800)
                                            showRightSeekFeedback = false
                                        }
                                    }
                                )
                            }
                        }
                        .pointerInput(isScreenLocked) {
                            if (!isScreenLocked) {
                                detectVerticalDragGestures(
                                    onDragEnd = { gestureHudLabel = null },
                                    onDragCancel = { gestureHudLabel = null }
                                ) { _, dragAmount ->
                                    val delta = (-dragAmount / 480f)
                                    val updated = (volumeFraction + delta).coerceIn(0f, 1f)
                                    volumeFraction = updated
                                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                    audioManager.setStreamVolume(
                                        AudioManager.STREAM_MUSIC,
                                        (updated * maxVol).toInt(),
                                        0
                                    )
                                    gestureHudIsBrightness = false
                                    gestureHudProgress = updated
                                    gestureHudLabel = "Volume ${(updated * 100).toInt()}%"
                                }
                            }
                        }
                )
            }
        }

        // Brightness / Volume Gesture HUD Pill
        if (!pipState.isInPipMode && gestureHudLabel != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.78f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 64.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (gestureHudIsBrightness) Icons.Filled.BrightnessHigh else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.width(140.dp)) {
                        Text(
                            text = gestureHudLabel.orEmpty(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { gestureHudProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showLeftSeekFeedback && !pipState.isInPipMode,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 48.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.size(72.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Replay10, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(28.dp))
                    Text("-10s", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                }
            }
        }

        AnimatedVisibility(
            visible = showRightSeekFeedback && !pipState.isInPipMode,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 48.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.size(72.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(28.dp))
                    Text("+10s", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                }
            }
        }

        if (isBuffering && !pipState.isInPipMode) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(54.dp)
                    .align(Alignment.Center)
            )
        }

        if (isScreenLocked && !pipState.isInPipMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.78f),
                    modifier = Modifier
                        .clickable {
                            isScreenLocked = false
                            areControlsVisible = true
                        }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = "Déverrouiller", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Écran verrouillé • Toucher pour déverrouiller",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Netflix-style Overlay Chrome (hidden cleanly during PiP)
        AnimatedVisibility(
            visible = areControlsVisible && !isScreenLocked && !pipState.isInPipMode,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(180)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.78f),
                            0.25f to Color.Black.copy(alpha = 0.25f),
                            0.72f to Color.Black.copy(alpha = 0.25f),
                            1.0f to Color.Black.copy(alpha = 0.90f)
                        )
                    )
            ) {
                // Netflix Top Bar: Back + Centered Title & Source Pill + Sleep Timer + Mute / PiP
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("player_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = activeEpisodeLabel ?: movie.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isOffline) "Hors-ligne" else movie.mediaType,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOffline) Color(0xFF22C55E) else Color.White.copy(alpha = 0.72f)
                            )
                            Text("•", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                            Text(
                                text = selectedQuality,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            sleepTimerRemainingSec?.let { sec ->
                                Text("•", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                                Text(
                                    text = "Arrêt ${sec / 60}m${sec % 60}s",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { showSleepTimerDialog = true },
                        modifier = Modifier.testTag("player_sleep_timer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = "Minuterie",
                            tint = if (sleepTimerRemainingSec != null) MaterialTheme.colorScheme.secondary else Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            isMuted = !isMuted
                            exoPlayer.volume = if (isMuted) 0f else 1f
                        },
                        modifier = Modifier.testTag("player_mute_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Volume",
                            tint = Color.White
                        )
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        IconButton(
                            onClick = enterPip,
                            modifier = Modifier.testTag("player_pip_button")
                        ) {
                            Icon(Icons.Filled.PictureInPicture, contentDescription = "Picture-in-Picture", tint = Color.White)
                        }
                    }
                }

                // Netflix Center Transport Controls: Clean Replay10, Large Crisp White Play/Pause, Forward10
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(42.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(newPos)
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("player_rewind_10_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay10,
                            contentDescription = "-10s",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f))
                            .clickable {
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }
                            .testTag("player_play_pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Lecture",
                            tint = Color.White,
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val durationCap = exoPlayer.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                            val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(durationCap)
                            exoPlayer.seekTo(newPos)
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("player_forward_10_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Forward10,
                            contentDescription = "+10s",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // Netflix Bottom Scrubber + Action Strip
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    var isUserSeeking by remember { mutableStateOf(false) }
                    var seekSliderValue by remember { mutableFloatStateOf(0f) }

                    val sliderFraction = if (isUserSeeking) {
                        seekSliderValue
                    } else if (totalDurationMs > 0L) {
                        (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    val displayedPosMs = if (isUserSeeking) {
                        (seekSliderValue * totalDurationMs).toLong()
                    } else {
                        currentPositionMs
                    }
                    val remainingMs = (totalDurationMs - displayedPosMs).coerceAtLeast(0L)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = formatTime(displayedPosMs),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White.copy(alpha = 0.85f)
                        )

                        Slider(
                            value = sliderFraction,
                            onValueChange = { fraction ->
                                isUserSeeking = true
                                seekSliderValue = fraction
                            },
                            onValueChangeFinished = {
                                isUserSeeking = false
                                val seekTarget = (seekSliderValue * totalDurationMs).toLong()
                                exoPlayer.seekTo(seekTarget)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.30f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("player_seekbar")
                        )

                        Text(
                            text = if (totalDurationMs > 0L) "-${formatTime(remainingMs)}" else "00:00",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Netflix Centered Bottom Quick-Action Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NetflixPlayerActionItem(
                            icon = Icons.Filled.Speed,
                            label = "Vitesse (${playbackSpeed}x)",
                            testTagName = "player_speed_button",
                            onClick = { showSpeedDialog = true }
                        )

                        NetflixPlayerActionItem(
                            icon = Icons.Filled.LockOpen,
                            label = "Verrouiller",
                            testTagName = "player_lock_button",
                            onClick = {
                                isScreenLocked = true
                                areControlsVisible = false
                            }
                        )

                        NetflixPlayerActionItem(
                            icon = Icons.Filled.HighQuality,
                            label = selectedQuality,
                            testTagName = "player_quality_button",
                            onClick = { showQualityDialog = true }
                        )

                        if (seriesEpisodes.isNotEmpty()) {
                            NetflixPlayerActionItem(
                                icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                                label = "Épisodes",
                                testTagName = "player_episodes_button",
                                onClick = { showEpisodesDialog = true }
                            )
                        } else {
                            NetflixPlayerActionItem(
                                icon = Icons.Filled.ClosedCaption,
                                label = "Sous-titres",
                                testTagName = "player_subtitles_button",
                                onClick = { showSubtitleDialog = true }
                            )
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            NetflixPlayerActionItem(
                                icon = Icons.Filled.PictureInPicture,
                                label = "PiP",
                                testTagName = "player_pip_bottom_button",
                                onClick = enterPip
                            )
                        }

                        NetflixPlayerActionItem(
                            icon = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                            label = if (isFullscreen) "Réduire" else "Plein écran",
                            testTagName = "player_fullscreen_button",
                            onClick = {
                                isFullscreen = !isFullscreen
                                activity?.requestedOrientation = if (isFullscreen) {
                                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showSleepTimerDialog) {
        val timerOptions = listOf(
            null to "Désactivée",
            15 * 60 to "15 minutes",
            30 * 60 to "30 minutes",
            45 * 60 to "45 minutes",
            60 * 60 to "60 minutes"
        )
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Minuterie de sommeil") },
            text = {
                Column {
                    timerOptions.forEach { (sec, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    sleepTimerRemainingSec = sec
                                    showSleepTimerDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (sec == null && sleepTimerRemainingSec == null) ||
                                    (sec != null && sleepTimerRemainingSec != null && kotlin.math.abs(sleepTimerRemainingSec!! - sec) <= 60),
                                onClick = {
                                    sleepTimerRemainingSec = sec
                                    showSleepTimerDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) { Text("Fermer") }
            }
        )
    }

    if (showEpisodesDialog) {
        AlertDialog(
            onDismissRequest = { showEpisodesDialog = false },
            title = { Text("Épisodes (${movie.title})") },
            text = {
                LazyColumn(modifier = Modifier.height(260.dp)) {
                    items(seriesEpisodes) { ep ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    activeEpisodeLabel = "${movie.title} • S${ep.seasonNumber}E${ep.episodeNumber} : ${ep.name}"
                                    exoPlayer.seekTo(0L)
                                    exoPlayer.play()
                                    showEpisodesDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ép. ${ep.episodeNumber} • ${ep.name}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${ep.runtimeMinutes} min",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEpisodesDialog = false }) { Text("Fermer") }
            }
        )
    }

    if (showSpeedDialog) {
        val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Vitesse de lecture") },
            text = {
                Column {
                    speedOptions.forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playbackSpeed = speed
                                    exoPlayer.playbackParameters = PlaybackParameters(speed)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = playbackSpeed == speed,
                                onClick = {
                                    playbackSpeed = speed
                                    exoPlayer.playbackParameters = PlaybackParameters(speed)
                                    showSpeedDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = if (speed == 1.0f) "Normale (1.0x)" else "${speed}x")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) { Text("Fermer") }
            }
        )
    }

    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Qualité vidéo disponible") },
            text = {
                Column {
                    movie.qualities.forEach { quality ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectQuality(quality)
                                    showQualityDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedQuality == quality,
                                onClick = {
                                    onSelectQuality(quality)
                                    showQualityDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = quality)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) { Text("Fermer") }
            }
        )
    }

    if (showSubtitleDialog) {
        AlertDialog(
            onDismissRequest = { showSubtitleDialog = false },
            title = { Text("Audio et sous-titres") },
            text = {
                if (movie.subtitles.size <= 1) {
                    Text(
                        text = "Aucun fichier de sous-titres externe n'est fourni par la source pour ce titre.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Column {
                        movie.subtitles.forEach { sub ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedSubtitle = sub.label
                                        showSubtitleDialog = false
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedSubtitle == sub.label,
                                    onClick = {
                                        selectedSubtitle = sub.label
                                        showSubtitleDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = sub.label)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubtitleDialog = false }) { Text("Fermer") }
            }
        )
    }
}

@Composable
private fun NetflixPlayerActionItem(
    icon: ImageVector,
    label: String,
    testTagName: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .testTag(testTagName)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            ),
            color = Color.White.copy(alpha = 0.88f),
            maxLines = 1
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun OfficialTrailerPlayerView(
    movie: Movie,
    youtubeKey: String,
    viewModel: GpnFlixViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val pipState by viewModel.pipPlaybackState.collectAsStateWithLifecycle()
    val embedUrl = "https://www.youtube-nocookie.com/embed/$youtubeKey?autoplay=1&playsinline=1&rel=0"
    val watchUrl = "https://www.youtube.com/watch?v=$youtubeKey"

    LaunchedEffect(movie.id) {
        viewModel.updatePipPlaybackState(
            isPlayerActive = true,
            isPlaying = true,
            title = movie.title
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.updatePipPlaybackState(isPlayerActive = false, isPlaying = false)
        }
    }

    BackHandler { onBackClick() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(if (!pipState.isInPipMode) Modifier.statusBarsPadding().navigationBarsPadding() else Modifier)
            .testTag("video_player_container")
    ) {
        if (!pipState.isInPipMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("player_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = movie.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "BANDE-ANNONCE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                        Text(
                            text = "${movie.mediaType} • ${movie.highestQualityBadge}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    IconButton(
                        onClick = {
                            if (activity != null) {
                                runCatching {
                                    activity.enterPictureInPictureMode(
                                        PipActions.buildPipParams(context, isPlaying = true, autoEnter = true)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.testTag("trailer_pip_button")
                    ) {
                        Icon(Icons.Filled.PictureInPicture, contentDescription = "Picture-in-Picture", tint = Color.White)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val host = request?.url?.host.orEmpty()
                                return !host.contains("youtube") && !host.contains("youtu.be")
                            }
                        }
                        loadUrl(embedUrl)
                    }
                },
                modifier = if (pipState.isInPipMode) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                }
            )
        }

        if (!pipState.isInPipMode) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Seule la bande-annonce officielle en français est diffusée pour les œuvres référencées par TMDB.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(watchUrl))
                        context.startActivity(intent)
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ouvrir dans YouTube", color = Color.White)
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
