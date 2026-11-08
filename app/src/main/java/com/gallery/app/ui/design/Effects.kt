package com.gallery.app.ui.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.gallery.app.ui.theme.LocalAppTheme

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
