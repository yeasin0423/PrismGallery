package com.example.amoledgallery.ui.screens
import com.example.amoledgallery.ui.i18n.LanguageState
import com.example.amoledgallery.ui.i18n.displayLabel
import com.example.amoledgallery.ui.i18n.strings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amoledgallery.R
import com.example.amoledgallery.data.ThumbnailManager
import com.example.amoledgallery.model.MediaDateSection
import com.example.amoledgallery.model.MediaItem
import com.example.amoledgallery.ui.components.DateHeader
import com.example.amoledgallery.ui.components.ExpressiveHeroCard
import com.example.amoledgallery.ui.components.FastScroller
import com.example.amoledgallery.ui.components.FilterChips
import com.example.amoledgallery.ui.components.LargeHeader
import com.example.amoledgallery.ui.components.MediaGridItem
import com.example.amoledgallery.ui.components.detectPinchToZoomGrid
import com.example.amoledgallery.ui.components.SelectionTopBar
import com.example.amoledgallery.ui.components.detectDragSelect
import com.example.amoledgallery.ui.design.GlassRim
import com.example.amoledgallery.ui.design.GlassSpec
import com.example.amoledgallery.ui.design.LocalContentHaze
import com.example.amoledgallery.ui.design.MenuEntry
import com.example.amoledgallery.ui.design.ThemedIconButton
import com.example.amoledgallery.ui.design.ThemedPopupMenu
import com.example.amoledgallery.ui.design.ThemedTopBar
import com.example.amoledgallery.ui.design.bottomBarContentPadding
import com.example.amoledgallery.ui.design.glass
import com.example.amoledgallery.ui.design.pressScale
import com.example.amoledgallery.ui.qualityTween
import com.example.amoledgallery.ui.theme.DesignLanguage
import com.example.amoledgallery.ui.theme.LocalAppTheme
import com.example.amoledgallery.ui.theme.isLight
import com.example.amoledgallery.ui.theme.onAccent
import com.example.amoledgallery.ui.viewmodel.MediaFilter
import com.example.amoledgallery.util.DateUtils
import dev.chrisbanes.haze.HazeState
import com.example.amoledgallery.ui.design.HazeSource
import com.example.amoledgallery.ui.design.MediaZoomTransition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


/**
 * The "Bilder" media grid, laid out per design language:
 * - One UI: Samsung Gallery's expanded app bar (big centred title in the upper third), a frosted
 *   pinned bar once scrolled, filters in the ⋮ popup, and One UI 9's floating frosted selection card.
 * - Material 3 Expressive: headline, a hero card with the newest photo, filter chips, rounded tiles.
 * - iOS: photos run edge-to-edge under a progressive glass title overlay, the filter floats as a
 *   glass segmented control above the tab bar.
 * - HyperOS: light layout, text-tab filters, compact frosted bars.
 *
 * Grid behaviour (pinch zoom, long-press drag selection, fast scroller, detail overlay that keeps the
 * LazyGridState intact) is shared by all of them.
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
    graphicsQuality: com.example.amoledgallery.PerformanceProfile = com.example.amoledgallery.PerformanceProfile.MEDIUM
) {
    // Intercept back button if in selection mode and detail overlay is not open
    if (isSelectionMode && selectedDetailIndex == null) {
        BackHandler {
            onClearSelection()
        }
    }

    val theme = LocalAppTheme.current
    val palette = theme.palette
    val language = theme.language
    val countText = strings.items(items.size)
    // While the fullscreen viewer covers everything, the grid below it is not drawn at all.
    var gridCoveredByViewer by remember { mutableStateOf(false) }

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
                    ThemedTopBar(title = title, onBack = onBackClick)
                } else {
                    LargeHeader(title = title)
                }

                if (onFilterSelected != null) {
                    FilterChips(
                        selectedFilter = selectedFilter,
                        onFilterSelected = onFilterSelected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (language == DesignLanguage.IOS_LIQUID_GLASS) 20.dp else 0.dp)
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
            // Group items into date sections (Day for columns 2-3, Month for 4-5)
            val dateSections = remember(items, columnCount, LanguageState.language) {
                DateUtils.groupMediaByDate(items, columnCount)
            }

            val gridSpacing = theme.shapes.gridSpacing
            val gridPadding = theme.shapes.gridHorizontalPadding

            // Preserved in memory permanently because GalleryScreen stays composed!
            val gridState = rememberLazyGridState()
            val coroutineScope = rememberCoroutineScope()
            val density = LocalDensity.current

            // 0f = expanded large header, 1f = scrolled far enough that the compact bar takes over.
            val collapseDistancePx = with(density) {
                (if (language == DesignLanguage.SAMSUNG_ONEUI) 220.dp else 110.dp).toPx()
            }
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

            // Backdrop for this screen's own top bars and floating filter (they sit inside the tab
            // content, so they can't blur the tab-level backdrop they are part of).
            val gridHaze = remember { HazeState() }
            var viewMenuOpen by remember { mutableStateOf(false) }
            var viewMenuAnchor by remember { mutableStateOf<Rect?>(null) }

            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val iosHeaderHeight = statusBarTop + 96.dp
            val showFloatingFilter = language == DesignLanguage.IOS_LIQUID_GLASS && onFilterSelected != null
            val bottomPadding = bottomBarContentPadding(extra = if (showFloatingFilter) 62.dp else 12.dp)

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
                        start = gridPadding,
                        end = gridPadding,
                        top = when {
                            onBackClick != null -> statusBarTop + 64.dp
                            language == DesignLanguage.IOS_LIQUID_GLASS -> iosHeaderHeight
                            else -> 0.dp
                        },
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
                                title = title,
                                countText = countText,
                                items = items,
                                language = language,
                                collapseFraction = { headerCollapseFraction },
                                selectedFilter = selectedFilter,
                                onFilterSelected = onFilterSelected,
                                onOpenViewMenu = openViewMenu,
                                onHeroClick = { onItemClick(items.first(), 0) },
                                hazeState = gridHaze
                            )
                        } else {
                            Spacer(modifier = Modifier.height(0.dp))
                        }
                    }

                    // Date sections with full-span headers and optimized media tiles
                    dateSections.forEach { section ->
                        item(
                            span = { GridItemSpan(maxLineSpan) },
                            key = "header_${section.title}_$columnCount",
                            contentType = "date_header"
                        ) {
                            DateHeader(title = section.title)
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
            }

            val allSelected = selectedIds.size == items.size
            val toggleSelectAll = { if (allSelected) onDeselectAll() else onSelectAll() }

            if (!isSelectionMode) {
                when {
                    onBackClick != null -> ThemedTopBar(
                        title = title,
                        subtitle = countText,
                        onBack = onBackClick,
                        hazeState = gridHaze,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )

                    language == DesignLanguage.IOS_LIQUID_GLASS -> IosTitleOverlay(
                        title = title,
                        subtitle = countText,
                        height = iosHeaderHeight,
                        hazeState = gridHaze,
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        var anchor by remember { mutableStateOf<Rect?>(null) }
                        ThemedIconButton(
                            icon = Icons.Rounded.MoreHoriz,
                            contentDescription = strings.options,
                            onClick = { openViewMenu(anchor) },
                            hazeState = gridHaze,
                            modifier = Modifier.onGloballyPositioned { anchor = it.boundsInRoot() }
                        )
                    }

                    showPinnedBar -> ThemedTopBar(
                        title = title,
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
            } else {
                SelectionTopBar(
                    selectedCount = selectedIds.size,
                    totalCount = items.size,
                    onToggleSelectAll = toggleSelectAll,
                    onClose = onClearSelection,
                    hazeState = gridHaze,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            // iOS: the filter is a glass segmented control floating above the tab bar.
            AnimatedVisibility(
                visible = showFloatingFilter && !isSelectionMode,
                enter = fadeIn(qualityTween(graphicsQuality, 200)),
                exit = fadeOut(qualityTween(graphicsQuality, 150)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomBarContentPadding(extra = 6.dp))
                    .padding(horizontal = 44.dp)
                    .widthIn(max = 360.dp)
            ) {
                if (onFilterSelected != null) {
                    CompositionLocalProvider(LocalContentHaze provides gridHaze) {
                        FilterChips(
                            selectedFilter = selectedFilter,
                            onFilterSelected = onFilterSelected,
                            floatingGlass = true
                        )
                    }
                }
            }

            ThemedPopupMenu(
                expanded = viewMenuOpen,
                onDismissRequest = { viewMenuOpen = false },
                anchorBounds = viewMenuAnchor,
                entries = buildList {
                    if (onFilterSelected != null && language == DesignLanguage.SAMSUNG_ONEUI) {
                        MediaFilter.entries.forEach { filter ->
                            add(MenuEntry(filter.displayLabel, checked = filter == selectedFilter) { onFilterSelected(filter) })
                        }
                    }
                    add(MenuEntry(strings.largerThumbnails, icon = Icons.Rounded.ZoomIn) { onZoomIn() })
                    add(MenuEntry(strings.smallerThumbnails, icon = Icons.Rounded.ZoomOut) { onZoomOut() })
                }
            )

            // Draggable Fast-Scroll Scrubber with Month & Year pill
            if (items.size > 20 && !isSelectionMode) {
                FastScroller(
                    scrollFraction = { scrollFraction },
                    indicatorText = scrubberDateText,
                    onScrollProgress = { fraction ->
                        val targetIndex = (fraction * (items.size - 1)).roundToInt().coerceIn(0, items.size - 1)
                        scrubberDateText = DateUtils.formatMonthYear(items[targetIndex].date)
                        val gridIndex = getGridItemIndexForMedia(dateSections, targetIndex)
                        coroutineScope.launch {
                            gridState.scrollToItem(gridIndex)
                        }
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
                    mediaList = items,
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

/** The scrolling header at the top of the grid (index 0), per design language. */
@Composable
private fun GalleryHeader(
    title: String,
    countText: String,
    items: List<MediaItem>,
    language: DesignLanguage,
    collapseFraction: () -> Float,
    selectedFilter: MediaFilter,
    onFilterSelected: ((MediaFilter) -> Unit)?,
    onOpenViewMenu: (Rect?) -> Unit,
    onHeroClick: () -> Unit,
    hazeState: HazeState
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

    when (language) {
        DesignLanguage.SAMSUNG_ONEUI -> LargeHeader(
            title = title,
            collapseFraction = collapseFraction,
            actions = menuButton
        )

        DesignLanguage.GOOGLE_EXPRESSIVE -> Column(modifier = Modifier.fillMaxWidth()) {
            LargeHeader(title = title, subtitle = countText, actions = menuButton)
            ExpressiveHeroCard(
                imageModel = items.firstOrNull()?.uri,
                title = DateUtils.formatMonthYear(items.first().date),
                caption = strings.latestShot,
                onClick = onHeroClick
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (onFilterSelected != null) {
                FilterChips(selectedFilter = selectedFilter, onFilterSelected = onFilterSelected)
            }
        }

        DesignLanguage.XIAOMI_HYPEROS -> Column(modifier = Modifier.fillMaxWidth()) {
            LargeHeader(title = title, subtitle = countText, actions = menuButton)
            if (onFilterSelected != null) {
                FilterChips(selectedFilter = selectedFilter, onFilterSelected = onFilterSelected)
            }
        }

        // iOS draws its title as a fixed glass overlay instead (IosTitleOverlay).
        DesignLanguage.IOS_LIQUID_GLASS -> Spacer(modifier = Modifier.height(0.dp))
    }
}

/**
 * iOS 26 Photos title: pinned over the photos, which scroll underneath a progressive glass blur that
 * is fully frosted at the top and fades out towards the grid, plus a legibility gradient.
 */
@Composable
private fun IosTitleOverlay(
    title: String,
    subtitle: String,
    height: Dp,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit
) {
    val palette = LocalAppTheme.current.palette
    val light = palette.isLight
    val shade = if (light) Color.White else Color.Black
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height + 28.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .glass(
                    RectangleShape,
                    GlassSpec(shade.copy(alpha = 0.22f), Color.Transparent, 14.dp, GlassRim.None),
                    hazeState,
                    mask = Brush.verticalGradient(0f to Color.Black, 0.6f to Color.Black, 1f to Color.Transparent)
                )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to shade.copy(alpha = if (light) 0.80f else 0.62f),
                        0.65f to shade.copy(alpha = if (light) 0.35f else 0.25f),
                        1f to Color.Transparent
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, end = 150.dp, bottom = 34.dp)
        ) {
            Text(
                text = title,
                color = palette.onBackground,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = subtitle,
                color = palette.onBackground.copy(alpha = 0.72f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}

@Composable
private fun GlassTextButton(text: String, hazeState: HazeState, prominent: Boolean, onClick: () -> Unit) {
    val palette = LocalAppTheme.current.palette
    val interaction = remember { MutableInteractionSource() }
    val shape = CircleShape
    Box(
        modifier = Modifier
            .pressScale(interaction, 0.92f)
            .height(44.dp)
            .then(
                if (prominent) Modifier.clip(shape).background(palette.accent) else Modifier.glass(shape, state = hazeState)
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (prominent) palette.onAccent else palette.onBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SelectionTextAction(text: String, onClick: () -> Unit) {
    val theme = LocalAppTheme.current
    val palette = theme.palette
    Text(
        text = text,
        color = if (theme.language == DesignLanguage.SAMSUNG_ONEUI) palette.onBackground else palette.accent,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
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

