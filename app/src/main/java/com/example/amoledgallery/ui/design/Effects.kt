package com.example.amoledgallery.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.amoledgallery.PerformanceProfile
import com.example.amoledgallery.ui.theme.DesignLanguage
import com.example.amoledgallery.ui.theme.LocalAppTheme
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ---------------------------------------------------------------------------------------------
// Sparkle tap (One UI, Material Expressive)
// ---------------------------------------------------------------------------------------------

private const val SPARKLE_MAX_TAP_MS = 240L

private class SparkleBurst(val center: Offset, val seed: Int) {
    val progress = Animatable(0f)
}

/**
 * A small sparkling ripple at the touch point: a soft glowing ring plus a few star-like sparks that
 * fly outwards and fade. Only for One UI and Material Expressive, and only for a clean tap - a gesture
 * that turns into a slide or drag (which consumes the pointer) never triggers it, so sliding across
 * tabs costs nothing. Disabled on LOW graphics quality.
 */
@Composable
fun Modifier.sparkleTap(color: Color = Color.Unspecified): Modifier {
    val theme = LocalAppTheme.current
    val quality = LocalGraphicsQuality.current
    if (quality == PerformanceProfile.LOW) return this
    if (theme.language != DesignLanguage.SAMSUNG_ONEUI && theme.language != DesignLanguage.GOOGLE_EXPRESSIVE) return this

    val sparkColor = if (color != Color.Unspecified) {
        color
    } else if (theme.language == DesignLanguage.GOOGLE_EXPRESSIVE) {
        theme.palette.accent
    } else {
        theme.palette.onBackground
    }
    val bursts = remember { mutableStateListOf<SparkleBurst>() }
    val scope = rememberCoroutineScope()

    return this
        .pointerInput(Unit) {
            // Observed on the Initial pass without consuming anything, so the element's own click and
            // slide handling is untouched. A gesture counts as a tap only if the finger neither moved
            // past touch slop nor stayed down long enough to start a hold-slide.
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                var isTap = true
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop ||
                        change.uptimeMillis - down.uptimeMillis > SPARKLE_MAX_TAP_MS
                    ) {
                        isTap = false
                    }
                    if (!change.pressed) break
                }
                if (isTap) {
                    val burst = SparkleBurst(down.position, Random.nextInt())
                    bursts.add(burst)
                    scope.launch {
                        burst.progress.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
                        bursts.remove(burst)
                    }
                }
            }
        }
        .drawWithContent {
            drawContent()
            for (burst in bursts) drawSparkle(burst, sparkColor)
        }
}

private fun DrawScope.drawSparkle(burst: SparkleBurst, color: Color) {
    val p = burst.progress.value
    val fade = 1f - p
    val center = burst.center

    // Soft ripple: a glow that expands and a thin ring on its edge.
    val ringRadius = 6.dp.toPx() + 40.dp.toPx() * p
    drawCircle(
        brush = Brush.radialGradient(
            listOf(color.copy(alpha = 0.20f * fade), color.copy(alpha = 0.06f * fade), Color.Transparent),
            center = center,
            radius = ringRadius
        ),
        radius = ringRadius,
        center = center
    )
    drawCircle(
        color = color.copy(alpha = 0.40f * fade),
        radius = ringRadius,
        center = center,
        style = Stroke(width = 1.4.dp.toPx() * fade + 0.2f)
    )

    // Sparks: short four-point stars flying outwards on slightly randomized angles and distances.
    val random = Random(burst.seed)
    val sparks = 9
    val eased = 1f - (1f - p) * (1f - p)
    for (i in 0 until sparks) {
        val angle = (i.toFloat() / sparks) * 2f * PI.toFloat() + random.nextFloat() * 0.5f
        val distance = 4.dp.toPx() + (22.dp.toPx() + random.nextFloat() * 20.dp.toPx()) * eased
        val sparkCenter = Offset(center.x + cos(angle) * distance, center.y + sin(angle) * distance)
        val size = (2.6.dp.toPx() + random.nextFloat() * 1.6.dp.toPx()) * (1f - p * 0.7f)
        val alpha = (fade * (0.6f + random.nextFloat() * 0.4f)).coerceIn(0f, 1f)
        val stroke = 1.1.dp.toPx()
        drawLine(color.copy(alpha = alpha), Offset(sparkCenter.x - size, sparkCenter.y), Offset(sparkCenter.x + size, sparkCenter.y), stroke, StrokeCap.Round)
        drawLine(color.copy(alpha = alpha), Offset(sparkCenter.x, sparkCenter.y - size), Offset(sparkCenter.x, sparkCenter.y + size), stroke, StrokeCap.Round)
        drawCircle(color.copy(alpha = alpha), radius = stroke * 0.9f, center = sparkCenter)
    }
}

// ---------------------------------------------------------------------------------------------
// Bottom edge fade
// ---------------------------------------------------------------------------------------------

/**
 * A soft gradient into the background colour at the bottom edge, sized to the system navigation area
 * (tall behind 3-button navigation, short for gesture navigation), so content fades out smoothly behind
 * the bottom bar instead of ending at a hard line. Draws nothing interactive.
 */
@Composable
fun BottomEdgeFade(modifier: Modifier = Modifier, visible: Boolean = true) {
    val palette = LocalAppTheme.current.palette
    val inset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(220), label = "bottom_fade")
    val background = palette.background
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(inset + 64.dp)
            .graphicsLayer { this.alpha = alpha }
            .background(
                Brush.verticalGradient(
                    0f to background.copy(alpha = 0f),
                    0.45f to background.copy(alpha = 0.38f),
                    0.8f to background.copy(alpha = 0.78f),
                    1f to background.copy(alpha = 0.92f)
                )
            )
    )
}

// ---------------------------------------------------------------------------------------------
// Samsung Gallery tab icons
// ---------------------------------------------------------------------------------------------

/** Tab icons drawn after Samsung Gallery's own: a book-like album frame and an even three-line menu. */
object SamsungGalleryIcons {

    val Albums: ImageVector by lazy { albums(selected = false) }
    val AlbumsSelected: ImageVector by lazy { albums(selected = true) }
    val Menu: ImageVector by lazy { menu(strokeWidth = 1.8f) }
    val MenuSelected: ImageVector by lazy { menu(strokeWidth = 2.4f) }

    private fun albums(selected: Boolean): ImageVector =
        ImageVector.Builder(
            name = if (selected) "SamsungAlbumsSelected" else "SamsungAlbums",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            if (selected) {
                // Filled frame with the spine cut out (even-odd fill).
                path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                    roundedRect(4f, 2.5f, 20f, 21.5f, 3.2f)
                    moveTo(8.4f, 2.5f)
                    lineTo(9.8f, 2.5f)
                    lineTo(9.8f, 21.5f)
                    lineTo(8.4f, 21.5f)
                    close()
                }
            } else {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.8f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    roundedRect(4.5f, 3f, 19.5f, 21f, 3f)
                    moveTo(9f, 3.4f)
                    lineTo(9f, 20.6f)
                }
            }
        }.build()

    private fun menu(strokeWidth: Float): ImageVector =
        ImageVector.Builder(
            name = "SamsungMenu$strokeWidth",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(4f, 6.5f)
                lineTo(20f, 6.5f)
                moveTo(4f, 12f)
                lineTo(20f, 12f)
                moveTo(4f, 17.5f)
                lineTo(20f, 17.5f)
            }
        }.build()

    private fun androidx.compose.ui.graphics.vector.PathBuilder.roundedRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radius: Float
    ) {
        moveTo(left + radius, top)
        lineTo(right - radius, top)
        quadTo(right, top, right, top + radius)
        lineTo(right, bottom - radius)
        quadTo(right, bottom, right - radius, bottom)
        lineTo(left + radius, bottom)
        quadTo(left, bottom, left, bottom - radius)
        lineTo(left, top + radius)
        quadTo(left, top, left + radius, top)
        close()
    }
}
