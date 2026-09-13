package com.example.amoledgallery.ui

import android.graphics.Shader
import android.os.Build
import android.view.ViewParent
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import com.example.amoledgallery.AmoledGalleryApp
import com.example.amoledgallery.PerformanceProfile
import com.example.amoledgallery.ui.theme.ThemeState

/**
 * Applies a real background blur (One UI 8/9 pop-out menus, iOS Liquid Glass panels) when
 * the active design language wants blur ([com.example.amoledgallery.ui.theme.AppShapeTokens.useBlur])
 * AND the device is on HIGH/ULTRA graphics quality AND the platform supports RenderEffect
 * (API 31+). Otherwise a no-op, so LOW/MEDIUM and older devices stay flat and fast.
 *
 * IMPORTANT: [RenderEffect] blurs everything drawn INSIDE the layer it's attached to - it does
 * NOT sample content behind/below that layer. Only apply this to a decorative, non-interactive,
 * text-free layer (e.g. a standalone background photo). Applying it to a container that also
 * hosts readable text/icons (a card, a nav bar) blurs that text into illegibility - use
 * [WindowBackgroundBlur] for "frost what's behind this dialog/sheet" instead.
 */
fun Modifier.themedBackgroundBlur(radiusDp: Float = 24f): Modifier {
    val quality = AmoledGalleryApp.performanceProfile
    val wantsBlur = ThemeState.shapes.useBlur &&
        (quality == PerformanceProfile.HIGH || quality == PerformanceProfile.ULTRA)
    if (!wantsBlur || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return this

    return this.graphicsLayer {
        renderEffect = android.graphics.RenderEffect
            .createBlurEffect(radiusDp, radiusDp, Shader.TileMode.DECAL)
            .asComposeRenderEffect()
        clip = true
    }
}

private tailrec fun ViewParent?.findDialogWindowProvider(): DialogWindowProvider? = when (this) {
    null -> null
    is DialogWindowProvider -> this
    else -> this.parent.findDialogWindowProvider()
}

/**
 * Call from inside a Dialog/AlertDialog/ModalBottomSheet's content to blur whatever is behind
 * that floating window (the real screen content, e.g. the media grid) - a genuine "frosted
 * glass" look that never touches the dialog's own text, since the blur happens at the window
 * compositor level rather than inside the dialog's render tree. No-op below API 31, when the
 * active design language doesn't want blur, or outside HIGH/ULTRA graphics quality.
 */
@Composable
fun WindowBackgroundBlur(radiusDp: Int = 48) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val quality = AmoledGalleryApp.performanceProfile
    if (!ThemeState.shapes.useBlur || (quality != PerformanceProfile.HIGH && quality != PerformanceProfile.ULTRA)) return

    val view = LocalView.current
    val density = LocalDensity.current
    LaunchedEffect(view, radiusDp) {
        val window = view.parent.findDialogWindowProvider()?.window ?: return@LaunchedEffect
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        window.attributes = window.attributes.apply {
            blurBehindRadius = with(density) { radiusDp.dp.roundToPx() }
        }
    }
}
