package com.example.amoledgallery.ui.components
import com.example.amoledgallery.ui.i18n.strings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amoledgallery.model.MediaItem
import com.example.amoledgallery.ui.design.GlassRim
import com.example.amoledgallery.ui.design.GlassSpec
import com.example.amoledgallery.ui.design.ThemedIconButton
import com.example.amoledgallery.ui.design.ThemedTopBar
import com.example.amoledgallery.ui.design.glass
import com.example.amoledgallery.ui.design.pressScale
import com.example.amoledgallery.ui.theme.DesignLanguage
import com.example.amoledgallery.ui.theme.LocalAppTheme
import com.example.amoledgallery.ui.theme.frost
import com.example.amoledgallery.ui.theme.isLight
import com.example.amoledgallery.ui.theme.onAccent
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Top bar shown while items are selected, shared by the Bilder grid and album media grids:
 * - One UI 9: a floating frosted card with back chevron, n/total and "Alle".
 * - iOS: glass bar with "Alle" on the left, the count centred, a prominent strings.done on the right.
 * - Material / HyperOS: contextual top bar with a close icon and an "Alle" text action.
 */
@Composable
fun SelectionTopBar(
    selectedCount: Int,
    totalCount: Int,
    onToggleSelectAll: () -> Unit,
    onClose: () -> Unit,
    hazeState: HazeState?,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val palette = theme.palette
    val allSelected = totalCount > 0 && selectedCount == totalCount
    val toggleLabel = if (allSelected) strings.none else strings.all
    val title = strings.selectionTitle(selectedCount)

    when (theme.language) {
        DesignLanguage.SAMSUNG_ONEUI -> Box(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .glass(
                        RoundedCornerShape(30.dp),
                        GlassSpec(palette.frost(Color(0xFF2A2A2A), 0.60f), palette.frost(Color(0xFF2A2A2A), 0.97f), 40.dp, GlassRim.Subtle),
                        hazeState
                    )
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemedIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBackIos,
                    contentDescription = strings.endSelection,
                    onClick = onClose
                )
                Text(
                    text = "$selectedCount/$totalCount",
                    color = palette.onBackground,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                SelectionTextAction(toggleLabel, palette.onBackground, onToggleSelectAll)
            }
        }

        DesignLanguage.IOS_LIQUID_GLASS -> {
            val shade = if (palette.isLight) Color.White else Color.Black
            Box(modifier = modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .glass(
                            RectangleShape,
                            GlassSpec(shade.copy(alpha = 0.45f), shade.copy(alpha = 0.92f), 18.dp, GlassRim.None),
                            hazeState,
                            mask = Brush.verticalGradient(0f to Color.Black, 0.7f to Color.Black, 1f to Color.Transparent)
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SelectionGlassButton(toggleLabel, hazeState, prominent = false, onClick = onToggleSelectAll)
                    Text(
                        text = title,
                        color = palette.onBackground,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                    )
                    SelectionGlassButton(strings.done, hazeState, prominent = true, onClick = onClose)
                }
            }
        }

        else -> ThemedTopBar(
            title = title,
            navigationIcon = Icons.Rounded.Close,
            onBack = onClose,
            hazeState = hazeState,
            modifier = modifier
        ) {
            SelectionTextAction(toggleLabel, palette.accent, onToggleSelectAll)
        }
    }
}

@Composable
private fun SelectionTextAction(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text = text,
        color = color,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
private fun SelectionGlassButton(text: String, hazeState: HazeState?, prominent: Boolean, onClick: () -> Unit) {
    val palette = LocalAppTheme.current.palette
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .pressScale(interaction, 0.92f)
            .height(44.dp)
            .then(
                if (prominent) Modifier.clip(CircleShape).background(palette.accent)
                else Modifier.glass(CircleShape, state = hazeState)
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (prominent) palette.onAccent else palette.onBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

/**
 * Samsung Gallery style continuous block drag-to-select, shared by every media grid:
 * - Starts when long-pressing directly on a media tile (never on headers or album tiles).
 * - Dragging from item A to item B selects everything between them; dragging back deselects again.
 * - Scrolls automatically when dragging near the top/bottom edge.
 *
 * Pointer positions are mapped into the grid's item coordinate space by adding the viewport start
 * offset, which is minus the top content padding. Without that, grids with a large top padding (the
 * iOS title overlay) hit-tested rows below the finger.
 */
internal fun Modifier.detectDragSelect(
    items: List<MediaItem>,
    gridState: LazyGridState,
    isSelectionMode: () -> Boolean,
    onStartSelection: (Long) -> Unit,
    onSetSelection: (Long, Boolean) -> Unit,
    onUpdateSelectedIds: (Set<Long>) -> Unit,
    selectedIds: () -> Set<Long>,
    haptic: HapticFeedback,
    coroutineScope: CoroutineScope,
    onLongPressRecognized: (Long) -> Unit,
    onLongPressEnded: () -> Unit
): Modifier = pointerInput(gridState, items) {
    var dragSelectMode = true
    var autoScrollJob: Job? = null
    var currentTouch = Offset.Zero
    var dragStartIndex: Int? = null
    var initialSelectedIds = emptySet<Long>()
    var lastProcessedRange: IntRange? = null
    var idToIndex = emptyMap<Long, Int>()
    var dragStartPosition = Offset.Zero

    fun findMediaIndexAt(pos: Offset, allowClosest: Boolean): Int? {
        val layoutInfo = gridState.layoutInfo
        val visible = layoutInfo.visibleItemsInfo
        if (visible.isEmpty()) return null
        val x = pos.x
        val y = pos.y + layoutInfo.viewportStartOffset

        // 1. Exact item hit test
        val exactHit = visible.find { info ->
            info.key is Long &&
                x >= info.offset.x && x <= (info.offset.x + info.size.width) &&
                y >= info.offset.y && y <= (info.offset.y + info.size.height)
        }
        if (exactHit != null) {
            idToIndex[exactHit.key as Long]?.let { return it }
        }
        if (!allowClosest) return null

        // 2. Finger not on a tile: resolve in reading order instead of by nearest centre. First the row
        // under (or vertically nearest to) the finger, then the last tile in that row at or left of the
        // finger. So the empty space of a short last row, or the area below the grid, still reaches the
        // last tiles - a nearest-centre search picked the full row above instead.
        val tiles = visible.filter { it.key is Long }
        if (tiles.isEmpty()) return null
        val nearestRowTile = tiles.minBy { info ->
            val top = info.offset.y.toFloat()
            val bottom = top + info.size.height
            when {
                y < top -> top - y
                y > bottom -> y - bottom
                else -> 0f
            }
        }
        val row = tiles.filter { it.offset.y == nearestRowTile.offset.y }.sortedBy { it.offset.x }
        val target = row.lastOrNull { x >= it.offset.x } ?: row.first()
        return idToIndex[target.key as Long]
    }

    fun updateRangeSelection(currentIdx: Int) {
        val startIdx = dragStartIndex ?: return
        val minIdx = minOf(startIdx, currentIdx).coerceIn(0, items.size - 1)
        val maxIdx = maxOf(startIdx, currentIdx).coerceIn(0, items.size - 1)
        val currentRange = minIdx..maxIdx

        if (currentRange != lastProcessedRange) {
            lastProcessedRange = currentRange
            val rangeIds = (minIdx..maxIdx).map { items[it].id }.toSet()
            val newSelection = if (dragSelectMode) initialSelectedIds + rangeIds else initialSelectedIds - rangeIds
            onUpdateSelectedIds(newSelection)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun endGesture() {
        autoScrollJob?.cancel()
        autoScrollJob = null
        dragStartIndex = null
        lastProcessedRange = null
        onLongPressEnded()
    }

    detectDragGesturesAfterLongPress(
        onDragStart = { offset ->
            idToIndex = items.mapIndexed { idx, item -> item.id to idx }.toMap()
            currentTouch = offset
            dragStartPosition = offset
            initialSelectedIds = selectedIds().toSet()
            lastProcessedRange = null

            val startIdx = findMediaIndexAt(offset, allowClosest = false)
            if (startIdx != null && startIdx in items.indices) {
                dragStartIndex = startIdx
                val startItem = items[startIdx]
                val alreadySelected = initialSelectedIds.contains(startItem.id)
                dragSelectMode = if (isSelectionMode()) !alreadySelected else true

                if (!isSelectionMode()) {
                    onStartSelection(startItem.id)
                    initialSelectedIds = setOf(startItem.id)
                } else {
                    onSetSelection(startItem.id, dragSelectMode)
                    initialSelectedIds = if (dragSelectMode) initialSelectedIds + startItem.id else initialSelectedIds - startItem.id
                }
                lastProcessedRange = startIdx..startIdx
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongPressRecognized(startItem.id)
            } else {
                dragStartIndex = null
            }
            // Auto-scroll and range updates only begin once the finger has really moved away from the
            // long-press origin (see onDrag), so holding still never re-evaluates the selection.
        },
        onDrag = { change, _ ->
            if (dragStartIndex == null) return@detectDragGesturesAfterLongPress
            change.consume()
            currentTouch = change.position

            if ((currentTouch - dragStartPosition).getDistance() > 8f) {
                findMediaIndexAt(currentTouch, allowClosest = true)?.let { updateRangeSelection(it) }

                if (autoScrollJob == null) {
                    autoScrollJob = coroutineScope.launch {
                        val scrollMarginPx = 100.dp.toPx()
                        while (isActive) {
                            val viewportH = gridState.layoutInfo.viewportSize.height.toFloat()
                            if (viewportH > 0 && dragStartIndex != null) {
                                val scrollAmount = when {
                                    currentTouch.y < scrollMarginPx ->
                                        -((scrollMarginPx - currentTouch.y) / scrollMarginPx).coerceIn(0f, 1f) * 24f
                                    currentTouch.y > viewportH - scrollMarginPx ->
                                        ((currentTouch.y - (viewportH - scrollMarginPx)) / scrollMarginPx).coerceIn(0f, 1f) * 24f
                                    else -> 0f
                                }
                                if (scrollAmount != 0f) {
                                    gridState.scrollBy(scrollAmount)
                                    findMediaIndexAt(currentTouch, allowClosest = true)?.let { updateRangeSelection(it) }
                                }
                            }
                            delay(16)
                        }
                    }
                }
            }
        },
        onDragEnd = { endGesture() },
        onDragCancel = { endGesture() }
    )
}
