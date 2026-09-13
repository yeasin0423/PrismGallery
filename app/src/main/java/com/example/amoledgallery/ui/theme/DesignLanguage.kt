package com.example.amoledgallery.ui.theme

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
import com.example.amoledgallery.PerformanceProfile

/**
 * The 4 selectable "look & feel" languages. They share nothing but the gallery's function: each one
 * brings its own background (AMOLED black is One UI only), bar structure, dialogs, lists, progress
 * bars and motion. The graphics quality tier (Low-Ultra) separately controls how much of the blur
 * and animation budget is actually spent on a given device.
 */
enum class DesignLanguage(val displayName: String, val shortDescription: String) {
    SAMSUNG_ONEUI(
        "Samsung One UI",
        "AMOLED-Schwarz, riesiger zentrierter Titel und schwebende Milchglas-Leiste wie in der Samsung Galerie"
    ),
    GOOGLE_EXPRESSIVE(
        "Google Material 3 Expressive",
        "Tonale Farbflächen, große weiche Formen, federnde Animationen und wellenförmige Fortschrittsbalken"
    ),
    IOS_LIQUID_GLASS(
        "iOS Liquid Glass",
        "Klares Flüssigglas über randlosen Fotos, schwebende Glas-Bedienelemente und zentrierte Dialoge"
    ),
    XIAOMI_HYPEROS(
        "Xiaomi HyperOS",
        "Helle, luftige Karten, kompakte Milchglas-Leiste und Dialoge am unteren Bildschirmrand"
    )
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
    DesignLanguage.GOOGLE_EXPRESSIVE -> AppShapeTokens(
        cardCornerRadius = 28.dp,
        dialogCornerRadius = 28.dp,
        pillCornerRadius = 20.dp,
        iconButtonSize = 44.dp,
        useBlur = true,
        motionStyle = MotionStyle.SPRING,
        gridSpacing = 4.dp,
        gridItemCornerRadius = 12.dp,
        gridHorizontalPadding = 8.dp,
        headerTitleSize = 34.sp,
        bottomBarHeight = 90.dp
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
    DesignLanguage.XIAOMI_HYPEROS -> AppShapeTokens(
        cardCornerRadius = 20.dp,
        dialogCornerRadius = 28.dp,
        pillCornerRadius = 24.dp,
        iconButtonSize = 40.dp,
        useBlur = true,
        motionStyle = MotionStyle.FAST_CUT,
        gridSpacing = 3.dp,
        gridItemCornerRadius = 6.dp,
        gridHorizontalPadding = 12.dp,
        headerTitleSize = 32.sp,
        bottomBarHeight = 80.dp
    )
}

/**
 * Scales a base animation duration by graphics quality (see [com.example.amoledgallery.ui.qualityTween])
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

/** Destructive action color tuned for the palette's brightness. */
val AppColorPalette.destructive: Color get() = if (isLight) Color(0xFFE5302E) else Color(0xFFFF6961)

const val CUSTOM_PALETTE_ID = "custom"

private fun AppColorPalette.variant(id: String, label: String, accent: Color) =
    copy(id = id, label = label, accent = accent)

// Shared neutral bases; each language adds its accents on top. AMOLED black stays One UI's dark mode.
private val OneUiDark = AppColorPalette(
    id = "oneui_blue", label = "OneUI Blau",
    background = Color(0xFF000000), onBackground = Color(0xFFFAFAFA),
    textMuted = Color(0xFF9A9A9A), surface = Color(0xFF171717),
    surfaceVariant = Color(0xFF252525), accent = Color(0xFF3E91FF),
    cardBorder = Color(0xFF2A2A2A), overlayScrim = Color(0x99000000)
)
private val OneUiLight = AppColorPalette(
    id = "oneui_light_blue", label = "OneUI Hell Blau",
    background = Color(0xFFF6F6F6), onBackground = Color(0xFF111111),
    textMuted = Color(0xFF7A7A7A), surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEAEAEA), accent = Color(0xFF0A7CFF),
    cardBorder = Color(0xFFE0E0E0), overlayScrim = Color(0x66000000)
)
private val IosDark = AppColorPalette(
    id = "glass_blue", label = "Glass Blau",
    background = Color(0xFF0A0A0C), onBackground = Color(0xFFF5F5F7),
    textMuted = Color(0xFF8E8E93), surface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFF2C2C2E), accent = Color(0xFF0A84FF),
    cardBorder = Color(0xFF38383A), overlayScrim = Color(0x66000000)
)
private val IosLight = AppColorPalette(
    id = "glass_light", label = "Glass Hell Blau",
    background = Color(0xFFF2F2F7), onBackground = Color(0xFF000000),
    textMuted = Color(0xFF6C6C70), surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE5E5EA), accent = Color(0xFF007AFF),
    cardBorder = Color(0xFFD1D1D6), overlayScrim = Color(0x33000000)
)
private val HyperOsLight = AppColorPalette(
    id = "hyperos_light", label = "HyperOS Hell Blau",
    background = Color(0xFFF7F7F7), onBackground = Color(0xFF1A1A1A),
    textMuted = Color(0xFF8A8A8E), surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEFEFEF), accent = Color(0xFF3482FF),
    cardBorder = Color(0xFFE8E8E8), overlayScrim = Color(0x66000000)
)
private val HyperOsDark = AppColorPalette(
    id = "hyperos_dark", label = "HyperOS Dunkel Blau",
    background = Color(0xFF111111), onBackground = Color(0xFFF2F2F2),
    textMuted = Color(0xFF8A8A8E), surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2A2A2A), accent = Color(0xFF3D8BFF),
    cardBorder = Color(0xFF2C2C2C), overlayScrim = Color(0x9E000000)
)

/**
 * All palettes of a language: three accents in dark mode and three in light mode. The first entry is
 * the language's default (HyperOS defaults to light, the others to dark).
 */
fun presetsFor(language: DesignLanguage): List<AppColorPalette> = when (language) {
    DesignLanguage.SAMSUNG_ONEUI -> listOf(
        OneUiDark,
        OneUiDark.variant("oneui_lavender", "OneUI Lavendel", Color(0xFFA99BFF)),
        OneUiDark.variant("oneui_green", "OneUI Grün", Color(0xFF3DD68C)),
        OneUiLight,
        OneUiLight.variant("oneui_light_lavender", "OneUI Hell Lavendel", Color(0xFF6F5FD6)),
        OneUiLight.variant("oneui_light_green", "OneUI Hell Grün", Color(0xFF1E9E5A))
    )
    // Material 3 Expressive is tonal: every surface carries the accent's hue, so each accent has its
    // own full palette.
    DesignLanguage.GOOGLE_EXPRESSIVE -> listOf(
        AppColorPalette(
            id = "expressive_mint", label = "Material Petrol",
            background = Color(0xFF0B1B1C), onBackground = Color(0xFFDCF3F0),
            textMuted = Color(0xFF8FB0AC), surface = Color(0xFF132A2B),
            surfaceVariant = Color(0xFF1C3939), accent = Color(0xFF6FF0DA),
            cardBorder = Color(0xFF2A4A49), overlayScrim = Color(0x99041011)
        ),
        AppColorPalette(
            id = "expressive_violet", label = "Material Violett",
            background = Color(0xFF131020), onBackground = Color(0xFFEAE0FA),
            textMuted = Color(0xFFA097B4), surface = Color(0xFF1D182F),
            surfaceVariant = Color(0xFF2A2341), accent = Color(0xFFC9B4FF),
            cardBorder = Color(0xFF3A3052), overlayScrim = Color(0x99080415)
        ),
        AppColorPalette(
            id = "expressive_amber", label = "Material Bernstein",
            background = Color(0xFF191309), onBackground = Color(0xFFF6E7D0),
            textMuted = Color(0xFFB3A084), surface = Color(0xFF251C10),
            surfaceVariant = Color(0xFF352918), accent = Color(0xFFFFC66E),
            cardBorder = Color(0xFF4A3A22), overlayScrim = Color(0x99120B03)
        ),
        AppColorPalette(
            id = "expressive_light_mint", label = "Material Hell Petrol",
            background = Color(0xFFF1FAF7), onBackground = Color(0xFF0F1F1C),
            textMuted = Color(0xFF5C736E), surface = Color(0xFFE2F2EE),
            surfaceVariant = Color(0xFFD0E7E1), accent = Color(0xFF00796B),
            cardBorder = Color(0xFFBFD6D0), overlayScrim = Color(0x66001410)
        ),
        AppColorPalette(
            id = "expressive_light_violet", label = "Material Hell Violett",
            background = Color(0xFFF8F4FF), onBackground = Color(0xFF1D1829),
            textMuted = Color(0xFF6C6480), surface = Color(0xFFEEE7FB),
            surfaceVariant = Color(0xFFE0D6F5), accent = Color(0xFF6750A4),
            cardBorder = Color(0xFFD0C4E6), overlayScrim = Color(0x66080415)
        ),
        AppColorPalette(
            id = "expressive_light_amber", label = "Material Hell Bernstein",
            background = Color(0xFFFFF8F0), onBackground = Color(0xFF261A0A),
            textMuted = Color(0xFF7D6B54), surface = Color(0xFFF8EBDA),
            surfaceVariant = Color(0xFFEEDCC4), accent = Color(0xFF8C5000),
            cardBorder = Color(0xFFDECBB0), overlayScrim = Color(0x66120B03)
        )
    )
    DesignLanguage.IOS_LIQUID_GLASS -> listOf(
        IosDark,
        IosDark.variant("glass_purple", "Glass Lila", Color(0xFFBF5AF2)),
        IosDark.variant("glass_mint", "Glass Mint", Color(0xFF63E6E2)),
        IosLight,
        IosLight.variant("glass_light_purple", "Glass Hell Lila", Color(0xFFAF52DE)),
        IosLight.variant("glass_light_mint", "Glass Hell Mint", Color(0xFF0FA5B5))
    )
    DesignLanguage.XIAOMI_HYPEROS -> listOf(
        HyperOsLight,
        HyperOsLight.variant("hyperos_orange", "HyperOS Hell Orange", Color(0xFFFF6A00)),
        HyperOsLight.variant("hyperos_green", "HyperOS Hell Grün", Color(0xFF1DB36B)),
        HyperOsDark,
        HyperOsDark.variant("hyperos_dark_orange", "HyperOS Dunkel Orange", Color(0xFFFF7A1A)),
        HyperOsDark.variant("hyperos_dark_green", "HyperOS Dunkel Grün", Color(0xFF2BC77A))
    )
}

/** The palettes of [language] in one brightness mode (light or dark). */
fun presetsFor(language: DesignLanguage, light: Boolean): List<AppColorPalette> =
    presetsFor(language).filter { it.isLight == light }
