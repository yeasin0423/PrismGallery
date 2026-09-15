package com.example.amoledgallery.ui.screens
import com.example.amoledgallery.ui.i18n.strings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.SdCard
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.amoledgallery.model.AlbumItem
import com.example.amoledgallery.model.MediaItem
import com.example.amoledgallery.ui.components.LargeHeader
import com.example.amoledgallery.ui.components.MediaGridItem
import com.example.amoledgallery.ui.components.SelectionTopBar
import com.example.amoledgallery.ui.components.detectDragSelect
import com.example.amoledgallery.ui.components.detectPinchToZoomGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.amoledgallery.ui.design.DialogButton
import com.example.amoledgallery.ui.design.DialogButtonStyle
import com.example.amoledgallery.ui.design.ListRow
import com.example.amoledgallery.ui.design.MenuEntry
import com.example.amoledgallery.ui.design.ThemedButton
import com.example.amoledgallery.ui.design.ThemedCard
import com.example.amoledgallery.ui.design.ThemedDialog
import com.example.amoledgallery.ui.design.ThemedIconButton
import com.example.amoledgallery.ui.design.ThemedListSection
import com.example.amoledgallery.ui.design.ThemedPopupMenu
import com.example.amoledgallery.ui.design.ThemedRadioIndicator
import com.example.amoledgallery.ui.design.ThemedSheet
import com.example.amoledgallery.ui.design.ThemedTextField
import com.example.amoledgallery.ui.design.ThemedTopBar
import com.example.amoledgallery.ui.design.bottomBarContentPadding
import com.example.amoledgallery.ui.design.barGlass
import com.example.amoledgallery.ui.theme.isLight
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.AutoAwesomeMotion
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import com.example.amoledgallery.ui.design.glass
import com.example.amoledgallery.ui.design.pressScale
import com.example.amoledgallery.ui.theme.DesignLanguage
import com.example.amoledgallery.ui.theme.LocalAppTheme
import com.example.amoledgallery.ui.theme.destructive
import com.example.amoledgallery.ui.theme.onAccent
import dev.chrisbanes.haze.HazeState
import com.example.amoledgallery.ui.design.HazeSource
import com.example.amoledgallery.ui.design.albumNavSpec
import com.example.amoledgallery.ui.design.albumNavTransition
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import androidx.compose.ui.platform.LocalContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.size.Precision
import com.example.amoledgallery.ui.components.thumbnailRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.example.amoledgallery.ui.design.MediaZoomTransition
import kotlin.math.abs
import com.example.amoledgallery.ui.design.HyperOsDivider
import com.example.amoledgallery.ui.design.HyperOsFab
import com.example.amoledgallery.ui.design.HyperOsIconRow
import com.example.amoledgallery.ui.design.HyperOsSectionLabel
import com.example.amoledgallery.ui.design.bleedHorizontally

/**
 * Albums tab. Root albums are laid out per design language:
 * - One UI: 3 columns of rounded covers with name and count underneath.
 * - Material 3 Expressive: 2 columns of tonal cards holding a rounded cover.
 * - iOS: 2 columns of large rounded covers under a "Meine Alben" section title; long-press opens a
 *   Liquid Glass context menu.
 * - HyperOS: 3 columns of white cards with the cover flush on top.
 * One UI, Material and HyperOS let the ⋮ menu (or a pinch) change how many albums share a row; iOS keeps
 * its fixed layout.
 *
 * Inside an album a frosted top bar with back navigation replaces the large header; sub-albums and the
 * album's media follow. A pinch changes the media grid. Swiping right opens One UI's album drawer, and
 * goes back to the album list on iOS. Album actions open as a popup (One UI, iOS) or an action sheet
 * (Material, HyperOS), all dialogs use the language's own dialog style.
 */
@Composable
fun AlbumsScreen(
    albums: List<AlbumItem>,
    trashedCount: Int = 0,
    isSdCardAvailable: Boolean,
    onAlbumClick: (AlbumItem) -> Unit,
    onOpenTrash: () -> Unit = {},
    onCreateAlbum: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    currentAlbum: AlbumItem? = null,
    breadcrumbs: List<AlbumItem> = emptyList(),
    albumMedia: List<MediaItem> = emptyList(),
    columnCount: Int = 3,
    onBreadcrumbClick: (Int) -> Unit = {},
    onCreateSubAlbum: (AlbumItem, String) -> Unit = { _, _ -> },
    onMediaItemClick: (MediaItem, Int) -> Unit = { _, _ -> },
    // Top-level albums (for One UI's album drawer inside an album) and grid sizes
    rootAlbums: List<AlbumItem> = emptyList(),
    /** Bucket id of the album whose media and sub-albums are currently loaded. */
    loadedAlbumKey: String? = null,
    albumColumns: Int? = null,
    onSetAlbumColumns: (Int) -> Unit = {},
    subAlbumColumns: Int? = null,
    onSetSubAlbumColumns: (Int) -> Unit = {},
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    onOpenRootAlbum: (AlbumItem) -> Unit = {},
    // Hidden albums support
    hiddenAlbums: List<AlbumItem> = emptyList(),
    onHideAlbum: (AlbumItem) -> Unit = {},
    onUnhideAlbum: (String) -> Unit = {},
    // Overlay detail viewer state
    selectedDetailIndex: Int? = null,
    onCloseDetail: () -> Unit = {},
    onDetailIndexChanged: (Int) -> Unit = {},
    onTrashItem: (MediaItem) -> Unit = {},
    onRenameItem: (MediaItem, String) -> Unit = { _, _ -> },
    isFavorite: (Long) -> Boolean = { false },
    onToggleFavorite: (Long) -> Unit = {},
    // Album management actions
    onRenameAlbum: (AlbumItem, String) -> Unit = { _, _ -> },
    onDeleteAlbum: (AlbumItem) -> Unit = {},
    onMigrateAlbum: (AlbumItem, Boolean) -> Unit = { _, _ -> },
    graphicsQuality: com.example.amoledgallery.PerformanceProfile = com.example.amoledgallery.PerformanceProfile.MEDIUM,
    // Multi-select for the media inside an album (same selection state as the Bilder tab)
    isSelectionMode: Boolean = false,
    selectedIds: Set<Long> = emptySet(),
    onStartSelection: (Long) -> Unit = {},
    onToggleSelection: (Long) -> Unit = {},
    onSetSelection: (Long, Boolean) -> Unit = { _, _ -> },
    onUpdateSelectedIds: (Set<Long>) -> Unit = {},
    onSelectAll: () -> Unit = {},
    onDeselectAll: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    /** Enters selection mode with nothing selected yet (iOS "Auswählen"). */
    onEnterSelection: () -> Unit = {},
    // HyperOS: the "All photos", "Videos" and "Favorites" tiles above the albums.
    allMedia: List<MediaItem> = emptyList(),
    favoriteMedia: List<MediaItem> = emptyList(),
    onOpenAllPhotos: () -> Unit = {},
    onOpenVideos: () -> Unit = {},
    onOpenFavorites: () -> Unit = {}
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showCreateSubDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var menuAnchor by remember { mutableStateOf<Rect?>(null) }
    var showSubAlbumMenu by remember { mutableStateOf(false) }
    var subAlbumMenuAnchor by remember { mutableStateOf<Rect?>(null) }
    var showManageHiddenDialog by remember { mutableStateOf(false) }
    var contextMenuAlbum by remember { mutableStateOf<AlbumItem?>(null) }
    var contextAnchor by remember { mutableStateOf<Rect?>(null) }
    var albumToRename by remember { mutableStateOf<AlbumItem?>(null) }
    var albumToDelete by remember { mutableStateOf<AlbumItem?>(null) }
    var albumToMigrate by remember { mutableStateOf<AlbumItem?>(null) }
    var albumToHide by remember { mutableStateOf<AlbumItem?>(null) }

    val theme = LocalAppTheme.current
    val palette = theme.palette
    val language = theme.language
    val albumsHaze = remember { HazeState() }
    val isInsideAlbum = currentAlbum != null
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val density = LocalDensity.current

    // iOS keeps its fixed album layout; the others let the user choose how many albums share a row.
    val gridOptions = language != DesignLanguage.IOS_LIQUID_GLASS
    val defaultRootColumns = when (language) {
        DesignLanguage.GOOGLE_EXPRESSIVE, DesignLanguage.IOS_LIQUID_GLASS -> 2
        else -> 3
    }
    val rootColumns = if (gridOptions) albumColumns ?: defaultRootColumns else defaultRootColumns
    val subColumns = if (gridOptions) subAlbumColumns ?: 3 else 3
    val outerPadding = if (isInsideAlbum) theme.shapes.gridHorizontalPadding else when (language) {
        DesignLanguage.GOOGLE_EXPRESSIVE -> 12.dp
        DesignLanguage.IOS_LIQUID_GLASS, DesignLanguage.XIAOMI_HYPEROS -> 20.dp
        else -> 16.dp
    }
    val horizontalSpacing = if (isInsideAlbum) theme.shapes.gridSpacing else when (language) {
        DesignLanguage.SAMSUNG_ONEUI -> if (rootColumns >= 4) 8.dp else 12.dp
        DesignLanguage.IOS_LIQUID_GLASS -> 16.dp
        DesignLanguage.XIAOMI_HYPEROS -> if (rootColumns >= 4) 10.dp else 14.dp
        else -> if (rootColumns >= 4) 8.dp else 10.dp
    }
    val verticalSpacing = if (isInsideAlbum) theme.shapes.gridSpacing else when (language) {
        DesignLanguage.SAMSUNG_ONEUI -> if (rootColumns >= 4) 14.dp else 20.dp
        DesignLanguage.IOS_LIQUID_GLASS -> 16.dp
        DesignLanguage.XIAOMI_HYPEROS -> 20.dp
        else -> 10.dp
    }
    // Section rows inside an album need their own inset when the media grid runs edge-to-edge.
    val sectionInset = if (outerPadding < 8.dp) 16.dp else 4.dp
    val isIos = language == DesignLanguage.IOS_LIQUID_GLASS
    // iOS Photos lists an album's photos oldest first, like the Mediathek. Grid, drag selection and the viewer
    // all work on this displayed order.
    val displayAlbumMedia = remember(albumMedia, isIos) { if (isIos) albumMedia.asReversed() else albumMedia }
    // iOS: an album with photos opens with a large header that cycles through them.
    val iosAlbumHero = isIos && isInsideAlbum && albumMedia.isNotEmpty()
    val heroHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp * 0.56f
    // One UI 8: an album opens with its first photo as a large cover with the name and counts on it.
    val oneUiAlbumHero = language == DesignLanguage.SAMSUNG_ONEUI && isInsideAlbum && albumMedia.isNotEmpty()
    val oneUiHeroHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp * 0.38f

    // Opening an album (or going back up) moves the page in with the design language's album motion.
    val navProgress = remember { androidx.compose.animation.core.Animatable(1f) }
    val navDirection = remember { intArrayOf(1) }
    val lastAlbumKey = remember { arrayOf(currentAlbum?.bucketId) }
    val lastDepth = remember { intArrayOf(breadcrumbs.size) }
    // Set by the iOS back swipe: the album list then continues from its parallax position.
    val popFromSwipe = remember { booleanArrayOf(false) }
    // iOS back swipe: how far the album page has been pulled to the right (px).
    var backSwipeOffset by remember { mutableFloatStateOf(0f) }
    val navSpec = albumNavSpec(language)
    val latestLoadedKey = rememberUpdatedState(loadedAlbumKey)
    // The album whose page was last animated in. A different current album means a navigation happened;
    // that is known during composition, so the new page is hidden from its very first frame (an effect
    // only runs a frame later, which briefly flashed the new, still empty page fully visible).
    var animatedAlbumKey by remember { mutableStateOf(currentAlbum?.bucketId) }
    val navPending = currentAlbum?.bucketId != animatedAlbumKey
    val navStartProgress = if (popFromSwipe[0]) 0.7f else 0f
    if (navPending) {
        navDirection[0] = if (breadcrumbs.size >= lastDepth[0] && currentAlbum != null) 1 else -1
    }
    val forcedNavProgress = if (navPending) navStartProgress else null
    val context = LocalContext.current
    val latestMedia = rememberUpdatedState(displayAlbumMedia)
    val latestSubAlbums = rememberUpdatedState(albums)
    LaunchedEffect(currentAlbum?.bucketId) {
        val key = currentAlbum?.bucketId
        if (key != animatedAlbumKey) {
            navProgress.snapTo(navStartProgress)
            popFromSwipe[0] = false
            backSwipeOffset = 0f
            lastDepth[0] = breadcrumbs.size
            animatedAlbumKey = key
            if (key != null) {
                // Media and sub-albums load in the background. The page stays hidden until exactly this
                // album's content has arrived, so it never shows empty or with the previous album's content.
                withTimeoutOrNull(700) {
                    snapshotFlow { latestLoadedKey.value == key }.first { it }
                }
                // Then the first screenful of thumbnails and the sub-album covers are loaded into the memory
                // cache, so the page moves in with its images instead of them popping in during the motion.
                withTimeoutOrNull(400) {
                    val loader = SingletonImageLoader.get(context)
                    coroutineScope {
                        val thumbnails = latestMedia.value.take(columnCount * 10).map { item ->
                            async { loader.execute(thumbnailRequest(context, item)) }
                        }
                        val covers = latestSubAlbums.value.take(subColumns * 2).mapNotNull { it.coverUri }.map { uri ->
                            async { loader.execute(albumCoverRequest(context, uri)) }
                        }
                        // iOS: the header's first photo, so the album opens with its big image already there.
                        val hero = if (isIos || language == DesignLanguage.SAMSUNG_ONEUI) {
                            listOfNotNull(latestMedia.value.firstOrNull()).map { item ->
                                async { loader.execute(albumHeroRequest(context, item.uri)) }
                            }
                        } else {
                            emptyList()
                        }
                        (thumbnails + covers + hero).awaitAll()
                    }
                }
            }
            navProgress.animateTo(1f, navSpec)
        }
    }

    // The album list and each open album level keep their own scroll position, so going back from an
    // album lands exactly where the list was left instead of at the top.
    val rootGridState = rememberLazyGridState()
    val albumGridStates = remember { mutableMapOf<Any, androidx.compose.foundation.lazy.grid.LazyGridState>() }
    albumGridStates.keys.retainAll(breadcrumbs.map { it.bucketId }.toSet() + listOfNotNull(currentAlbum?.bucketId))
    val albumGridState = currentAlbum?.let { album ->
        albumGridStates.getOrPut(album.bucketId) { androidx.compose.foundation.lazy.grid.LazyGridState() }
    } ?: rootGridState
    // One UI's big title fades out while the list scrolls instead of sliding under the status bar.
    val headerCollapsePx = with(density) { 220.dp.toPx() }
    val rootHeaderCollapse by remember(rootGridState, headerCollapsePx) {
        androidx.compose.runtime.derivedStateOf {
            if (rootGridState.firstVisibleItemIndex > 0) 1f
            else (rootGridState.firstVisibleItemScrollOffset / headerCollapsePx).coerceIn(0f, 1f)
        }
    }
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    // Same long-press/tap race guard as the Bilder grid: the tap that ends a long-press must not
    // immediately toggle the item it just selected.
    var lastLongPressId by remember { mutableStateOf<Long?>(null) }
    var lastLongPressTime by remember { mutableLongStateOf(0L) }
    var isLongPressGestureActive by remember { mutableStateOf(false) }

    // While the fullscreen viewer covers everything, the grid below it is not drawn at all.
    var gridCoveredByViewer by remember { mutableStateOf(false) }

    // Horizontal swipe inside an album: One UI's album drawer (0..1 open) or iOS's back swipe (px).
    val drawerWidthPx = with(density) { 118.dp.toPx() }
    var drawerProgress by remember { mutableFloatStateOf(0f) }
    var swipeJob by remember { mutableStateOf<Job?>(null) }
    var pageWidthPx by remember { mutableFloatStateOf(0f) }
    // Whether the drawer was open when the current swipe began: a short pull decides either way.
    val drawerWasOpen = remember { booleanArrayOf(false) }
    val swipeEnabled = isInsideAlbum && !isSelectionMode && selectedDetailIndex == null &&
        (language == DesignLanguage.SAMSUNG_ONEUI || language == DesignLanguage.IOS_LIQUID_GLASS)
    if (!isInsideAlbum && drawerProgress != 0f) {
        drawerProgress = 0f
    }

    fun settleDrawer(open: Boolean) {
        swipeJob?.cancel()
        swipeJob = coroutineScope.launch {
            animate(drawerProgress, if (open) 1f else 0f, animationSpec = androidx.compose.animation.core.spring(dampingRatio = 1f, stiffness = 520f)) { v, _ ->
                drawerProgress = v
            }
        }
    }

    fun onSwipeStart() {
        swipeJob?.cancel()
        drawerWasOpen[0] = drawerProgress > 0.5f
    }

    fun onSwipeDrag(dx: Float) {
        if (language == DesignLanguage.SAMSUNG_ONEUI) {
            drawerProgress = (drawerProgress + dx / drawerWidthPx).coerceIn(0f, 1f)
        } else {
            backSwipeOffset = (backSwipeOffset + dx).coerceAtLeast(0f)
        }
    }

    /**
     * A swipe decides by its direction alone - however short, slow or long it was: to the right opens the
     * drawer (One UI) or goes back (iOS), to the left closes the drawer. [netX] is the finger's horizontal
     * travel over the whole swipe; a [cancelled] swipe (second finger) returns to where it started.
     */
    fun onSwipeEnd(netX: Float, cancelled: Boolean) {
        if (language == DesignLanguage.SAMSUNG_ONEUI) {
            settleDrawer(if (cancelled) drawerWasOpen[0] else netX > 0f)
        } else {
            val width = pageWidthPx.coerceAtLeast(1f)
            val goBack = !cancelled && netX > 0f
            swipeJob = coroutineScope.launch {
                if (goBack) {
                    val remaining = ((width - backSwipeOffset) / width).coerceIn(0f, 1f)
                    animate(
                        backSwipeOffset,
                        width,
                        animationSpec = androidx.compose.animation.core.tween((80 + 160 * remaining).toInt(), easing = androidx.compose.animation.core.FastOutLinearInEasing)
                    ) { v, _ -> backSwipeOffset = v }
                    // The page stays pulled off-screen until the album list is shown; the navigation effect
                    // then resets the offset in the same step as it starts the list's parallax motion.
                    popFromSwipe[0] = true
                    onBreadcrumbClick(breadcrumbs.size - 2)
                } else {
                    animate(backSwipeOffset, 0f, animationSpec = androidx.compose.animation.core.spring(dampingRatio = 1f, stiffness = 520f)) { v, _ -> backSwipeOffset = v }
                }
            }
        }
    }

    BackHandler(enabled = drawerProgress > 0.5f) { settleDrawer(false) }
    // One UI with a cover header: the cover stays put and only the rows below it move aside for the drawer.
    val drawerShift = if (oneUiAlbumHero) Modifier.graphicsLayer { translationX = drawerProgress * drawerWidthPx } else Modifier

    val openAlbumActions: (AlbumItem, Rect?) -> Unit = { album, anchor ->
        contextAnchor = anchor
        contextMenuAlbum = album
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { pageWidthPx = it.width.toFloat() }
            // On the whole screen - the album page and One UI's drawer - so a swipe to the left closes the
            // drawer wherever it starts. Runs after the children (Main pass): a vertical scroll or a drag
            // selection has consumed the moves by then, so only a horizontal swipe is taken over.
            .pointerInput(swipeEnabled) {
                if (!swipeEnabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var total = Offset.Zero
                    var lastX = down.position.x
                    var dragging = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        val multiTouch = event.changes.count { it.pressed } > 1
                        if (change == null || !change.pressed || multiTouch) {
                            if (dragging) {
                                val endX = change?.position?.x ?: lastX
                                onSwipeEnd(netX = endX - down.position.x, cancelled = multiTouch)
                            }
                            break
                        }
                        lastX = change.position.x
                        if (!dragging) {
                            if (change.isConsumed) break
                            total += change.positionChange()
                            if (total.getDistance() > viewConfiguration.touchSlop) {
                                val horizontal = abs(total.x) > abs(total.y)
                                val allowed = total.x > 0f || drawerProgress > 0f || backSwipeOffset > 0f
                                if (!horizontal || !allowed) break
                                dragging = true
                                change.consume()
                                onSwipeStart()
                                // Catch up with the finger: the distance moved before the swipe was recognized counts too.
                                onSwipeDrag(total.x)
                                continue
                            }
                        }
                        if (dragging) {
                            val dx = change.positionChange().x
                            change.consume()
                            onSwipeDrag(dx)
                        }
                    }
                }
            }
    ) {
        // Everything of the album page (grid and top bar) moves together with the drawer and the back swipe.
        Box(
            modifier = Modifier
                .fillMaxSize()
                // While the drawer is open, a tap on the album page closes it without opening what it hit. It used
                // to be a full-page tap catcher, which also swallowed scrolling and closed the drawer on release.
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        if (drawerProgress <= 0.01f) return@awaitEachGesture
                        var moved = false
                        while (true) {
                            val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) moved = true
                            if (!change.pressed) {
                                if (!moved) {
                                    change.consume()
                                    settleDrawer(false)
                                }
                                break
                            }
                        }
                    }
                }
                .graphicsLayer {
                    // While a navigation is pending the swiped-away page is already replaced: no offset.
                    // With One UI's cover header only the rows below the cover move aside (see drawerShift).
                    translationX = (if (oneUiAlbumHero) 0f else drawerProgress * drawerWidthPx) + if (navPending) 0f else backSwipeOffset
                }
        ) {
            HazeSource(
                albumsHaze,
                Modifier
                    .fillMaxSize()
                    .albumNavTransition(language, { navProgress.value }, { navDirection[0] }, forcedNavProgress)
                    .graphicsLayer { alpha = if (gridCoveredByViewer) 0f else 1f }
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (isInsideAlbum) columnCount else rootColumns),
                    state = albumGridState,
                    modifier = Modifier
                        .fillMaxSize()
                        // One detector for both pages that decides on every pinch: inside an album it sizes the
                        // photos, on the album list the albums (not on iOS).
                        .detectPinchToZoomGrid(
                            onZoomIn = {
                                if (isInsideAlbum) onZoomIn()
                                else if (gridOptions && rootColumns > 2) onSetAlbumColumns(rootColumns - 1)
                            },
                            onZoomOut = {
                                if (isInsideAlbum) onZoomOut()
                                else if (gridOptions && rootColumns < 5) onSetAlbumColumns(rootColumns + 1)
                            }
                        )
                        .then(
                            if (isInsideAlbum && albumMedia.isNotEmpty()) {
                                Modifier.detectDragSelect(
                                    items = displayAlbumMedia,
                                    gridState = albumGridState,
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
                                )
                            } else {
                                Modifier
                            }
                        ),
                    contentPadding = PaddingValues(
                        start = outerPadding,
                        end = outerPadding,
                        top = when {
                            iosAlbumHero || oneUiAlbumHero -> 0.dp
                            // Gemini's bar fades out 28dp below itself; the first row starts below that fade.
                            isInsideAlbum && language == DesignLanguage.GOOGLE_EXPRESSIVE -> statusBarTop + 96.dp
                            isInsideAlbum -> statusBarTop + 68.dp
                            language == DesignLanguage.IOS_LIQUID_GLASS -> statusBarTop + 76.dp
                            else -> 0.dp
                        },
                        bottom = bottomBarContentPadding(16.dp)
                    ),
                    horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                    verticalArrangement = Arrangement.spacedBy(verticalSpacing)
                ) {
                    if (!isInsideAlbum) {
                        // iOS pins its "Alben" title bar over the list instead (see below).
                        if (language != DesignLanguage.IOS_LIQUID_GLASS) item(span = { GridItemSpan(maxLineSpan) }, key = "albums_header") {
                            LargeHeader(
                                title = strings.tabAlbums,
                                subtitle = if (language == DesignLanguage.SAMSUNG_ONEUI) null else albumCountText(albums.size),
                                collapseFraction = { rootHeaderCollapse },
                                tall = true,
                                modifier = if (language == DesignLanguage.XIAOMI_HYPEROS) Modifier.bleedHorizontally(outerPadding) else Modifier
                            ) {
                                // HyperOS creates albums with its floating + button instead.
                                if (language != DesignLanguage.XIAOMI_HYPEROS) {
                                    ThemedIconButton(
                                        icon = Icons.Rounded.Add,
                                        contentDescription = strings.newAlbum,
                                        onClick = { showCreateDialog = true },
                                        hazeState = albumsHaze
                                    )
                                }
                                ThemedIconButton(
                                    icon = if (language == DesignLanguage.IOS_LIQUID_GLASS) Icons.Rounded.MoreHoriz else Icons.Rounded.MoreVert,
                                    contentDescription = strings.tabMenu,
                                    onClick = { showMenu = true },
                                    hazeState = albumsHaze,
                                    modifier = Modifier.onGloballyPositioned { menuAnchor = it.boundsInRoot() }
                                )
                            }
                        }

                        if (language == DesignLanguage.XIAOMI_HYPEROS) {
                            item(span = { GridItemSpan(maxLineSpan) }, key = "hyperos_featured") {
                                HyperOsFeaturedAlbums(
                                    allMedia = allMedia,
                                    favoriteMedia = favoriteMedia,
                                    onOpenAllPhotos = onOpenAllPhotos,
                                    onOpenVideos = onOpenVideos,
                                    onOpenFavorites = onOpenFavorites
                                )
                            }
                            item(span = { GridItemSpan(maxLineSpan) }, key = "albums_section") {
                                Column {
                                    HyperOsDivider()
                                    HyperOsSectionLabel(strings.allAlbums, Modifier.padding(top = 22.dp))
                                }
                            }
                        }

                        items(albums, key = { it.bucketId }) { album ->
                            AlbumTile(
                                album = album,
                                onClick = { onAlbumClick(album) },
                                onLongClick = { anchor -> openAlbumActions(album, anchor) },
                                compact = rootColumns >= 4
                            )
                        }

                        if (albums.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }, key = "albums_empty") {
                                Text(
                                    text = strings.noAlbums,
                                    color = palette.textMuted,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp)
                                )
                            }
                        }
                        if (language == DesignLanguage.XIAOMI_HYPEROS) {
                            item(span = { GridItemSpan(maxLineSpan) }, key = "hyperos_organize") {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    HyperOsDivider()
                                    HyperOsSectionLabel(strings.organizePhotos, Modifier.padding(top = 22.dp, bottom = 6.dp))
                                    HyperOsIconRow(
                                        icon = Icons.Rounded.Delete,
                                        iconColor = palette.accent,
                                        title = strings.recentlyDeleted,
                                        value = "$trashedCount",
                                        horizontalPadding = 0.dp,
                                        onClick = onOpenTrash
                                    )
                                    HyperOsIconRow(
                                        icon = Icons.Rounded.VisibilityOff,
                                        iconColor = Color(0xFF8A90A6),
                                        title = strings.hiddenAlbums,
                                        value = "${hiddenAlbums.size}",
                                        horizontalPadding = 0.dp,
                                        onClick = { showManageHiddenDialog = true }
                                    )
                                }
                            }
                        }
                    } else {
                        if (iosAlbumHero && currentAlbum != null) {
                            item(span = { GridItemSpan(maxLineSpan) }, key = "ios_album_hero") {
                                IosAlbumHero(
                                    title = currentAlbum.name,
                                    media = displayAlbumMedia,
                                    height = heroHeight,
                                    onPlay = { onMediaItemClick(displayAlbumMedia.first(), 0) },
                                    onOpenGridMenu = { anchor ->
                                        subAlbumMenuAnchor = anchor
                                        showSubAlbumMenu = true
                                    }
                                )
                            }
                        }
                        if (oneUiAlbumHero && currentAlbum != null) {
                            item(span = { GridItemSpan(maxLineSpan) }, key = "oneui_album_hero") {
                                OneUiAlbumHero(
                                    title = currentAlbum.name,
                                    media = albumMedia,
                                    height = oneUiHeroHeight,
                                    modifier = Modifier
                                        .bleedHorizontally(outerPadding)
                                        .padding(bottom = 8.dp)
                                )
                            }
                        }
                        if (breadcrumbs.size > 1) {
                            item(span = { GridItemSpan(maxLineSpan) }, key = "breadcrumbs") {
                                Breadcrumbs(
                                    breadcrumbs = breadcrumbs,
                                    onBreadcrumbClick = onBreadcrumbClick,
                                    modifier = drawerShift.padding(horizontal = sectionInset, vertical = 4.dp)
                                )
                            }
                        }

                        if (albums.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }, key = "sub_albums_title") {
                                AlbumsSectionTitle(strings.subAlbums, albums.size, drawerShift.padding(horizontal = sectionInset))
                            }
                            item(span = { GridItemSpan(maxLineSpan) }, key = "sub_albums") {
                                Column(
                                    modifier = drawerShift.padding(horizontal = sectionInset, vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    albums.chunked(subColumns).forEach { rowAlbums ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(if (subColumns >= 4) 8.dp else 10.dp)
                                        ) {
                                            rowAlbums.forEach { subAlbum ->
                                                AlbumTile(
                                                    album = subAlbum,
                                                    onClick = { onAlbumClick(subAlbum) },
                                                    onLongClick = { anchor -> openAlbumActions(subAlbum, anchor) },
                                                    compact = true,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            repeat(subColumns - rowAlbums.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (albumMedia.isNotEmpty()) {
                            if (albums.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }, key = "album_media_title") {
                                    AlbumsSectionTitle(strings.media, albumMedia.size, drawerShift.padding(horizontal = sectionInset, vertical = 6.dp))
                                }
                            }
                            items(
                                items = displayAlbumMedia,
                                key = { it.id },
                                contentType = { "media_tile" }
                            ) { item ->
                                MediaGridItem(
                                    item = item,
                                    isFavorite = isFavorite(item.id),
                                    isSelectionMode = isSelectionMode,
                                    isSelected = selectedIds.contains(item.id),
                                    onClick = {
                                        val isTrailingClickFromLongPress = item.id == lastLongPressId &&
                                            (isLongPressGestureActive || (System.currentTimeMillis() - lastLongPressTime) < 600)
                                        when {
                                            isTrailingClickFromLongPress -> Unit
                                            isSelectionMode -> onToggleSelection(item.id)
                                            else -> onMediaItemClick(item, displayAlbumMedia.indexOf(item))
                                        }
                                    },
                                    modifier = Modifier
                                        .then(drawerShift)
                                        .aspectRatio(1f)
                                        .clipToBounds()
                                )
                            }
                        } else if (albums.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }, key = "album_empty") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 64.dp, horizontal = 32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(strings.albumEmpty, color = palette.textMuted, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(18.dp))
                                    ThemedButton(
                                        text = strings.createSubAlbum,
                                        icon = Icons.Rounded.CreateNewFolder,
                                        onClick = { showCreateSubDialog = true }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isInsideAlbum && currentAlbum != null && isSelectionMode) {
                SelectionTopBar(
                    // Set lookup: scanning the album for every selected id was quadratic, which froze the
                    // selection bar after "select all" in albums with thousands of items.
                    selectedCount = remember(albumMedia, selectedIds) {
                        val albumIds = albumMedia.mapTo(HashSet(albumMedia.size)) { it.id }
                        selectedIds.count { it in albumIds }
                    },
                    totalCount = albumMedia.size,
                    onToggleSelectAll = {
                        if (albumMedia.isNotEmpty() && albumMedia.all { selectedIds.contains(it.id) }) onDeselectAll() else onSelectAll()
                    },
                    onClose = onClearSelection,
                    hazeState = albumsHaze,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            } else if (iosAlbumHero && currentAlbum != null) {
                // iOS album: buttons float over the header; once it has scrolled away a frosted bar with the
                // small title fades in behind them.
                val heroHeightPx = with(density) { heroHeight.toPx() }
                IosAlbumTopBar(
                    title = currentAlbum.name,
                    subtitle = strings.objects(albumMedia.size),
                    collapse = {
                        if (albumGridState.firstVisibleItemIndex > 0) 1f
                        else ((albumGridState.firstVisibleItemScrollOffset - heroHeightPx * 0.55f) / (heroHeightPx * 0.3f)).coerceIn(0f, 1f)
                    },
                    hazeState = albumsHaze,
                    onBack = { onBreadcrumbClick(breadcrumbs.size - 2) },
                    onNewSubAlbum = { showCreateSubDialog = true },
                    onMore = { anchor ->
                        subAlbumMenuAnchor = anchor
                        showSubAlbumMenu = true
                    },
                    onSelect = onEnterSelection,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .albumNavTransition(language, { navProgress.value }, { navDirection[0] }, forcedNavProgress)
                )
            } else if (oneUiAlbumHero && currentAlbum != null) {
                val heroHeightPx = with(density) { oneUiHeroHeight.toPx() }
                OneUiAlbumTopBar(
                    title = currentAlbum.name,
                    collapse = {
                        if (albumGridState.firstVisibleItemIndex > 0) 1f
                        else ((albumGridState.firstVisibleItemScrollOffset - heroHeightPx * 0.55f) / (heroHeightPx * 0.3f)).coerceIn(0f, 1f)
                    },
                    hazeState = albumsHaze,
                    onBack = { onBreadcrumbClick(breadcrumbs.size - 2) },
                    onMore = { anchor ->
                        subAlbumMenuAnchor = anchor
                        showSubAlbumMenu = true
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .albumNavTransition(language, { navProgress.value }, { navDirection[0] }, forcedNavProgress)
                )
            } else if (isInsideAlbum && currentAlbum != null) {
                ThemedTopBar(
                    title = currentAlbum.name,
                    subtitle = strings.items(albumMedia.size),
                    onBack = { onBreadcrumbClick(breadcrumbs.size - 2) },
                    hazeState = albumsHaze,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .albumNavTransition(language, { navProgress.value }, { navDirection[0] }, forcedNavProgress)
                ) {
                    ThemedIconButton(
                        icon = Icons.Rounded.CreateNewFolder,
                        contentDescription = strings.newSubAlbum,
                        onClick = { showCreateSubDialog = true },
                        hazeState = albumsHaze
                    )
                    // Grid sizes for the album's photos and (not on iOS) its sub-albums.
                    if (albumMedia.isNotEmpty() || (gridOptions && albums.isNotEmpty())) {
                        ThemedIconButton(
                            icon = Icons.Rounded.MoreVert,
                            contentDescription = strings.options,
                            onClick = { showSubAlbumMenu = true },
                            hazeState = albumsHaze,
                            modifier = Modifier.onGloballyPositioned { subAlbumMenuAnchor = it.boundsInRoot() }
                        )
                    }
                }
            }

            // iOS "Alben": the centred title stays pinned over the list, + and ⋯ share one glass capsule.
            if (!isInsideAlbum && language == DesignLanguage.IOS_LIQUID_GLASS) {
                ThemedTopBar(
                    title = strings.tabAlbums,
                    hazeState = albumsHaze,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .albumNavTransition(language, { navProgress.value }, { navDirection[0] }, forcedNavProgress)
                ) {
                    Row(
                        modifier = Modifier
                            .height(48.dp)
                            .glass(CircleShape, barGlass(), albumsHaze)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 50.dp, height = 44.dp)
                                .clip(CircleShape)
                                .clickable { showCreateDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Add, strings.newAlbum, tint = palette.onBackground, modifier = Modifier.size(26.dp))
                        }
                        Box(
                            modifier = Modifier
                                .onGloballyPositioned { menuAnchor = it.boundsInRoot() }
                                .size(width = 50.dp, height = 44.dp)
                                .clip(CircleShape)
                                .clickable { showMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.MoreHoriz, strings.tabMenu, tint = palette.onBackground, modifier = Modifier.size(26.dp))
                        }
                    }
                }
            }

            if (!isInsideAlbum && language == DesignLanguage.XIAOMI_HYPEROS) {
                HyperOsFab(
                    icon = Icons.Rounded.Add,
                    contentDescription = strings.newAlbum,
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = bottomBarContentPadding(20.dp))
                        .albumNavTransition(language, { navProgress.value }, { navDirection[0] }, forcedNavProgress)
                )
            }

        }

        // One UI: the other albums in a single column at the left edge, revealed by swiping right.
        if (language == DesignLanguage.SAMSUNG_ONEUI && isInsideAlbum && drawerProgress > 0f) {
            AlbumDrawer(
                albums = rootAlbums,
                currentRootId = breadcrumbs.firstOrNull()?.bucketId,
                widthPx = drawerWidthPx,
                progress = { drawerProgress },
                // With the cover header, the drawer starts right below the cover and follows it while scrolling.
                topOffsetPx = if (oneUiAlbumHero) {
                    val heroPx = with(density) { oneUiHeroHeight.toPx() }
                    ({
                        if (albumGridState.firstVisibleItemIndex > 0) 0f
                        else (heroPx - albumGridState.firstVisibleItemScrollOffset).coerceAtLeast(0f)
                    })
                } else {
                    ({ 0f })
                },
                onAlbumClick = { album ->
                    settleDrawer(false)
                    if (album.bucketId != breadcrumbs.firstOrNull()?.bucketId || breadcrumbs.size > 1) {
                        onOpenRootAlbum(album)
                    }
                }
            )
        }

        // FULLSCREEN OVERLAY: Media Detail Viewer inside Albums
        // Preserves scroll position of album media when returning
        val detailIndexHolder = remember { IntArray(1) }
        selectedDetailIndex?.let { detailIndexHolder[0] = it }
        MediaZoomTransition(
            visible = selectedDetailIndex != null,
            onCoveringChange = { gridCoveredByViewer = it }
        ) {
                MediaDetailScreen(
                    mediaList = displayAlbumMedia,
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

    ThemedPopupMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
        anchorBounds = menuAnchor,
        entries = buildList {
            if (gridOptions) {
                add(MenuEntry(strings.largerThumbnails, icon = Icons.Rounded.ZoomIn) { if (rootColumns > 2) onSetAlbumColumns(rootColumns - 1) })
                add(MenuEntry(strings.smallerThumbnails, icon = Icons.Rounded.ZoomOut) { if (rootColumns < 5) onSetAlbumColumns(rootColumns + 1) })
            }
            add(MenuEntry(strings.hiddenAlbums, icon = Icons.Rounded.VisibilityOff) { showManageHiddenDialog = true })
        }
    )

    ThemedPopupMenu(
        expanded = showSubAlbumMenu,
        onDismissRequest = { showSubAlbumMenu = false },
        anchorBounds = subAlbumMenuAnchor,
        entries = buildList {
            // One UI's album ⋮ also starts a selection and creates sub-albums (its header has no other buttons).
            if (language == DesignLanguage.SAMSUNG_ONEUI && oneUiAlbumHero) {
                add(MenuEntry(strings.select) { onEnterSelection() })
                add(MenuEntry(strings.newSubAlbum) { showCreateSubDialog = true })
            }
            if (albumMedia.isNotEmpty()) {
                add(MenuEntry(strings.largerThumbnails, icon = Icons.Rounded.ZoomIn) { onZoomIn() })
                add(MenuEntry(strings.smallerThumbnails, icon = Icons.Rounded.ZoomOut) { onZoomOut() })
            }
            if (gridOptions && albums.isNotEmpty()) {
                add(MenuEntry(strings.largerSubAlbums, icon = Icons.Rounded.ZoomIn) { if (subColumns > 2) onSetSubAlbumColumns(subColumns - 1) })
                add(MenuEntry(strings.smallerSubAlbums, icon = Icons.Rounded.ZoomOut) { if (subColumns < 5) onSetSubAlbumColumns(subColumns + 1) })
            }
        }
    )

    // Album actions: a popup anchored on the tile for One UI / iOS, an action sheet elsewhere.
    contextMenuAlbum?.let { album ->
        val canMigrate = album.isSdCard || isSdCardAvailable
        val migrateLabel = if (album.isSdCard) strings.moveToInternal else strings.moveToSd
        if (language == DesignLanguage.SAMSUNG_ONEUI || language == DesignLanguage.IOS_LIQUID_GLASS) {
            ThemedPopupMenu(
                expanded = true,
                onDismissRequest = { contextMenuAlbum = null },
                anchorBounds = contextAnchor,
                entries = buildList {
                    add(MenuEntry(strings.rename, icon = Icons.Rounded.Edit) { albumToRename = album })
                    if (canMigrate) {
                        add(MenuEntry(migrateLabel, icon = if (album.isSdCard) Icons.Rounded.PhoneAndroid else Icons.Rounded.SdCard) { albumToMigrate = album })
                    }
                    add(MenuEntry(strings.hide, icon = Icons.Rounded.VisibilityOff) { albumToHide = album })
                    add(MenuEntry(strings.delete, icon = Icons.Rounded.Delete, destructive = true) { albumToDelete = album })
                }
            )
        } else {
            ThemedSheet(
                onDismissRequest = { contextMenuAlbum = null },
                title = album.name
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 20.dp)
                ) {
                    Text(
                        text = strings.items(album.count) + " · " + (if (album.isSdCard) strings.sdCard else strings.internal),
                        color = palette.textMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                    )
                    ThemedListSection(
                        inSheet = true,
                        rows = buildList {
                            add(ListRow(strings.renameAlbum, icon = Icons.Rounded.Edit, iconColor = palette.accent, showChevron = false, onClick = {
                                contextMenuAlbum = null
                                albumToRename = album
                            }))
                            add(ListRow(
                                migrateLabel,
                                subtitle = if (!canMigrate) strings.noSdCard else null,
                                icon = if (album.isSdCard) Icons.Rounded.PhoneAndroid else Icons.Rounded.SdCard,
                                iconColor = Color(0xFF34C759),
                                showChevron = false,
                                onClick = if (canMigrate) {
                                    {
                                        contextMenuAlbum = null
                                        albumToMigrate = album
                                    }
                                } else {
                                    null
                                }
                            ))
                            add(ListRow(strings.hideAlbum, icon = Icons.Rounded.VisibilityOff, iconColor = Color(0xFF8E8E93), showChevron = false, onClick = {
                                contextMenuAlbum = null
                                albumToHide = album
                            }))
                            add(ListRow(strings.deleteAlbum, icon = Icons.Rounded.Delete, iconColor = palette.destructive, titleColor = palette.destructive, showChevron = false, onClick = {
                                contextMenuAlbum = null
                                albumToDelete = album
                            }))
                        }
                    )
                }
            }
        }
    }

    // Create Root Album Dialog
    if (showCreateDialog) {
        CreateAlbumDialog(
            isSdCardAvailable = isSdCardAvailable,
            onDismiss = { showCreateDialog = false },
            onCreate = { name, onSdCard ->
                showCreateDialog = false
                onCreateAlbum(name, onSdCard)
            }
        )
    }

    // Create Sub-Album Dialog
    if (showCreateSubDialog && currentAlbum != null) {
        CreateSubAlbumDialog(
            parentAlbum = currentAlbum,
            onDismiss = { showCreateSubDialog = false },
            onCreate = { name ->
                showCreateSubDialog = false
                onCreateSubAlbum(currentAlbum, name)
            }
        )
    }

    albumToHide?.let { album ->
        ThemedDialog(
            onDismissRequest = { albumToHide = null },
            title = strings.hideAlbumTitle,
            message = strings.hideAlbumMessage(album.name),
            buttons = listOf(
                DialogButton(strings.cancel, DialogButtonStyle.Cancel) { albumToHide = null },
                DialogButton(strings.hide, DialogButtonStyle.Primary) {
                    albumToHide = null
                    onHideAlbum(album)
                }
            )
        )
    }

    albumToRename?.let { album ->
        RenameAlbumDialog(
            album = album,
            onDismiss = { albumToRename = null },
            onRename = { newName ->
                albumToRename = null
                onRenameAlbum(album, newName)
            }
        )
    }

    albumToDelete?.let { album ->
        ThemedDialog(
            onDismissRequest = { albumToDelete = null },
            title = strings.deleteAlbumTitle(album.name),
            message = if (album.count > 0) {
                strings.deleteAlbumMessage(album.count)
            } else {
                strings.deleteEmptyAlbumMessage(album.name)
            },
            buttons = listOf(
                DialogButton(strings.cancel, DialogButtonStyle.Cancel) { albumToDelete = null },
                DialogButton(strings.delete, DialogButtonStyle.Destructive) {
                    albumToDelete = null
                    onDeleteAlbum(album)
                }
            )
        )
    }

    albumToMigrate?.let { album ->
        val toSdCard = !album.isSdCard
        val destName = toSdCard
        ThemedDialog(
            onDismissRequest = { albumToMigrate = null },
            title = strings.changeLocationTitle,
            message = strings.migrateMessage(album.name, album.count, destName),
            buttons = listOf(
                DialogButton(strings.cancel, DialogButtonStyle.Cancel) { albumToMigrate = null },
                DialogButton(strings.move, DialogButtonStyle.Primary) {
                    albumToMigrate = null
                    onMigrateAlbum(album, toSdCard)
                }
            )
        )
    }

    if (showManageHiddenDialog) {
        ManageHiddenAlbumsSheet(
            hiddenAlbums = hiddenAlbums,
            onDismiss = { showManageHiddenDialog = false },
            onUnhideAlbum = onUnhideAlbum
        )
    }
}

private fun albumCountText(count: Int) = strings.albums(count)

/** Screen-sized image request for the iOS album header, loadable ahead of time under the same key. */
private fun albumHeroRequest(context: android.content.Context, uri: android.net.Uri): ImageRequest =
    ImageRequest.Builder(context)
        .data(uri)
        .size(1080)
        .precision(Precision.INEXACT)
        .build()

/**
 * iOS Photos album header: a large photo of the album that slowly cycles through its contents with a
 * crossfade, the album name and item count on it, and round buttons for the grid options and playback.
 */
@Composable
private fun IosAlbumHero(
    title: String,
    media: List<MediaItem>,
    height: androidx.compose.ui.unit.Dp,
    onPlay: () -> Unit,
    onOpenGridMenu: (Rect?) -> Unit
) {
    val context = LocalContext.current
    // Up to 12 photos spread over the whole album (videos only when there are no photos).
    val slides = remember(media) {
        val photos = media.filter { !it.isVideo }.ifEmpty { media }
        val step = (photos.size / 12).coerceAtLeast(1)
        photos.filterIndexed { index, _ -> index % step == 0 }.take(12)
    }
    var slide by remember(slides) { androidx.compose.runtime.mutableIntStateOf(0) }
    LaunchedEffect(slides) {
        if (slides.size < 2) return@LaunchedEffect
        val loader = SingletonImageLoader.get(context)
        while (true) {
            kotlinx.coroutines.delay(4500)
            val next = (slide + 1) % slides.size
            // The next photo is decoded before the crossfade starts, so it never fades in from an empty frame.
            loader.execute(albumHeroRequest(context, slides[next].uri))
            slide = next
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clipToBounds()
    ) {
        androidx.compose.animation.Crossfade(
            targetState = slides.getOrNull(slide),
            animationSpec = androidx.compose.animation.core.tween(900),
            label = "album_hero"
        ) { item ->
            if (item != null) {
                val request = remember(item.id) { albumHeroRequest(context, item.uri) }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.35f),
                        0.25f to Color.Transparent,
                        0.6f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.55f)
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, end = 130.dp, bottom = 22.dp)
        ) {
            Text(
                text = title,
                style = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 1f), 10f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesomeMotion, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.objects(media.size), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            var anchor by remember { mutableStateOf<Rect?>(null) }
            HeroCircleButton(
                icon = Icons.Rounded.GridView,
                description = strings.options,
                modifier = Modifier.onGloballyPositioned { anchor = it.boundsInRoot() }
            ) { onOpenGridMenu(anchor) }
            HeroCircleButton(icon = Icons.Rounded.PlayArrow, description = title, onClick = onPlay)
        }
    }
}

@Composable
private fun HeroCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interaction, 0.9f)
            .size(50.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.38f))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

/**
 * Top bar of an iOS album with a header: the back circle, a glass capsule (new sub-album, ⋯) and "Auswählen"
 * float over the photo; once the header has scrolled away ([collapse] 1) a frosted bar with the small title
 * fades in behind them.
 */
@Composable
private fun IosAlbumTopBar(
    title: String,
    subtitle: String,
    collapse: () -> Float,
    hazeState: HazeState,
    onBack: () -> Unit,
    onNewSubAlbum: () -> Unit,
    onMore: (Rect?) -> Unit,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppTheme.current.palette
    val shade = if (palette.isLight) Color.White else Color.Black
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = collapse() }
                .glass(
                    androidx.compose.ui.graphics.RectangleShape,
                    com.example.amoledgallery.ui.design.GlassSpec(shade.copy(alpha = 0.40f), shade.copy(alpha = 0.92f), 18.dp, com.example.amoledgallery.ui.design.GlassRim.None),
                    hazeState,
                    mask = androidx.compose.ui.graphics.Brush.verticalGradient(0f to Color.Black, 0.75f to Color.Black, 1f to Color.Transparent)
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(60.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemedIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBackIos,
                contentDescription = strings.back,
                onClick = onBack,
                hazeState = hazeState
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
                    .graphicsLayer { alpha = collapse() }
            ) {
                Text(title, color = palette.onBackground, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = palette.textMuted, fontSize = 12.sp, maxLines = 1)
            }
            var moreAnchor by remember { mutableStateOf<Rect?>(null) }
            Row(
                modifier = Modifier
                    .height(44.dp)
                    .glass(CircleShape, barGlass(), hazeState)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 46.dp, height = 40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onNewSubAlbum),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.CreateNewFolder, strings.newSubAlbum, tint = palette.onBackground, modifier = Modifier.size(22.dp))
                }
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { moreAnchor = it.boundsInRoot() }
                        .size(width = 46.dp, height = 40.dp)
                        .clip(CircleShape)
                        .clickable { onMore(moreAnchor) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.MoreHoriz, strings.options, tint = palette.onBackground, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .glass(CircleShape, barGlass(), hazeState)
                    .clip(CircleShape)
                    .clickable(onClick = onSelect)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(strings.select, color = palette.onBackground, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            }
        }
    }
}

/** One UI 8's album cover header: the first photo, darkened towards the bottom, with the name and counts. */
@Composable
private fun OneUiAlbumHero(
    title: String,
    media: List<MediaItem>,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cover = remember(media) { media.firstOrNull() }
    val images = remember(media) { media.count { !it.isVideo } }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(Color.Black)
    ) {
        if (cover != null) {
            val request = remember(cover.uri, context) { albumHeroRequest(context, cover.uri) }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.30f),
                        0.4f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.6f)
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 24.dp, bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(strings.imagesAndVideos(images, media.size - images), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

/**
 * One UI 8 album top bar: dark circles with the back chevron and ⋮ float over the cover; once it has scrolled
 * away ([collapse] 1) a frosted bar with the album name fades in behind them.
 */
@Composable
private fun OneUiAlbumTopBar(
    title: String,
    collapse: () -> Float,
    hazeState: HazeState,
    onBack: () -> Unit,
    onMore: (Rect?) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppTheme.current.palette
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = collapse() }
                .glass(
                    androidx.compose.ui.graphics.RectangleShape,
                    com.example.amoledgallery.ui.design.GlassSpec(palette.background.copy(alpha = 0.72f), palette.background.copy(alpha = 0.97f), 30.dp, com.example.amoledgallery.ui.design.GlassRim.None),
                    hazeState
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(76.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OneUiDarkCircle(Icons.AutoMirrored.Rounded.ArrowBackIos, strings.back, Modifier, onBack)
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
                    .graphicsLayer { alpha = collapse() }
            )
            var anchor by remember { mutableStateOf<Rect?>(null) }
            OneUiDarkCircle(
                Icons.Rounded.MoreVert,
                strings.options,
                Modifier.onGloballyPositioned { anchor = it.boundsInRoot() }
            ) { onMore(anchor) }
        }
    }
}

@Composable
private fun OneUiDarkCircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interaction, 0.9f)
            .size(56.dp)
            .clip(CircleShape)
            .background(Color(0xFF232325).copy(alpha = 0.9f))
            .clickable(interactionSource = interaction, indication = ripple(color = Color.White), onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = Color.White, modifier = Modifier.size(25.dp))
    }
}

/** One UI's album drawer: every top-level album as a cover with its name, the open one outlined. */
@Composable
private fun AlbumDrawer(
    albums: List<AlbumItem>,
    currentRootId: String?,
    widthPx: Float,
    progress: () -> Float,
    topOffsetPx: () -> Float,
    onAlbumClick: (AlbumItem) -> Unit
) {
    val palette = LocalAppTheme.current.palette
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val widthDp = with(LocalDensity.current) { widthPx.toDp() }
    LazyColumn(
        modifier = Modifier
            .width(widthDp)
            .fillMaxHeight()
            .layout { measurable, constraints ->
                // Shift down (and shorten) by the space the album cover still takes at the top.
                val top = topOffsetPx().toInt().coerceIn(0, constraints.maxHeight)
                val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = constraints.maxHeight - top))
                layout(placeable.width, constraints.maxHeight) { placeable.place(0, top) }
            }
            .graphicsLayer {
                val p = progress()
                translationX = (p - 1f) * size.width * 0.6f
                alpha = p
            },
        contentPadding = PaddingValues(top = statusBarTop + 16.dp, bottom = bottomBarContentPadding(16.dp), start = 14.dp, end = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(albums.size, key = { albums[it].bucketId }) { index ->
            val album = albums[index]
            val isCurrent = album.bucketId == currentRootId
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onAlbumClick(album) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.surfaceVariant)
                        .then(if (isCurrent) Modifier.border(2.5.dp, palette.accent, RoundedCornerShape(14.dp)) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    if (album.coverUri != null) {
                        AsyncImage(
                            model = album.coverUri,
                            contentDescription = album.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Rounded.Folder, null, tint = palette.textMuted, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = album.name,
                    color = if (isCurrent) palette.accent else palette.onBackground,
                    fontSize = 12.sp,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumTile(
    album: AlbumItem,
    onClick: () -> Unit,
    onLongClick: (Rect?) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val theme = LocalAppTheme.current
    val palette = theme.palette
    val language = theme.language
    var bounds by remember { mutableStateOf<Rect?>(null) }
    val interaction = remember { MutableInteractionSource() }
    val countText = buildString {
        append("${album.count}")
        if (album.subAlbumCount > 0) append(strings.subAlbumSuffix(album.subAlbumCount))
    }
    val clickModifier = Modifier
        .onGloballyPositioned { bounds = it.boundsInRoot() }
        .combinedClickable(
            interactionSource = interaction,
            indication = if (language == DesignLanguage.SAMSUNG_ONEUI) ripple() else null,
            onClick = onClick,
            onLongClick = { onLongClick(bounds) }
        )
    val nameSize = if (compact) 13.sp else 15.sp
    val countSize = if (compact) 11.sp else 13.sp

    when (language) {
        // One UI 8: a large rounded cover with the name and count centred on the photo in white.
        DesignLanguage.SAMSUNG_ONEUI -> {
            val shape = RoundedCornerShape(if (compact) 14.dp else 22.dp)
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .then(clickModifier)
            ) {
                AlbumCover(album, shape)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                0.45f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.55f)
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 8.dp, vertical = if (compact) 6.dp else 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.5f), androidx.compose.ui.geometry.Offset(0f, 1f), 6f)
                    Text(
                        text = album.name,
                        style = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = if (compact) 13.sp else 18.sp, fontWeight = FontWeight.Medium, shadow = shadow),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = countText,
                        style = androidx.compose.ui.text.TextStyle(color = Color.White.copy(alpha = 0.9f), fontSize = if (compact) 11.sp else 15.sp, shadow = shadow),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Google Photos: a rounded cover straight on the background, name and count underneath.
        DesignLanguage.GOOGLE_EXPRESSIVE -> Column(
            modifier = modifier
                .fillMaxWidth()
                .pressScale(interaction, 0.95f)
                .then(clickModifier)
        ) {
            AlbumCover(album, RoundedCornerShape(if (compact) 14.dp else 18.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(album.name, color = palette.onBackground, fontSize = if (compact) 13.sp else 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(countText, color = palette.textMuted, fontSize = countSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        // iOS 26 Photos: a large rounded cover with the album name set in white on the photo itself.
        DesignLanguage.IOS_LIQUID_GLASS -> {
            val shape = RoundedCornerShape(if (compact) 16.dp else 24.dp)
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .pressScale(interaction, 0.95f)
                    .clip(shape)
                    .then(clickModifier)
            ) {
                AlbumCover(album, shape)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                0.55f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.45f)
                            )
                        )
                )
                Text(
                    text = album.name,
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = if (compact) 14.sp else 18.sp,
                        fontWeight = FontWeight.Medium,
                        shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.45f), androidx.compose.ui.geometry.Offset(0f, 1f), 6f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = if (compact) 10.dp else 14.dp, end = 10.dp, bottom = if (compact) 8.dp else 12.dp)
                )
            }
        }

        // HyperOS: a rounded cover straight on the background, name and count underneath; squeezes on touch.
        DesignLanguage.XIAOMI_HYPEROS -> Column(
            modifier = modifier
                .fillMaxWidth()
                .pressScale(interaction, 0.95f)
                .then(clickModifier)
        ) {
            AlbumCover(album, RoundedCornerShape(if (compact) 16.dp else 22.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(album.name, color = palette.onBackground, fontSize = if (compact) 14.sp else 17.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(countText, color = palette.textMuted, fontSize = if (compact) 12.sp else 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Album cover request with a fixed decode size (sharp enough for the largest tile), so it can be loaded
 * into the memory cache ahead of time under the same key the cover then looks up.
 */
private fun albumCoverRequest(context: android.content.Context, uri: android.net.Uri): ImageRequest =
    ImageRequest.Builder(context)
        .data(uri)
        .size(512)
        .precision(Precision.INEXACT)
        .build()

@Composable
private fun AlbumCover(album: AlbumItem, shape: Shape) {
    val palette = LocalAppTheme.current.palette
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .background(palette.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (album.coverUri != null) {
            val request = remember(album.coverUri, context) { albumCoverRequest(context, album.coverUri) }
            AsyncImage(
                model = request,
                contentDescription = album.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(Icons.Rounded.Folder, null, tint = palette.textMuted, modifier = Modifier.size(36.dp))
        }
        if (album.isSdCard) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(4.dp)
            ) {
                Icon(Icons.Rounded.SdCard, strings.sdCard, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun AlbumsSectionTitle(text: String, count: Int, modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    val palette = theme.palette
    when (theme.language) {
        DesignLanguage.SAMSUNG_ONEUI -> Row(modifier = modifier.padding(top = 8.dp), verticalAlignment = Alignment.Bottom) {
            Text(text, color = palette.onBackground, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Text("$count", color = palette.textMuted, fontSize = 15.sp)
        }
        DesignLanguage.GOOGLE_EXPRESSIVE -> Row(modifier = modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = palette.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text("$count", color = palette.textMuted, fontSize = 14.sp)
        }
        DesignLanguage.IOS_LIQUID_GLASS -> Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = palette.onBackground, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("$count", color = palette.textMuted, fontSize = 17.sp)
        }
        DesignLanguage.XIAOMI_HYPEROS -> HyperOsSectionLabel(text, modifier.padding(top = 10.dp, bottom = 4.dp))
    }
}

@Composable
private fun Breadcrumbs(
    breadcrumbs: List<AlbumItem>,
    onBreadcrumbClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val palette = theme.palette
    val language = theme.language
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (language == DesignLanguage.GOOGLE_EXPRESSIVE) 6.dp else 0.dp)
    ) {
        val crumbs = listOf(strings.tabAlbums) + breadcrumbs.map { it.name }
        crumbs.forEachIndexed { position, name ->
            val index = position - 1
            val isCurrent = position == crumbs.lastIndex
            if (position > 0 && language != DesignLanguage.GOOGLE_EXPRESSIVE) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = palette.textMuted, modifier = Modifier.size(18.dp))
            }
            if (language == DesignLanguage.GOOGLE_EXPRESSIVE) {
                Text(
                    text = name,
                    color = if (isCurrent) palette.onAccent else palette.onBackground,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isCurrent) palette.accent else lerp(palette.surfaceVariant, palette.accent, 0.08f))
                        .clickable(enabled = !isCurrent) { onBreadcrumbClick(index) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                )
            } else {
                Text(
                    text = name,
                    color = when {
                        isCurrent -> palette.onBackground
                        language == DesignLanguage.IOS_LIQUID_GLASS -> palette.accent
                        else -> palette.textMuted
                    },
                    fontSize = 15.sp,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !isCurrent) { onBreadcrumbClick(index) }
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ManageHiddenAlbumsSheet(
    hiddenAlbums: List<AlbumItem>,
    onDismiss: () -> Unit,
    onUnhideAlbum: (String) -> Unit
) {
    val palette = LocalAppTheme.current.palette
    ThemedSheet(onDismissRequest = onDismiss, title = strings.hiddenAlbums) {
        if (hiddenAlbums.isEmpty()) {
            Text(
                text = strings.noHiddenAlbums,
                color = palette.textMuted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 20.dp)
            ) {
                ThemedCard(inSheet = true) {
                    hiddenAlbums.forEachIndexed { index, album ->
                        if (index > 0) Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(palette.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                if (album.coverUri != null) {
                                    AsyncImage(
                                        model = album.coverUri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(Icons.Rounded.Folder, null, tint = palette.textMuted, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(album.name, color = palette.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(strings.items(album.count), color = palette.textMuted, fontSize = 13.sp)
                            }
                            ThemedIconButton(
                                icon = Icons.Rounded.Visibility,
                                contentDescription = strings.unhide,
                                onClick = { onUnhideAlbum(album.bucketId) },
                                tint = palette.accent,
                                hazeState = null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateSubAlbumDialog(
    parentAlbum: AlbumItem,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var subAlbumName by remember { mutableStateOf("") }
    val location = if (parentAlbum.isSdCard) strings.sdCard else strings.internal
    ThemedDialog(
        onDismissRequest = onDismiss,
        title = strings.newSubAlbum,
        message = strings.inAlbum(parentAlbum.name, location),
        buttons = listOf(
            DialogButton(strings.cancel, DialogButtonStyle.Cancel, onClick = onDismiss),
            DialogButton(strings.create, DialogButtonStyle.Primary, enabled = subAlbumName.isNotBlank()) {
                if (subAlbumName.isNotBlank()) onCreate(subAlbumName.trim())
            }
        )
    ) {
        ThemedTextField(
            value = subAlbumName,
            onValueChange = { subAlbumName = it },
            placeholder = strings.subAlbumName,
            autoFocus = true
        )
    }
}

@Composable
fun CreateAlbumDialog(
    isSdCardAvailable: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, Boolean) -> Unit
) {
    var albumName by remember { mutableStateOf("") }
    var onSdCard by remember { mutableStateOf(false) }

    ThemedDialog(
        onDismissRequest = onDismiss,
        title = strings.newAlbum,
        buttons = listOf(
            DialogButton(strings.cancel, DialogButtonStyle.Cancel, onClick = onDismiss),
            DialogButton(strings.create, DialogButtonStyle.Primary, enabled = albumName.isNotBlank()) {
                if (albumName.isNotBlank()) onCreate(albumName.trim(), onSdCard)
            }
        )
    ) {
        ThemedTextField(
            value = albumName,
            onValueChange = { albumName = it },
            placeholder = strings.albumName,
            autoFocus = true
        )
        Spacer(modifier = Modifier.height(14.dp))
        StorageOption(
            title = strings.internalStorage,
            subtitle = "Pictures/${albumName.ifBlank { "…" }}",
            selected = !onSdCard,
            enabled = true
        ) { onSdCard = false }
        StorageOption(
            title = if (isSdCardAvailable) strings.sdCard else strings.sdCardUnavailable,
            subtitle = strings.externalStorage,
            selected = onSdCard,
            enabled = isSdCardAvailable
        ) { onSdCard = true }
    }
}

@Composable
private fun StorageOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalAppTheme.current.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) palette.onBackground else palette.textMuted,
                fontSize = 16.sp
            )
            Text(text = subtitle, color = palette.textMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(modifier = Modifier.width(12.dp))
        ThemedRadioIndicator(selected = selected)
    }
}

@Composable
private fun RenameAlbumDialog(
    album: AlbumItem,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var newName by remember { mutableStateOf(album.name) }
    val valid = newName.isNotBlank() && newName.trim() != album.name
    ThemedDialog(
        onDismissRequest = onDismiss,
        title = strings.renameAlbum,
        buttons = listOf(
            DialogButton(strings.cancel, DialogButtonStyle.Cancel, onClick = onDismiss),
            DialogButton(strings.rename, DialogButtonStyle.Primary, enabled = valid) {
                if (valid) onRename(newName.trim())
            }
        )
    ) {
        ThemedTextField(
            value = newName,
            onValueChange = { newName = it },
            placeholder = strings.newAlbumName,
            autoFocus = true,
            maxLines = 3
        )
    }
}
