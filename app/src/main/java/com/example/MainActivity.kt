package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val app = context.applicationContext as Application
            val viewModel: GpnFlixViewModel = viewModel(
                factory = GpnFlixViewModel.Factory(app)
            )

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
}

@Composable
fun GpnFlixApp(viewModel: GpnFlixViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isTopLevelDestination = Screen.bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (isTopLevelDestination) {
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
                .padding(if (isTopLevelDestination) innerPadding else androidx.compose.foundation.layout.PaddingValues()),
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
                    onNavigateHistory = { navController.navigate(NavDestinations.HISTORY) }
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
                val movie = viewModel.allMovies.find { it.id == movieId } ?: viewModel.allMovies.first()

                DetailsScreen(
                    movie = movie,
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onPlayClick = { selectedMovie ->
                        navController.navigate(NavDestinations.playerRoute(selectedMovie.id))
                    },
                    onMovieClick = { similarMovie ->
                        navController.navigate(NavDestinations.detailsRoute(similarMovie.id))
                    }
                )
            }

            // Screen 7: Video Player
            composable(
                route = NavDestinations.PLAYER,
                arguments = listOf(navArgument("movieId") { type = NavType.StringType })
            ) { backStackEntry ->
                val movieId = backStackEntry.arguments?.getString("movieId") ?: ""
                val movie = viewModel.allMovies.find { it.id == movieId }
                    ?: viewModel.selectedMovie.value
                    ?: viewModel.allMovies.first()

                PlayerScreen(
                    movie = movie,
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
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
