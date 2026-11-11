package com.gallery.app.ui.components
import com.gallery.app.ui.i18n.strings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.app.model.MediaItem
import com.gallery.app.ui.design.GlassRim
import com.gallery.app.ui.design.GlassSpec
import com.gallery.app.ui.design.glass
import com.gallery.app.ui.theme.LocalAppTheme
import com.gallery.app.ui.theme.isLight
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Top bar shown while items are selected, shared by the Bilder grid and album media grids: a floating
 * frosted One UI card with a back chevron, n/total and the select-all circle.
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
    val palette = LocalAppTheme.current.palette
    val allSelected = totalCount > 0 && selectedCount == totalCount
    val toggleLabel = if (allSelected) strings.none else strings.all
    val title = strings.selectionTitle(selectedCount)

    // One UI: the select-all check circle, the count and "Cancel" on a frosted bar (the expanded form
    // of this is the big "14 selected" title at the top of the grid).
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .glass(
                    RectangleShape,
                    GlassSpec(palette.background.copy(alpha = 0.72f), palette.background.copy(alpha = 0.97f), 30.dp, GlassRim.None),
                    hazeState
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(70.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OneUiSelectAll(checked = allSelected, onClick = onToggleSelectAll)
            Text(
                text = title,
                color = palette.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            )
            SelectionTextAction(strings.cancel, palette.onBackground, onClose)
        }
    }
}

/** One UI's round check: a white ring, filled white with a black check when [checked]. */
@Composable
fun OneUiCheckCircle(checked: Boolean, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 30.dp) {
    val palette = LocalAppTheme.current.palette
    val ring = if (palette.isLight) Color(0xFF1A1A1A) else Color.White
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (checked) ring else Color.Transparent)
            .then(if (!checked) Modifier.border(2.dp, ring, CircleShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            androidx.compose.material3.Icon(
                androidx.compose.material.icons.Icons.Rounded.Check,
                null,
                tint = if (palette.isLight) Color.White else Color.Black,
                modifier = Modifier.size(size * 0.66f)
            )
        }
    }
}

/** One UI's select-all toggle: just the round check, with no label underneath. */
@Composable
fun OneUiSelectAll(checked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        OneUiCheckCircle(checked, size = 28.dp)
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

/**
 * Samsung Gallery style continuous block drag-to-select, shared by every media grid:
 * - Starts when long-pressing directly on a media tile (never on headers or album tiles).
 * - Dragging from item A to item B selects everything between them; dragging back deselects again.
 * - Scrolls automatically when dragging near the top/bottom edge.
 *
 * Pointer positions are mapped into the grid's item coordinate space by adding the viewport start
 * offset, which is minus the top content padding. Without that, grids with a large top padding
 * hit-tested rows below the finger.
 */
@Composable
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
): Modifier {
    // The gesture handler keeps running across recompositions, so it must read the selection through the latest
    // callbacks. It used to keep the ones from when it started - "not selecting, nothing selected" - so a second
    // drag while already selecting started a new selection and replaced everything selected so far.
    val latestIsSelectionMode = androidx.compose.runtime.rememberUpdatedState(isSelectionMode)
    val latestSelectedIds = androidx.compose.runtime.rememberUpdatedState(selectedIds)
    val latestOnStartSelection = androidx.compose.runtime.rememberUpdatedState(onStartSelection)
    val latestOnSetSelection = androidx.compose.runtime.rememberUpdatedState(onSetSelection)
    val latestOnUpdateSelectedIds = androidx.compose.runtime.rememberUpdatedState(onUpdateSelectedIds)
    val latestOnLongPressRecognized = androidx.compose.runtime.rememberUpdatedState(onLongPressRecognized)
    val latestOnLongPressEnded = androidx.compose.runtime.rememberUpdatedState(onLongPressEnded)
    return pointerInput(gridState, items) {
    val isSelectionMode: () -> Boolean = { latestIsSelectionMode.value() }
    val selectedIds: () -> Set<Long> = { latestSelectedIds.value() }
    val onStartSelection: (Long) -> Unit = { latestOnStartSelection.value(it) }
    val onSetSelection: (Long, Boolean) -> Unit = { id, selected -> latestOnSetSelection.value(id, selected) }
    val onUpdateSelectedIds: (Set<Long>) -> Unit = { latestOnUpdateSelectedIds.value(it) }
    val onLongPressRecognized: (Long) -> Unit = { latestOnLongPressRecognized.value(it) }
    val onLongPressEnded: () -> Unit = { latestOnLongPressEnded.value() }
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
}
