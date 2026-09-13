package com.example.amoledgallery.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

/** Shape/size/motion character of a design language, independent of color. */
data class AppShapeTokens(
    val cardCornerRadius: Dp,
    val dialogCornerRadius: Dp,
    val pillCornerRadius: Dp,
    val iconButtonSize: Dp,
    val useBlur: Boolean,
    val motionStyle: MotionStyle
)

fun shapeTokensFor(language: DesignLanguage): AppShapeTokens = when (language) {
    DesignLanguage.SAMSUNG_ONEUI -> AppShapeTokens(
        cardCornerRadius = 20.dp,
        dialogCornerRadius = 28.dp,
        pillCornerRadius = 24.dp,
        iconButtonSize = 44.dp,
        useBlur = true,
        motionStyle = MotionStyle.STANDARD
    )
    DesignLanguage.GOOGLE_EXPRESSIVE -> AppShapeTokens(
        cardCornerRadius = 28.dp,
        dialogCornerRadius = 32.dp,
        pillCornerRadius = 28.dp,
        iconButtonSize = 48.dp,
        useBlur = false,
        motionStyle = MotionStyle.SPRING
    )
    DesignLanguage.IOS_LIQUID_GLASS -> AppShapeTokens(
        cardCornerRadius = 24.dp,
        dialogCornerRadius = 28.dp,
        pillCornerRadius = 30.dp,
        iconButtonSize = 44.dp,
        useBlur = true,
        motionStyle = MotionStyle.GLASS_EASE
    )
    DesignLanguage.XIAOMI_HYPEROS -> AppShapeTokens(
        cardCornerRadius = 12.dp,
        dialogCornerRadius = 16.dp,
        pillCornerRadius = 14.dp,
        iconButtonSize = 40.dp,
        useBlur = false,
        motionStyle = MotionStyle.FAST_CUT
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
    DesignLanguage.GOOGLE_EXPRESSIVE -> listOf(
        AppColorPalette(
            id = "expressive_blue", label = "Material Blau",
            background = Color(0xFF000000), onBackground = Color(0xFFE3E2E6),
            textMuted = Color(0xFF9AA0A6), surface = Color(0xFF131316),
            surfaceVariant = Color(0xFF1F2024), accent = Color(0xFF8AB4F8),
            cardBorder = Color(0xFF2E3036), overlayScrim = Color(0x99000000)
        ),
        AppColorPalette(
            id = "expressive_green", label = "Material Grün",
            background = Color(0xFF000000), onBackground = Color(0xFFE3E2E6),
            textMuted = Color(0xFF9AA0A6), surface = Color(0xFF131316),
            surfaceVariant = Color(0xFF1F2024), accent = Color(0xFF81C995),
            cardBorder = Color(0xFF2E3036), overlayScrim = Color(0x99000000)
        ),
        AppColorPalette(
            id = "expressive_peach", label = "Material Pfirsich",
            background = Color(0xFF000000), onBackground = Color(0xFFE3E2E6),
            textMuted = Color(0xFF9AA0A6), surface = Color(0xFF131316),
            surfaceVariant = Color(0xFF1F2024), accent = Color(0xFFFFB4AB),
            cardBorder = Color(0xFF2E3036), overlayScrim = Color(0x99000000)
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
