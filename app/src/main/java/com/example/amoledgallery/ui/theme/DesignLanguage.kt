package com.example.amoledgallery.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amoledgallery.PerformanceProfile

/**
 * The 4 selectable "look & feel" languages. Each keeps the app's core AMOLED-black
 * foundation but expresses it with a different color/shape/motion character; the graphics
 * quality tier (Low-Ultra) separately controls how much of that character's animation/blur
 * budget is actually spent on a given device.
 */
enum class DesignLanguage(val displayName: String, val shortDescription: String) {
    SAMSUNG_ONEUI(
        "Samsung One UI",
        "Große abgerundete Karten, kräftige Typografie, One UI 8/9 Stil"
    ),
    GOOGLE_EXPRESSIVE(
        "Google Material Expressive",
        "Sehr weiche große Formen, federnde Animationen, lebendige Akzente"
    ),
    IOS_LIQUID_GLASS(
        "iOS Liquid Glass",
        "Durchscheinendes Glas, weicher Blur, klare helle Typografie"
    ),
    XIAOMI_HYPEROS(
        "Xiaomi HyperOS",
        "Scharfkantige Karten, ein kräftiger Akzent, schnelle direkte Übergänge"
    )
}

/** How a design language wants its transitions to feel, before quality-tier scaling. */
enum class MotionStyle { STANDARD, SPRING, GLASS_EASE, FAST_CUT }

/**
 * Structural layout of the bottom navigation, which differs far more between these OSes than
 * colour does - it is the single most recognisable element on screen.
 */
enum class NavStyle {
    /** One UI: edge-to-edge bar, neutral capsule with an animated label behind the active tab. */
    ONEUI_BAR,
    /** Material 3 Expressive: edge-to-edge bar, tonal accent capsule and a springy icon bounce. */
    EXPRESSIVE_BAR,
    /** iOS Photos: a narrow floating glass pill for the content tabs plus a detached round button. */
    GLASS_SPLIT_PILL,
    /** HyperOS: a small, compact, icon-only floating pill centred above the gesture bar. */
    HYPEROS_COMPACT_PILL
}

/** How the selection checkbox on a grid tile is drawn. */
enum class SelectionStyle {
    /** One UI: large white ring, filled white with a dark check, plus a light scrim on the tile. */
    ONEUI_LARGE_RING,
    /** Material: accent-filled circle with a white check. */
    ACCENT_FILLED,
    /** iOS: smaller blue filled circle with a white check, bottom-right of the tile. */
    IOS_BLUE_BADGE
}

/** Shape/size/motion character of a design language, independent of color. */
data class AppShapeTokens(
    val cardCornerRadius: Dp,
    val dialogCornerRadius: Dp,
    val pillCornerRadius: Dp,
    val iconButtonSize: Dp,
    val useBlur: Boolean,
    val motionStyle: MotionStyle,
    /** Gap between grid tiles. One UI and iOS Photos both run a completely flush grid. */
    val gridSpacing: Dp,
    /** Corner rounding of a single grid tile. */
    val gridItemCornerRadius: Dp,
    val navStyle: NavStyle,
    val selectionStyle: SelectionStyle,
    /** Large-title size on the main header. */
    val headerTitleSize: TextUnit
)

fun shapeTokensFor(language: DesignLanguage): AppShapeTokens = when (language) {
    DesignLanguage.SAMSUNG_ONEUI -> AppShapeTokens(
        cardCornerRadius = 20.dp,
        dialogCornerRadius = 28.dp,
        pillCornerRadius = 24.dp,
        iconButtonSize = 44.dp,
        useBlur = true,
        motionStyle = MotionStyle.STANDARD,
        gridSpacing = 0.dp,
        gridItemCornerRadius = 0.dp,
        navStyle = NavStyle.ONEUI_BAR,
        selectionStyle = SelectionStyle.ONEUI_LARGE_RING,
        headerTitleSize = 34.sp
    )
    DesignLanguage.GOOGLE_EXPRESSIVE -> AppShapeTokens(
        cardCornerRadius = 28.dp,
        dialogCornerRadius = 32.dp,
        pillCornerRadius = 28.dp,
        iconButtonSize = 48.dp,
        useBlur = false,
        motionStyle = MotionStyle.SPRING,
        gridSpacing = 6.dp,
        gridItemCornerRadius = 18.dp,
        navStyle = NavStyle.EXPRESSIVE_BAR,
        selectionStyle = SelectionStyle.ACCENT_FILLED,
        headerTitleSize = 36.sp
    )
    DesignLanguage.IOS_LIQUID_GLASS -> AppShapeTokens(
        cardCornerRadius = 24.dp,
        dialogCornerRadius = 28.dp,
        pillCornerRadius = 30.dp,
        iconButtonSize = 44.dp,
        useBlur = true,
        motionStyle = MotionStyle.GLASS_EASE,
        gridSpacing = 0.dp,
        gridItemCornerRadius = 0.dp,
        navStyle = NavStyle.GLASS_SPLIT_PILL,
        selectionStyle = SelectionStyle.IOS_BLUE_BADGE,
        headerTitleSize = 34.sp
    )
    DesignLanguage.XIAOMI_HYPEROS -> AppShapeTokens(
        cardCornerRadius = 12.dp,
        dialogCornerRadius = 16.dp,
        pillCornerRadius = 14.dp,
        iconButtonSize = 40.dp,
        useBlur = false,
        motionStyle = MotionStyle.FAST_CUT,
        gridSpacing = 2.dp,
        gridItemCornerRadius = 8.dp,
        navStyle = NavStyle.HYPEROS_COMPACT_PILL,
        selectionStyle = SelectionStyle.ACCENT_FILLED,
        headerTitleSize = 26.sp
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
        MotionStyle.FAST_CUT -> tween(durationMillis = (duration * 0.6f).toInt().coerceAtLeast(1))
        MotionStyle.GLASS_EASE, MotionStyle.STANDARD -> tween(durationMillis = duration)
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

const val CUSTOM_PALETTE_ID = "custom"

fun presetsFor(language: DesignLanguage): List<AppColorPalette> = when (language) {
    DesignLanguage.SAMSUNG_ONEUI -> listOf(
        AppColorPalette(
            id = "oneui_blue", label = "OneUI Blau",
            background = Color(0xFF000000), onBackground = Color(0xFFFFFFFF),
            textMuted = Color(0xFF8E8E93), surface = Color(0xFF121212),
            surfaceVariant = Color(0xFF1C1C1E), accent = Color(0xFF0072DE),
            cardBorder = Color(0xFF2C2C2E), overlayScrim = Color(0x99000000)
        ),
        AppColorPalette(
            id = "oneui_lavender", label = "OneUI Lavendel",
            background = Color(0xFF000000), onBackground = Color(0xFFFFFFFF),
            textMuted = Color(0xFF8E8E93), surface = Color(0xFF121212),
            surfaceVariant = Color(0xFF1C1C1E), accent = Color(0xFF8B7FD6),
            cardBorder = Color(0xFF2C2C2E), overlayScrim = Color(0x99000000)
        ),
        AppColorPalette(
            id = "oneui_green", label = "OneUI Grün",
            background = Color(0xFF000000), onBackground = Color(0xFFFFFFFF),
            textMuted = Color(0xFF8E8E93), surface = Color(0xFF121212),
            surfaceVariant = Color(0xFF1C1C1E), accent = Color(0xFF34C77E),
            cardBorder = Color(0xFF2C2C2E), overlayScrim = Color(0x99000000)
        )
    )
    // Material 3 Expressive is tonal: surfaces carry the accent's hue rather than being neutral
    // black, which is what makes it read as "Google" at a glance.
    DesignLanguage.GOOGLE_EXPRESSIVE -> listOf(
        AppColorPalette(
            id = "expressive_mint", label = "Material Mint",
            background = Color(0xFF04110E), onBackground = Color(0xFFD6F5EC),
            textMuted = Color(0xFF89A8A0), surface = Color(0xFF0D1F1B),
            surfaceVariant = Color(0xFF16302A), accent = Color(0xFF5CE6C8),
            cardBorder = Color(0xFF22423B), overlayScrim = Color(0x99001410)
        ),
        AppColorPalette(
            id = "expressive_violet", label = "Material Violett",
            background = Color(0xFF0C0716), onBackground = Color(0xFFE8DEF8),
            textMuted = Color(0xFF9D93B0), surface = Color(0xFF16102A),
            surfaceVariant = Color(0xFF231A3D), accent = Color(0xFFB69DFF),
            cardBorder = Color(0xFF322A4D), overlayScrim = Color(0x99080415)
        ),
        AppColorPalette(
            id = "expressive_amber", label = "Material Bernstein",
            background = Color(0xFF140D04), onBackground = Color(0xFFF7E6CE),
            textMuted = Color(0xFFB09A7E), surface = Color(0xFF22180B),
            surfaceVariant = Color(0xFF342614), accent = Color(0xFFFFB95C),
            cardBorder = Color(0xFF493720), overlayScrim = Color(0x99120B03)
        )
    )
    DesignLanguage.IOS_LIQUID_GLASS -> listOf(
        AppColorPalette(
            id = "glass_blue", label = "Glass Blau",
            background = Color(0xFF000000), onBackground = Color(0xFFF5F5F7),
            textMuted = Color(0xFFA1A1A6), surface = Color(0xFF161618),
            surfaceVariant = Color(0xFF232326), accent = Color(0xFF0A84FF),
            cardBorder = Color(0xFF48484C), overlayScrim = Color(0x88000000)
        ),
        AppColorPalette(
            id = "glass_purple", label = "Glass Lila",
            background = Color(0xFF000000), onBackground = Color(0xFFF5F5F7),
            textMuted = Color(0xFFA1A1A6), surface = Color(0xFF161618),
            surfaceVariant = Color(0xFF232326), accent = Color(0xFFBF5AF2),
            cardBorder = Color(0xFF48484C), overlayScrim = Color(0x88000000)
        ),
        AppColorPalette(
            id = "glass_mint", label = "Glass Mint",
            background = Color(0xFF000000), onBackground = Color(0xFFF5F5F7),
            textMuted = Color(0xFFA1A1A6), surface = Color(0xFF161618),
            surfaceVariant = Color(0xFF232326), accent = Color(0xFF63E6E2),
            cardBorder = Color(0xFF48484C), overlayScrim = Color(0x88000000)
        )
    )
    DesignLanguage.XIAOMI_HYPEROS -> listOf(
        AppColorPalette(
            id = "hyperos_orange", label = "HyperOS Orange",
            background = Color(0xFF000000), onBackground = Color(0xFFFFFFFF),
            textMuted = Color(0xFF8A8A8E), surface = Color(0xFF0E0E0E),
            surfaceVariant = Color(0xFF191919), accent = Color(0xFFFF6B35),
            cardBorder = Color(0xFF262626), overlayScrim = Color(0x9E000000)
        ),
        AppColorPalette(
            id = "hyperos_yellow", label = "HyperOS Gelb",
            background = Color(0xFF000000), onBackground = Color(0xFFFFFFFF),
            textMuted = Color(0xFF8A8A8E), surface = Color(0xFF0E0E0E),
            surfaceVariant = Color(0xFF191919), accent = Color(0xFFFFD60A),
            cardBorder = Color(0xFF262626), overlayScrim = Color(0x9E000000)
        ),
        AppColorPalette(
            id = "hyperos_red", label = "HyperOS Rot",
            background = Color(0xFF000000), onBackground = Color(0xFFFFFFFF),
            textMuted = Color(0xFF8A8A8E), surface = Color(0xFF0E0E0E),
            surfaceVariant = Color(0xFF191919), accent = Color(0xFFFF3B30),
            cardBorder = Color(0xFF262626), overlayScrim = Color(0x9E000000)
        )
    )
}
