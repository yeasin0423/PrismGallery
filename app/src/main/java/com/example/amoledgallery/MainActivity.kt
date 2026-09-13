package com.example.amoledgallery

import android.Manifest
import android.app.Activity
import android.content.Context
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.amoledgallery.ui.qualityTween
import com.example.amoledgallery.ui.components.BottomNavigationBar
import com.example.amoledgallery.ui.components.MoveToAlbumBottomSheet
import com.example.amoledgallery.ui.screens.AlbumsScreen
import com.example.amoledgallery.ui.screens.CreateAlbumDialog
import com.example.amoledgallery.ui.screens.FavoritesScreen
import com.example.amoledgallery.ui.screens.GalleryScreen
import com.example.amoledgallery.ui.screens.MediaDetailScreen
import com.example.amoledgallery.ui.screens.MenuScreen
import com.example.amoledgallery.ui.screens.PermissionScreen
import com.example.amoledgallery.ui.screens.TrashScreen
import com.example.amoledgallery.ui.theme.AmoledBlack
import com.example.amoledgallery.ui.theme.AmoledGalleryTheme
import com.example.amoledgallery.ui.theme.OneUiBlue
import com.example.amoledgallery.ui.theme.PureWhite
import com.example.amoledgallery.ui.theme.SurfaceDark
import com.example.amoledgallery.ui.theme.SurfaceVariantDark
import com.example.amoledgallery.ui.theme.TextMuted
import com.example.amoledgallery.ui.viewmodel.DetailSource
import com.example.amoledgallery.ui.viewmodel.GalleryTab
import com.example.amoledgallery.ui.viewmodel.GalleryUiState
import com.example.amoledgallery.ui.viewmodel.GalleryViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GalleryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AmoledGalleryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AmoledBlack
                ) {
                    GalleryApp(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GalleryApp(viewModel: GalleryViewModel) {
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
    val isMoveProgressVisible by viewModel.isMoveProgressVisible.collectAsStateWithLifecycle()

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

    // Explicitly request POST_NOTIFICATIONS on Android 13+ (API 33+) if not yet granted
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
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
        PermissionScreen(
            onRequestPermission = {
                permissionLauncher.launch(getRequiredPermissions())
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
                columnCount = 5
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

            // Main App Scaffold with Bottom Navigation
            // Kept permanently mounted so GalleryScreen LazyGridState is preserved!
            Scaffold(
                containerColor = AmoledBlack,
                bottomBar = {
                    AnimatedVisibility(
                        visible = !isFullscreenViewer,
                        enter = slideInVertically(animationSpec = qualityTween(graphicsQuality, 250), initialOffsetY = { it }) +
                            fadeIn(animationSpec = qualityTween(graphicsQuality, 200)),
                        exit = slideOutVertically(animationSpec = qualityTween(graphicsQuality, 250), targetOffsetY = { it }) +
                            fadeOut(animationSpec = qualityTween(graphicsQuality, 200))
                    ) {
                        BottomNavigationBar(
                            currentTab = selectedTab,
                            onTabSelected = { viewModel.selectTab(it) }
                        )
                    }
                }
            ) { innerPadding ->
                val bottomPadding = if (isFullscreenViewer) 0.dp else innerPadding.calculateBottomPadding()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = bottomPadding)
                ) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            fadeIn(animationSpec = qualityTween(graphicsQuality, 200)) togetherWith
                                fadeOut(animationSpec = qualityTween(graphicsQuality, 200))
                        },
                        label = "tab_transition"
                    ) { tab ->
                        when (tab) {
                            GalleryTab.BILDER -> {
                                val allItems = (uiState as? GalleryUiState.Success)?.items ?: emptyList()
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
                                    onTrashItem = { item -> viewModel.trashMedia(item) },
                                    onRenameItem = { item, newName -> viewModel.renameMedia(item, newName) },
                                    isFavorite = { id -> favoriteIds.contains(id) },
                                    onToggleFavorite = { id -> viewModel.toggleFavorite(id) }
                                )
                            }

                            GalleryTab.MENU -> {
                                val internalStorage = remember(selectedTab) { viewModel.getInternalStorageSpace() }
                                val sdCardStorage = remember(selectedTab) { viewModel.getSdCardStorageSpace() }
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

    // Floating OneUI In-App Progress Dialog for Media Move / Album-Migration Operations.
    // Rendered outside the tab-dependent content so it stays visible regardless of the active tab.
    if (moveProgress.isMoving) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Verschiebe Medien...", color = PureWhite) },
            text = {
                Column {
                    LinearProgressIndicator(
                        progress = { if (moveProgress.total > 0) moveProgress.current.toFloat() / moveProgress.total.toFloat() else 0f },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                    Text("${moveProgress.current} von ${moveProgress.total}", color = TextMuted)
                }
            },
            confirmButton = {},
            containerColor = SurfaceVariantDark
        )
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
