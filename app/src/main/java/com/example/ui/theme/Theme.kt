package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GpnFlixRed,
    onPrimary = Color.White,
    primaryContainer = GpnFlixRedDark,
    onPrimaryContainer = Color.White,
    secondary = GpnFlixAmber,
    onSecondary = Color.Black,
    tertiary = GpnFlixCyan,
    background = GpnBackgroundDark,
    onBackground = GpnOnBackgroundDark,
    surface = GpnSurfaceDark,
    onSurface = GpnOnSurfaceDark,
    surfaceVariant = GpnSurfaceVariantDark,
    onSurfaceVariant = GpnOnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = GpnFlixRed,
    onPrimary = Color.White,
    secondary = GpnFlixAmber,
    onSecondary = Color.Black,
    tertiary = GpnFlixCyan,
    background = GpnBackgroundLight,
    onBackground = GpnOnBackgroundLight,
    surface = GpnSurfaceLight,
    onSurface = GpnOnSurfaceLight,
    surfaceVariant = GpnSurfaceVariantLight,
    onSurfaceVariant = GpnOnSurfaceVariantLight
)

@Composable
fun GpnFlixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep signature streaming red/obsidian brand
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
