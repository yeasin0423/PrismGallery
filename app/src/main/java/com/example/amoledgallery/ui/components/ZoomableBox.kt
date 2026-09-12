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
 */
@Composable
fun ZoomableBox(
    modifier: Modifier = Modifier,
    maxScale: Float = 4.0f,
    onSingleTap: () -> Unit = {},
    onZoomChange: (Boolean) -> Unit = {},
    content: @Composable BoxScope.(scale: Float) -> Unit
) {
    val scope = rememberCoroutineScope()
    var size by remember { mutableStateOf(IntSize.Zero) }

    val scaleAnim = remember { Animatable(1f) }
    val offsetXAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(0f) }
    
    androidx.compose.runtime.LaunchedEffect(scaleAnim.value) {
        onZoomChange(scaleAnim.value > 1.05f)
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
                                // Zoom in towards tap location
                                val targetScale = 2.5f
                                val maxX = (size.width * (targetScale - 1f)) / 2f
                                val maxY = (size.height * (targetScale - 1f)) / 2f

                                val center = Offset(size.width / 2f, size.height / 2f)
                                val targetOffsetX = ((center.x - tapOffset.x) * (targetScale - 1f)).coerceIn(-maxX, maxX)
                                val targetOffsetY = ((center.y - tapOffset.y) * (targetScale - 1f)).coerceIn(-maxY, maxY)

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
                        val newScale = (scaleAnim.value * zoom).coerceIn(1f, maxScale)
                        val maxX = (size.width * (newScale - 1f)) / 2f
                        val maxY = (size.height * (newScale - 1f)) / 2f

                        val newOffsetX = if (newScale > 1f) {
                            (offsetXAnim.value + pan.x * newScale).coerceIn(-maxX, maxX)
                        } else 0f
                        
                        val newOffsetY = if (newScale > 1f) {
                            (offsetYAnim.value + pan.y * newScale).coerceIn(-maxY, maxY)
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
            content(scaleAnim.value)
        }
    }
}
