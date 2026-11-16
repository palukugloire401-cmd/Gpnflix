package com.example

import android.app.Application
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gpnflix.ui.navigation.GpnFlixBottomNav
import com.example.gpnflix.ui.navigation.NavDestinations
import com.example.gpnflix.ui.navigation.Screen
import com.example.gpnflix.ui.screens.CategoriesScreen
import com.example.gpnflix.ui.screens.ComingSoonScreen
import com.example.gpnflix.ui.screens.DetailsScreen
import com.example.gpnflix.ui.screens.DownloadsScreen
import com.example.gpnflix.ui.screens.FavoritesScreen
import com.example.gpnflix.ui.screens.HistoryScreen
import com.example.gpnflix.ui.screens.HomeScreen
import com.example.gpnflix.ui.screens.PlayerScreen
import com.example.gpnflix.ui.screens.ProfileScreen
import com.example.gpnflix.ui.screens.SearchScreen
import com.example.gpnflix.ui.screens.SettingsScreen
import com.example.gpnflix.ui.viewmodel.GpnFlixViewModel
import com.example.ui.theme.GpnFlixTheme

object PipActions {
    const val ACTION_PIP_CONTROL = "com.example.gpnflix.PIP_CONTROL"
    const val EXTRA_CONTROL_TYPE = "control_type"
    const val CONTROL_TYPE_REWIND = 1
    const val CONTROL_TYPE_PLAY_PAUSE = 2
    const val CONTROL_TYPE_FORWARD = 3

    @RequiresApi(Build.VERSION_CODES.O)
    fun buildPipParams(
        context: Context,
        isPlaying: Boolean,
        autoEnter: Boolean
    ): PictureInPictureParams {
        val rewindIntent = PendingIntent.getBroadcast(
            context,
            CONTROL_TYPE_REWIND,
            Intent(ACTION_PIP_CONTROL)
                .setPackage(context.packageName)
                .putExtra(EXTRA_CONTROL_TYPE, CONTROL_TYPE_REWIND),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseIntent = PendingIntent.getBroadcast(
            context,
            CONTROL_TYPE_PLAY_PAUSE,
            Intent(ACTION_PIP_CONTROL)
                .setPackage(context.packageName)
                .putExtra(EXTRA_CONTROL_TYPE, CONTROL_TYPE_PLAY_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val forwardIntent = PendingIntent.getBroadcast(
            context,
            CONTROL_TYPE_FORWARD,
            Intent(ACTION_PIP_CONTROL)
                .setPackage(context.packageName)
                .putExtra(EXTRA_CONTROL_TYPE, CONTROL_TYPE_FORWARD),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val actions = listOf(
            RemoteAction(
                Icon.createWithResource(context, android.R.drawable.ic_media_rew),
                "-10s",
                "Reculer de 10 secondes",
                rewindIntent
            ),
            RemoteAction(
                Icon.createWithResource(
                    context,
                    if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
                ),
                if (isPlaying) "Pause" else "Lecture",
                if (isPlaying) "Mettre en pause" else "Reprendre la lecture",
                playPauseIntent
            ),
            RemoteAction(
                Icon.createWithResource(context, android.R.drawable.ic_media_ff),
                "+10s",
                "Avancer de 10 secondes",
                forwardIntent
            )
        )

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(actions)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(autoEnter)
            builder.setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }
}

class MainActivity : ComponentActivity() {

    private var appViewModel: GpnFlixViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val app = context.applicationContext as Application
            val viewModel: GpnFlixViewModel = viewModel(
                factory = GpnFlixViewModel.Factory(app)
            )
            appViewModel = viewModel

            val pipState by viewModel.pipPlaybackState.collectAsStateWithLifecycle()
            LaunchedEffect(pipState.isPlayerActive, pipState.isPlaying) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && pipState.isPlayerActive) {
                    runCatching {
                        setPictureInPictureParams(
                            PipActions.buildPipParams(
                                context = this@MainActivity,
                                isPlaying = pipState.isPlaying,
                                autoEnter = pipState.isPlaying
                            )
                        )
                    }
                }
            }

            val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
            val isDarkTheme = when (userSettings.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            GpnFlixTheme(darkTheme = isDarkTheme) {
                GpnFlixApp(viewModel = viewModel)
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val pipState = appViewModel?.pipPlaybackState?.value ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
            pipState.isPlayerActive &&
            pipState.isPlaying
        ) {
            runCatching {
                enterPictureInPictureMode(
                    PipActions.buildPipParams(
                        context = this,
                        isPlaying = true,
                        autoEnter = false
                    )
                )
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        appViewModel?.setInPictureInPictureMode(isInPictureInPictureMode)
    }
}

@Composable
fun GpnFlixApp(viewModel: GpnFlixViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val pipState by viewModel.pipPlaybackState.collectAsStateWithLifecycle()

    val isTopLevelDestination = Screen.bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (isTopLevelDestination && !pipState.isInPipMode) {
                GpnFlixBottomNav(
                    navController = navController,
                    currentRoute = currentRoute
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavDestinations.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isTopLevelDestination && !pipState.isInPipMode) innerPadding else PaddingValues()),
            enterTransition = { fadeIn(animationSpec = tween(250)) + slideInHorizontally(initialOffsetX = { it / 4 }, animationSpec = tween(250)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) },
            popEnterTransition = { fadeIn(animationSpec = tween(250)) },
            popExitTransition = { fadeOut(animationSpec = tween(200)) + slideOutHorizontally(targetOffsetX = { it / 4 }, animationSpec = tween(200)) }
        ) {
            // Screen 1: Home
            composable(NavDestinations.HOME) {
                HomeScreen(
                    viewModel = viewModel,
                    onMovieClick = { movie ->
                        viewModel.selectMovie(movie)
                        navController.navigate(NavDestinations.detailsRoute(movie.id))
                    },
                    onPlayMovie = { movie ->
                        viewModel.selectMovie(movie)
                        navController.navigate(NavDestinations.playerRoute(movie.id))
                    },
                    onNavigateSearch = { navController.navigate(NavDestinations.SEARCH) },
                    onNavigateCategories = { navController.navigate(NavDestinations.CATEGORIES) },
                    onNavigateFavorites = { navController.navigate(NavDestinations.FAVORITES) },
                    onNavigateDownloads = { navController.navigate(NavDestinations.DOWNLOADS) },
                    onNavigateHistory = { navController.navigate(NavDestinations.HISTORY) },
                    onNavigateSettings = { navController.navigate(NavDestinations.SETTINGS) }
                )
            }

            // Screen 2: Search
            composable(NavDestinations.SEARCH) {
                SearchScreen(
                    viewModel = viewModel,
                    onMovieClick = { movie ->
                        viewModel.selectMovie(movie)
                        navController.navigate(NavDestinations.detailsRoute(movie.id))
                    }
                )
            }

            // Screen 3: Categories
            composable(NavDestinations.CATEGORIES) {
                CategoriesScreen(
                    viewModel = viewModel,
                    onMovieClick = { movie ->
                        viewModel.selectMovie(movie)
                        navController.navigate(NavDestinations.detailsRoute(movie.id))
                    }
                )
            }

            // Screen 4: Coming Soon
            composable(NavDestinations.COMING_SOON) {
                ComingSoonScreen(
                    viewModel = viewModel,
                    onPlayTrailer = { trailerMovie ->
                        viewModel.selectMovie(trailerMovie)
                        navController.navigate(NavDestinations.playerRoute(trailerMovie.id))
                    }
                )
            }

            // Screen 5: Profile
            composable(NavDestinations.PROFILE) {
                ProfileScreen(
                    viewModel = viewModel,
                    onNavigateFavorites = { navController.navigate(NavDestinations.FAVORITES) },
                    onNavigateHistory = { navController.navigate(NavDestinations.HISTORY) },
                    onNavigateDownloads = { navController.navigate(NavDestinations.DOWNLOADS) },
                    onNavigateSettings = { navController.navigate(NavDestinations.SETTINGS) }
                )
            }

            // Screen 6: Details
            composable(
                route = NavDestinations.DETAILS,
                arguments = listOf(navArgument("movieId") { type = NavType.StringType })
            ) { backStackEntry ->
                val movieId = backStackEntry.arguments?.getString("movieId") ?: ""
                val selectedMovie by viewModel.selectedMovie.collectAsStateWithLifecycle()
                val movie = selectedMovie?.takeIf { it.id == movieId }
                    ?: viewModel.resolveMovieById(movieId)

                if (movie != null) {
                    DetailsScreen(
                        movie = movie,
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() },
                        onPlayClick = { selected ->
                            viewModel.selectMovie(selected)
                            navController.navigate(NavDestinations.playerRoute(selected.id))
                        },
                        onMovieClick = { similarMovie ->
                            viewModel.selectMovie(similarMovie)
                            navController.navigate(NavDestinations.detailsRoute(similarMovie.id))
                        }
                    )
                }
            }

            // Screen 7: Video Player
            composable(
                route = NavDestinations.PLAYER,
                arguments = listOf(navArgument("movieId") { type = NavType.StringType })
            ) { backStackEntry ->
                val movieId = backStackEntry.arguments?.getString("movieId") ?: ""
                val selectedMovie by viewModel.selectedMovie.collectAsStateWithLifecycle()
                val movie = selectedMovie?.takeIf { it.id == movieId }
                    ?: viewModel.resolveMovieById(movieId)

                if (movie != null) {
                    PlayerScreen(
                        movie = movie,
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            // Screen 8: Favorites
            composable(NavDestinations.FAVORITES) {
                FavoritesScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onMovieClick = { movie ->
                        viewModel.selectMovie(movie)
                        navController.navigate(NavDestinations.detailsRoute(movie.id))
                    },
                    onExploreClick = {
                        navController.navigate(NavDestinations.HOME) {
                            popUpTo(NavDestinations.HOME) { inclusive = true }
                        }
                    }
                )
            }

            // Screen 9: History
            composable(NavDestinations.HISTORY) {
                HistoryScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onPlayMovie = { movie ->
                        viewModel.selectMovie(movie)
                        navController.navigate(NavDestinations.playerRoute(movie.id))
                    },
                    onMovieClick = { movie ->
                        viewModel.selectMovie(movie)
                        navController.navigate(NavDestinations.detailsRoute(movie.id))
                    }
                )
            }

            // Screen 10: Downloads
            composable(NavDestinations.DOWNLOADS) {
                DownloadsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onPlayMovie = { movie ->
                        viewModel.selectMovie(movie)
                        navController.navigate(NavDestinations.playerRoute(movie.id))
                    },
                    onExploreClick = {
                        navController.navigate(NavDestinations.HOME) {
                            popUpTo(NavDestinations.HOME) { inclusive = true }
                        }
                    }
                )
            }

            // Screen 11: Settings
            composable(NavDestinations.SETTINGS) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
