package com.gallery.app.ui.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * One freehand stroke, stored in normalized coordinates of the rotated/flipped frame. That keeps it
 * attached to the photo when the frame is rotated or mirrored, and lets the exporter map it into the
 * crop.
 */
data class DrawStroke(
    val points: List<Offset>,
    val color: Color,
    /** Brush width as a fraction of the frame's shorter side. */
    val width: Float
)

/** One UI friendly palette: white, black, and eight saturated photo-annotation colours. */
val EditorDrawColors: List<Color> = listOf(
    Color.White,
    Color(0xFF111111),
    Color(0xFFE53935),
    Color(0xFFFB8C00),
    Color(0xFFFDD835),
    Color(0xFF43A047),
    Color(0xFF00ACC1),
    Color(0xFF1E88E5),
    Color(0xFF8E24AA),
    Color(0xFFEC407A)
)

const val MinDrawWidth = 0.006f
const val MaxDrawWidth = 0.06f
const val DefaultDrawWidth = 0.02f

fun DrawStroke.rotateClockwise() = copy(points = points.map { Offset(1f - it.y, it.x) })

fun DrawStroke.rotateCounterClockwise() = copy(points = points.map { Offset(it.y, 1f - it.x) })

fun DrawStroke.flipHorizontal() = copy(points = points.map { Offset(1f - it.x, it.y) })

fun DrawStroke.flipVertical() = copy(points = points.map { Offset(it.x, 1f - it.y) })

/**
 * Draws one stroke into a rectangle on screen. [strokeWidth] is already in pixels; callers scale it
 * from the frame's shorter side so zooming the crop keeps the ink proportional.
 */
fun DrawScope.drawEditorStroke(
    stroke: DrawStroke,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    strokeWidth: Float
) {
    if (stroke.points.isEmpty()) return
    if (stroke.points.size == 1) {
        val point = stroke.points.first()
        drawCircle(
            color = stroke.color,
            radius = (strokeWidth / 2f).coerceAtLeast(0.75f),
            center = Offset(left + point.x * width, top + point.y * height)
        )
        return
    }
    val path = Path().apply {
        stroke.points.forEachIndexed { index, point ->
            val x = left + point.x * width
            val y = top + point.y * height
            if (index == 0) moveTo(x, y) else lineTo(x, y)
        }
    }
    drawPath(
        path = path,
        color = stroke.color,
        style = Stroke(
            width = strokeWidth.coerceAtLeast(1f),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}
