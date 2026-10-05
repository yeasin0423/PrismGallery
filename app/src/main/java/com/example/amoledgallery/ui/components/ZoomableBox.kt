package com.example.amoledgallery.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.max

suspend fun PointerInputScope.detectZoomAndPan(
    shouldConsumePan: () -> Boolean,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit
) {
    awaitEachGesture {
        var rotation = 0f
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val rotationChange = event.calculateRotation()
                val panChange = event.calculatePan()
                val centroid = event.calculateCentroid(useCurrent = false)

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    rotation += rotationChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val rotationMotion = abs(rotation * PI.toFloat() / 180f) * centroidSize
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop ||
                        rotationMotion > touchSlop ||
                        panMotion > touchSlop
                    ) {
                        pastTouchSlop = true
                    }
                }

                if (pastTouchSlop) {
                    val pointers = event.changes.size
                    if (pointers > 1 || shouldConsumePan()) {
                        onGesture(centroid, panChange, zoomChange, rotationChange)
                        event.changes.forEach {
                            if (it.positionChanged()) {
                                it.consume()
                            }
                        }
                    }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })
    }
}

/**
 * Container supporting double-tap zoom, pinch-to-zoom, panning, and single-tap callbacks.
 *
 * With [contentAspectRatio] (width / height of the fitted content) the zoom adapts to extreme shapes: a very tall
 * scroll screenshot can be zoomed until it fills the width and well beyond, a double tap fills the width, and
 * panning stops at the content's edges instead of the container's.
 */
@Composable
fun ZoomableBox(
    modifier: Modifier = Modifier,
    maxScale: Float = 4.0f,
    contentAspectRatio: Float = 0f,
    onSingleTap: () -> Unit = {},
    onZoomChange: (Boolean) -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    var size by remember { mutableStateOf(IntSize.Zero) }
    val aspect by rememberUpdatedState(contentAspectRatio)

    val scaleAnim = remember { Animatable(1f) }
    val offsetXAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(0f) }

    /** How much the fitted content has to grow to fill the container in its short direction. */
    fun fillScale(): Float {
        if (aspect <= 0f || size.width == 0 || size.height == 0) return 1f
        val containerAspect = size.width.toFloat() / size.height
        return max(containerAspect / aspect, aspect / containerAspect)
    }

    fun maxScaleNow(): Float = max(maxScale, fillScale() * 4f)

    /** Largest offsets at [scale] that keep the content covering the container (or centred where it is smaller). */
    fun bounds(scale: Float): Offset {
        if (aspect <= 0f || size.width == 0 || size.height == 0) {
            return Offset(size.width * (scale - 1f) / 2f, size.height * (scale - 1f) / 2f)
        }
        val containerAspect = size.width.toFloat() / size.height
        val fitWidth = if (aspect > containerAspect) size.width.toFloat() else size.height * aspect
        val fitHeight = fitWidth / aspect
        return Offset(
            ((fitWidth * scale - size.width) / 2f).coerceAtLeast(0f),
            ((fitHeight * scale - size.height) / 2f).coerceAtLeast(0f)
        )
    }

    // Observed outside composition: reading the scale here directly recomposed the whole viewer page
    // (image included) on every frame of a pinch or double-tap zoom.
    val latestOnZoomChange by androidx.compose.runtime.rememberUpdatedState(onZoomChange)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        androidx.compose.runtime.snapshotFlow { scaleAnim.value > 1.05f }
            .collect { latestOnZoomChange(it) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        scope.launch {
                            if (scaleAnim.value > 1.05f) {
                                // Reset to normal
                                launch { scaleAnim.animateTo(1f, tween(250, easing = FastOutSlowInEasing)) }
                                launch { offsetXAnim.animateTo(0f, tween(250, easing = FastOutSlowInEasing)) }
                                launch { offsetYAnim.animateTo(0f, tween(250, easing = FastOutSlowInEasing)) }
                            } else {
                                // Zoom in towards the tap; very narrow or wide content fills the screen instead.
                                val targetScale = max(2.5f, fillScale()).coerceAtMost(maxScaleNow())
                                val limit = bounds(targetScale)

                                val center = Offset(size.width / 2f, size.height / 2f)
                                val targetOffsetX = ((center.x - tapOffset.x) * (targetScale - 1f)).coerceIn(-limit.x, limit.x)
                                val targetOffsetY = ((center.y - tapOffset.y) * (targetScale - 1f)).coerceIn(-limit.y, limit.y)

                                launch { scaleAnim.animateTo(targetScale, tween(250, easing = FastOutSlowInEasing)) }
                                launch { offsetXAnim.animateTo(targetOffsetX, tween(250, easing = FastOutSlowInEasing)) }
                                launch { offsetYAnim.animateTo(targetOffsetY, tween(250, easing = FastOutSlowInEasing)) }
                            }
                        }
                    },
                    onTap = {
                        onSingleTap()
                    }
                )
            }
            .pointerInput(Unit) {
                detectZoomAndPan(shouldConsumePan = { scaleAnim.value > 1.05f }) { _, pan, zoom, _ ->
                    scope.launch {
                        val newScale = (scaleAnim.value * zoom).coerceIn(1f, maxScaleNow())
                        // The offset is in screen pixels whatever the zoom, so the photo follows the finger
                        // 1:1. (It used to be multiplied by the scale: at 4x a small move flung it across.)
                        val limit = bounds(newScale)

                        val newOffsetX = if (newScale > 1f) {
                            (offsetXAnim.value * (newScale / scaleAnim.value) + pan.x).coerceIn(-limit.x, limit.x)
                        } else 0f

                        val newOffsetY = if (newScale > 1f) {
                            (offsetYAnim.value * (newScale / scaleAnim.value) + pan.y).coerceIn(-limit.y, limit.y)
                        } else 0f

                        scaleAnim.snapTo(newScale)
                        offsetXAnim.snapTo(newOffsetX)
                        offsetYAnim.snapTo(newOffsetY)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                    translationX = offsetXAnim.value
                    translationY = offsetYAnim.value
                }
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
