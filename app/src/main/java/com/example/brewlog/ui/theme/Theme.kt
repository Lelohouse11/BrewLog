package com.example.brewlog.ui.theme

import android.app.Activity
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

/**
 * Monochrome Dark Color Scheme.
 * Uses deep greys and blacks for a sleek, minimal look.
 */
private val DarkColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Black,
    primaryContainer = Grey800,
    onPrimaryContainer = Grey100,
    secondary = Grey300,
    onSecondary = Black,
    background = DarkGrey,
    onBackground = Grey200,
    surface = Grey900,
    onSurface = Grey100,
    surfaceVariant = Grey800,
    onSurfaceVariant = Grey300,
    outline = Grey500
)

/**
 * Monochrome Light Color Scheme.
 * Uses clean whites and light greys.
 */
private val LightColorScheme = lightColorScheme(
    primary = Black,
    onPrimary = White,
    primaryContainer = Grey200,
    onPrimaryContainer = Grey900,
    secondary = Grey700,
    onSecondary = White,
    background = White,
    onBackground = Black,
    surface = Grey100,
    onSurface = Black,
    surfaceVariant = Grey200,
    onSurfaceVariant = Grey700,
    outline = Grey500
)

@Composable
fun BrewLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamic color to maintain the custom monochrome branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
