package com.example.amoledgallery.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun AmoledGalleryTheme(
    content: @Composable () -> Unit
) {
    val theme = ThemeState.currentTheme()
    val palette = theme.palette

    val colorScheme = darkColorScheme(
        primary = palette.onBackground,
        onPrimary = palette.background,
        secondary = palette.surfaceVariant,
        onSecondary = palette.onBackground,
        tertiary = palette.accent,
        background = palette.background,
        onBackground = palette.onBackground,
        surface = palette.background,
        onSurface = palette.onBackground,
        surfaceVariant = palette.surfaceVariant,
        onSurfaceVariant = palette.textMuted
    )

    val view = LocalView.current
    val statusBarColorArgb = palette.background.toArgb()

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = statusBarColorArgb
                window.navigationBarColor = statusBarColorArgb
                val insetsController = WindowCompat.getInsetsController(window, view)
                // False means icons/text on status and navigation bars are white (for black bars)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    CompositionLocalProvider(LocalAppTheme provides theme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
