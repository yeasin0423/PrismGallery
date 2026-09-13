package com.example.amoledgallery.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amoledgallery.R
import com.example.amoledgallery.data.ThumbnailManager
import com.example.amoledgallery.ui.qualityTween
import com.example.amoledgallery.model.MediaDateSection
import com.example.amoledgallery.model.MediaItem
import com.example.amoledgallery.ui.components.DateHeader
import com.example.amoledgallery.ui.components.FastScroller
import com.example.amoledgallery.ui.components.FilterChips
import com.example.amoledgallery.ui.components.MediaGridItem
import com.example.amoledgallery.ui.components.OneUiHeader
import com.example.amoledgallery.ui.components.detectPinchToZoomGrid
import com.example.amoledgallery.ui.theme.AmoledBlack
import com.example.amoledgallery.ui.theme.PureWhite
import com.example.amoledgallery.ui.theme.SurfaceDark
import com.example.amoledgallery.ui.theme.TextMuted
import com.example.amoledgallery.ui.viewmodel.MediaFilter
import com.example.amoledgallery.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


/**
 * High-performance Samsung OneUI AMOLED Gallery media grid screen:
 * - Permanent internal fast thumbnail cache integration eliminating MicroSD latency
 * - Fixed 1:1 aspect ratio with clipToBounds and contentType = "media_tile" for optimal 120fps scrolling
 * - Decoupled Fullscreen-Overlay viewer via AnimatedVisibility keeping LazyGridState intact
 * - OneUI Selection Mode (long-press activation, animated circular checkbox, selection top bar)
 * - Draggable FastScroller with generous 36.dp touch hitbox
 * - Reactive media type filter chips and dynamic Pinch-to-Zoom column scaling (2 to 5)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    items: List<MediaItem>,
    isLoading: Boolean,
    columnCount: Int,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onItemClick: (MediaItem, Int) -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.header_media),
    selectedFilter: MediaFilter = MediaFilter.ALL,
    onFilterSelected: ((MediaFilter) -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    // Overlay detail viewer state
    selectedDetailIndex: Int? = null,
    onCloseDetail: () -> Unit = {},
    onTrashItem: (MediaItem) -> Unit = {},
    onRenameItem: (MediaItem, String) -> Unit = { _, _ -> },
    isFavorite: (Long) -> Boolean = { false },
    onToggleFavorite: (Long) -> Unit = {},
    // Multi-select state
    isSelectionMode: Boolean = false,
    selectedIds: Set<Long> = emptySet(),
    onStartSelection: (Long) -> Unit = {},
    onToggleSelection: (Long) -> Unit = {},
    onSetSelection: (Long, Boolean) -> Unit = { _, _ -> },
    onSetSelections: (Collection<Long>, Boolean) -> Unit = { _, _ -> },
    onUpdateSelectedIds: (Set<Long>) -> Unit = {},
    onSelectAll: () -> Unit = {},
    onDeselectAll: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onMoveClick: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    graphicsQuality: com.example.amoledgallery.PerformanceProfile = com.example.amoledgallery.PerformanceProfile.MEDIUM
) {
    // Intercept back button if in selection mode and detail overlay is not open
    if (isSelectionMode && selectedDetailIndex == null) {
        BackHandler {
            onClearSelection()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PureWhite)
            }
        } else if (items.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (onBackClick != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Zurück",
                                tint = PureWhite
                            )
                        }
                        Text(
                            text = title,
                            color = PureWhite,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                } else {
                    OneUiHeader(
                        mediaCount = 0,
                        title = title,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (onFilterSelected != null) {
                    FilterChips(
                        selectedFilter = selectedFilter,
                        onFilterSelected = onFilterSelected,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.empty_gallery),
                        color = TextMuted,
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            // Group items into date sections (Day for columns 2-3, Month for 4-5)
            val dateSections = remember(items, columnCount) {
                DateUtils.groupMediaByDate(items, columnCount)
            }

            // Preserved in memory permanently because GalleryScreen stays composed!
            val gridState = rememberLazyGridState()
            val coroutineScope = rememberCoroutineScope()

            // Calculate OneUI header collapse fraction (0f = expanded Large Title, 1f = compact Top Bar)
            val headerCollapseFraction by remember {
                derivedStateOf {
                    if (gridState.firstVisibleItemIndex > 0) {
                        1f
                    } else {
                        val scrollOffsetPx = gridState.firstVisibleItemScrollOffset.toFloat()
                        (scrollOffsetPx / 140f).coerceIn(0f, 1f)
                    }
                }
            }

            val firstVisibleIndex by remember { derivedStateOf { gridState.firstVisibleItemIndex } }
            val totalGridItems by remember { derivedStateOf { gridState.layoutInfo.totalItemsCount } }
            val scrollFraction by remember {
                derivedStateOf {
                    if (totalGridItems > 1) (firstVisibleIndex.toFloat() / (totalGridItems - 1)).coerceIn(0f, 1f) else 0f
                }
            }

            var scrubberDateText by remember { mutableStateOf("") }
            val haptic = LocalHapticFeedback.current
            val context = LocalContext.current

            // Compose's low-level long-press-drag detector (used for selection) and each tile's
            // plain `clickable` both observe the same raw touch stream. A press-then-release with
            // no movement is recognized by BOTH: the long-press already selects the item, and then
            // the tile's own clickable ALSO fires as a completed tap and immediately toggles it
            // back off - a genuine race, not something tied to any particular tile/position. This
            // records which item a long-press just (de)selected so the trailing click for that same
            // item can be ignored as the tail end of the same gesture rather than a fresh tap.
            var lastLongPressId by remember { mutableStateOf<Long?>(null) }
            var lastLongPressTime by remember { mutableLongStateOf(0L) }
            var isLongPressGestureActive by remember { mutableStateOf(false) }

            // Pause background thumbnail pre-indexing while actively flinging so it never competes
            // with visible-tile decode threads, resume it as soon as the grid settles.
            LaunchedEffect(gridState.isScrollInProgress) {
                val thumbnailManager = ThumbnailManager.getInstance(context)
                if (gridState.isScrollInProgress) {
                    thumbnailManager.pausePreindexing()
                } else {
                    thumbnailManager.resumePreindexing(items)
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(columnCount),
                state = gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .detectPinchToZoomGrid(
                        onZoomIn = onZoomIn,
                        onZoomOut = onZoomOut
                    )
                    .detectDragSelect(
                        items = items,
                        gridState = gridState,
                        isSelectionMode = { isSelectionMode },
                        onStartSelection = onStartSelection,
                        onSetSelection = onSetSelection,
                        onUpdateSelectedIds = onUpdateSelectedIds,
                        selectedIds = { selectedIds },
                        haptic = haptic,
                        coroutineScope = coroutineScope,
                        onLongPressRecognized = { id ->
                            lastLongPressId = id
                            isLongPressGestureActive = true
                        },
                        onLongPressEnded = {
                            lastLongPressTime = System.currentTimeMillis()
                            isLongPressGestureActive = false
                        }
                    ),
                contentPadding = PaddingValues(
                    start = 3.dp,
                    end = if (items.size > 20) 14.dp else 3.dp,
                    top = if (onBackClick != null) 60.dp else 0.dp,
                    bottom = 80.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Top Header: Generous OneUI Large Title + Filter Chips
                item(
                    span = { GridItemSpan(maxLineSpan) },
                    key = "grid_top_header",
                    contentType = "header"
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (onBackClick == null) {
                            OneUiHeader(
                                mediaCount = items.size,
                                title = title,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Horizontal row of filter chips ("Alle" | "Bilder" | "Videos" | "GIFs")
                        if (onFilterSelected != null) {
                            FilterChips(
                                selectedFilter = selectedFilter,
                                onFilterSelected = onFilterSelected,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Date sections with full-span headers and optimized media tiles
                dateSections.forEach { section ->
                    item(
                        span = { GridItemSpan(maxLineSpan) },
                        key = "header_${section.title}_$columnCount",
                        contentType = "date_header"
                    ) {
                        DateHeader(
                            title = section.title
                        )
                    }

                    // Keyed items with fixed dimensions, contentType="media_tile", and clipToBounds for zero-lag recycling
                    items(
                        items = section.items,
                        key = { it.id },
                        contentType = { "media_tile" }
                    ) { item ->
                        MediaGridItem(
                            item = item,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedIds.contains(item.id),
                            isFavorite = isFavorite(item.id),
                            onClick = {
                                // Suppressed for the entire duration of an active long-press-drag on
                                // this same item (covers holds of any length), plus a short grace
                                // period after it ends as a backstop in case the child's tap and the
                                // parent's onDragEnd/onDragCancel resolve in the opposite order.
                                val isTrailingClickFromLongPress = item.id == lastLongPressId &&
                                    (isLongPressGestureActive || (System.currentTimeMillis() - lastLongPressTime) < 600)
                                if (isTrailingClickFromLongPress) {
                                    // Same press-release the long-press gesture already handled -
                                    // ignore it instead of letting the tile's own tap detector
                                    // immediately toggle the selection it just made back off.
                                } else if (isSelectionMode) {
                                    onToggleSelection(item.id)
                                } else {
                                    val index = items.indexOf(item)
                                    onItemClick(item, index)
                                }
                            },
                            modifier = Modifier.clipToBounds()
                        )
                    }
                }
            }

            // Normal OneUI Pinned Top-Bar (Visible when not in selection mode)
            if (!isSelectionMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .drawBehind {
                            val alpha = if (onBackClick != null) 1f else headerCollapseFraction
                            drawRect(AmoledBlack.copy(alpha = alpha))
                        }
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onBackClick != null) {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Zurück",
                                    tint = PureWhite
                                )
                            }
                        }

                        // Compact title fading in smoothly as large title shrinks/scrolls
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = if (onBackClick != null) 4.dp else 12.dp)
                                .graphicsLayer {
                                    alpha = if (onBackClick != null) 1f else (headerCollapseFraction - 0.2f).coerceAtLeast(0f) / 0.8f
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                color = PureWhite,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            if (items.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "(${items.size})",
                                    color = TextMuted,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Subtle OneUI divider hairline fading in when collapsed
                    HorizontalDivider(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .graphicsLayer {
                                alpha = if (onBackClick != null) 1f else headerCollapseFraction
                            },
                        thickness = 0.5.dp,
                        color = Color(0xFF1E1E1E)
                    )
                }
            } else {
                // OneUI Selection Mode Top Bar: Count, Select All, and Move to Album
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .statusBarsPadding(),
                    color = AmoledBlack,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClearSelection) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Auswahl beenden",
                                tint = PureWhite
                            )
                        }

                        Text(
                            text = if (selectedIds.isEmpty()) "Elemente auswählen" else "${selectedIds.size} ausgewählt",
                            color = PureWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        )

                        // Select all / deselect all
                        TextButton(
                            onClick = {
                                if (selectedIds.size == items.size) {
                                    onDeselectAll()
                                } else {
                                    onSelectAll()
                                }
                            }
                        ) {
                            Text(
                                text = if (selectedIds.size == items.size) "Keine" else "Alle",
                                color = PureWhite,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        // Move to album button
                        IconButton(
                            onClick = onMoveClick,
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.DriveFileMove,
                                contentDescription = "In Album verschieben",
                                tint = if (selectedIds.isNotEmpty()) PureWhite else TextMuted
                            )
                        }

                        // Batch delete button
                        IconButton(
                            onClick = onDeleteSelected,
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "In Papierkorb verschieben",
                                tint = if (selectedIds.isNotEmpty()) PureWhite else TextMuted
                            )
                        }
                    }
                }
            }

            // Draggable Fast-Scroll Scrubber with Month & Year pill
            if (items.size > 20 && !isSelectionMode) {
                FastScroller(
                    scrollFraction = { scrollFraction },
                    indicatorText = scrubberDateText,
                    onScrollProgress = { fraction ->
                        val targetIndex = (fraction * (items.size - 1)).roundToInt().coerceIn(0, items.size - 1)
                        scrubberDateText = DateUtils.formatMonthYear(items[targetIndex].dateAdded)
                        val gridIndex = getGridItemIndexForMedia(dateSections, targetIndex)
                        coroutineScope.launch {
                            gridState.scrollToItem(gridIndex)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(top = 90.dp, bottom = 80.dp)
                )
            }
        }

        // FULLSCREEN OVERLAY: Media Detail Viewer
        // Overlaid directly onto the LazyVerticalGrid without recreating GalleryScreen.
        // Preserves LazyGridState scroll position down to the exact pixel when dismissed!
        AnimatedVisibility(
            visible = selectedDetailIndex != null,
            enter = fadeIn(animationSpec = qualityTween(graphicsQuality, 200)),
            exit = fadeOut(animationSpec = qualityTween(graphicsQuality, 200))
        ) {
            selectedDetailIndex?.let { index ->
                MediaDetailScreen(
                    mediaList = items,
                    initialIndex = index,
                    onBack = onCloseDetail,
                    onTrashItem = onTrashItem,
                    onRenameItem = onRenameItem,
                    isFavorite = isFavorite,
                    onToggleFavorite = onToggleFavorite
                )
            }
        }
    }
}

/**
 * Calculates the exact index in LazyVerticalGrid for a given media index,
 * accounting for the main header and all preceding date headers.
 */
private fun getGridItemIndexForMedia(
    dateSections: List<MediaDateSection>,
    mediaIndex: Int
): Int {
    var accumulatedMedia = 0
    var gridIndex = 1 // 1 for the main top header
    for (section in dateSections) {
        if (mediaIndex < accumulatedMedia + section.items.size) {
            val offsetInSection = mediaIndex - accumulatedMedia
            return gridIndex + 1 + offsetInSection // +1 for the section's DateHeader
        }
        accumulatedMedia += section.items.size
        gridIndex += 1 + section.items.size
    }
    return gridIndex.coerceAtLeast(0)
}

/**
 * Detects Samsung Gallery style continuous block drag-to-select:
 * - Starts immediately upon long-pressing an initial media tile.
 * - Continuous index range selection: dragging from item A to item B selects all items between A and B
 *   (the entire row and everything in between, matching standard Samsung Gallery / Google Photos behavior).
 * - Dragging backwards automatically deselects items back to the current position.
 * - Automatically scrolls smoothly when dragging near top/bottom margins.
 */
private fun Modifier.detectDragSelect(
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

    fun findMediaIndexAt(pos: Offset): Int? {
        val x = pos.x.toInt()
        val y = pos.y.toInt()
        val visible = gridState.layoutInfo.visibleItemsInfo
        if (visible.isEmpty()) return null

        // 1. Exact item hit test
        val exactHit = visible.find { info ->
            info.key is Long &&
            x >= info.offset.x && x <= (info.offset.x + info.size.width) &&
            y >= info.offset.y && y <= (info.offset.y + info.size.height)
        }
        if (exactHit?.key is Long) {
            val idx = idToIndex[exactHit.key as Long]
            if (idx != null) return idx
        }

        // 2. Closest visible media tile center test
        var closestIdx: Int? = null
        var minDistanceSq = Float.MAX_VALUE
        for (info in visible) {
            if (info.key !is Long) continue
            val centerX = info.offset.x + info.size.width / 2f
            val centerY = info.offset.y + info.size.height / 2f
            val dx = pos.x - centerX
            val dy = pos.y - centerY
            val distSq = dx * dx + dy * dy
            if (distSq < minDistanceSq) {
                minDistanceSq = distSq
                closestIdx = idToIndex[info.key as Long]
            }
        }
        return closestIdx
    }

    fun updateRangeSelection(currentIdx: Int) {
        val startIdx = dragStartIndex ?: return
        val minIdx = minOf(startIdx, currentIdx).coerceIn(0, items.size - 1)
        val maxIdx = maxOf(startIdx, currentIdx).coerceIn(0, items.size - 1)
        val currentRange = minIdx..maxIdx

        if (currentRange != lastProcessedRange) {
            lastProcessedRange = currentRange
            val rangeIds = (minIdx..maxIdx).map { items[it].id }.toSet()
            val newSelection = if (dragSelectMode) {
                initialSelectedIds + rangeIds
            } else {
                initialSelectedIds - rangeIds
            }
            onUpdateSelectedIds(newSelection)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    detectDragGesturesAfterLongPress(
        onDragStart = { offset ->
            idToIndex = items.mapIndexed { idx, item -> item.id to idx }.toMap()
            currentTouch = offset
            initialSelectedIds = selectedIds().toSet()
            lastProcessedRange = null

            val startIdx = findMediaIndexAt(offset)
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
            dragStartPosition = offset
            // Auto-scroll and range re-evaluation are intentionally NOT started here: onDragStart
            // fires purely from a long-press with zero finger movement. Starting the timer-driven
            // auto-scroll loop immediately meant that simply holding still on a tile sitting inside
            // the top/bottom scroll margin could re-evaluate the range selection on every tick with
            // no drag at all, occasionally toggling the very first selected item back off. Both are
            // gated on real net displacement from the long-press origin in onDrag instead - NOT on
            // "onDrag fired at all", and NOT on a running sum of per-tick deltas: some input sources
            // (and a real finger's sensor noise, given enough time) report many tiny non-zero moves
            // while effectively held still, and a running sum keeps growing the longer you hold even
            // though the finger never actually travels anywhere - comparing against the fixed start
            // position instead stays near-zero for as long as the hold is genuinely stationary.
        },
        onDrag = { change, _ ->
            change.consume()
            currentTouch = change.position

            // Ignore everything until the finger has actually moved away from where the long-press
            // started (beyond a tiny touch-slop threshold).
            if ((currentTouch - dragStartPosition).getDistance() > 8f) {
                val curIdx = findMediaIndexAt(currentTouch)
                if (curIdx != null) {
                    updateRangeSelection(curIdx)
                }

                if (autoScrollJob == null) {
                    autoScrollJob = coroutineScope.launch {
                        val scrollMarginPx = 100.dp.toPx()
                        while (isActive) {
                            val viewportH = gridState.layoutInfo.viewportSize.height.toFloat()
                            if (viewportH > 0 && dragStartIndex != null) {
                                val scrollAmount = when {
                                    currentTouch.y < scrollMarginPx -> {
                                        -((scrollMarginPx - currentTouch.y) / scrollMarginPx).coerceIn(0f, 1f) * 24f
                                    }
                                    currentTouch.y > viewportH - scrollMarginPx -> {
                                        ((currentTouch.y - (viewportH - scrollMarginPx)) / scrollMarginPx).coerceIn(0f, 1f) * 24f
                                    }
                                    else -> 0f
                                }

                                if (scrollAmount != 0f) {
                                    gridState.scrollBy(scrollAmount)
                                    val scrolledIdx = findMediaIndexAt(currentTouch)
                                    if (scrolledIdx != null) {
                                        updateRangeSelection(scrolledIdx)
                                    }
                                }
                            }
                            delay(16)
                        }
                    }
                }
            }
        },
        onDragEnd = {
            autoScrollJob?.cancel()
            autoScrollJob = null
            dragStartIndex = null
            lastProcessedRange = null
            onLongPressEnded()
        },
        onDragCancel = {
            autoScrollJob?.cancel()
            autoScrollJob = null
            dragStartIndex = null
            lastProcessedRange = null
            onLongPressEnded()
        }
    )
}

