package com.example.amoledgallery.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Self-contained HSV picker (saturation/value square + hue strip) for the custom-palette
 * editor - no external dependency. Reports the picked color continuously via [onColorChanged].
 */
@Composable
fun HsvColorPicker(
    initialColor: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialHsv = remember(initialColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
        hsv
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    fun currentColor() = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))

    Column(modifier = modifier) {
        val hueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            saturation = (offset.x / size.width).coerceIn(0f, 1f)
                            value = (1f - offset.y / size.height).coerceIn(0f, 1f)
                            onColorChanged(currentColor())
                        }
                    ) { change, _ ->
                        saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                        value = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                        onColorChanged(currentColor())
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                drawRect(color = hueColor)
                drawRect(brush = Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
                drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))

                val indicatorCenter = Offset(saturation * size.width, (1f - value) * size.height)
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = indicatorCenter,
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.4f),
                    radius = 9.dp.toPx(),
                    center = indicatorCenter,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            hue = (offset.x / size.width * 360f).coerceIn(0f, 360f)
                            onColorChanged(currentColor())
                        }
                    ) { change, _ ->
                        hue = (change.position.x / size.width * 360f).coerceIn(0f, 360f)
                        onColorChanged(currentColor())
                    }
                }
        ) {
            val hueStops = remember {
                (0..12).map { i -> Color(android.graphics.Color.HSVToColor(floatArrayOf(i * 30f, 1f, 1f))) }
            }
            Canvas(modifier = Modifier.fillMaxWidth().height(28.dp)) {
                drawRect(brush = Brush.horizontalGradient(hueStops))
                val knobX = (hue / 360f) * size.width
                drawCircle(
                    color = Color.White,
                    radius = size.height / 2f,
                    center = Offset(knobX.coerceIn(size.height / 2f, size.width - size.height / 2f), size.height / 2f),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(top = 14.dp)
                .size(36.dp)
                .clip(CircleShape)
                .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
        ) {
            Canvas(modifier = Modifier.size(36.dp)) {
                drawRect(color = currentColor())
            }
        }
    }
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255f).roundToInt(),
    (red * 255f).roundToInt(),
    (green * 255f).roundToInt(),
    (blue * 255f).roundToInt()
)
