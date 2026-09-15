package com.example.amoledgallery.ui.theme

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

private const val THEME_PREFS_NAME = "amoled_gallery_theme"
private const val KEY_DESIGN_LANGUAGE = "design_language"
private const val KEY_PALETTE_ID = "palette_id"
private const val KEY_CUSTOM_BG = "custom_background"
private const val KEY_CUSTOM_ON_BG = "custom_onBackground"
private const val KEY_CUSTOM_TEXT_MUTED = "custom_textMuted"
private const val KEY_CUSTOM_SURFACE = "custom_surface"
private const val KEY_CUSTOM_SURFACE_VARIANT = "custom_surfaceVariant"
private const val KEY_CUSTOM_ACCENT = "custom_accent"
private const val KEY_CUSTOM_CARD_BORDER = "custom_cardBorder"
private const val KEY_CUSTOM_OVERLAY_SCRIM = "custom_overlayScrim"

/** Full theme bundle handed out via [LocalAppTheme] for structural (non-color) styling decisions. */
data class AppTheme(
    val language: DesignLanguage,
    val palette: AppColorPalette,
    val shapes: AppShapeTokens
)

val LocalAppTheme = compositionLocalOf {
    AppTheme(DesignLanguage.SAMSUNG_ONEUI, presetsFor(DesignLanguage.SAMSUNG_ONEUI).first(), shapeTokensFor(DesignLanguage.SAMSUNG_ONEUI))
}

/**
 * Live-swappable app-wide color/shape state. Colors are exposed as plain (non-@Composable)
 * State-backed top-level properties in [Color.kt] under their original names (AmoledBlack,
 * PureWhite, ...) so every existing call site across the app keeps compiling unchanged while
 * still recomposing automatically whenever the user switches design language or palette.
 */
object ThemeState {
    private val defaultPalette = presetsFor(DesignLanguage.SAMSUNG_ONEUI).first()

    var background by mutableStateOf(defaultPalette.background)
        private set
    var onBackground by mutableStateOf(defaultPalette.onBackground)
        private set
    var textMuted by mutableStateOf(defaultPalette.textMuted)
        private set
    var surface by mutableStateOf(defaultPalette.surface)
        private set
    var surfaceVariant by mutableStateOf(defaultPalette.surfaceVariant)
        private set
    var accent by mutableStateOf(defaultPalette.accent)
        private set
    var cardBorder by mutableStateOf(defaultPalette.cardBorder)
        private set
    var overlayScrim by mutableStateOf(defaultPalette.overlayScrim)
        private set

    var designLanguage by mutableStateOf(DesignLanguage.SAMSUNG_ONEUI)
        private set
    var palette by mutableStateOf(defaultPalette)
        private set
    var shapes by mutableStateOf(shapeTokensFor(DesignLanguage.SAMSUNG_ONEUI))
        private set

    fun currentTheme(): AppTheme = AppTheme(designLanguage, palette, shapes)

    /** Reads the persisted design language/palette (or the OneUI default) at app start. */
    fun load(context: Context) {
        val prefs = context.getSharedPreferences(THEME_PREFS_NAME, Context.MODE_PRIVATE)
        val language = prefs.getString(KEY_DESIGN_LANGUAGE, null)
            ?.let { runCatching { DesignLanguage.valueOf(it) }.getOrNull() }
            ?: DesignLanguage.SAMSUNG_ONEUI
        val savedPaletteId = prefs.getString(KEY_PALETTE_ID, null)

        val resolved = if (savedPaletteId == CUSTOM_PALETTE_ID) {
            val fallback = presetsFor(language).first()
            AppColorPalette(
                id = CUSTOM_PALETTE_ID,
                label = "Eigene Palette",
                background = Color(prefs.getInt(KEY_CUSTOM_BG, fallback.background.toArgb())),
                onBackground = Color(prefs.getInt(KEY_CUSTOM_ON_BG, fallback.onBackground.toArgb())),
                textMuted = Color(prefs.getInt(KEY_CUSTOM_TEXT_MUTED, fallback.textMuted.toArgb())),
                surface = Color(prefs.getInt(KEY_CUSTOM_SURFACE, fallback.surface.toArgb())),
                surfaceVariant = Color(prefs.getInt(KEY_CUSTOM_SURFACE_VARIANT, fallback.surfaceVariant.toArgb())),
                accent = Color(prefs.getInt(KEY_CUSTOM_ACCENT, fallback.accent.toArgb())),
                cardBorder = Color(prefs.getInt(KEY_CUSTOM_CARD_BORDER, fallback.cardBorder.toArgb())),
                overlayScrim = Color(prefs.getInt(KEY_CUSTOM_OVERLAY_SCRIM, fallback.overlayScrim.toArgb()))
            )
        } else {
            presetsFor(language).find { it.id == savedPaletteId } ?: presetsFor(language).first()
        }
        apply(language, resolved, persist = false, context = context)
    }

    /** Switches the live theme immediately and (by default) persists the choice. */
    fun apply(language: DesignLanguage, newPalette: AppColorPalette, persist: Boolean = true, context: Context? = null) {
        // The user's pick is persisted as chosen; on screen the accent is kept readable.
        val live = newPalette.withReadableAccent()
        designLanguage = language
        palette = live
        shapes = shapeTokensFor(language)
        background = live.background
        onBackground = live.onBackground
        textMuted = live.textMuted
        surface = live.surface
        surfaceVariant = live.surfaceVariant
        accent = live.accent
        cardBorder = live.cardBorder
        overlayScrim = live.overlayScrim

        if (persist && context != null) {
            persistToPrefs(context, language, newPalette)
        }
    }

    private fun persistToPrefs(context: Context, language: DesignLanguage, palette: AppColorPalette) {
        context.getSharedPreferences(THEME_PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString(KEY_DESIGN_LANGUAGE, language.name)
            putString(KEY_PALETTE_ID, palette.id)
            if (palette.id == CUSTOM_PALETTE_ID) {
                putInt(KEY_CUSTOM_BG, palette.background.toArgb())
                putInt(KEY_CUSTOM_ON_BG, palette.onBackground.toArgb())
                putInt(KEY_CUSTOM_TEXT_MUTED, palette.textMuted.toArgb())
                putInt(KEY_CUSTOM_SURFACE, palette.surface.toArgb())
                putInt(KEY_CUSTOM_SURFACE_VARIANT, palette.surfaceVariant.toArgb())
                putInt(KEY_CUSTOM_ACCENT, palette.accent.toArgb())
                putInt(KEY_CUSTOM_CARD_BORDER, palette.cardBorder.toArgb())
                putInt(KEY_CUSTOM_OVERLAY_SCRIM, palette.overlayScrim.toArgb())
            }
        }.apply()
    }
}
