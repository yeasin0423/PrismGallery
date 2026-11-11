package com.gallery.app.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.app.PerformanceProfile

/**
 * The design language the app is built around. Prism Gallery only ships Samsung One UI; the enum is
 * kept as a single value so the shared components can keep asking the theme which language is active.
 * The graphics quality tier (Low-Ultra) separately controls how much of the blur and animation budget
 * is actually spent on a given device.
 */
enum class DesignLanguage(val displayName: String) {
    SAMSUNG_ONEUI("Samsung One UI")
}

/** Shape/size/motion character of the active design language, independent of color. */
data class AppShapeTokens(
    val cardCornerRadius: Dp,
    val dialogCornerRadius: Dp,
    val pillCornerRadius: Dp,
    val iconButtonSize: Dp,
    val useBlur: Boolean,
    /** Gap between grid tiles. */
    val gridSpacing: Dp,
    /** Corner rounding of a single grid tile. */
    val gridItemCornerRadius: Dp,
    /** Inset of the whole media grid from the screen edges. */
    val gridHorizontalPadding: Dp,
    /** Large-title size on the main headers. */
    val headerTitleSize: TextUnit,
    /**
     * Height the bottom bar occupies above the system navigation inset, including its floating
     * margin. Content scrolls underneath the bar and reserves this much bottom padding.
     */
    val bottomBarHeight: Dp
)

fun shapeTokensFor(language: DesignLanguage): AppShapeTokens = when (language) {
    DesignLanguage.SAMSUNG_ONEUI -> AppShapeTokens(
        cardCornerRadius = 26.dp,
        dialogCornerRadius = 28.dp,
        pillCornerRadius = 30.dp,
        iconButtonSize = 48.dp,
        useBlur = true,
        gridSpacing = 1.5.dp,
        gridItemCornerRadius = 0.dp,
        gridHorizontalPadding = 6.dp,
        headerTitleSize = 40.sp,
        bottomBarHeight = 76.dp
    )
}

/**
 * Scales a base animation duration by graphics quality (see [com.gallery.app.ui.qualityTween]).
 */
fun <T> designTween(
    quality: PerformanceProfile,
    baseDurationMs: Int
): FiniteAnimationSpec<T> {
    val duration = when (quality) {
        PerformanceProfile.LOW -> 0
        PerformanceProfile.MEDIUM -> (baseDurationMs * 0.75f).toInt()
        PerformanceProfile.HIGH -> baseDurationMs
        PerformanceProfile.ULTRA -> (baseDurationMs * 1.25f).toInt()
    }
    return tween(durationMillis = duration.coerceAtLeast(0))
}

/** A named set of the 8 core colors every screen is built from. */
data class AppColorPalette(
    val id: String,
    val label: String,
    val background: Color,
    val onBackground: Color,
    val textMuted: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val accent: Color,
    val cardBorder: Color,
    val overlayScrim: Color
)

/** True for light palettes - drives status bar icons and glass tints. */
val AppColorPalette.isLight: Boolean get() = background.luminance() > 0.5f

/** Readable content color on top of a filled accent surface. */
val AppColorPalette.onAccent: Color get() = if (accent.luminance() > 0.45f) Color(0xFF0B0B0B) else Color.White

/** Frosted surface tint: [darkBase] on dark palettes, white on light ones. */
fun AppColorPalette.frost(darkBase: Color, alpha: Float): Color =
    if (isLight) Color.White.copy(alpha = alpha) else darkBase.copy(alpha = alpha)

private fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
}

/**
 * The palette with an accent that stays visible: accents are drawn as text and icons on the background
 * and on cards, so a custom accent too close to them (white on a light theme, black on AMOLED black) is
 * pulled towards [AppColorPalette.onBackground] until it reads again. Presets pass unchanged.
 */
fun AppColorPalette.withReadableAccent(): AppColorPalette {
    val minContrast = 2.4f
    fun readable(c: Color) = contrastRatio(c, background) >= minContrast && contrastRatio(c, surface) >= minContrast * 0.8f
    if (readable(accent)) return this
    var t = 0.1f
    while (t < 1f) {
        val candidate = androidx.compose.ui.graphics.lerp(accent, onBackground, t)
        if (readable(candidate)) return copy(accent = candidate)
        t += 0.1f
    }
    return copy(accent = onBackground)
}

/** Destructive action color tuned for the palette's brightness. */
val AppColorPalette.destructive: Color get() = if (isLight) Color(0xFFE5302E) else Color(0xFFFF6961)

const val CUSTOM_PALETTE_ID = "custom"

private fun AppColorPalette.variant(id: String, label: String, accent: Color) =
    copy(id = id, label = label, accent = accent)

// AMOLED black stays One UI's dark mode.
private val OneUiDark = AppColorPalette(
    id = "oneui_blue", label = "OneUI Blue",
    background = Color(0xFF000000), onBackground = Color(0xFFFAFAFA),
    textMuted = Color(0xFF9A9A9A), surface = Color(0xFF171717),
    surfaceVariant = Color(0xFF252525), accent = Color(0xFF3E91FF),
    cardBorder = Color(0xFF2A2A2A), overlayScrim = Color(0x99000000)
)
private val OneUiLight = AppColorPalette(
    id = "oneui_light_blue", label = "OneUI Light Blue",
    background = Color(0xFFF6F6F6), onBackground = Color(0xFF111111),
    textMuted = Color(0xFF7A7A7A), surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEAEAEA), accent = Color(0xFF0A7CFF),
    cardBorder = Color(0xFFE0E0E0), overlayScrim = Color(0x66000000)
)

/** All palettes: three accents in dark mode and three in light mode; the first is the default. */
fun presetsFor(language: DesignLanguage): List<AppColorPalette> = when (language) {
    DesignLanguage.SAMSUNG_ONEUI -> listOf(
        OneUiDark,
        OneUiDark.variant("oneui_lavender", "OneUI Lavender", Color(0xFFA99BFF)),
        OneUiDark.variant("oneui_green", "OneUI Green", Color(0xFF3DD68C)),
        OneUiLight,
        OneUiLight.variant("oneui_light_lavender", "OneUI Light Lavender", Color(0xFF6F5FD6)),
        OneUiLight.variant("oneui_light_green", "OneUI Light Green", Color(0xFF1E9E5A))
    )
}

/** The palettes of [language] in one brightness mode (light or dark). */
fun presetsFor(language: DesignLanguage, light: Boolean): List<AppColorPalette> =
    presetsFor(language).filter { it.isLight == light }
