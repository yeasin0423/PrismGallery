package com.gallery.app
import com.gallery.app.ui.i18n.strings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.app.ui.components.BottomNavigationBar
import com.gallery.app.ui.components.MoveToAlbumBottomSheet
import com.gallery.app.ui.components.SelectionActionBar
import com.gallery.app.ui.design.LocalContentHaze
import com.gallery.app.ui.design.LocalGraphicsQuality
import com.gallery.app.ui.design.LocalOverlayHost
import com.gallery.app.ui.design.LocalRootHaze
import com.gallery.app.ui.design.OverlayHost
import com.gallery.app.ui.design.OverlayHostState
import com.gallery.app.ui.design.ThemedDialog
import com.gallery.app.ui.design.ThemedProgressBar
import com.gallery.app.ui.design.screenBackground
import com.gallery.app.ui.qualityTween
import com.gallery.app.ui.screens.AlbumsScreen
import com.gallery.app.ui.screens.CreateAlbumDialog
import com.gallery.app.ui.screens.FavoritesScreen
import com.gallery.app.ui.screens.GalleryScreen
import com.gallery.app.ui.screens.MediaDetailScreen
import com.gallery.app.ui.screens.MenuScreen
import com.gallery.app.ui.screens.PermissionScreen
import com.gallery.app.ui.screens.TrashScreen
import com.gallery.app.ui.theme.GalleryTheme
import com.gallery.app.ui.theme.LocalAppTheme
import com.gallery.app.ui.viewmodel.DetailSource
import com.gallery.app.ui.viewmodel.GalleryTab
import com.gallery.app.ui.viewmodel.GalleryUiState
import com.gallery.app.ui.viewmodel.GalleryViewModel
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.createLifecycleAwareWindowRecomposer
import com.gallery.app.ui.design.AppMotionDurationScale
import com.gallery.app.ui.design.LocalMediaOpenOrigin
import com.gallery.app.ui.design.MediaOpenOrigin
import com.gallery.app.ui.design.MediaZoomTransition
import com.gallery.app.ui.design.SwitchTransitionState
import com.gallery.app.ui.design.blockTouchesBelow
import com.gallery.app.ui.design.pushSpec
import com.gallery.app.ui.design.pushTransition
import com.gallery.app.ui.design.tabSwitchSpec
import com.gallery.app.ui.design.tabSwitchTransition
import kotlin.math.sign
import dev.chrisbanes.haze.HazeState
import com.gallery.app.ui.design.HazeSource
import com.gallery.app.ui.design.BottomEdgeFade
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import com.gallery.app.ui.editor.LocalOnMediaEdited
import com.gallery.app.ui.screens.ExternalMediaViewer
import com.gallery.app.ui.screens.ExternalViewRequest
import com.gallery.app.ui.screens.loadExternalRequest
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Screens pushed over the main tabs. */
private enum class PushedScreen { FAVORITES, TRASH, SETTINGS }

class MainActivity : ComponentActivity() {

    private val viewModel: GalleryViewModel by viewModels()

    /** Set when another app opened a photo or video with "Open with"; the activity then shows only that file. */
    private val viewRequest = mutableStateOf<ExternalViewRequest?>(null)

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleViewIntent(intent)

        // Compose would otherwise scale every animation by the system "animator duration scale"
        // developer option, and at 0x skip them all. The app's own graphics quality decides instead.
        val recomposer = window.decorView.createLifecycleAwareWindowRecomposer(AppMotionDurationScale, lifecycle)
        setContent(parent = recomposer) {
            GalleryTheme {
                // Closing a handed-over file returns to the app that sent it, like the system gallery does.
                GalleryApp(viewModel = viewModel, viewRequest = viewRequest.value, onCloseViewRequest = { finish() })
            }
        }
    }

    /**
     * Keeps the single One UI launcher entry enabled, but only once the gallery is out of sight: Android
     * closes a task whose launcher entry gets disabled, so switching it while the app is open would shut
     * the app in front of the user. (A rotation also stops the activity; that is left alone.)
     */
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            com.gallery.app.util.LauncherIcon.apply(applicationContext)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleViewIntent(intent)
    }

    private fun handleViewIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val uri = intent.data ?: return
        // A camera may attach every shot of its session as ClipData, so they can be swiped through. The
        // opened file is always part of the list, even if the sender left it out.
        val clip = intent.clipData
        val attached = if (clip != null) (0 until clip.itemCount).mapNotNull { clip.getItemAt(it).uri } else emptyList()
        val uris = (if (uri in attached) attached else listOf(uri) + attached).distinct()
        val request = ExternalViewRequest(uri, uris)
        viewRequest.value = request
        val typeHint = intent.type
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { loadExternalRequest(applicationContext, request, typeHint) }
        }
    }
}

@Composable
fun GalleryApp(
    viewModel: GalleryViewModel,
    viewRequest: ExternalViewRequest? = null,
    onCloseViewRequest: () -> Unit = {}
) {
    val graphicsQuality by viewModel.graphicsQuality.collectAsStateWithLifecycle()

    // Two blur backdrops: the whole window (behind dialogs, sheets and menus) and the tab content
    // (behind the floating bottom bar). Overlays render in-window through the OverlayHost so they can
    // blur real app content on every API 31+ device.
    val rootHaze = remember { HazeState() }
    val contentHaze = remember { HazeState() }
    val overlayHost = remember { OverlayHostState() }
    val mediaOpenOrigin = remember { MediaOpenOrigin() }
    // An edited copy was saved: reload so it shows up in the grid and its album.
    val onMediaEdited: () -> Unit = remember(viewModel) { { viewModel.refreshAfterEdit() } }
    CompositionLocalProvider(
        LocalMediaOpenOrigin provides mediaOpenOrigin,
        LocalGraphicsQuality provides graphicsQuality,
        LocalRootHaze provides rootHaze,
        LocalContentHaze provides contentHaze,
        LocalOverlayHost provides overlayHost,
        LocalOnMediaEdited provides onMediaEdited
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // In landscape the navigation bar and camera cutout sit at the sides; content keeps clear of them so
            // no button ends up underneath. The background still reaches the screen edges.
            val sideInsets = androidx.compose.foundation.layout.WindowInsets.safeDrawing
                .only(androidx.compose.foundation.layout.WindowInsetsSides.Horizontal)
            HazeSource(rootHaze, Modifier.fillMaxSize().screenBackground()) {
                Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(sideInsets)) {
                    // Opened with "Open with": only that file, and no gallery (or permission request) behind it.
                    if (viewRequest != null) {
                        val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
                        ExternalMediaViewer(
                            viewRequest,
                            onClose = onCloseViewRequest,
                            favoriteIds = favoriteIds,
                            onToggleFavorite = { viewModel.toggleFavorite(it) }
                        )
                    } else {
                        GalleryAppContent(viewModel, contentHaze)
                    }
                }
            }
            Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(sideInsets)) {
                OverlayHost(overlayHost)
            }
        }
    }
}

@Composable
private fun GalleryAppContent(viewModel: GalleryViewModel, contentHaze: HazeState) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // "1.1", plus what kind of build it is: a debug build, or a release build of a version still in testing.
    val appVersionName = remember(strings) {
        val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: ""
        when {
            version.isBlank() -> ""
            BuildConfig.DEBUG -> "$version ${strings.debugBuild}"
            BuildConfig.PRERELEASE -> "$version ${strings.prereleaseBuild}"
            else -> version
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedAlbum by viewModel.selectedAlbum.collectAsStateWithLifecycle()
    val breadcrumbStack by viewModel.breadcrumbStack.collectAsStateWithLifecycle()
    val rootAlbums by viewModel.rootAlbums.collectAsStateWithLifecycle()
    // Every visible album, root and nested, for the album search (which finds subfolders too).
    val allAlbumsFlat by viewModel.albumsState.collectAsStateWithLifecycle()
    val albumSort by viewModel.albumSort.collectAsStateWithLifecycle()
    val albumStorageFilter by viewModel.albumStorageFilter.collectAsStateWithLifecycle()
    val selectionBytes by viewModel.selectionBytes.collectAsStateWithLifecycle()
    // Re-read whenever the selection changes, so the sheets show what is free right now.
    val freeByStorage = remember(selectionBytes) { viewModel.freeByStorage() }
    val currentSubAlbums by viewModel.currentSubAlbums.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val isTrashOpen by viewModel.isTrashOpen.collectAsStateWithLifecycle()
    val selectedItemIndex by viewModel.selectedItemIndex.collectAsStateWithLifecycle()
    val detailSource by viewModel.detailSource.collectAsStateWithLifecycle()
    val columnCount by viewModel.columnCount.collectAsStateWithLifecycle()
    val albumMedia by viewModel.albumMediaState.collectAsStateWithLifecycle()
    val trashedMedia by viewModel.trashedMediaState.collectAsStateWithLifecycle()
    val favoriteMedia by viewModel.favoriteMediaState.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val albumColumns by viewModel.albumColumns.collectAsStateWithLifecycle()
    val subAlbumColumns by viewModel.subAlbumColumns.collectAsStateWithLifecycle()
    val loadedAlbumKey by viewModel.loadedAlbumKey.collectAsStateWithLifecycle()

    // Multi-Select and Hidden Albums state
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val isMoveSheetOpen by viewModel.isMoveSheetOpen.collectAsStateWithLifecycle()
    val hiddenAlbums by viewModel.hiddenAlbums.collectAsStateWithLifecycle()
    val graphicsQuality by viewModel.graphicsQuality.collectAsStateWithLifecycle()
    val moveProgress by viewModel.moveProgress.collectAsStateWithLifecycle()

    var isFavoritesOpen by remember { mutableStateOf(false) }
    // The settings hub, pushed from the ⋮ menu over the tabs.
    var isSettingsOpen by remember { mutableStateOf(false) }
    var showCreateAlbumFromMove by remember { mutableStateOf(false) }

    // Permission Launcher for media storage access
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.updatePermissionState(hasMediaPermission(context))
    }

    // IntentSender Launcher for MediaStore Trash, Delete & Write requests
    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onIntentSenderSuccess()
        } else {
            viewModel.onIntentSenderCancelled()
        }
    }

    // Listen for PendingIntents to launch Android authorization dialogs
    LaunchedEffect(Unit) {
        viewModel.pendingIntentSender.collect { pendingIntent ->
            val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
            intentSenderLauncher.launch(request)
        }
    }

    // Listen for user feedback messages (Toasts)
    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Show the permission screen on first launch, but never open the system dialog by itself: the
    // screen has a button for that. Coming back from Settings is handled by the ON_RESUME observer.
    LaunchedEffect(Unit) {
        viewModel.updatePermissionState(hasMediaPermission(context))
    }

    // Check permissions on start and when resuming from settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasPerm = hasMediaPermission(context)
                viewModel.updatePermissionState(hasPerm)
                // Files other apps wrote past MediaStore (downloads, file managers) are indexed now
                // instead of whenever the system next gets round to it.
                if (hasPerm) viewModel.rescanMediaFolders()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (uiState is GalleryUiState.PermissionRequired) {
        PermissionScreen(
            onRequestPermission = {
                permissionLauncher.launch(getRequiredPermissions())
            },
            missingMedia = true,
            onOpenSettings = {
                context.startActivity(
                    Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.fromParts("package", context.packageName, null)
                    )
                )
            }
        )
    } else {
        // Intercept back gesture if inside an album hierarchy
        if (breadcrumbStack.isNotEmpty() && selectedItemIndex == null && !isTrashOpen && !isFavoritesOpen && !isSelectionMode) {
            BackHandler {
                viewModel.popAlbumBreadcrumb()
            }
        }

        // Return to Bilder tab on back press if on Alben or Menü
        if (selectedTab != GalleryTab.BILDER && !isTrashOpen && !isFavoritesOpen && !isSelectionMode && breadcrumbStack.isEmpty() && selectedItemIndex == null) {
            BackHandler {
                viewModel.selectTab(GalleryTab.BILDER)
            }
        }

        // Favoriten and Papierkorb are pushed over the main tabs instead of replacing them, so the tabs
        // (and their scroll positions) stay alive underneath while the push animates.
        val pushedScreen = when {
            isFavoritesOpen -> PushedScreen.FAVORITES
            isTrashOpen -> PushedScreen.TRASH
            isSettingsOpen -> PushedScreen.SETTINGS
            else -> null
        }
        val lastPushedScreen = remember { arrayOf(PushedScreen.FAVORITES) }
        if (pushedScreen != null) lastPushedScreen[0] = pushedScreen
        val pushProgress = animateFloatAsState(
            targetValue = if (pushedScreen != null) 1f else 0f,
            animationSpec = pushSpec(),
            label = "push_screen"
        )
        val pushStillVisible by remember { derivedStateOf { pushProgress.value > 0.001f } }

        Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pushTransition({ pushProgress.value }, isTop = false)
        ) {
            val isFullscreenViewer = selectedItemIndex != null
            val allItems = (uiState as? GalleryUiState.Success)?.items ?: emptyList()

            val tabTransition = remember { SwitchTransitionState(selectedTab) }
            val tabSpec = tabSwitchSpec()
            LaunchedEffect(selectedTab) { tabTransition.animateTo(selectedTab, tabSpec) }

            Box(modifier = Modifier.fillMaxSize()) {
                // Tab content draws edge-to-edge underneath the (floating) bottom bar, which blurs it.
                // Each screen reserves bottomBarContentPadding() so its last row scrolls clear of it.
                HazeSource(contentHaze, Modifier.fillMaxSize()) {
                    // All three tabs stay permanently composed (only cross-faded via alpha) instead
                    // of being torn down and rebuilt on every switch, which would destroy and recreate
                    // GalleryScreen's LazyGridState, gesture detectors and thumbnail pre-indexing.
                    GalleryTab.entries.forEach { tab ->
                        val isActiveTab = tab == selectedTab
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(if (isActiveTab) 1f else 0f)
                                .graphicsLayer {
                                    // Only the leaving and the arriving tab are visible during a switch;
                                    // they move apart in the direction of the tab order.
                                    val from = tabTransition.from
                                    val to = tabTransition.to
                                    val p = tabTransition.progress.value
                                    val direction = (GalleryTab.entries.indexOf(to) - GalleryTab.entries.indexOf(from)).sign.toFloat()
                                    when (tab) {
                                        to -> tabSwitchTransition((1f - p) * direction)
                                        from -> tabSwitchTransition(-p * direction)
                                        else -> alpha = 0f
                                    }
                                }
                        ) {
                            when (tab) {
                                GalleryTab.BILDER -> {
                                    val isLoading = uiState is GalleryUiState.Loading

                                    GalleryScreen(
                                        items = allItems,
                                        isLoading = isLoading,
                                        columnCount = columnCount,
                                        onZoomIn = { viewModel.zoomIn() },
                                        onZoomOut = { viewModel.zoomOut() },
                                        onItemClick = { _, index -> viewModel.openDetail(index, DetailSource.ALL) },
                                        selectedFilter = selectedFilter,
                                        onFilterSelected = { viewModel.selectFilter(it) },
                                        // Overlay detail integration:
                                        selectedDetailIndex = if (detailSource == DetailSource.ALL) selectedItemIndex else null,
                                        onCloseDetail = { viewModel.closeDetail() },
                                        onDetailIndexChanged = { viewModel.updateDetailIndex(it) },
                                        onTrashItem = { item -> viewModel.trashMedia(item) },
                                        onRenameItem = { item, newName -> viewModel.renameMedia(item, newName) },
                                        isFavorite = { id -> favoriteIds.contains(id) },
                                        onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                                        // Multi-selection integration:
                                        isSelectionMode = isSelectionMode,
                                        selectedIds = selectedIds,
                                        onStartSelection = { id -> viewModel.startSelection(id) },
                                        onEnterSelection = { viewModel.startSelection(null) },
                                        onToggleSelection = { id -> viewModel.toggleSelection(id) },
                                        onSetSelection = { id, sel -> viewModel.setItemSelection(id, sel) },
                                        onSetSelections = { ids, sel -> viewModel.setItemSelections(ids, sel) },
                                        onUpdateSelectedIds = { viewModel.setSelectedIds(it) },
                                        onSelectAll = { viewModel.selectAll(allItems) },
                                        onDeselectAll = { viewModel.deselectAll() },
                                        onClearSelection = { viewModel.clearSelection() },
                                        onMoveClick = { viewModel.openMoveSheet() },
                                        onDeleteSelected = { viewModel.trashSelectedMedia() },
                                        graphicsQuality = graphicsQuality,
                                        onOpenSettings = { isSettingsOpen = true }
                                    )
                                }

                                GalleryTab.ALBEN -> {
                                    AlbumsScreen(
                                        albums = if (breadcrumbStack.isNotEmpty()) currentSubAlbums else rootAlbums,
                                        rootAlbums = rootAlbums,
                                        loadedAlbumKey = loadedAlbumKey,
                                        albumColumns = albumColumns,
                                        onSetAlbumColumns = { viewModel.setAlbumColumns(it) },
                                        subAlbumColumns = subAlbumColumns,
                                        onSetSubAlbumColumns = { viewModel.setSubAlbumColumns(it) },
                                        onZoomIn = { viewModel.zoomIn() },
                                        onZoomOut = { viewModel.zoomOut() },
                                        onOpenRootAlbum = { album -> viewModel.openRootAlbum(album) },
                                        allAlbumsFlat = allAlbumsFlat,
                                        onJumpToAlbum = { album -> viewModel.jumpToAlbum(album) },
                                        onHideAlbums = { albums -> viewModel.hideAlbums(albums) },
                                        onMigrateAlbums = { albums, toSd -> viewModel.migrateAlbums(albums, toSd) },
                                        isSdCardAvailable = viewModel.isSdCardAvailable,
                                        graphicsQuality = graphicsQuality,
                                        onAlbumClick = { album -> viewModel.openAlbum(album) },
                                        onCreateAlbum = { name, onSdCard ->
                                            viewModel.createAlbum(name, onSdCard)
                                        },
                                        currentAlbum = selectedAlbum,
                                        breadcrumbs = breadcrumbStack,
                                        albumMedia = albumMedia,
                                        columnCount = columnCount,
                                        onBreadcrumbClick = { index -> viewModel.navigateToBreadcrumb(index) },
                                        onCreateSubAlbum = { parent, name -> viewModel.createSubAlbum(parent, name) },
                                        onMediaItemClick = { _, index -> viewModel.openDetail(index, DetailSource.ALBUM) },
                                        // Hidden albums integration:
                                        hiddenAlbums = hiddenAlbums,
                                        onHideAlbum = { album -> viewModel.hideAlbum(album) },
                                        onUnhideAlbum = { bucketId -> viewModel.unhideAlbum(bucketId) },
                                        // Album management actions:
                                        onRenameAlbum = { album, newName -> viewModel.renameAlbum(album, newName) },
                                        onDeleteAlbum = { album -> viewModel.deleteAlbum(album) },
                                        onMigrateAlbum = { album, toSdCard -> viewModel.migrateAlbum(album, toSdCard) },
                                        // Overlay detail integration:
                                        selectedDetailIndex = if (detailSource == DetailSource.ALBUM) selectedItemIndex else null,
                                        onCloseDetail = { viewModel.closeDetail() },
                                        onDetailIndexChanged = { viewModel.updateDetailIndex(it) },
                                        onTrashItem = { item -> viewModel.trashMedia(item) },
                                        onRenameItem = { item, newName -> viewModel.renameMedia(item, newName) },
                                        isFavorite = { id -> favoriteIds.contains(id) },
                                        onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                                        // Multi-selection inside an album:
                                        isSelectionMode = isSelectionMode,
                                        selectedIds = selectedIds,
                                        onStartSelection = { id -> viewModel.startSelection(id) },
                                        onToggleSelection = { id -> viewModel.toggleSelection(id) },
                                        onSetSelection = { id, sel -> viewModel.setItemSelection(id, sel) },
                                        onUpdateSelectedIds = { viewModel.setSelectedIds(it) },
                                        onSelectAll = { viewModel.selectAll(albumMedia) },
                                        onDeselectAll = { viewModel.deselectAll() },
                                        onClearSelection = { viewModel.clearSelection() },
                                        onEnterSelection = { viewModel.startSelection(null) },
                                        onOpenTrash = { viewModel.openTrash() },
                                        albumSort = albumSort,
                                        onSetAlbumSort = { viewModel.setAlbumSort(it) },
                                        onMoveAlbum = { album, steps, toTop -> viewModel.moveAlbum(album, steps, toTop) },
                                        onReorderAlbums = { viewModel.reorderAlbums(it) },
                                        externalKind = viewModel.externalTargetKind,
                                        storageFilter = albumStorageFilter,
                                        onSetStorageFilter = { viewModel.setAlbumStorageFilter(it) },
                                        albumBytes = { viewModel.albumBytes(it) },
                                        freeByStorage = freeByStorage,
                                        onOpenSettings = { isSettingsOpen = true }
                                    )
                                }
                            }
                        }
                    }
                }

                // Content fades softly into the background behind the bottom bar and the system
                // navigation area (sized to its inset, so it fits both 3-button and gesture navigation).
                BottomEdgeFade(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    visible = !isFullscreenViewer
                )

                val barEnter = slideInVertically(animationSpec = qualityTween(graphicsQuality, 260), initialOffsetY = { it }) +
                    fadeIn(animationSpec = qualityTween(graphicsQuality, 200))
                // Opening a photo: the bar stays until the zooming photo has grown over it and then leaves
                // with the end of the zoom, instead of vanishing before the photo is fully open.
                val barExit = if (isFullscreenViewer) {
                    slideOutVertically(animationSpec = tween(260, delayMillis = 110, easing = FastOutSlowInEasing), targetOffsetY = { it }) +
                        fadeOut(animationSpec = tween(200, delayMillis = 150))
                } else {
                    slideOutVertically(animationSpec = qualityTween(graphicsQuality, 220), targetOffsetY = { it }) +
                        fadeOut(animationSpec = qualityTween(graphicsQuality, 160))
                }

                // One UI hides the tab bar inside an album.
                val oneUiPushedPage = selectedTab == GalleryTab.ALBEN && breadcrumbStack.isNotEmpty()
                AnimatedVisibility(
                    visible = !isFullscreenViewer && !isSelectionMode && !oneUiPushedPage,
                    enter = barEnter,
                    exit = barExit,
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    BottomNavigationBar(
                        currentTab = selectedTab,
                        onTabSelected = { tab -> viewModel.selectTab(tab) }
                    )
                }

                AnimatedVisibility(
                    visible = !isFullscreenViewer && isSelectionMode,
                    enter = barEnter,
                    exit = barExit,
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    SelectionActionBar(
                        selectedCount = selectedIds.size,
                        onShare = {
                            val uris = (allItems + albumMedia).distinctBy { it.id }
                                .filter { selectedIds.contains(it.id) }
                                .map { it.uri }
                            if (uris.isNotEmpty()) {
                                val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                    type = "*/*"
                                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, strings.shareMedia))
                            }
                        },
                        onMove = { viewModel.openMoveSheet() },
                        onDelete = { viewModel.trashSelectedMedia() }
                    )
                }
            }

            // Move to Album Bottom Sheet
            if (isMoveSheetOpen) {
                MoveToAlbumBottomSheet(
                    albums = rootAlbums,
                    allAlbums = allAlbumsFlat,
                    onDismiss = { viewModel.closeMoveSheet() },
                    onSelectAlbum = { targetAlbum -> viewModel.moveSelectedItems(targetAlbum) },
                    onCreateNewAlbum = { showCreateAlbumFromMove = true },
                    selectionCount = selectedIds.size,
                    selectionBytes = selectionBytes,
                    freeByStorage = freeByStorage
                )
            }

            // Create Album dialog triggered from move bottom sheet
            if (showCreateAlbumFromMove) {
                CreateAlbumDialog(
                    isSdCardAvailable = viewModel.isSdCardAvailable,
                    externalKind = viewModel.externalTargetKind,
                    freeByStorage = freeByStorage,
                    onDismiss = { showCreateAlbumFromMove = false },
                    onCreate = { name, onSdCard ->
                        showCreateAlbumFromMove = false
                        viewModel.createAlbumAndMove(name, onSdCard)
                    }
                )
            }
        }

        if (pushedScreen != null || pushStillVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pushTransition({ pushProgress.value }, isTop = true)
                    .screenBackground()
                    .blockTouchesBelow()
            ) {
                when (lastPushedScreen[0]) {
                    PushedScreen.FAVORITES -> FavoritesScreen(
                        favoriteItems = favoriteMedia,
                        onBack = { isFavoritesOpen = false },
                        onTrashItem = { item -> viewModel.trashMedia(item) },
                        onRenameItem = { item, newName -> viewModel.renameMedia(item, newName) },
                        isFavorite = { id -> favoriteIds.contains(id) },
                        onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                        columnCount = 4
                    )

                    PushedScreen.TRASH -> {
                        BackHandler(enabled = isTrashOpen) {
                            if (selectedItemIndex != null) {
                                viewModel.closeDetail()
                            } else {
                                viewModel.closeTrash()
                            }
                        }
                        val trashDetailIndex = remember { IntArray(1) }
                        if (detailSource == DetailSource.TRASH) selectedItemIndex?.let { trashDetailIndex[0] = it }

                        Box(modifier = Modifier.fillMaxSize()) {
                            TrashScreen(
                                trashedItems = trashedMedia,
                                onBack = { viewModel.closeTrash() },
                                onItemClick = { _, index -> viewModel.openDetail(index, DetailSource.TRASH) },
                                onRestoreAll = { viewModel.restoreAllTrash() },
                                onEmptyTrash = { viewModel.emptyTrash() },
                                onRestoreItems = { viewModel.restoreItems(it) },
                                onDeleteItems = { viewModel.deleteItemsPermanently(it) }
                            )
                            // TrashScreen draws its own bottom fade so the selection action bar stays
                            // above it, exactly like the album grids.

                            // A deleted item opens full size too, with only restore / delete permanently / details.
                            MediaZoomTransition(visible = selectedItemIndex != null && detailSource == DetailSource.TRASH) {
                                MediaDetailScreen(
                                    mediaList = trashedMedia,
                                    initialIndex = trashDetailIndex[0],
                                    onBack = { viewModel.closeDetail() },
                                    onTrashItem = {},
                                    onRenameItem = { _, _ -> },
                                    onPageChanged = { viewModel.updateDetailIndex(it) },
                                    trashMode = true,
                                    onRestoreItem = { item -> viewModel.restoreMedia(item) },
                                    onDeletePermanently = { item -> viewModel.deletePermanently(item) }
                                )
                            }
                        }
                    }

                    PushedScreen.SETTINGS -> {
                        BackHandler(enabled = isSettingsOpen && !isFavoritesOpen && !isTrashOpen) {
                            isSettingsOpen = false
                        }
                        val internalStorage = remember { viewModel.getInternalStorageSpace() }
                        val sdCardStorage = remember { viewModel.getSdCardStorageSpace() }
                        MenuScreen(
                            favoriteCount = favoriteMedia.size,
                            trashedCount = trashedMedia.size,
                            internalFreeBytes = internalStorage.freeBytes,
                            internalTotalBytes = internalStorage.totalBytes,
                            sdCardFreeBytes = sdCardStorage?.freeBytes,
                            sdCardTotalBytes = sdCardStorage?.totalBytes,
                            appVersionName = appVersionName,
                            graphicsQuality = graphicsQuality,
                            onSetGraphicsQuality = { viewModel.setGraphicsQuality(it) },
                            onOpenFavorites = { isFavoritesOpen = true },
                            onOpenTrash = { viewModel.openTrash() },
                            loadStorageStats = { viewModel.loadStorageStats() },
                            onEmptyTrash = { viewModel.emptyTrash() },
                            onOpenLibrary = {
                                isSettingsOpen = false
                                viewModel.selectTab(GalleryTab.BILDER)
                            },
                            onBack = { isSettingsOpen = false }
                        )
                    }
                }
            }
        }
        }
    }

    // In-app progress dialog for media move / album migration, in One UI style.
    // Rendered outside the tab-dependent content so it stays visible regardless of the active tab.
    if (moveProgress.isMoving) {
        val palette = LocalAppTheme.current.palette
        ThemedDialog(
            onDismissRequest = { },
            title = strings.movingMediaTitle,
            dismissible = false
        ) {
            ThemedProgressBar(
                progress = if (moveProgress.total > 0) moveProgress.current.toFloat() / moveProgress.total.toFloat() else 0f,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = strings.progressOf(moveProgress.current, moveProgress.total),
                color = palette.textMuted,
                fontSize = 14.sp
            )
        }
    }
}

private fun getRequiredPermissions(): Array<String> {
    val media = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    } else {
        // Android 10 still uses legacy storage, where writing needs its own permission.
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
    // Where a photo was taken: without it Android strips the GPS tags, so the details never show the place.
    // Optional - the gallery works without it (hasMediaPermission does not ask for it).
    return media + Manifest.permission.ACCESS_MEDIA_LOCATION
}

private fun hasMediaPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
    }
}
