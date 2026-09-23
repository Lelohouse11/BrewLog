package com.example.brewlog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.brewlog.data.ThemeMode

/**
 * Warm Minimalist Coffee Dark Color Scheme.
 * Deep espresso roast tones with golden crema amber primary highlights.
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
 * Warm Minimalist Coffee Light Color Scheme.
 * Unbleached filter paper off-white background with dark espresso roast primary highlights.
 */
private val LightColorScheme = lightColorScheme(
    primary = DarkRoastBrown,
    onPrimary = CoffeeLightBackground,
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
    onSurfaceVariant = Color(0xFF5C524A),
    outline = CoffeeLightContainerBorder
)

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
