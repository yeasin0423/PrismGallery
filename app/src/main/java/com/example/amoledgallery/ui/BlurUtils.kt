package com.example.amoledgallery.ui

import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.example.amoledgallery.AmoledGalleryApp
import com.example.amoledgallery.PerformanceProfile
import com.example.amoledgallery.ui.theme.ThemeState

/**
 * Applies a real background blur (One UI 8/9 pop-out menus, iOS Liquid Glass panels) when
 * the active design language wants blur ([com.example.amoledgallery.ui.theme.AppShapeTokens.useBlur])
 * AND the device is on HIGH/ULTRA graphics quality AND the platform supports RenderEffect
 * (API 31+). Otherwise a no-op, so LOW/MEDIUM and older devices stay flat and fast.
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
