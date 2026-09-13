package com.example.amoledgallery

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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.amoledgallery.ui.components.BottomNavigationBar
import com.example.amoledgallery.ui.components.MoveToAlbumBottomSheet
import com.example.amoledgallery.ui.components.SelectionActionBar
import com.example.amoledgallery.ui.design.LocalContentHaze
import com.example.amoledgallery.ui.design.LocalGraphicsQuality
import com.example.amoledgallery.ui.design.LocalOverlayHost
import com.example.amoledgallery.ui.design.LocalRootHaze
import com.example.amoledgallery.ui.design.OverlayHost
import com.example.amoledgallery.ui.design.OverlayHostState
import com.example.amoledgallery.ui.design.ThemedDialog
import com.example.amoledgallery.ui.design.ThemedProgressBar
import com.example.amoledgallery.ui.design.screenBackground
import com.example.amoledgallery.ui.qualityTween
import com.example.amoledgallery.ui.screens.AlbumsScreen
import com.example.amoledgallery.ui.screens.CreateAlbumDialog
import com.example.amoledgallery.ui.screens.FavoritesScreen
import com.example.amoledgallery.ui.screens.GalleryScreen
import com.example.amoledgallery.ui.screens.MediaDetailScreen
import com.example.amoledgallery.ui.screens.MenuScreen
import com.example.amoledgallery.ui.screens.PermissionScreen
import com.example.amoledgallery.ui.screens.TrashScreen
import com.example.amoledgallery.ui.theme.AmoledGalleryTheme
import com.example.amoledgallery.ui.theme.LocalAppTheme
import com.example.amoledgallery.ui.viewmodel.DetailSource
import com.example.amoledgallery.ui.viewmodel.GalleryTab
import com.example.amoledgallery.ui.viewmodel.GalleryUiState
import com.example.amoledgallery.ui.viewmodel.GalleryViewModel
import dev.chrisbanes.haze.HazeState
import com.example.amoledgallery.ui.design.HazeSource

class MainActivity : ComponentActivity() {

    private val viewModel: GalleryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AmoledGalleryTheme {
                GalleryApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun GalleryApp(viewModel: GalleryViewModel) {
    val graphicsQuality by viewModel.graphicsQuality.collectAsStateWithLifecycle()

    // Two blur backdrops: the whole window (behind dialogs, sheets and menus) and the tab content
    // (behind the floating bottom bar). Overlays render in-window through the OverlayHost so they can
    // blur real app content on every API 31+ device.
    val rootHaze = remember { HazeState() }
    val contentHaze = remember { HazeState() }
    val overlayHost = remember { OverlayHostState() }

    CompositionLocalProvider(
        LocalGraphicsQuality provides graphicsQuality,
        LocalRootHaze provides rootHaze,
        LocalContentHaze provides contentHaze,
        LocalOverlayHost provides overlayHost
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            HazeSource(rootHaze, Modifier.fillMaxSize().screenBackground()) {
                GalleryAppContent(viewModel, contentHaze)
            }
            OverlayHost(overlayHost)
        }
    }
}

@Composable
private fun GalleryAppContent(viewModel: GalleryViewModel, contentHaze: HazeState) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val appVersionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: ""
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedAlbum by viewModel.selectedAlbum.collectAsStateWithLifecycle()
    val breadcrumbStack by viewModel.breadcrumbStack.collectAsStateWithLifecycle()
    val rootAlbums by viewModel.rootAlbums.collectAsStateWithLifecycle()
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

    // Multi-Select and Hidden Albums state
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val isMoveSheetOpen by viewModel.isMoveSheetOpen.collectAsStateWithLifecycle()
    val hiddenAlbums by viewModel.hiddenAlbums.collectAsStateWithLifecycle()
    val graphicsQuality by viewModel.graphicsQuality.collectAsStateWithLifecycle()
    val moveProgress by viewModel.moveProgress.collectAsStateWithLifecycle()

    var isFavoritesOpen by remember { mutableStateOf(false) }
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

    // Ask for everything the app needs in one go on first launch - media access and notifications
    // together - instead of popping the notification dialog first and then making the user tap
    // through a separate screen for media access.
    var hasRequestedPermissions by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!hasRequestedPermissions) {
            hasRequestedPermissions = true
            val missing = getRequiredPermissions().filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
            if (missing.isNotEmpty()) {
                permissionLauncher.launch(missing.toTypedArray())
            }
        }
    }

    // Check permissions on start and when resuming from settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasPerm = hasMediaPermission(context)
                viewModel.updatePermissionState(hasPerm)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (uiState is GalleryUiState.PermissionRequired) {
        val missingNotifications = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        PermissionScreen(
            onRequestPermission = {
                permissionLauncher.launch(getRequiredPermissions())
            },
            missingMedia = true,
            missingNotifications = missingNotifications,
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

        if (isFavoritesOpen) {
            FavoritesScreen(
                favoriteItems = favoriteMedia,
                onBack = { isFavoritesOpen = false },
                onTrashItem = { item -> viewModel.trashMedia(item) },
                onRenameItem = { item, newName -> viewModel.renameMedia(item, newName) },
                isFavorite = { id -> favoriteIds.contains(id) },
                onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                columnCount = 4
            )
        } else if (isTrashOpen) {
            BackHandler {
                if (selectedItemIndex != null) {
                    viewModel.closeDetail()
                } else {
                    viewModel.closeTrash()
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                TrashScreen(
                    trashedItems = trashedMedia,
                    onBack = { viewModel.closeTrash() },
                    onRestoreItem = { item -> viewModel.restoreMedia(item) },
                    onRestoreAll = { viewModel.restoreAllTrash() },
                    onDeletePermanently = { item -> viewModel.deletePermanently(item) },
                    onEmptyTrash = { viewModel.emptyTrash() }
                )

                if (selectedItemIndex != null && detailSource == DetailSource.TRASH) {
                    MediaDetailScreen(
                        mediaList = trashedMedia,
                        initialIndex = selectedItemIndex ?: 0,
                        onBack = { viewModel.closeDetail() },
                        onTrashItem = { item -> viewModel.trashMedia(item) },
                        onRenameItem = { item, newName -> viewModel.renameMedia(item, newName) },
                        isFavorite = { id -> favoriteIds.contains(id) },
                        onToggleFavorite = { id -> viewModel.toggleFavorite(id) }
                    )
                }
            }
        } else {
            val isFullscreenViewer = selectedItemIndex != null
            val allItems = (uiState as? GalleryUiState.Success)?.items ?: emptyList()

            Box(modifier = Modifier.fillMaxSize()) {
                // Tab content draws edge-to-edge underneath the (floating) bottom bar, which blurs it.
                // Each screen reserves bottomBarContentPadding() so its last row scrolls clear of it.
                HazeSource(contentHaze, Modifier.fillMaxSize()) {
                    // All three tabs stay permanently composed (only cross-faded via alpha) instead
                    // of being torn down and rebuilt on every switch, which would destroy and recreate
                    // GalleryScreen's LazyGridState, gesture detectors and thumbnail pre-indexing.
                    GalleryTab.entries.forEach { tab ->
                        val isActiveTab = tab == selectedTab
                        val tabAlpha by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = if (isActiveTab) 1f else 0f,
                            animationSpec = qualityTween(graphicsQuality, 200),
                            label = "tab_fade_$tab"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(if (isActiveTab) 1f else 0f)
                                .graphicsLayer { alpha = tabAlpha }
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
                                        onToggleSelection = { id -> viewModel.toggleSelection(id) },
                                        onSetSelection = { id, sel -> viewModel.setItemSelection(id, sel) },
                                        onSetSelections = { ids, sel -> viewModel.setItemSelections(ids, sel) },
                                        onUpdateSelectedIds = { viewModel.setSelectedIds(it) },
                                        onSelectAll = { viewModel.selectAll(allItems) },
                                        onDeselectAll = { viewModel.deselectAll() },
                                        onClearSelection = { viewModel.clearSelection() },
                                        onMoveClick = { viewModel.openMoveSheet() },
                                        onDeleteSelected = { viewModel.trashSelectedMedia() },
                                        graphicsQuality = graphicsQuality
                                    )
                                }

                                GalleryTab.ALBEN -> {
                                    AlbumsScreen(
                                        albums = if (breadcrumbStack.isNotEmpty()) currentSubAlbums else rootAlbums,
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
                                        onClearSelection = { viewModel.clearSelection() }
                                    )
                                }

                                GalleryTab.MENU -> {
                                    // Keyed on becoming the active tab (not on every selectedTab change) since
                                    // this Box stays permanently composed alongside the other two tabs.
                                    val internalStorage = remember(isActiveTab) { viewModel.getInternalStorageSpace() }
                                    val sdCardStorage = remember(isActiveTab) { viewModel.getSdCardStorageSpace() }
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
                                        onOpenTrash = { viewModel.openTrash() }
                                    )
                                }
                            }
                        }
                    }
                }

                val barEnter = slideInVertically(animationSpec = qualityTween(graphicsQuality, 260), initialOffsetY = { it }) +
                    fadeIn(animationSpec = qualityTween(graphicsQuality, 200))
                val barExit = slideOutVertically(animationSpec = qualityTween(graphicsQuality, 220), targetOffsetY = { it }) +
                    fadeOut(animationSpec = qualityTween(graphicsQuality, 160))

                AnimatedVisibility(
                    visible = !isFullscreenViewer && !isSelectionMode,
                    enter = barEnter,
                    exit = barExit,
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    BottomNavigationBar(
                        currentTab = selectedTab,
                        onTabSelected = { viewModel.selectTab(it) }
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
                                context.startActivity(Intent.createChooser(shareIntent, "Medien teilen"))
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
                    onDismiss = { viewModel.closeMoveSheet() },
                    onSelectAlbum = { targetAlbum -> viewModel.moveSelectedItems(targetAlbum) },
                    onCreateNewAlbum = { showCreateAlbumFromMove = true }
                )
            }

            // Create Album dialog triggered from move bottom sheet
            if (showCreateAlbumFromMove) {
                CreateAlbumDialog(
                    isSdCardAvailable = viewModel.isSdCardAvailable,
                    onDismiss = { showCreateAlbumFromMove = false },
                    onCreate = { name, onSdCard ->
                        showCreateAlbumFromMove = false
                        viewModel.createAlbumAndMove(name, onSdCard)
                    }
                )
            }
        }
    }

    // In-app progress dialog for media move / album migration, styled per design language.
    // Rendered outside the tab-dependent content so it stays visible regardless of the active tab.
    if (moveProgress.isMoving) {
        val palette = LocalAppTheme.current.palette
        ThemedDialog(
            onDismissRequest = { },
            title = "Verschiebe Medien…",
            dismissible = false
        ) {
            ThemedProgressBar(
                progress = if (moveProgress.total > 0) moveProgress.current.toFloat() / moveProgress.total.toFloat() else 0f,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "${moveProgress.current} von ${moveProgress.total}",
                color = palette.textMuted,
                fontSize = 14.sp
            )
        }
    }
}

private fun getRequiredPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
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
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}
