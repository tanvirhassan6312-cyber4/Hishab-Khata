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

    background = OffWhite,
    onBackground = DarkCharcoal,
    surface = CardBackground,
    onSurface = DarkCharcoal,
    surfaceVariant = LightGraySurface,
    onSurfaceVariant = CharcoalMedium,
    outline = CardBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = RoyalBlue500,
    onPrimary = Color.White,
    primaryContainer = RoyalBlue900,
    onPrimaryContainer = RoyalBlue100,

    secondary = Emerald500,
    onSecondary = Color.White,
    secondaryContainer = Emerald900,
    onSecondaryContainer = Emerald100,

    tertiary = Sky500,
    onTertiary = Color.White,
    tertiaryContainer = Sky700,
    onTertiaryContainer = Sky100,

    error = Red500,
    onError = Color.White,
    errorContainer = Red700,
    onErrorContainer = Red100,

    background = Color(0xFF0B1120),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569)
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
