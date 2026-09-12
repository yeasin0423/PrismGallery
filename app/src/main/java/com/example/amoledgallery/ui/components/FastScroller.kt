package com.example.amoledgallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
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
 * Features:
 * - Narrow vertical draggable thumb along right screen border
 * - Proportional drag calculation
 * - Floating black/white rounded OneUI pill displaying current month & year
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


        // Generous 36.dp touch capture area along the right edge with transparent padding
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(36.dp)
                .align(Alignment.TopEnd)
                .pointerInput(trackHeightPx) {
                    detectTapGestures(
                        onPress = { offset ->
                            isDragging = true
                            dragY = (offset.y - thumbHeightPx / 2f).coerceIn(0f, maxTravelPx)
                            val fraction = dragY / maxTravelPx
                            onScrollProgress(fraction)
                            tryAwaitRelease()
                            isDragging = false
                        }
                    )
                }
                .pointerInput(trackHeightPx) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragY = (offset.y - thumbHeightPx / 2f).coerceIn(0f, maxTravelPx)
                            val fraction = dragY / maxTravelPx
                            onScrollProgress(fraction)
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            dragY = (change.position.y - thumbHeightPx / 2f).coerceIn(0f, maxTravelPx)
                            val fraction = dragY / maxTravelPx
                            onScrollProgress(fraction)
                        },
                        onDragEnd = {
                            isDragging = false
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                }
        ) {
            // Slim, subtle scrubber bar thumb inside generous touch hit-area
            Box(
                modifier = Modifier
                    .offset { 
                        val y = if (isDragging) dragY.coerceIn(0f, maxTravelPx) else (scrollFraction().coerceIn(0f, 1f) * maxTravelPx)
                        IntOffset(x = 0, y = y.roundToInt()) 
                    }
                    .align(Alignment.TopEnd)
                    .padding(end = 2.dp)
                    .size(width = 4.dp, height = thumbHeightDp)
                    .clip(CircleShape)
                    .background(
                        if (isDragging) PureWhite else Color.White.copy(alpha = 0.35f)
                    )
            )

            // Floating OneUI Scrubber Pill (Month & Year)
            if (isDragging && indicatorText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = -with(density) { 44.dp.roundToPx() },
                                y = ((if (isDragging) dragY.coerceIn(0f, maxTravelPx) else (scrollFraction().coerceIn(0f, 1f) * maxTravelPx)) - with(density) { 4.dp.roundToPx() })
                                    .roundToInt()
                                    .coerceIn(0, (trackHeightPx - with(density) { 48.dp.toPx() }).roundToInt())
                            )
                        }
                        .align(Alignment.TopEnd)
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
}
