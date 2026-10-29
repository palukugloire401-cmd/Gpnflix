package com.example.gpnflix.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : Screen("home", "Accueil", Icons.Filled.Home, Icons.Outlined.Home)
    object Search : Screen("search", "Recherche", Icons.Filled.Search, Icons.Outlined.Search)
    object Categories : Screen("categories", "Catégories", Icons.Filled.Explore, Icons.Outlined.Explore)
    object ComingSoon : Screen("coming_soon", "À venir", Icons.Filled.Schedule, Icons.Outlined.Schedule)
    object Profile : Screen("profile", "Profil", Icons.Filled.Person, Icons.Outlined.Person)

    companion object {
        val bottomNavItems = listOf(Home, Search, Categories, ComingSoon, Profile)
    }
}

object NavDestinations {
    const val HOME = "home"
    const val SEARCH = "search"
    const val CATEGORIES = "categories"
    const val COMING_SOON = "coming_soon"
    const val PROFILE = "profile"
    const val DETAILS = "details/{movieId}"
    const val PLAYER = "player/{movieId}"
    const val FAVORITES = "favorites"
    const val HISTORY = "history"
    const val DOWNLOADS = "downloads"
    const val SETTINGS = "settings"

    fun detailsRoute(movieId: String) = "details/$movieId"
    fun playerRoute(movieId: String) = "player/$movieId"
}
