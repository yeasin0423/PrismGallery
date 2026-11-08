package com.gallery.app.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.app.PerformanceProfile

/**
 * The design-language code paths that used to be selectable. Only Samsung One UI is exposed now;
 * the remaining values keep the shared per-design components compiling but are forced back to
 * [SAMSUNG_ONEUI] by [ThemeState]. The graphics quality tier (Low-Ultra) separately controls how
 * much of the blur and animation budget is actually spent on a given device.
 */
enum class DesignLanguage(val displayName: String) {
    SAMSUNG_ONEUI("Samsung One UI"),
    GOOGLE_EXPRESSIVE("Google Gemini"),
    IOS_LIQUID_GLASS("iOS Liquid Glass"),
    XIAOMI_HYPEROS("Xiaomi HyperOS")
}

/** How a design language wants its transitions to feel, before quality-tier scaling. */
enum class MotionStyle { STANDARD, SPRING, GLASS_EASE, FAST_CUT }

/** Shape/size/motion character of a design language, independent of color. */
data class AppShapeTokens(
    val cardCornerRadius: Dp,
    val dialogCornerRadius: Dp,
    val pillCornerRadius: Dp,
    val iconButtonSize: Dp,
    val useBlur: Boolean,
    val motionStyle: MotionStyle,
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
        motionStyle = MotionStyle.STANDARD,
        gridSpacing = 1.5.dp,
        gridItemCornerRadius = 0.dp,
        gridHorizontalPadding = 6.dp,
        headerTitleSize = 40.sp,
        bottomBarHeight = 76.dp
    )
    // Google Photos with Gemini's look: photos edge to edge with thin gaps, a floating pill bar, big soft cards.
    DesignLanguage.GOOGLE_EXPRESSIVE -> AppShapeTokens(
        cardCornerRadius = 28.dp,
        dialogCornerRadius = 32.dp,
        pillCornerRadius = 28.dp,
        iconButtonSize = 44.dp,
        useBlur = true,
        motionStyle = MotionStyle.SPRING,
        gridSpacing = 2.dp,
        gridItemCornerRadius = 0.dp,
        gridHorizontalPadding = 0.dp,
        headerTitleSize = 30.sp,
        bottomBarHeight = 92.dp
    )
    DesignLanguage.IOS_LIQUID_GLASS -> AppShapeTokens(
        cardCornerRadius = 26.dp,
        dialogCornerRadius = 34.dp,
        pillCornerRadius = 30.dp,
        iconButtonSize = 44.dp,
        useBlur = true,
        motionStyle = MotionStyle.GLASS_EASE,
        gridSpacing = 1.5.dp,
        gridItemCornerRadius = 0.dp,
        gridHorizontalPadding = 0.dp,
        headerTitleSize = 34.sp,
        bottomBarHeight = 86.dp
    )
    // HyperOS Gallery: square photos edge to edge with hairline gaps, a big thin title, a full-width tab bar.
    DesignLanguage.XIAOMI_HYPEROS -> AppShapeTokens(
        cardCornerRadius = 24.dp,
        dialogCornerRadius = 32.dp,
        pillCornerRadius = 24.dp,
        iconButtonSize = 40.dp,
        useBlur = true,
        motionStyle = MotionStyle.FAST_CUT,
        gridSpacing = 1.5.dp,
        gridItemCornerRadius = 0.dp,
        gridHorizontalPadding = 0.dp,
        headerTitleSize = 40.sp,
        bottomBarHeight = 66.dp
    )
}

/**
 * Scales a base animation duration by graphics quality (see [com.gallery.app.ui.qualityTween])
 * AND shapes the curve itself by the active design language's [MotionStyle].
 */
fun <T> designTween(
    language: DesignLanguage,
    quality: PerformanceProfile,
    baseDurationMs: Int
): FiniteAnimationSpec<T> {
    val duration = when (quality) {
        PerformanceProfile.LOW -> 0
        PerformanceProfile.MEDIUM -> (baseDurationMs * 0.75f).toInt()
        PerformanceProfile.HIGH -> baseDurationMs
        PerformanceProfile.ULTRA -> (baseDurationMs * 1.25f).toInt()
    }
    if (duration <= 0) return tween(durationMillis = 0)
    return when (shapeTokensFor(language).motionStyle) {
        MotionStyle.SPRING -> spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
        MotionStyle.GLASS_EASE -> spring(dampingRatio = 0.82f, stiffness = 380f)
        MotionStyle.FAST_CUT -> tween(durationMillis = (duration * 0.6f).toInt().coerceAtLeast(1))
        MotionStyle.STANDARD -> tween(durationMillis = duration)
    }
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

/** True for light palettes (HyperOS default, iOS light) - drives status bar icons and glass tints. */
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

// Shared neutral bases; each language adds its accents on top. AMOLED black stays One UI's dark mode.
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
private val GeminiDark = AppColorPalette(
    id = "expressive_mint", label = "Gemini Blue",
    background = Color(0xFF000000), onBackground = Color(0xFFE3E3E3),
    textMuted = Color(0xFF9AA0A6), surface = Color(0xFF1E1F20),
    surfaceVariant = Color(0xFF2A2B2E), accent = Color(0xFF3D7BF2),
    cardBorder = Color(0xFF3C4043), overlayScrim = Color(0x99000000)
)
private val GeminiLight = AppColorPalette(
    id = "expressive_light_mint", label = "Gemini Light Blue",
    background = Color(0xFFF8F8F8), onBackground = Color(0xFF1F1F1F),
    textMuted = Color(0xFF5F6368), surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDEFF2), accent = Color(0xFF0B57D0),
    cardBorder = Color(0xFFDADCE0), overlayScrim = Color(0x66000000)
)
private val IosDark = AppColorPalette(
    id = "glass_blue", label = "Glass Blue",
    background = Color(0xFF0A0A0C), onBackground = Color(0xFFF5F5F7),
    textMuted = Color(0xFF8E8E93), surface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFF2C2C2E), accent = Color(0xFF0A84FF),
    cardBorder = Color(0xFF38383A), overlayScrim = Color(0x66000000)
)
private val IosLight = AppColorPalette(
    id = "glass_light", label = "Glass Light Blue",
    background = Color(0xFFF2F2F7), onBackground = Color(0xFF000000),
    textMuted = Color(0xFF6C6C70), surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE5E5EA), accent = Color(0xFF007AFF),
    cardBorder = Color(0xFFD1D1D6), overlayScrim = Color(0x33000000)
)
private val HyperOsLight = AppColorPalette(
    id = "hyperos_light", label = "HyperOS Light Blue",
    background = Color(0xFFF7F7F7), onBackground = Color(0xFF1A1A1A),
    textMuted = Color(0xFF8A8A8E), surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEFEFEF), accent = Color(0xFF3482FF),
    cardBorder = Color(0xFFE8E8E8), overlayScrim = Color(0x66000000)
)
private val HyperOsDark = AppColorPalette(
    id = "hyperos_dark", label = "HyperOS Dark Blue",
    background = Color(0xFF000000), onBackground = Color(0xFFF2F2F2),
    textMuted = Color(0xFF8C8C8C), surface = Color(0xFF1E1E1F),
    surfaceVariant = Color(0xFF262628), accent = Color(0xFF3482FF),
    cardBorder = Color(0xFF2C2C2C), overlayScrim = Color(0x9E000000)
)

/**
 * All palettes of a language: three accents in dark mode and three in light mode. The first entry is
 * the language's default (HyperOS defaults to light, the others to dark).
 */
fun presetsFor(language: DesignLanguage): List<AppColorPalette> = when (language) {
    DesignLanguage.SAMSUNG_ONEUI -> listOf(
        OneUiDark,
        OneUiDark.variant("oneui_lavender", "OneUI Lavender", Color(0xFFA99BFF)),
        OneUiDark.variant("oneui_green", "OneUI Green", Color(0xFF3DD68C)),
        OneUiLight,
        OneUiLight.variant("oneui_light_lavender", "OneUI Light Lavender", Color(0xFF6F5FD6)),
        OneUiLight.variant("oneui_light_green", "OneUI Light Green", Color(0xFF1E9E5A))
    )
    // Gemini: neutral black (or white) surfaces; the accent only colours icons, selections and buttons.
    // The ids stay from the previous Material palettes so a saved choice keeps its slot.
    DesignLanguage.GOOGLE_EXPRESSIVE -> listOf(
        GeminiDark,
        GeminiDark.variant("expressive_violet", "Gemini Violet", Color(0xFF9B5CF0)),
        GeminiDark.variant("expressive_amber", "Gemini Green", Color(0xFF2E9E54)),
        GeminiLight,
        GeminiLight.variant("expressive_light_violet", "Gemini Light Violet", Color(0xFF8430CE)),
        GeminiLight.variant("expressive_light_amber", "Gemini Light Green", Color(0xFF188038))
    )
    DesignLanguage.IOS_LIQUID_GLASS -> listOf(
        IosDark,
        IosDark.variant("glass_purple", "Glass Purple", Color(0xFFBF5AF2)),
        IosDark.variant("glass_mint", "Glass Mint", Color(0xFF63E6E2)),
        IosLight,
        IosLight.variant("glass_light_purple", "Glass Light Purple", Color(0xFFAF52DE)),
        IosLight.variant("glass_light_mint", "Glass Light Mint", Color(0xFF0FA5B5))
    )
    DesignLanguage.XIAOMI_HYPEROS -> listOf(
        HyperOsLight,
        HyperOsLight.variant("hyperos_orange", "HyperOS Light Orange", Color(0xFFFF6A00)),
        HyperOsLight.variant("hyperos_green", "HyperOS Light Green", Color(0xFF1DB36B)),
        HyperOsDark,
        HyperOsDark.variant("hyperos_dark_orange", "HyperOS Dark Orange", Color(0xFFFF7A1A)),
        HyperOsDark.variant("hyperos_dark_green", "HyperOS Dark Green", Color(0xFF2BC77A))
    )
}

/** The palettes of [language] in one brightness mode (light or dark). */
fun presetsFor(language: DesignLanguage, light: Boolean): List<AppColorPalette> =
    presetsFor(language).filter { it.isLight == light }
