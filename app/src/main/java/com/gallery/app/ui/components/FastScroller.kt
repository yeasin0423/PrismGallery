package com.gallery.app.ui.components

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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.app.ui.theme.LocalAppTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Fast scroll scrubber for a media grid.
 *
 * Only the thumb itself is grabbable, like Samsung's own gallery: a touch anywhere else on the right edge
 * falls straight through to the grid. The thumb position and dragging use one and the same mapping of the
 * grid's scroll position (item position within the scrollable range, including the offset inside the top
 * row), so the thumb reaches the very top and bottom, follows the finger exactly and stays where it was
 * released instead of jumping. While dragging, a pill shows [labelForGridIndex] of the top row.
 */
@Composable
fun FastScroller(
    gridState: LazyGridState,
    labelForGridIndex: (Int) -> String,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppTheme.current.palette
    val latestLabel by rememberUpdatedState(labelForGridIndex)
    // Finger position while dragging (0..1).
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    // Where the finger let go; kept until the grid is scrolled by hand again, so releasing never jumps.
    var releasedFraction by remember { mutableStateOf<Float?>(null) }
    // The item the drag wants at the top. Collected below with collectLatest, so fast drags don't pile up jumps.
    var targetIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(gridState) {
        snapshotFlow { targetIndex }.filterNotNull().collectLatest { gridState.scrollToItem(it) }
    }
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.isScrollInProgress }.collect { scrolling ->
            if (scrolling && dragFraction == null) releasedFraction = null
        }
    }

    /** The grid's scroll position as 0 (top) .. 1 (bottom). */
    fun gridFraction(): Float {
        val info = gridState.layoutInfo
        val visible = info.visibleItemsInfo
        if (info.totalItemsCount == 0 || visible.isEmpty()) return 0f
        if (!gridState.canScrollBackward) return 0f
        if (!gridState.canScrollForward) return 1f
        val first = visible.first()
        val rowItems = visible.count { it.offset.y == first.offset.y }.coerceAtLeast(1)
        val rowHeight = first.size.height.coerceAtLeast(1)
        val position = gridState.firstVisibleItemIndex +
            (gridState.firstVisibleItemScrollOffset.toFloat() / rowHeight).coerceIn(0f, 1f) * rowItems
        val scrollable = (info.totalItemsCount - visible.size).coerceAtLeast(1)
        return (position / scrollable).coerceIn(0f, 1f)
    }

    /** The item index that belongs at the top for [fraction]. */
    fun indexFor(fraction: Float): Int {
        val info = gridState.layoutInfo
        val total = info.totalItemsCount
        if (total == 0) return 0
        if (fraction >= 0.999f) return total - 1
        val scrollable = (total - info.visibleItemsInfo.size).coerceAtLeast(1)
        return floor(fraction * scrollable).toInt().coerceIn(0, total - 1)
    }

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

        // Read only inside the offset lambdas (layout phase): scrolling moves the thumb without recomposing.
        fun thumbOffsetPx(): Float = (dragFraction ?: releasedFraction ?: gridFraction()) * maxTravelPx

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset(x = 0, y = thumbOffsetPx().roundToInt()) }
                .width(36.dp)
                .height(thumbHeightDp)
                .pointerInput(maxTravelPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // Anchor on where the thumb is, then follow the finger by its travel, so the thumb
                        // doesn't jump to the finger on touch-down.
                        val startFraction = releasedFraction ?: gridFraction()
                        val startY = down.position.y
                        dragFraction = startFraction
                        down.consume()

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) break
                            val fraction = (startFraction + (change.position.y - startY) / maxTravelPx).coerceIn(0f, 1f)
                            dragFraction = fraction
                            targetIndex = indexFor(fraction)
                            change.consume()
                        }
                        releasedFraction = dragFraction
                        dragFraction = null
                    }
                }
        ) {
            val dragging = dragFraction != null
            // Slim visible thumb inside the generous grab area, in the theme's text colour so it shows on
            // light and dark backgrounds alike.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 2.dp)
                    .size(width = if (dragging) 5.dp else 4.dp, height = thumbHeightDp)
                    .clip(CircleShape)
                    .background(palette.onBackground.copy(alpha = if (dragging) 0.9f else 0.35f))
            )
        }

        // Month & year of the top row while dragging.
        if (dragFraction != null) {
            val label by remember { derivedStateOf { latestLabel(gridState.firstVisibleItemIndex) } }
            if (label.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset {
                            IntOffset(
                                x = -44.dp.roundToPx(),
                                y = (thumbOffsetPx() - 4.dp.toPx())
                                    .roundToInt()
                                    .coerceIn(0, (trackHeightPx - 48.dp.toPx()).roundToInt().coerceAtLeast(0))
                            )
                        }
                        // The scroller strip is only 36.dp wide: let the pill take its own width to the left.
                        .wrapContentWidth(align = Alignment.End, unbounded = true)
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(palette.surface)
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        color = palette.onBackground,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
