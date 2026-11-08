package com.gallery.app.ui

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import com.gallery.app.PerformanceProfile

/**
 * Scales a base animation duration by the manually selected graphics quality tier:
 * LOW skips the animation entirely (snap, 0ms) so only the essential state change happens,
 * higher tiers get progressively richer/longer transitions - ULTRA is the most "complex" feel.
 */
fun <T> qualityTween(quality: PerformanceProfile, baseDurationMs: Int): FiniteAnimationSpec<T> {
    val duration = when (quality) {
        PerformanceProfile.LOW -> 0
        PerformanceProfile.MEDIUM -> (baseDurationMs * 0.75f).toInt()
        PerformanceProfile.HIGH -> baseDurationMs
        PerformanceProfile.ULTRA -> (baseDurationMs * 1.25f).toInt()
    }
    return tween(durationMillis = duration)
}
