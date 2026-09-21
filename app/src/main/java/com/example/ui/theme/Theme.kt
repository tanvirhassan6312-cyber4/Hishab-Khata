package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = DeepIndigo,
    onPrimary = Color.White,
    primaryContainer = RoyalBlue50,
    onPrimaryContainer = DeepIndigo,

    secondary = Emerald500,
    onSecondary = Color.White,
    secondaryContainer = Emerald50,
    onSecondaryContainer = Emerald900,

    tertiary = Sky500,
    onTertiary = Color.White,
    tertiaryContainer = Sky100,
    onTertiaryContainer = Sky700,

    error = Red600,
    onError = Color.White,
    errorContainer = Red50,
    onErrorContainer = Red700,

    background = PureWhite,
    onBackground = DarkCharcoal,
    surface = CardBackground,
    onSurface = DarkCharcoal,
    surfaceVariant = LightGraySurface,
    onSurfaceVariant = CharcoalMedium,
    outline = CardBorder
)

private val DarkColorScheme = lightColorScheme(
    primary = DeepIndigo,
    onPrimary = Color.White,
    primaryContainer = RoyalBlue50,
    onPrimaryContainer = DeepIndigo,

    secondary = Emerald500,
    onSecondary = Color.White,
    secondaryContainer = Emerald50,
    onSecondaryContainer = Emerald900,

    tertiary = Sky500,
    onTertiary = Color.White,
    tertiaryContainer = Sky100,
    onTertiaryContainer = Sky700,

    error = Red600,
    onError = Color.White,
    errorContainer = Red50,
    onErrorContainer = Red700,

    background = PureWhite,
    onBackground = DarkCharcoal,
    surface = CardBackground,
    onSurface = DarkCharcoal,
    surfaceVariant = LightGraySurface,
    onSurfaceVariant = CharcoalMedium,
    outline = CardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
