package com.example.amoledgallery.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
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
import com.example.amoledgallery.ui.design.pressScale
import com.example.amoledgallery.ui.qualityTween
import com.example.amoledgallery.ui.theme.DesignLanguage
import com.example.amoledgallery.ui.theme.LocalAppTheme
import com.example.amoledgallery.ui.theme.destructive
import com.example.amoledgallery.ui.theme.onAccent
import dev.chrisbanes.haze.HazeState
import com.example.amoledgallery.ui.design.HazeSource

/**
 * Albums tab. Root albums are laid out per design language:
 * - One UI: 3 columns of rounded covers with name and count underneath.
 * - Material 3 Expressive: 2 columns of tonal cards holding a rounded cover.
 * - iOS: 2 columns of large rounded covers under a "Meine Alben" section title; long-press opens a
 *   Liquid Glass context menu.
 * - HyperOS: 3 columns of white cards with the cover flush on top.
 *
 * Inside an album a frosted top bar with back navigation replaces the large header; sub-albums and the
 * album's media follow. Album actions open as a popup (One UI, iOS) or an action sheet (Material,
 * HyperOS), all dialogs use the language's own dialog style.
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
    onClearSelection: () -> Unit = {}
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showCreateSubDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var menuAnchor by remember { mutableStateOf<Rect?>(null) }
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

    val rootColumns = when (language) {
        DesignLanguage.GOOGLE_EXPRESSIVE, DesignLanguage.IOS_LIQUID_GLASS -> 2
        else -> 3
    }
    val outerPadding = if (isInsideAlbum) theme.shapes.gridHorizontalPadding else when (language) {
        DesignLanguage.GOOGLE_EXPRESSIVE, DesignLanguage.XIAOMI_HYPEROS -> 12.dp
        else -> 16.dp
    }
    val horizontalSpacing = if (isInsideAlbum) theme.shapes.gridSpacing else when (language) {
        DesignLanguage.SAMSUNG_ONEUI -> 12.dp
        DesignLanguage.IOS_LIQUID_GLASS -> 14.dp
        else -> 10.dp
    }
    val verticalSpacing = if (isInsideAlbum) theme.shapes.gridSpacing else when (language) {
        DesignLanguage.SAMSUNG_ONEUI, DesignLanguage.IOS_LIQUID_GLASS -> 20.dp
        else -> 10.dp
    }
    // Section rows inside an album need their own inset when the media grid runs edge-to-edge.
    val sectionInset = if (outerPadding < 8.dp) 16.dp else 4.dp

    val albumGridState = rememberLazyGridState()
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    // Same long-press/tap race guard as the Bilder grid: the tap that ends a long-press must not
    // immediately toggle the item it just selected.
    var lastLongPressId by remember { mutableStateOf<Long?>(null) }
    var lastLongPressTime by remember { mutableLongStateOf(0L) }
    var isLongPressGestureActive by remember { mutableStateOf(false) }

    val openAlbumActions: (AlbumItem, Rect?) -> Unit = { album, anchor ->
        contextAnchor = anchor
        contextMenuAlbum = album
    }

    Box(modifier = modifier.fillMaxSize()) {
        HazeSource(albumsHaze, Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isInsideAlbum) columnCount else rootColumns),
                state = albumGridState,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isInsideAlbum && albumMedia.isNotEmpty()) {
                            Modifier.detectDragSelect(
                                items = albumMedia,
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
                    top = if (isInsideAlbum) statusBarTop + 68.dp else 0.dp,
                    bottom = bottomBarContentPadding(16.dp)
                ),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                verticalArrangement = Arrangement.spacedBy(verticalSpacing)
            ) {
                if (!isInsideAlbum) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "albums_header") {
                        LargeHeader(
                            title = "Alben",
                            subtitle = if (language == DesignLanguage.SAMSUNG_ONEUI) null else albumCountText(albums.size)
                        ) {
                            ThemedIconButton(
                                icon = Icons.Rounded.Add,
                                contentDescription = "Neues Album",
                                onClick = { showCreateDialog = true },
                                hazeState = albumsHaze
                            )
                            ThemedIconButton(
                                icon = if (language == DesignLanguage.IOS_LIQUID_GLASS) Icons.Rounded.MoreHoriz else Icons.Rounded.MoreVert,
                                contentDescription = "Menü",
                                onClick = { showMenu = true },
                                hazeState = albumsHaze,
                                modifier = Modifier.onGloballyPositioned { menuAnchor = it.boundsInRoot() }
                            )
                        }
                    }

                    if (language == DesignLanguage.IOS_LIQUID_GLASS || language == DesignLanguage.XIAOMI_HYPEROS) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "albums_section") {
                            AlbumsSectionTitle(
                                text = if (language == DesignLanguage.IOS_LIQUID_GLASS) "Meine Alben" else "Alle Alben",
                                count = albums.size,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }

                    items(albums, key = { it.bucketId }) { album ->
                        AlbumTile(
                            album = album,
                            onClick = { onAlbumClick(album) },
                            onLongClick = { anchor -> openAlbumActions(album, anchor) }
                        )
                    }

                    if (albums.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "albums_empty") {
                            Text(
                                text = "Noch keine Alben",
                                color = palette.textMuted,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp)
                            )
                        }
                    }
                } else {
                    if (breadcrumbs.size > 1) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "breadcrumbs") {
                            Breadcrumbs(
                                breadcrumbs = breadcrumbs,
                                onBreadcrumbClick = onBreadcrumbClick,
                                modifier = Modifier.padding(horizontal = sectionInset, vertical = 4.dp)
                            )
                        }
                    }

                    if (albums.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "sub_albums_title") {
                            AlbumsSectionTitle("Unteralben", albums.size, Modifier.padding(horizontal = sectionInset))
                        }
                        item(span = { GridItemSpan(maxLineSpan) }, key = "sub_albums") {
                            Column(
                                modifier = Modifier.padding(horizontal = sectionInset, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                albums.chunked(3).forEach { rowAlbums ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                        repeat(3 - rowAlbums.size) {
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
                                AlbumsSectionTitle("Medien", albumMedia.size, Modifier.padding(horizontal = sectionInset, vertical = 6.dp))
                            }
                        }
                        items(
                            items = albumMedia,
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
                                        else -> onMediaItemClick(item, albumMedia.indexOf(item))
                                    }
                                },
                                modifier = Modifier
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
                                Text("Dieses Album ist leer", color = palette.textMuted, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(18.dp))
                                ThemedButton(
                                    text = "Unteralbum anlegen",
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
                selectedCount = selectedIds.count { id -> albumMedia.any { it.id == id } },
                totalCount = albumMedia.size,
                onToggleSelectAll = {
                    if (albumMedia.isNotEmpty() && albumMedia.all { selectedIds.contains(it.id) }) onDeselectAll() else onSelectAll()
                },
                onClose = onClearSelection,
                hazeState = albumsHaze,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        } else if (isInsideAlbum && currentAlbum != null) {
            ThemedTopBar(
                title = currentAlbum.name,
                subtitle = if (albumMedia.size == 1) "1 Element" else "${albumMedia.size} Elemente",
                onBack = { onBreadcrumbClick(breadcrumbs.size - 2) },
                hazeState = albumsHaze,
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                ThemedIconButton(
                    icon = Icons.Rounded.CreateNewFolder,
                    contentDescription = "Neues Unteralbum",
                    onClick = { showCreateSubDialog = true },
                    hazeState = albumsHaze
                )
            }
        }

        // FULLSCREEN OVERLAY: Media Detail Viewer inside Albums
        // Preserves scroll position of album media when returning
        AnimatedVisibility(
            visible = selectedDetailIndex != null,
            enter = fadeIn(animationSpec = qualityTween(graphicsQuality, 200)),
            exit = fadeOut(animationSpec = qualityTween(graphicsQuality, 200))
        ) {
            selectedDetailIndex?.let { index ->
                MediaDetailScreen(
                    mediaList = albumMedia,
                    initialIndex = index,
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

    ThemedPopupMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
        anchorBounds = menuAnchor,
        entries = listOf(
            MenuEntry("Neues Album", icon = Icons.Rounded.Add) { showCreateDialog = true },
            MenuEntry("Ausgeblendete Alben", icon = Icons.Rounded.VisibilityOff) { showManageHiddenDialog = true }
        )
    )

    // Album actions: a popup anchored on the tile for One UI / iOS, an action sheet elsewhere.
    contextMenuAlbum?.let { album ->
        val canMigrate = album.isSdCard || isSdCardAvailable
        val migrateLabel = if (album.isSdCard) "Auf internen Speicher verschieben" else "Auf SD-Karte verschieben"
        if (language == DesignLanguage.SAMSUNG_ONEUI || language == DesignLanguage.IOS_LIQUID_GLASS) {
            ThemedPopupMenu(
                expanded = true,
                onDismissRequest = { contextMenuAlbum = null },
                anchorBounds = contextAnchor,
                entries = buildList {
                    add(MenuEntry("Umbenennen", icon = Icons.Rounded.Edit) { albumToRename = album })
                    if (canMigrate) {
                        add(MenuEntry(migrateLabel, icon = if (album.isSdCard) Icons.Rounded.PhoneAndroid else Icons.Rounded.SdCard) { albumToMigrate = album })
                    }
                    add(MenuEntry("Ausblenden", icon = Icons.Rounded.VisibilityOff) { albumToHide = album })
                    add(MenuEntry("Löschen", icon = Icons.Rounded.Delete, destructive = true) { albumToDelete = album })
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
                        text = "${album.count} Elemente · " + if (album.isSdCard) "SD-Karte" else "Intern",
                        color = palette.textMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                    )
                    ThemedListSection(
                        inSheet = true,
                        rows = buildList {
                            add(ListRow("Album umbenennen", icon = Icons.Rounded.Edit, iconColor = palette.accent, showChevron = false, onClick = {
                                contextMenuAlbum = null
                                albumToRename = album
                            }))
                            add(ListRow(
                                migrateLabel,
                                subtitle = if (!canMigrate) "Keine SD-Karte verfügbar" else null,
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
                            add(ListRow("Album ausblenden", icon = Icons.Rounded.VisibilityOff, iconColor = Color(0xFF8E8E93), showChevron = false, onClick = {
                                contextMenuAlbum = null
                                albumToHide = album
                            }))
                            add(ListRow("Album löschen", icon = Icons.Rounded.Delete, iconColor = palette.destructive, titleColor = palette.destructive, showChevron = false, onClick = {
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
            title = "Album ausblenden?",
            message = "„${album.name}“ und seine Inhalte werden in der Galerie und in den Alben ausgeblendet. Über „Ausgeblendete Alben“ kannst du es jederzeit wieder einblenden.",
            buttons = listOf(
                DialogButton("Abbrechen", DialogButtonStyle.Cancel) { albumToHide = null },
                DialogButton("Ausblenden", DialogButtonStyle.Primary) {
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
            title = "„${album.name}“ löschen?",
            message = if (album.count > 0) {
                "Alle ${album.count} Medien in diesem Album werden in den Papierkorb verschoben und der Ordner wird entfernt."
            } else {
                "Der leere Ordner „${album.name}“ wird dauerhaft gelöscht."
            },
            buttons = listOf(
                DialogButton("Abbrechen", DialogButtonStyle.Cancel) { albumToDelete = null },
                DialogButton("Löschen", DialogButtonStyle.Destructive) {
                    albumToDelete = null
                    onDeleteAlbum(album)
                }
            )
        )
    }

    albumToMigrate?.let { album ->
        val toSdCard = !album.isSdCard
        val destName = if (toSdCard) "die SD-Karte" else "den internen Speicher"
        ThemedDialog(
            onDismissRequest = { albumToMigrate = null },
            title = "Speicherort ändern?",
            message = "„${album.name}“ mit ${album.count} Element(en) wird auf $destName verschoben. Die Dateien werden physisch verschoben und der Medienindex sofort aktualisiert.",
            buttons = listOf(
                DialogButton("Abbrechen", DialogButtonStyle.Cancel) { albumToMigrate = null },
                DialogButton("Verschieben", DialogButtonStyle.Primary) {
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

private fun albumCountText(count: Int) = if (count == 1) "1 Album" else "$count Alben"

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
        if (album.subAlbumCount > 0) append(" · ${album.subAlbumCount} Unteralben")
    }
    val clickModifier = Modifier
        .onGloballyPositioned { bounds = it.boundsInRoot() }
        .combinedClickable(
            interactionSource = interaction,
            indication = if (language == DesignLanguage.IOS_LIQUID_GLASS || language == DesignLanguage.GOOGLE_EXPRESSIVE) null else ripple(),
            onClick = onClick,
            onLongClick = { onLongClick(bounds) }
        )
    val nameSize = if (compact) 13.sp else 15.sp
    val countSize = if (compact) 11.sp else 13.sp

    when (language) {
        DesignLanguage.SAMSUNG_ONEUI -> Column(
            modifier = modifier
                .fillMaxWidth()
                .then(clickModifier)
        ) {
            AlbumCover(album, RoundedCornerShape(if (compact) 14.dp else 18.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(album.name, color = palette.onBackground, fontSize = nameSize, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(countText, color = palette.textMuted, fontSize = countSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        DesignLanguage.GOOGLE_EXPRESSIVE -> Column(
            modifier = modifier
                .fillMaxWidth()
                .pressScale(interaction, 0.95f)
                .clip(RoundedCornerShape(if (compact) 20.dp else 28.dp))
                .background(lerp(palette.surface, palette.accent, 0.05f))
                .then(clickModifier)
                .padding(if (compact) 6.dp else 8.dp)
        ) {
            AlbumCover(album, RoundedCornerShape(if (compact) 14.dp else 22.dp))
            Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 6.dp)) {
                Text(album.name, color = palette.onBackground, fontSize = if (compact) 13.sp else 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(countText, color = palette.textMuted, fontSize = countSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        DesignLanguage.IOS_LIQUID_GLASS -> Column(
            modifier = modifier
                .fillMaxWidth()
                .pressScale(interaction, 0.95f)
                .then(clickModifier)
        ) {
            AlbumCover(album, RoundedCornerShape(if (compact) 10.dp else 14.dp))
            Spacer(modifier = Modifier.height(7.dp))
            Text(album.name, color = palette.onBackground, fontSize = nameSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(countText, color = palette.textMuted, fontSize = nameSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        DesignLanguage.XIAOMI_HYPEROS -> Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(if (compact) 14.dp else 18.dp))
                .background(palette.surface)
                .then(clickModifier)
        ) {
            AlbumCover(album, RoundedCornerShape(0.dp))
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
                Text(album.name, color = palette.onBackground, fontSize = if (compact) 12.sp else 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(countText, color = palette.textMuted, fontSize = if (compact) 11.sp else 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun AlbumCover(album: AlbumItem, shape: Shape) {
    val palette = LocalAppTheme.current.palette
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .background(palette.surfaceVariant),
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
                Icon(Icons.Rounded.SdCard, "SD-Karte", tint = Color.White, modifier = Modifier.size(12.dp))
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
        DesignLanguage.GOOGLE_EXPRESSIVE -> Text(
            text = "$text · $count",
            color = palette.accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = modifier.padding(top = 8.dp)
        )
        DesignLanguage.IOS_LIQUID_GLASS -> Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = palette.onBackground, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("$count", color = palette.textMuted, fontSize = 17.sp)
        }
        DesignLanguage.XIAOMI_HYPEROS -> Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = palette.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text("$count", color = palette.textMuted, fontSize = 13.sp)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = palette.textMuted, modifier = Modifier.size(20.dp))
        }
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
        val crumbs = listOf("Alben") + breadcrumbs.map { it.name }
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
    ThemedSheet(onDismissRequest = onDismiss, title = "Ausgeblendete Alben") {
        if (hiddenAlbums.isEmpty()) {
            Text(
                text = "Keine ausgeblendeten Alben vorhanden",
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
                                Text("${album.count} Elemente", color = palette.textMuted, fontSize = 13.sp)
                            }
                            ThemedIconButton(
                                icon = Icons.Rounded.Visibility,
                                contentDescription = "Wieder einblenden",
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
    val location = if (parentAlbum.isSdCard) "SD-Karte" else "Intern"
    ThemedDialog(
        onDismissRequest = onDismiss,
        title = "Neues Unteralbum",
        message = "In „${parentAlbum.name}“ · $location",
        buttons = listOf(
            DialogButton("Abbrechen", DialogButtonStyle.Cancel, onClick = onDismiss),
            DialogButton("Erstellen", DialogButtonStyle.Primary, enabled = subAlbumName.isNotBlank()) {
                if (subAlbumName.isNotBlank()) onCreate(subAlbumName.trim())
            }
        )
    ) {
        ThemedTextField(
            value = subAlbumName,
            onValueChange = { subAlbumName = it },
            placeholder = "Name des Unteralbums",
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
        title = "Neues Album",
        buttons = listOf(
            DialogButton("Abbrechen", DialogButtonStyle.Cancel, onClick = onDismiss),
            DialogButton("Erstellen", DialogButtonStyle.Primary, enabled = albumName.isNotBlank()) {
                if (albumName.isNotBlank()) onCreate(albumName.trim(), onSdCard)
            }
        )
    ) {
        ThemedTextField(
            value = albumName,
            onValueChange = { albumName = it },
            placeholder = "Albumname",
            autoFocus = true
        )
        Spacer(modifier = Modifier.height(14.dp))
        StorageOption(
            title = "Interner Speicher",
            subtitle = "Pictures/${albumName.ifBlank { "…" }}",
            selected = !onSdCard,
            enabled = true
        ) { onSdCard = false }
        StorageOption(
            title = if (isSdCardAvailable) "SD-Karte" else "SD-Karte (nicht verfügbar)",
            subtitle = "Externer Speicher",
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
        title = "Album umbenennen",
        buttons = listOf(
            DialogButton("Abbrechen", DialogButtonStyle.Cancel, onClick = onDismiss),
            DialogButton("Umbenennen", DialogButtonStyle.Primary, enabled = valid) {
                if (valid) onRename(newName.trim())
            }
        )
    ) {
        ThemedTextField(
            value = newName,
            onValueChange = { newName = it },
            placeholder = "Neuer Albumname",
            autoFocus = true
        )
    }
}
