package com.example.amoledgallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amoledgallery.ui.theme.AmoledBlack
import com.example.amoledgallery.ui.theme.OneUiCardBorder
import com.example.amoledgallery.ui.theme.PureWhite
import kotlin.math.roundToInt

/**
 * Samsung OneUI-style fast scroll scrubber.
 *
 * Only the thumb itself is grabbable - like Samsung's own gallery. The scrubber used to capture
 * touches anywhere along a full-height strip on the right edge, so scrolling with a right thumb
 * would accidentally grab it and fling the list to a random position. Everywhere else the touch
 * now falls straight through to the grid, and the thumb just follows the scroll position.
 */
@Composable
fun FastScroller(
    scrollFraction: () -> Float,
    indicatorText: String,
    onScrollProgress: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragY by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(36.dp)
    ) {
        val density = LocalDensity.current
        val trackHeightPx = constraints.maxHeight.toFloat()
        val thumbHeightDp = 44.dp
        val thumbHeightPx = with(density) { thumbHeightDp.toPx() }
        val maxTravelPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)

        // Current thumb offset: driven by the grid while idle, by the finger while dragging.
        val thumbOffsetPx = if (isDragging) {
            dragY.coerceIn(0f, maxTravelPx)
        } else {
            scrollFraction().coerceIn(0f, 1f) * maxTravelPx
        }

        // Grab area: a 36.dp wide, thumb-tall box that travels with the thumb. Because it is the
        // only pointer-input region, a touch anywhere else on the right edge never reaches the
        // scrubber at all and simply scrolls the grid underneath.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset(x = 0, y = thumbOffsetPx.roundToInt()) }
                .width(36.dp)
                .height(thumbHeightDp)
                .pointerInput(maxTravelPx, thumbHeightPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // Anchor on where the thumb currently is, then follow the finger by delta
                        // so the thumb doesn't jump to the finger on touch-down.
                        val startThumbY = (scrollFraction().coerceIn(0f, 1f) * maxTravelPx)
                        val startY = down.position.y
                        dragY = startThumbY
                        isDragging = true
                        down.consume()

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) {
                                isDragging = false
                                break
                            }
                            val delta = change.position.y - startY
                            dragY = (startThumbY + delta).coerceIn(0f, maxTravelPx)
                            onScrollProgress(dragY / maxTravelPx)
                            change.consume()
                        }
                    }
                }
        ) {
            // Slim visible thumb inside the generous grab area
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 2.dp)
                    .size(width = 4.dp, height = thumbHeightDp)
                    .clip(CircleShape)
                    .background(if (isDragging) PureWhite else Color.White.copy(alpha = 0.35f))
            )
        }

        // Floating OneUI Scrubber Pill (Month & Year)
        if (isDragging && indicatorText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset {
                        IntOffset(
                            x = -with(density) { 44.dp.roundToPx() },
                            y = (thumbOffsetPx - with(density) { 4.dp.toPx() })
                                .roundToInt()
                                .coerceIn(0, (trackHeightPx - with(density) { 48.dp.toPx() }).roundToInt())
                        )
                    }
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(AmoledBlack)
                    .border(1.dp, OneUiCardBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = indicatorText,
                    color = PureWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
