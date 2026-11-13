// The viewer's bars keep their system-bar padding while Android's bars are hidden, so nothing jumps.
@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.gallery.app.ui.screens
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import com.gallery.app.ui.i18n.strings
import androidx.compose.material.icons.rounded.MoreVert

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.Info
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.gallery.app.model.MediaItem
import com.gallery.app.ui.components.AmoledVideoPlayer
import com.gallery.app.ui.components.MediaInfoContent
import com.gallery.app.ui.components.VideoPlayerScreen
import com.gallery.app.ui.components.ZoomableBox
import com.gallery.app.ui.design.DialogButton
import com.gallery.app.ui.design.DialogButtonStyle
import com.gallery.app.ui.design.ThemedDialog
import com.gallery.app.ui.design.ThemedTextField
import com.gallery.app.ui.design.pressScale
import com.gallery.app.ui.theme.LocalAppTheme
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dev.chrisbanes.haze.HazeState
import com.gallery.app.ui.design.HazeSource
import com.gallery.app.ui.design.OverlayWindow
import com.gallery.app.ui.editor.MediaEditorScreen
import androidx.compose.animation.core.animateFloat
import kotlin.math.abs

/** Share of the screen height the details panel takes when it is open underneath the photo. */
private const val INFO_PANEL_FRACTION = 0.52f

/**
 * Fullscreen media viewer: pager, pinch/double-tap zoom, swipe down to close, swipe up for details.
 * The details open like the OS galleries do: the photo moves up and the panel opens underneath it.
 * The chrome is One UI: a dark circle with the back chevron on the left, a ⋮ circle on the right, and
 * a floating dark capsule with the actions at the bottom.
 *
 * In [trashMode] (opened from the trash) only restore, delete permanently and details are offered.
 * In [viewOnly] mode (a file another app opened with "Open with") only sharing and details are offered.
 */
@Composable
fun MediaDetailScreen(
    mediaList: List<MediaItem>,
    initialIndex: Int,
    onBack: () -> Unit,
    onTrashItem: (MediaItem) -> Unit,
    onRenameItem: (MediaItem, String) -> Unit,
    isFavorite: (Long) -> Boolean = { false },
    onToggleFavorite: (Long) -> Unit = {},
    onPageChanged: (Int) -> Unit = {},
    trashMode: Boolean = false,
    onRestoreItem: (MediaItem) -> Unit = {},
    onDeletePermanently: (MediaItem) -> Unit = {},
    viewOnly: Boolean = false,
    /** Off for files opened from another app: renaming them needs the gallery's write-access flow. */
    allowRename: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (mediaList.isEmpty()) {
        // Closing changes state, which must not happen in the middle of composition.
        androidx.compose.runtime.LaunchedEffect(Unit) { onBack() }
        return
    }

    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { mediaList.size })

    var isZoomed by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(initialIndex) {
        val targetPage = initialIndex.coerceIn(0, (mediaList.size - 1).coerceAtLeast(0))
        if (pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
    }
    // Pull-down-to-close offset (px) and how far the details panel is open (0..1). Both follow the finger
    // directly and are settled with animate() afterwards.
    var dismissOffset by remember { mutableFloatStateOf(0f) }
    var infoProgress by remember { mutableFloatStateOf(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val context = LocalContext.current

    androidx.compose.runtime.LaunchedEffect(pagerState.currentPage) {
        isZoomed = false
        dismissOffset = 0f
        // Report the page back so a delete acts on the item actually on screen and the index is
        // left pointing at the following one.
        onPageChanged(pagerState.currentPage)
    }

    var showControls by remember { mutableStateOf(true) }
    // The video opened by the Play button, shown on its own full-screen page above the viewer.
    var playingVideo by remember { mutableStateOf<MediaItem?>(null) }
    // Hiding the controls also hides Android's status and navigation bars (a swipe from the edge shows them briefly).
    val hostView = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.LaunchedEffect(showControls) {
        val window = hostView.context.findActivity()?.window ?: return@LaunchedEffect
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, hostView)
        controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (showControls) {
            controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            hostView.context.findActivity()?.window?.let { window ->
                androidx.core.view.WindowCompat.getInsetsController(window, hostView)
                    .show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    var showRenameDialog by remember { mutableStateOf(false) }
    /** The photo whose "set as wallpaper" sheet is open. */
    var wallpaperItem by remember { mutableStateOf<MediaItem?>(null) }
    // The ⋯ menu in the top bar.
    var showMoreMenu by remember { mutableStateOf(false) }
    var moreMenuAnchor by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var editingItem by remember { mutableStateOf<MediaItem?>(null) }
    var viewerSize by remember { mutableStateOf(IntSize.Zero) }

    val currentItem = mediaList.getOrNull(pagerState.currentPage) ?: mediaList[0]
    val isFav = isFavorite(currentItem.id)
    var bounceTrigger by remember { mutableStateOf(false) }
    val favScale by animateFloatAsState(
        targetValue = if (bounceTrigger) 1.3f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        finishedListener = {
            if (bounceTrigger) bounceTrigger = false
        },
        label = "fav_scale"
    )

    val theme = LocalAppTheme.current
    val palette = theme.palette
    val detailHaze = remember { HazeState() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val statusBarPx = with(density) { WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx() }
    val topChromePx = statusBarPx + with(density) { 64.dp.toPx() }

    // The panel opens underneath the photo and grows upwards only as far as its details need to be seen
    // whole (what does not fit is its scroll range), and never over the top bar.
    val panelHeightPx = viewerSize.height * INFO_PANEL_FRACTION
    var infoExpand by remember { mutableFloatStateOf(0f) }
    androidx.compose.runtime.LaunchedEffect(pagerState.currentPage) { infoExpand = 0f }
    val infoScroll = rememberScrollState()
    fun infoExpandMax(): Float = minOf(
        infoScroll.maxValue + infoExpand,
        viewerSize.height - viewerSize.height * INFO_PANEL_FRACTION - topChromePx
    ).coerceAtLeast(0f)
    // Only read inside callbacks and small child scopes: read here, crossing the halfway point would
    // recompose the whole viewer (pager and images included) in the middle of the panel animation.
    val infoMostlyOpen by remember { derivedStateOf { infoProgress > 0.5f } }
    // The details panel is built once, shortly after the viewer has opened, and then kept (invisible while
    // closed), so opening it doesn't have to compose and lay it out during its first animation frames.
    var panelComposed by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(600)
        panelComposed = true
    }

    /**
     * Applies a vertical finger movement ([dy] > 0 = downwards) to the details and returns the part of it that
     * was used. Upwards past the open position expands the panel over the photo; downwards first undoes that
     * expand before closing the panel.
     */
    fun applyInfoDrag(dy: Float): Float {
        val expandMax = infoExpandMax()
        if (dy < 0f && infoProgress >= 1f && infoExpand < expandMax) {
            val take = maxOf(dy, -(expandMax - infoExpand))
            infoExpand -= take
            return take
        }
        var rest = dy
        if (rest > 0f && infoExpand > 0f) {
            val take = minOf(rest, infoExpand)
            infoExpand -= take
            rest -= take
            if (rest == 0f) return dy
        }
        if (panelHeightPx <= 0f) return dy - rest
        val before = infoProgress
        infoProgress = (infoProgress - rest / panelHeightPx).coerceIn(0f, 1f)
        return dy - rest + (before - infoProgress) * panelHeightPx
    }

    fun settleInfo(target: Float) {
        panelComposed = true
        settleJob?.cancel()
        settleJob = scope.launch {
            if (target == 0f && infoExpand > 0f) {
                launch {
                    animate(infoExpand, 0f, animationSpec = spring(dampingRatio = 1f, stiffness = 420f)) { value, _ -> infoExpand = value }
                }
            }
            animate(infoProgress, target, animationSpec = spring(dampingRatio = 1f, stiffness = 420f)) { value, _ ->
                infoProgress = value
            }
        }
    }

    fun settleDismiss() {
        settleJob?.cancel()
        settleJob = scope.launch {
            animate(dismissOffset, 0f, animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f)) { value, _ ->
                dismissOffset = value
            }
        }
    }

    // Back closes the details first, then the viewer.
    BackHandler(onBack = onBack)
    InfoBackHandler(isOpen = { infoMostlyOpen }, onClose = { settleInfo(0f) })

    // Pull-down-to-close: the photo follows the finger and shrinks, the background fades; released past
    // the threshold it closes (zooming back into the grid), otherwise it springs back.
    val dismissShrink = 0.25f
    val favoriteBurst = remember { androidx.compose.animation.core.Animatable(1f) }
    val backgroundColor = Color.Black

    val actions = DetailActions(
        isFavorite = isFav,
        favoriteScale = { favScale },
        favoriteBurst = { favoriteBurst.value },
        onBack = onBack,
        onToggleFavorite = {
            bounceTrigger = true
            if (!isFav) {
                scope.launch {
                    favoriteBurst.snapTo(0f)
                    favoriteBurst.animateTo(1f, tween(520))
                }
            }
            onToggleFavorite(currentItem.id)
        },
        onShare = {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (currentItem.isVideo) "video/*" else "image/*"
                putExtra(Intent.EXTRA_STREAM, currentItem.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, strings.shareItem))
        },
        onRename = { showRenameDialog = true },
        onEdit = { editingItem = currentItem },
        onInfo = { settleInfo(if (infoMostlyOpen) 0f else 1f) },
        onDelete = { onTrashItem(currentItem) },
        onRestore = { onRestoreItem(currentItem) },
        onDeletePermanently = { onDeletePermanently(currentItem) },
        showRename = !trashMode && !viewOnly && allowRename,
        onMore = { anchor ->
            moreMenuAnchor = anchor
            showMoreMenu = true
        }
    )
    val onPhotoTap: () -> Unit = {
        if (infoMostlyOpen) settleInfo(0f) else showControls = !showControls
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { viewerSize = it }
            .drawBehind {
                val fade = (dismissOffset / size.height * 2.2f).coerceIn(0f, 0.9f)
                drawRect(backgroundColor.copy(alpha = backgroundColor.alpha * (1f - fade)))
            }
    ) {
        HazeSource(
            detailHaze,
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val h = size.height
                    val w = size.width
                    val p = infoProgress
                    var infoScale = 1f
                    var infoShift = 0f
                    if (p > 0f && h > 0f && w > 0f) {
                        val aspect = currentItem.displayAspectRatio
                        val fittedHeight = if (aspect > 0f) minOf(h, w / aspect) else h
                        // Details: shrink the photo just enough to fit the space above the panel and centre it there.
                        // A panel pulled higher leaves less room, so the photo shrinks further.
                        val available = (h - panelHeightPx - infoExpand - topChromePx).coerceAtLeast(h * 0.2f)
                        val targetScale = (available / fittedHeight).coerceAtMost(1f)
                        infoScale = 1f + (targetScale - 1f) * p
                        infoShift = (topChromePx + available / 2f - h / 2f) * p
                    }
                    val offset = dismissOffset
                    val fraction = (offset / h).coerceIn(0f, 1f)
                    translationY = offset + infoShift
                    val s = (1f - fraction * dismissShrink) * infoScale
                    scaleX = s
                    scaleY = s
                }
                .pointerInput(isZoomed, panelHeightPx) {
                    if (isZoomed) return@pointerInput
                    // Decides the direction of a one-finger drag before the pager sees it (Initial pass).
                    // A drag that is mostly vertical - even with some sideways drift - closes the viewer or
                    // moves the details panel and is kept away from the pager; everything else is left to it.
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val velocity = VelocityTracker()
                        velocity.addPosition(down.uptimeMillis, down.position)
                        var total = Offset.Zero
                        var vertical = false
                        var dismissing = false
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id }
                            val multiTouch = event.changes.count { it.pressed } > 1
                            if (change == null || !change.pressed || multiTouch) {
                                if (vertical) {
                                    val vy = if (multiTouch) 0f else velocity.calculateVelocity().y
                                    if (dismissing) {
                                        if (dismissOffset > 170f || vy > 1800f) onBack() else settleDismiss()
                                    } else {
                                        val target = when {
                                            vy < -700f -> 1f
                                            vy > 700f -> 0f
                                            else -> if (infoProgress > 0.5f) 1f else 0f
                                        }
                                        settleInfo(target)
                                    }
                                }
                                break
                            }
                            velocity.addPosition(change.uptimeMillis, change.position)
                            val delta = change.positionChange()
                            if (!vertical) {
                                // The pager is already between pages: this drag belongs to it.
                                if (pagerState.currentPageOffsetFraction != 0f) break
                                total += delta
                                if (total.getDistance() > viewConfiguration.touchSlop) {
                                    if (abs(total.y) < abs(total.x) * 0.8f) break
                                    vertical = true
                                    settleJob?.cancel()
                                    dismissing = infoProgress <= 0.001f && total.y > 0f && dismissOffset >= 0f
                                    if (!dismissing) panelComposed = true
                                }
                            }
                            if (vertical) {
                                change.consume()
                                if (dismissing) {
                                    dismissOffset = (dismissOffset + delta.y).coerceAtLeast(0f)
                                } else if (panelHeightPx > 0f) {
                                    applyInfoDrag(delta.y)
                                }
                            }
                        }
                    }
                }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                // Keep the neighbouring page composed so its sharp preview is decoded before the
                // user swipes to it; the soft cached thumbnail covers it until then.
                beyondViewportPageCount = 1,
                key = { index -> mediaList[index].id }
            ) { page ->
                val currentMedia = mediaList[page]

                if (currentMedia.isVideo) {
                    AmoledVideoPlayer(
                        item = currentMedia,
                        // The Play button belongs to the page that is actually on screen. It is
                        // composed a page ahead and only enabled here, so it is ready the instant
                        // the pager makes this page current instead of fading in after the settle.
                        isActivePage = pagerState.currentPage == page,
                        controlsVisible = showControls,
                        onTap = onPhotoTap,
                        onPlay = { playingVideo = currentMedia },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // The sharp full-size version is only loaded once the photo is zoomed, so swiping through
                    // photos keeps using screen-sized bitmaps.
                    var showFull by remember(currentMedia.id) { mutableStateOf(false) }
                    ZoomableBox(
                        contentAspectRatio = currentMedia.displayAspectRatio,
                        onSingleTap = onPhotoTap,
                        onZoomChange = { zoomed ->
                            if (zoomed) showFull = true
                            if (pagerState.currentPage == page) {
                                isZoomed = zoomed
                            }
                        }
                    ) {
                        AsyncImage(
                            model = currentMedia.uri,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (showFull) {
                            val pageContext = LocalContext.current
                            // Drawn over the screen-sized image; it stays transparent until it has loaded.
                            AsyncImage(
                                model = remember(currentMedia.id, currentMedia.dateModified) { fullImageRequest(pageContext, currentMedia) },
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(160)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    alpha = 1f - (dismissOffset / 220f).coerceIn(0f, 1f)
                }
        ) {
            DetailTopBar(actions)
        }

        val bottomBar: @Composable BoxScope.() -> Unit = {
            AnimatedVisibility(
                // Fades with the panel through its alpha below (no visibility switch halfway, which recomposed);
                // once the panel is up it covers the bar and takes its touches.
                visible = showControls,
                enter = fadeIn(tween(160)),
                exit = fadeOut(tween(160)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        val infoFade = (1f - infoProgress * 2f).coerceIn(0f, 1f)
                        alpha = (1f - (dismissOffset / 220f).coerceIn(0f, 1f)) * infoFade
                    }
            ) {
                if (viewOnly) {
                    DetailViewOnlyBar(actions)
                } else if (trashMode) {
                    DetailTrashBar(actions)
                } else if (mediaList.size > 1) {
                    // The strip of neighbouring photos above the toolbar.
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OneUiFilmstrip(mediaList, pagerState)
                        DetailBottomBar(actions, editEnabled = !currentItem.isVideo)
                    }
                } else {
                    DetailBottomBar(actions, editEnabled = !currentItem.isVideo)
                }
            }
        }
        bottomBar()

        if (panelComposed && panelHeightPx > 0f) {
            InfoPanel(
                item = currentItem,
                onEdit = if (viewOnly) null else actions.onEdit,
                editEnabled = !currentItem.isVideo,
                heightDp = with(density) { panelHeightPx.toDp() },
                progress = { infoProgress },
                expand = { infoExpand },
                scrollState = infoScroll,
                onDrag = { dy ->
                    settleJob?.cancel()
                    applyInfoDrag(dy)
                },
                onSettle = { vy ->
                    settleInfo(
                        when {
                            vy < -700f -> 1f
                            vy > 700f -> 0f
                            else -> if (infoProgress > 0.5f) 1f else 0f
                        }
                    )
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // The video page opened by the Play button. It lives in the overlay host and fades in over the
        // viewer's still (which is the same sharp frame) while settling from a barely-there zoom, so
        // the picture never jumps: only the chrome and the video arrive.
        playingVideo?.let { video ->
            OverlayWindow { transition ->
                val fade by transition.animateFloat(
                    transitionSpec = { tween(if (targetState) 200 else 150) },
                    label = "player_fade"
                ) { visible -> if (visible) 1f else 0f }
                val settle by transition.animateFloat(
                    transitionSpec = {
                        if (targetState) spring(dampingRatio = 0.9f, stiffness = 420f) else tween(180)
                    },
                    label = "player_settle"
                ) { visible -> if (visible) 1f else 0f }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = fade
                            val s = 1.02f - 0.02f * settle
                            scaleX = s
                            scaleY = s
                        }
                ) {
                    VideoPlayerScreen(
                        item = video,
                        onClose = { playingVideo = null },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    com.gallery.app.ui.design.ThemedPopupMenu(
        expanded = showMoreMenu,
        onDismissRequest = { showMoreMenu = false },
        anchorBounds = moreMenuAnchor,
        entries = buildList {
            add(com.gallery.app.ui.design.MenuEntry(strings.copyToClipboard) {
                context.getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(
                    android.content.ClipData.newUri(context.contentResolver, currentItem.displayName, currentItem.uri)
                )
            })
            // Photos become the wallpaper; videos and GIFs become a live wallpaper (see the sheet).
            add(com.gallery.app.ui.design.MenuEntry(strings.setAsWallpaper) { wallpaperItem = currentItem })
            if (actions.showRename) {
                add(com.gallery.app.ui.design.MenuEntry(strings.rename) { showRenameDialog = true })
            }
        }
    )

    // Set as wallpaper: which screen, in the current design's sheet; set in-app (see setWallpaper).
    wallpaperItem?.let { item ->
        com.gallery.app.ui.design.ThemedSheet(
            onDismissRequest = { wallpaperItem = null },
            title = strings.setAsWallpaper
        ) {
            val palette = LocalAppTheme.current.palette
            // A GIF can be either: a still wallpaper of its first frame, or a moving one. A video can
            // only be the moving one.
            val movable = item.isVideo || item.mimeType.equals("image/gif", ignoreCase = true)
            com.gallery.app.ui.design.ThemedListSection(
                inSheet = true,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 20.dp),
                rows = buildList {
                    if (movable) {
                        add(
                            com.gallery.app.ui.design.ListRow(
                                strings.liveWallpaper,
                                icon = Icons.Outlined.Movie,
                                iconColor = palette.accent,
                                showChevron = false,
                                onClick = {
                                    val tooLong = item.isVideo &&
                                        item.durationMs > com.gallery.app.service.LiveWallpaperChoice.MAX_VIDEO_MS
                                    if (tooLong) {
                                        android.widget.Toast.makeText(context, strings.liveWallpaperTooLong, android.widget.Toast.LENGTH_LONG).show()
                                    } else {
                                        wallpaperItem = null
                                        com.gallery.app.util.setLiveWallpaper(context, item.uri, item.isVideo)
                                    }
                                }
                            )
                        )
                    }
                    if (!item.isVideo) {
                        listOf(
                            com.gallery.app.util.WallpaperTarget.HOME to strings.wallpaperHome,
                            com.gallery.app.util.WallpaperTarget.LOCK to strings.wallpaperLock,
                            com.gallery.app.util.WallpaperTarget.BOTH to strings.wallpaperBoth
                        ).forEach { (target, label) ->
                            add(
                                com.gallery.app.ui.design.ListRow(
                                    label,
                                    icon = Icons.Outlined.Wallpaper,
                                    iconColor = palette.accent,
                                    showChevron = false,
                                    onClick = {
                                        wallpaperItem = null
                                        scope.launch {
                                            val ok = withContext(Dispatchers.IO) {
                                                com.gallery.app.util.setWallpaper(context, item.uri, target)
                                            }
                                            android.widget.Toast.makeText(
                                                context,
                                                if (ok) strings.wallpaperSet else strings.wallpaperFailed,
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                            )
                        }
                    }
                }
            )
        }
    }

    if (showRenameDialog) {
        RenameDialog(
            currentName = currentItem.displayName,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                showRenameDialog = false
                onRenameItem(currentItem, newName)
            }
        )
    }

    // Crop / rotate / mirror editor, shown above everything (bars included) in the overlay host. It settles
    // in from a slight zoom while fading in, as if the photo shrinks into the editor's frame.
    editingItem?.let { item ->
        OverlayWindow { transition ->
            val progress by transition.animateFloat(
                transitionSpec = {
                    if (targetState) spring(dampingRatio = 0.9f, stiffness = 420f) else tween(180)
                },
                label = "editor_enter"
            ) { visible -> if (visible) 1f else 0f }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = (progress * 1.6f).coerceIn(0f, 1f)
                        val s = 1.06f - 0.06f * progress
                        scaleX = s
                        scaleY = s
                    }
            ) {
                MediaEditorScreen(item = item, onClose = { editingItem = null })
            }
        }
    }
}

/**
 * The photo decoded large enough to stay sharp when zoomed. By default it would be decoded at the screen size,
 * which leaves a very tall scroll screenshot only about a hundred pixels wide. It is capped at 12 million pixels
 * and 16384 px per side so the bitmap can still be drawn and stays affordable on low-memory phones.
 */
private fun fullImageRequest(context: android.content.Context, item: MediaItem): coil3.request.ImageRequest {
    val rotated = item.orientation % 180 != 0
    val w = if (rotated) item.height else item.width
    val h = if (rotated) item.width else item.height
    val builder = coil3.request.ImageRequest.Builder(context).data(item.uri)
    if (w > 0 && h > 0) {
        val maxPixels = 12_000_000.0
        val maxSide = 16384.0
        val scale = minOf(1.0, kotlin.math.sqrt(maxPixels / (w.toDouble() * h)), maxSide / maxOf(w, h))
        builder.size((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
        builder.precision(coil3.size.Precision.INEXACT)
    }
    return builder.build()
}

private tailrec fun android.content.Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * One UI's filmstrip: small thumbnails of the neighbouring photos with the current one larger and outlined.
 * Tapping a thumbnail opens it; sliding the strip flips through the photos as it passes them.
 */
@Composable
private fun OneUiFilmstrip(mediaList: List<MediaItem>, pagerState: androidx.compose.foundation.pager.PagerState) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = pagerState.currentPage)
    val dragged by listState.interactionSource.collectIsDraggedAsState()
    var userScrolling by remember { mutableStateOf(false) }
    if (dragged) userScrolling = true
    val itemWidth = 38.dp
    val gap = 3.dp
    val stepPx = with(density) { (itemWidth + gap).toPx() }

    // The strip follows the pager, unless the user is sliding the strip itself.
    androidx.compose.runtime.LaunchedEffect(pagerState.currentPage) {
        if (!userScrolling) listState.animateScrollToItem(pagerState.currentPage)
    }
    // While the user slides the strip, the photo under its centre opens.
    androidx.compose.runtime.LaunchedEffect(listState) {
        androidx.compose.runtime.snapshotFlow {
            listState.firstVisibleItemIndex + if (listState.firstVisibleItemScrollOffset > stepPx / 2f) 1 else 0
        }.collect { centre ->
            if (userScrolling && centre in mediaList.indices && centre != pagerState.currentPage) {
                pagerState.scrollToPage(centre)
            }
        }
    }
    androidx.compose.runtime.LaunchedEffect(listState) {
        androidx.compose.runtime.snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) userScrolling = false
        }
    }

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp),
        contentAlignment = Alignment.Center
    ) {
        val side = (maxWidth - itemWidth) / 2
        androidx.compose.foundation.lazy.LazyRow(
            state = listState,
            flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(listState),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = side),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(mediaList.size, key = { mediaList[it].id }) { index ->
                val item = mediaList[index]
                val current = index == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .width(itemWidth)
                        .height(if (current) 54.dp else 44.dp)
                        .graphicsLayer { scaleX = if (current) 1.2f else 1f }
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF2A2A2C))
                        .then(if (current) Modifier.border(2.dp, Color.White, RoundedCornerShape(6.dp)) else Modifier)
                        .clickable {
                            userScrolling = false
                            scope.launch {
                                pagerState.scrollToPage(index)
                                listState.animateScrollToItem(index)
                            }
                        }
                ) {
                    AsyncImage(
                        model = remember(item.id, context) { com.gallery.app.ui.components.thumbnailRequest(context, item) },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/** Back closes the details panel while it is mostly open. Reads the state in its own small scope. */
@Composable
private fun InfoBackHandler(isOpen: () -> Boolean, onClose: () -> Unit) {
    BackHandler(enabled = isOpen(), onBack = onClose)
}

/**
 * The details panel underneath the photo. Its content scrolls; dragging it down at the top of the list
 * (or anywhere while it is still opening) moves the panel itself.
 */
@Composable
private fun InfoPanel(
    item: MediaItem,
    onEdit: (() -> Unit)?,
    editEnabled: Boolean = true,
    heightDp: Dp,
    progress: () -> Float,
    /** How far the open panel has been pulled up beyond [heightDp], in px (read at layout, not composition). */
    expand: () -> Float,
    scrollState: androidx.compose.foundation.ScrollState,
    /** Applies a vertical drag (px, > 0 = down) to opening / growing the panel; returns the part it used. */
    onDrag: (Float) -> Float,
    onSettle: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppTheme.current.palette
    val latestOnDrag by androidx.compose.runtime.rememberUpdatedState(onDrag)
    val latestOnSettle by androidx.compose.runtime.rememberUpdatedState(onSettle)
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Upwards: first open the panel fully, then pull it up until all details fit, then scroll.
                if (available.y < 0f && source == NestedScrollSource.UserInput) {
                    val used = latestOnDrag(available.y)
                    if (used != 0f) return Offset(0f, used)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // Downwards at the top of the list: lower the panel, then close it.
                if (available.y > 0f && source == NestedScrollSource.UserInput) {
                    return Offset(0f, latestOnDrag(available.y))
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val p = progress()
                if (p < 1f) {
                    latestOnSettle(available.y)
                    return available
                }
                return Velocity.Zero
            }
        }
    }
    // The details simply continue on the black page below the photo.
    val corner = 0.dp
    Column(
        modifier = modifier
            .fillMaxWidth()
            // The open height plus however far it has been pulled up; measured here, so growing the panel
            // re-lays it out without recomposing the viewer.
            .layout { measurable, constraints ->
                val height = (heightDp.roundToPx() + expand()).roundToInt().coerceAtMost(constraints.maxHeight)
                val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
                layout(placeable.width, height) { placeable.place(0, 0) }
            }
            .graphicsLayer {
                val p = progress()
                translationY = (1f - p) * size.height
                // Kept composed while closed; a zero alpha layer skips drawing it entirely.
                alpha = if (p <= 0f) 0f else 1f
            }
            .clip(RoundedCornerShape(topStart = corner, topEnd = corner))
            .background(palette.background)
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false) } }
            .nestedScroll(connection)
            .verticalScroll(scrollState)
            .windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(28.dp))
        MediaInfoContent(item = item, onEdit = onEdit, editEnabled = editEnabled)
    }
}

private class DetailActions(
    val isFavorite: Boolean,
    val favoriteScale: () -> Float,
    /** 0..1 progress of the ring that bursts around the heart when an item is favorited (1 = idle). */
    val favoriteBurst: () -> Float,
    val onBack: () -> Unit,
    val onToggleFavorite: () -> Unit,
    val onShare: () -> Unit,
    val onRename: () -> Unit,
    val onEdit: () -> Unit,
    val onInfo: () -> Unit,
    val onDelete: () -> Unit,
    val onRestore: () -> Unit,
    val onDeletePermanently: () -> Unit,
    /** False in the trash, where a deleted item cannot be renamed. */
    val showRename: Boolean,
    /** Opens the ⋯ menu (anchored at the given bounds) holding the less frequent actions. */
    val onMore: (androidx.compose.ui.geometry.Rect?) -> Unit = {}
)

@Composable
private fun DetailTopBar(actions: DetailActions) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OneUiChromeCircle(Icons.AutoMirrored.Rounded.ArrowBackIos, strings.back, Modifier, actions.onBack)
        Spacer(modifier = Modifier.weight(1f))
        var moreBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
        OneUiChromeCircle(
            Icons.Rounded.MoreVert,
            strings.options,
            Modifier.onGloballyPositioned { moreBounds = it.boundsInRoot() }
        ) { actions.onMore(moreBounds) }
    }
}

@Composable
private fun DetailBottomBar(
    actions: DetailActions,
    /** Editing is for photos only; on videos the edit action is shown but disabled. */
    editEnabled: Boolean = true
) {
    val favoriteIcon = if (actions.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder
    val favoriteModifier = Modifier
        .drawBehind {
            val b = actions.favoriteBurst()
            if (b < 1f) {
                drawCircle(
                    color = Color(0xFFFF4D6D).copy(alpha = (1f - b) * 0.85f),
                    radius = size.minDimension * (0.45f + b * 0.5f),
                    style = Stroke(width = 3.dp.toPx() * (1f - b))
                )
            }
        }
        .graphicsLayer {
            val s = actions.favoriteScale()
            scaleX = s
            scaleY = s
        }

    // One UI 8 viewer: a floating dark capsule with favourite, edit, details, share and delete.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
            .padding(top = 8.dp, bottom = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .height(64.dp)
                .clip(CircleShape)
                .background(Color(0xFF232325).copy(alpha = 0.94f))
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChromeIcon(favoriteIcon, strings.favorite, if (actions.isFavorite) Color(0xFFFF4D6D) else Color.White, actions.onToggleFavorite, favoriteModifier)
            ChromeIcon(Icons.Outlined.Edit, strings.edit, Color.White, actions.onEdit, enabled = editEnabled)
            ChromeIcon(Icons.Outlined.Info, strings.details, Color.White, actions.onInfo)
            ChromeIcon(Icons.Outlined.Share, strings.share, Color.White, actions.onShare)
            ChromeIcon(Icons.Outlined.Delete, strings.delete, Color.White, actions.onDelete)
        }
    }
}

/** Reduced action bar for an item in the trash: restore, details, delete permanently. */
@Composable
private fun DetailTrashBar(actions: DetailActions) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))))
            .windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
            .height(72.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LabeledChromeAction(Icons.Outlined.Restore, strings.restore, Color.White, actions.onRestore)
        LabeledChromeAction(Icons.Outlined.Info, strings.details, Color.White, actions.onInfo)
        LabeledChromeAction(Icons.Outlined.DeleteForever, strings.deletePermanently, Color(0xFFFF6961), actions.onDeletePermanently)
    }
}

/** Action bar for a file another app opened with "Open with": share and details only. */
@Composable
private fun DetailViewOnlyBar(actions: DetailActions) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
            .padding(top = 8.dp, bottom = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .height(64.dp)
                .clip(CircleShape)
                .background(Color(0xFF232325).copy(alpha = 0.94f))
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChromeIcon(Icons.Outlined.Info, strings.details, Color.White, actions.onInfo)
            ChromeIcon(Icons.Outlined.Share, strings.share, Color.White, actions.onShare)
        }
    }
}

@Composable
private fun ChromeIcon(
    icon: ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit,
    iconModifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val color = if (enabled) tint else tint.copy(alpha = 0.3f)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 24.dp),
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = color, modifier = iconModifier.size(25.dp))
    }
}

@Composable
private fun OneUiChromeCircle(icon: ImageVector, description: String, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interaction, 0.9f)
            .size(56.dp)
            .clip(CircleShape)
            .background(Color(0xFF232325).copy(alpha = 0.94f))
            .clickable(interactionSource = interaction, indication = ripple(color = Color.White), onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = Color.White, modifier = Modifier.size(25.dp))
    }
}

@Composable
private fun LabeledChromeAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    iconModifier: Modifier = Modifier,
    height: Dp? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .then(if (height != null) Modifier.height(height) else Modifier.fillMaxHeight())
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, label, tint = tint, modifier = iconModifier.size(24.dp))
        Spacer(modifier = Modifier.height(3.dp))
        Text(label, color = tint, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textValue by remember { mutableStateOf(currentName) }
    ThemedDialog(
        onDismissRequest = onDismiss,
        title = strings.renameFile,
        buttons = listOf(
            DialogButton(strings.cancel, DialogButtonStyle.Cancel, onClick = onDismiss),
            DialogButton(strings.rename, DialogButtonStyle.Primary, enabled = textValue.isNotBlank()) {
                if (textValue.isNotBlank()) onConfirm(textValue.trim())
            }
        )
    ) {
        // Long file names wrap instead of scrolling sideways, so the cursor can be placed anywhere.
        ThemedTextField(
            value = textValue,
            onValueChange = { textValue = it },
            placeholder = strings.name,
            autoFocus = true,
            maxLines = 4
        )
    }
}
