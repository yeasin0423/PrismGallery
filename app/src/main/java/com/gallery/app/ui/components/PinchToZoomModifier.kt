package com.gallery.app.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Modifier detecting two-finger pinch-to-zoom gestures on a LazyVerticalGrid.
 *
 * Single-finger touches are NOT consumed, ensuring buttery-smooth vertical scrolling.
 * Two-finger pinch gestures accumulate scale:
 * - Spreading fingers (pinch-out) -> triggers [onZoomIn] (fewer, larger columns)
 * - Pinching fingers (pinch-in) -> triggers [onZoomOut] (more, compact columns)
 *
 * The detector keeps running across recompositions, so it always calls the latest callbacks (a grid that
 * switches between the album list and an album's photos must zoom whichever is shown right now).
 */
@Composable
fun Modifier.detectPinchToZoomGrid(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
): Modifier {
    val latestZoomIn = rememberUpdatedState(onZoomIn)
    val latestZoomOut = rememberUpdatedState(onZoomOut)
    return this.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var accumulatedScale = 1f

            do {
                val event = awaitPointerEvent()
                val pressedPointers = event.changes.filter { it.pressed }

                if (pressedPointers.size >= 2) {
                    val p0 = pressedPointers[0].position
                    val p1 = pressedPointers[1].position
                    val prevP0 = pressedPointers[0].previousPosition
                    val prevP1 = pressedPointers[1].previousPosition

                    val currentDistance = (p0 - p1).getDistance()
                    val prevDistance = (prevP0 - prevP1).getDistance()

                    if (prevDistance > 0f) {
                        val zoomRatio = currentDistance / prevDistance
                        accumulatedScale *= zoomRatio

                        // Sensitivity thresholds
                        if (accumulatedScale > 1.30f) {
                            latestZoomIn.value()
                            accumulatedScale = 1f
                        } else if (accumulatedScale < 0.70f) {
                            latestZoomOut.value()
                            accumulatedScale = 1f
                        }
                    }

                    // Consume pointers during 2-finger pinch so grid doesn't scroll jittery
                    pressedPointers.forEach { it.consume() }
                } else if (pressedPointers.size < 2) {
                    accumulatedScale = 1f
                }
            } while (event.changes.any { it.pressed })
        }
    }
}
