package com.example.brewlog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.brewlog.data.ThemeMode

/**
 * BrewLog "Crema Glass" Dark Color Scheme.
 * Deep roasted espresso background with warm crema amber accents & glass highlights.
 */
private val DarkColorScheme = darkColorScheme(
    primary = CremaAmber,
    onPrimary = CoffeeDarkBackground,
    primaryContainer = CoffeeDarkContainer,
    onPrimaryContainer = WarmGreyText,
    secondary = CremaAmberDark,
    onSecondary = CoffeeDarkBackground,
    secondaryContainer = CoffeeDarkContainer,
    onSecondaryContainer = WarmGreyText,
    tertiary = CremaAmber,
    onTertiary = CoffeeDarkBackground,
    background = CoffeeDarkBackground,
    onBackground = WarmGreyText,
    surface = CoffeeDarkSurface,
    onSurface = WarmGreyText,
    surfaceVariant = CoffeeDarkContainer,
    onSurfaceVariant = Color(0xFFC7B8AD),
    outline = CoffeeDarkContainerBorder
)

/**
 * BrewLog "Vellum Paper" Light Color Scheme.
 * Unbleached filter paper off-white background with dark espresso roast primary highlights & warm copper accents.
 */
private val LightColorScheme = lightColorScheme(
    primary = DarkRoastBrown,
    onPrimary = White,
    primaryContainer = CoffeeLightContainer,
    onPrimaryContainer = DarkCharcoalText,
    secondary = MediumRoastBrown,
    onSecondary = White,
    secondaryContainer = CoffeeLightContainer,
    onSecondaryContainer = DarkCharcoalText,
    tertiary = MediumRoastBrown,
    onTertiary = White,
    background = CoffeeLightBackground,
    onBackground = DarkCharcoalText,
    surface = CoffeeLightSurface,
    onSurface = DarkCharcoalText,
    surfaceVariant = CoffeeLightContainer,
    onSurfaceVariant = Color(0xFF4A4038),
    outline = CoffeeLightContainerBorder
)

/**
 * Helper to check if the current active App Theme is dark.
 * Guarantees components adapt correctly whether theme is controlled by App Settings or System.
 */
@Composable
fun isAppInDarkTheme(): Boolean {
    return MaterialTheme.colorScheme.background == CoffeeDarkBackground
}

@Composable
fun BrewLogTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
