package com.example.amoledgallery.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun AmoledGalleryTheme(
    content: @Composable () -> Unit
) {
    val theme = ThemeState.currentTheme()
    val palette = theme.palette
    val light = palette.isLight

    val colorScheme = if (light) {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            secondary = palette.surfaceVariant,
            onSecondary = palette.onBackground,
            tertiary = palette.accent,
            background = palette.background,
            onBackground = palette.onBackground,
            surface = palette.surface,
            onSurface = palette.onBackground,
            surfaceVariant = palette.surfaceVariant,
            onSurfaceVariant = palette.textMuted
        )
    } else {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
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
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // Content is drawn edge-to-edge underneath both system bars (the glass bars blur it),
                // so the bars themselves stay transparent and only their icon color follows the theme.
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = light
                insetsController.isAppearanceLightNavigationBars = light
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
