package com.example.amoledgallery.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * These keep their original names for source-compatibility with every existing call site,
 * but now read live from [ThemeState] instead of being fixed constants - switching design
 * language/palette in Settings updates every screen that references them immediately.
 */
val AmoledBlack: Color get() = ThemeState.background
val PureWhite: Color get() = ThemeState.onBackground
val TextMuted: Color get() = ThemeState.textMuted
val SurfaceDark: Color get() = ThemeState.surface
val SurfaceVariantDark: Color get() = ThemeState.surfaceVariant
val OneUiBlue: Color get() = ThemeState.accent
val OneUiCardBorder: Color get() = ThemeState.cardBorder
val OverlayScrim: Color get() = ThemeState.overlayScrim
