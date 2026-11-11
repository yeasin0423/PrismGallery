package com.gallery.app.ui.screens
import com.gallery.app.ui.i18n.LanguageState
import com.gallery.app.ui.i18n.displayLabel
import com.gallery.app.ui.i18n.strings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.app.data.ThumbnailManager
import com.gallery.app.model.MediaItem
import com.gallery.app.ui.components.DateHeader
import com.gallery.app.ui.components.FastScroller
import com.gallery.app.ui.components.FilterChips
import com.gallery.app.ui.components.LargeHeader
import com.gallery.app.ui.components.MediaGridItem
import com.gallery.app.ui.components.detectPinchToZoomGrid
import com.gallery.app.ui.components.SelectionTopBar
import com.gallery.app.ui.components.OneUiSelectAll
import com.gallery.app.ui.components.detectDragSelect
import com.gallery.app.ui.design.MenuEntry
import com.gallery.app.ui.design.ThemedIconButton
import com.gallery.app.ui.design.ThemedPopupMenu
import com.gallery.app.ui.design.ThemedTopBar
import com.gallery.app.ui.design.bottomBarContentPadding
import com.gallery.app.ui.design.glass
import com.gallery.app.ui.theme.LocalAppTheme
import com.gallery.app.ui.viewmodel.MediaFilter
import com.gallery.app.util.DateUtils
import dev.chrisbanes.haze.HazeState
import com.gallery.app.ui.design.HazeSource
import com.gallery.app.ui.design.MediaZoomTransition


/**
 * The "Bilder" media grid in One UI style: Samsung Gallery's expanded app bar (big centred title in the
 * upper third), a frosted pinned bar once scrolled, filters in the ⋮ popup, and One UI 9's floating
 * frosted selection card.
 *
 * Grid behaviour (pinch zoom, long-press drag selection, fast scroller, detail overlay that keeps the
 * LazyGridState intact) is shared by every entry point.
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
    title: String = strings.tabPictures,
    selectedFilter: MediaFilter = MediaFilter.ALL,
    onFilterSelected: ((MediaFilter) -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    // Overlay detail viewer state
    selectedDetailIndex: Int? = null,
    onCloseDetail: () -> Unit = {},
    onDetailIndexChanged: (Int) -> Unit = {},
    onTrashItem: (MediaItem) -> Unit = {},
    onRenameItem: (MediaItem, String) -> Unit = { _, _ -> },
    isFavorite: (Long) -> Boolean = { false },
    onToggleFavorite: (Long) -> Unit = {},
    // Multi-select state
    isSelectionMode: Boolean = false,
    selectedIds: Set<Long> = emptySet(),
    onStartSelection: (Long) -> Unit = {},
    /** Enters selection mode with nothing selected yet. */
    onEnterSelection: () -> Unit = {},
    onToggleSelection: (Long) -> Unit = {},
    onSetSelection: (Long, Boolean) -> Unit = { _, _ -> },
    onSetSelections: (Collection<Long>, Boolean) -> Unit = { _, _ -> },
    onUpdateSelectedIds: (Set<Long>) -> Unit = {},
    onSelectAll: () -> Unit = {},
    onDeselectAll: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    // Move / delete for a selection live in the SelectionActionBar that replaces the tab bar.
    onMoveClick: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    graphicsQuality: com.gallery.app.PerformanceProfile = com.gallery.app.PerformanceProfile.MEDIUM,
    onOpenSettings: () -> Unit = {}
) {
    // Intercept back button if in selection mode and detail overlay is not open
    if (isSelectionMode && selectedDetailIndex == null) {
        BackHandler {
            onClearSelection()
        }
    }

    val theme = LocalAppTheme.current
    val palette = theme.palette
    val displayItems = items
    val screenTitle = title
    val countText = strings.items(items.size)
    // While the fullscreen viewer covers everything, the grid below it is not drawn at all.
    var gridCoveredByViewer by remember { mutableStateOf(false) }

    // Changing the filter lets the new tiles rise and fade in, so the switch is visibly acknowledged
    // instead of the grid being swapped out in a single frame. Drawn in graphicsLayer: no recomposition.
    val filterAnim = remember { androidx.compose.animation.core.Animatable(1f) }
    val lastFilter = remember { arrayOf(selectedFilter) }
    LaunchedEffect(selectedFilter) {
        if (lastFilter[0] != selectedFilter) {
            lastFilter[0] = selectedFilter
            filterAnim.snapTo(0f)
            filterAnim.animateTo(
                1f,
                androidx.compose.animation.core.tween(340, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            )
        }
    }
    val filterSwitchModifier = Modifier.graphicsLayer {
        val p = filterAnim.value
        alpha = 0.2f + 0.8f * p
        translationY = (1f - p) * 18.dp.toPx()
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = palette.accent)
            }
        } else if (items.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (onBackClick != null) {
                    ThemedTopBar(title = screenTitle, onBack = onBackClick)
                } else {
                    LargeHeader(title = screenTitle)
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
                        text = strings.emptyGallery,
                        color = palette.textMuted,
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            // Group items into date sections (Day for columns 2-3, Month for 4-5).
            val dateSections = remember(displayItems, columnCount, LanguageState.language) {
                DateUtils.groupMediaByDate(displayItems, columnCount)
            }

            val gridSpacing = theme.shapes.gridSpacing
            val gridPadding = theme.shapes.gridHorizontalPadding

            val gridState = remember { LazyGridState() }
            val coroutineScope = rememberCoroutineScope()
            val density = LocalDensity.current

            // 0f = expanded large header, 1f = scrolled far enough that the compact bar takes over.
            val collapseDistancePx = with(density) { 220.dp.toPx() }
            val headerCollapseFraction by remember(collapseDistancePx) {
                derivedStateOf {
                    if (gridState.firstVisibleItemIndex > 0) {
                        1f
                    } else {
                        (gridState.firstVisibleItemScrollOffset / collapseDistancePx).coerceIn(0f, 1f)
                    }
                }
            }
            val showPinnedBar by remember { derivedStateOf { headerCollapseFraction > 0.25f } }

            val haptic = LocalHapticFeedback.current
            val context = LocalContext.current

            // Backdrop for this screen's own top bars (they sit inside the tab content, so they can't blur
            // the tab-level backdrop they are part of).
            val gridHaze = remember { HazeState() }
            var viewMenuOpen by remember { mutableStateOf(false) }
            var viewMenuAnchor by remember { mutableStateOf<Rect?>(null) }

            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val bottomPadding = bottomBarContentPadding(extra = 12.dp)

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
            // Observed outside composition, so starting and stopping a fling doesn't recompose the screen.
            LaunchedEffect(gridState, items) {
                val thumbnailManager = ThumbnailManager.getInstance(context)
                androidx.compose.runtime.snapshotFlow { gridState.isScrollInProgress }.collect { scrolling ->
                    if (scrolling) {
                        thumbnailManager.pausePreindexing()
                    } else {
                        thumbnailManager.resumePreindexing(items)
                    }
                }
            }

            val openViewMenu: (Rect?) -> Unit = { anchor ->
                viewMenuAnchor = anchor
                viewMenuOpen = true
            }

            HazeSource(
                gridHaze,
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (gridCoveredByViewer) 0f else 1f }
            ) {
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
                            items = displayItems,
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
                        start = gridPadding,
                        end = gridPadding,
                        top = if (onBackClick != null) statusBarTop + 64.dp else 0.dp,
                        bottom = bottomPadding
                    ),
                    horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                    verticalArrangement = Arrangement.spacedBy(gridSpacing)
                ) {
                    // Index 0 is always this header item - getGridItemIndexForMedia relies on it.
                    item(
                        span = { GridItemSpan(maxLineSpan) },
                        key = "grid_top_header",
                        contentType = "header"
                    ) {
                        if (onBackClick == null) {
                            GalleryHeader(
                                title = screenTitle,
                                collapseFraction = { headerCollapseFraction },
                                onOpenViewMenu = openViewMenu,
                                hazeState = gridHaze,
                                isSelectionMode = isSelectionMode,
                                selectedCount = selectedIds.size,
                                allSelected = items.isNotEmpty() && selectedIds.size == items.size,
                                onToggleSelectAll = { if (items.isNotEmpty() && selectedIds.size == items.size) onDeselectAll() else onSelectAll() },
                                onCancelSelection = onClearSelection
                            )
                        } else {
                            Spacer(modifier = Modifier.height(0.dp))
                        }
                    }

                    // Date sections with full-span headers and optimized media tiles
                    dateSections.forEach { section ->
                        if (section.title.isNotEmpty()) {
                            item(
                                span = { GridItemSpan(maxLineSpan) },
                                key = "header_${section.title}_$columnCount",
                                contentType = "date_header"
                            ) {
                                // Each day can be selected or deselected as a whole while selecting.
                                val sectionSelectable = isSelectionMode
                                val sectionAllSelected = sectionSelectable && section.items.all { selectedIds.contains(it.id) }
                                DateHeader(
                                    title = section.title,
                                    modifier = filterSwitchModifier,
                                    actionText = if (!sectionSelectable) null else if (sectionAllSelected) strings.deselectAllItems else strings.selectAllItems,
                                    onAction = { onSetSelections(section.items.map { it.id }, !sectionAllSelected) },
                                    actionChecked = sectionAllSelected
                                )
                            }
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
                                        onItemClick(item, displayItems.indexOf(item))
                                    }
                                },
                                modifier = Modifier
                                    .clipToBounds()
                                    .then(filterSwitchModifier)
                            )
                        }
                    }
                }
            }

            val allSelected = items.isNotEmpty() && selectedIds.size == items.size
            val toggleSelectAll = { if (allSelected) onDeselectAll() else onSelectAll() }

            if (!isSelectionMode) {
                when {
                    onBackClick != null -> ThemedTopBar(
                        title = screenTitle,
                        subtitle = countText,
                        onBack = onBackClick,
                        hazeState = gridHaze,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )

                    showPinnedBar -> ThemedTopBar(
                        title = screenTitle,
                        hazeState = gridHaze,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .graphicsLayer { alpha = ((headerCollapseFraction - 0.25f) / 0.75f).coerceIn(0f, 1f) }
                    ) {
                        var anchor by remember { mutableStateOf<Rect?>(null) }
                        ThemedIconButton(
                            icon = Icons.Rounded.MoreVert,
                            contentDescription = strings.options,
                            onClick = { openViewMenu(anchor) },
                            hazeState = gridHaze,
                            modifier = Modifier.onGloballyPositioned { anchor = it.boundsInRoot() }
                        )
                    }
                }
            } else if (showPinnedBar && onBackClick == null) {
                // One UI shows the selection in the big header; this compact bar only takes over once scrolled.
                SelectionTopBar(
                    selectedCount = selectedIds.size,
                    totalCount = items.size,
                    onToggleSelectAll = toggleSelectAll,
                    onClose = onClearSelection,
                    hazeState = gridHaze,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            alpha = ((headerCollapseFraction - 0.25f) / 0.75f).coerceIn(0f, 1f)
                        }
                )
            }

            ThemedPopupMenu(
                expanded = viewMenuOpen,
                onDismissRequest = { viewMenuOpen = false },
                anchorBounds = viewMenuAnchor,
                entries = buildList {
                    add(MenuEntry(strings.select) { onEnterSelection() })
                    if (onFilterSelected != null) {
                        MediaFilter.entries.forEach { filter ->
                            add(MenuEntry(filter.displayLabel, checked = filter == selectedFilter) { onFilterSelected(filter) })
                        }
                    }
                    add(MenuEntry(strings.largerThumbnails, icon = Icons.Rounded.ZoomIn) { onZoomIn() })
                    add(MenuEntry(strings.smallerThumbnails, icon = Icons.Rounded.ZoomOut) { onZoomOut() })
                    add(MenuEntry(strings.settings, icon = Icons.Rounded.Settings) { onOpenSettings() })
                }
            )

            // Draggable Fast-Scroll Scrubber with Month & Year pill
            if (items.size > 20 && !isSelectionMode) {
                // Grid index -> media shown there (date headers map to their section's first item), for the
                // scrubber's month pill.
                val gridMedia = remember(dateSections) {
                    buildList<MediaItem?> {
                        add(null)
                        dateSections.forEach { section ->
                            if (section.title.isNotEmpty()) add(section.items.firstOrNull())
                            addAll(section.items)
                        }
                    }
                }
                FastScroller(
                    gridState = gridState,
                    labelForGridIndex = { index ->
                        (gridMedia.getOrNull(index) ?: gridMedia.getOrNull(index + 1))
                            ?.let { DateUtils.formatMonthYear(it.date) } ?: ""
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(top = statusBarTop + 80.dp, bottom = bottomPadding)
                )
            }
        }

        // FULLSCREEN OVERLAY: Media Detail Viewer
        // Overlaid directly onto the LazyVerticalGrid without recreating GalleryScreen.
        // Preserves LazyGridState scroll position down to the exact pixel when dismissed!
        val detailIndexHolder = remember { IntArray(1) }
        selectedDetailIndex?.let { detailIndexHolder[0] = it }
        MediaZoomTransition(
            visible = selectedDetailIndex != null,
            onCoveringChange = { gridCoveredByViewer = it }
        ) {
                MediaDetailScreen(
                    mediaList = displayItems,
                    initialIndex = detailIndexHolder[0],
                    onBack = onCloseDetail,
                    onTrashItem = onTrashItem,
                    onRenameItem = onRenameItem,
                    isFavorite = isFavorite,
                    onToggleFavorite = onToggleFavorite,
                    onPageChanged = onDetailIndexChanged
                )
        }
    }
}

/** The scrolling header at the top of the grid (index 0). */
@Composable
private fun GalleryHeader(
    title: String,
    collapseFraction: () -> Float,
    onOpenViewMenu: (Rect?) -> Unit,
    hazeState: HazeState,
    isSelectionMode: Boolean = false,
    selectedCount: Int = 0,
    allSelected: Boolean = false,
    onToggleSelectAll: () -> Unit = {},
    onCancelSelection: () -> Unit = {}
) {
    var anchor by remember { mutableStateOf<Rect?>(null) }
    val menuButton: @Composable RowScope.() -> Unit = {
        ThemedIconButton(
            icon = Icons.Rounded.MoreVert,
            contentDescription = strings.options,
            onClick = { onOpenViewMenu(anchor) },
            hazeState = hazeState,
            modifier = Modifier.onGloballyPositioned { anchor = it.boundsInRoot() }
        )
    }

    // One UI selection: the compact title becomes "14 selected", with All and Cancel on the right.
    if (isSelectionMode) {
        val palette = LocalAppTheme.current.palette
        LargeHeader(
            title = strings.selectionTitle(selectedCount),
            collapseFraction = collapseFraction,
            actions = {
                OneUiSelectAll(checked = allSelected, onClick = onToggleSelectAll, modifier = Modifier.padding(start = 12.dp))
                Text(
                    text = strings.cancel,
                    color = palette.onBackground,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onCancelSelection)
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        )
    } else {
        LargeHeader(
            title = title,
            collapseFraction = collapseFraction,
            actions = menuButton
        )
    }
}

