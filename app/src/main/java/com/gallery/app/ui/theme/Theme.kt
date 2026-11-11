package com.gallery.app.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.runtime.getValue
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
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private const val REFERENCE_SMALLEST_WIDTH_DP = 420f

@Composable
fun GalleryTheme(
    content: @Composable () -> Unit
) {
    val targetTheme = ThemeState.currentTheme()
    val target = targetTheme.palette

    // Palette changes (accent, light/dark) cross-fade instead of cutting over.
    val colorSpec = tween<Color>(durationMillis = 450)
    val background by animateColorAsState(target.background, colorSpec, label = "palette_background")
    val onBackground by animateColorAsState(target.onBackground, colorSpec, label = "palette_on_background")
    val textMuted by animateColorAsState(target.textMuted, colorSpec, label = "palette_text_muted")
    val surface by animateColorAsState(target.surface, colorSpec, label = "palette_surface")
    val surfaceVariant by animateColorAsState(target.surfaceVariant, colorSpec, label = "palette_surface_variant")
    val accent by animateColorAsState(target.accent, colorSpec, label = "palette_accent")
    val cardBorder by animateColorAsState(target.cardBorder, colorSpec, label = "palette_card_border")
    val overlayScrim by animateColorAsState(target.overlayScrim, colorSpec, label = "palette_scrim")
    val palette = target.copy(
        background = background,
        onBackground = onBackground,
        textMuted = textMuted,
        surface = surface,
        surfaceVariant = surfaceVariant,
        accent = accent,
        cardBorder = cardBorder,
        overlayScrim = overlayScrim
    )
    val theme = targetTheme.copy(palette = palette)
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

    // UI size follows the physical screen: layouts are tuned for a ~420dp wide phone, so narrower or
    // denser screens (e.g. the Galaxy A15 at 384dp) scale everything down proportionally instead of
    // showing bars and text larger than the system's own apps. Tablets keep the normal size.
    val baseDensity = LocalDensity.current
    val smallestWidthDp = LocalConfiguration.current.smallestScreenWidthDp
    val scaledDensity = remember(baseDensity, smallestWidthDp) {
        val uiScale = (smallestWidthDp / REFERENCE_SMALLEST_WIDTH_DP).coerceIn(0.8f, 1f)
        Density(baseDensity.density * uiScale, baseDensity.fontScale)
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
                // No system scrim behind 3-button navigation; the app's own bottom fade blends it instead.
                window.isNavigationBarContrastEnforced = false
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = light
                insetsController.isAppearanceLightNavigationBars = light
            }
        }
    }

    CompositionLocalProvider(LocalAppTheme provides theme, LocalDensity provides scaledDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography
        ) {
            // Every clickable without an explicit indication gets the One UI press feedback.
            CompositionLocalProvider(
                LocalIndication provides com.gallery.app.ui.design.rememberThemedIndication(),
                content = content
            )
        }
    }
}
