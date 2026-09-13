package com.example.amoledgallery.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.SdCard
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.os.Build
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.bitmapConfig
import android.graphics.Bitmap
import androidx.compose.ui.platform.LocalContext
import com.example.amoledgallery.model.AlbumItem
import com.example.amoledgallery.model.MediaItem
import com.example.amoledgallery.ui.components.MediaGridItem
import com.example.amoledgallery.ui.qualityTween
import com.example.amoledgallery.ui.theme.AmoledBlack
import com.example.amoledgallery.ui.theme.LocalAppTheme
import com.example.amoledgallery.ui.theme.OneUiCardBorder
import com.example.amoledgallery.ui.theme.PureWhite
import com.example.amoledgallery.ui.theme.SurfaceDark
import com.example.amoledgallery.ui.theme.SurfaceVariantDark
import com.example.amoledgallery.ui.theme.TextMuted

/**
 * Hierarchical Albums Screen featuring:
 * - 3-column compact grid layout (GridCells.Fixed(3)) for root albums
 * - OneUI typography and compact square album cards
 * - Long-press & menu actions to hide albums
 * - 3-dots top bar menu to manage hidden albums
 * - Sub-album creation and breadcrumb navigation
 * - Fullscreen detail viewer overlay over album media items
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
    onTrashItem: (MediaItem) -> Unit = {},
    onRenameItem: (MediaItem, String) -> Unit = { _, _ -> },
    isFavorite: (Long) -> Boolean = { false },
    onToggleFavorite: (Long) -> Unit = {},
    // Album management actions
    onRenameAlbum: (AlbumItem, String) -> Unit = { _, _ -> },
    onDeleteAlbum: (AlbumItem) -> Unit = {},
    onMigrateAlbum: (AlbumItem, Boolean) -> Unit = { _, _ -> },
    graphicsQuality: com.example.amoledgallery.PerformanceProfile = com.example.amoledgallery.PerformanceProfile.MEDIUM
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showCreateSubDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showManageHiddenDialog by remember { mutableStateOf(false) }
    var contextMenuAlbum by remember { mutableStateOf<AlbumItem?>(null) }
    var albumToRename by remember { mutableStateOf<AlbumItem?>(null) }
    var albumToDelete by remember { mutableStateOf<AlbumItem?>(null) }
    var albumToMigrate by remember { mutableStateOf<AlbumItem?>(null) }
    var albumToHide by remember { mutableStateOf<AlbumItem?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        // Grid columns: 3 for root album cards, or columnCount when inside an album
        val isInsideAlbum = currentAlbum != null
        val effectiveColumns = if (isInsideAlbum) columnCount else 3

        LazyVerticalGrid(
            columns = GridCells.Fixed(effectiveColumns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 12.dp,
                bottom = 76.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Header with Breadcrumbs & Action buttons
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 12.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isInsideAlbum && breadcrumbs.isNotEmpty()) {
                        // Breadcrumb Navigation ("Alben > Urlaub > Tag 1")
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Alben",
                                color = TextMuted,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onBreadcrumbClick(-1) }
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                            )

                            breadcrumbs.forEachIndexed { index, crumb ->
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )

                                val isCurrent = index == breadcrumbs.size - 1
                                Text(
                                    text = crumb.name,
                                    color = if (isCurrent) PureWhite else TextMuted,
                                    fontSize = 19.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable(enabled = !isCurrent) { onBreadcrumbClick(index) }
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // "+"-Button for creating sub-album inside active album
                        IconButton(
                            onClick = { showCreateSubDialog = true },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(SurfaceVariantDark)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Neues Unteralbum",
                                tint = PureWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        // Root Albums Header: Title, Count, Add Button & Overflow Menu
                        Column {
                            Text(
                                text = "Alben",
                                color = PureWhite,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (albums.size == 1) "1 Album" else "${albums.size} Alben",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showCreateDialog = true },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceVariantDark)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Neues Album",
                                    tint = PureWhite,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // TopBar 3-dots menu for hidden albums management
                            Box {
                                IconButton(
                                    onClick = { showMenu = true },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(SurfaceVariantDark)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MoreVert,
                                        contentDescription = "Menü",
                                        tint = PureWhite,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    modifier = Modifier.background(SurfaceVariantDark),
                                    shape = RoundedCornerShape(LocalAppTheme.current.shapes.cardCornerRadius)
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Rounded.VisibilityOff,
                                                    contentDescription = null,
                                                    tint = PureWhite,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text("Ausgeblendete Alben verwalten", color = PureWhite)
                                            }
                                        },
                                        onClick = {
                                            showMenu = false
                                            showManageHiddenDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sub-Albums Section (when viewing inside an album)
            if (isInsideAlbum && albums.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Unteralben (${albums.size})",
                        color = PureWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                // Display sub-albums in 3-column chunks
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        albums.chunked(3).forEach { rowAlbums ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowAlbums.forEach { subAlbum ->
                                    AlbumCard(
                                        album = subAlbum,
                                        onClick = { onAlbumClick(subAlbum) },
                                        onMenuClick = { contextMenuAlbum = subAlbum },
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

            // Root Albums List (3-column fixed grid with compact OneUI layout)
            if (!isInsideAlbum) {
                items(albums, key = { it.bucketId }) { album ->
                    AlbumCard(
                        album = album,
                        onClick = { onAlbumClick(album) },
                        onMenuClick = { contextMenuAlbum = album }
                    )
                }
            }

            // Media items belonging directly to the active album
            if (isInsideAlbum) {
                if (albumMedia.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "Medien (${albumMedia.size})",
                            color = PureWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                    }

                    items(
                        items = albumMedia,
                        key = { it.id },
                        contentType = { "media_tile" }
                    ) { item ->
                        val index = albumMedia.indexOf(item)
                        MediaGridItem(
                            item = item,
                            isFavorite = isFavorite(item.id),
                            onClick = { onMediaItemClick(item, index) },
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clipToBounds()
                        )
                    }
                } else if (albums.isEmpty()) {
                    // Completely empty album
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Dieses Album ist leer",
                                    color = TextMuted,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { showCreateSubDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SurfaceVariantDark,
                                        contentColor = PureWhite
                                    ),
                                    shape = RoundedCornerShape(LocalAppTheme.current.shapes.dialogCornerRadius)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Unteralbum anlegen")
                                }
                            }
                        }
                    }
                }
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
                    onToggleFavorite = onToggleFavorite
                )
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

        // Hide Album Confirmation Dialog
        albumToHide?.let { album ->
            AlertDialog(
                onDismissRequest = { albumToHide = null },
                containerColor = SurfaceVariantDark,
                title = {
                    Text(
                        text = "Album ausblenden?",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                },
                text = {
                    Text(
                        text = "Das Album '${album.name}' und seine Inhalte werden in der Galerie und der Alben-Übersicht ausgeblendet. Du kannst es jederzeit über 'Ausgeblendete Alben verwalten' wieder einblenden.",
                        color = PureWhite,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            albumToHide = null
                            onHideAlbum(album)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PureWhite,
                            contentColor = AmoledBlack
                        ),
                        shape = RoundedCornerShape(LocalAppTheme.current.shapes.dialogCornerRadius)
                    ) {
                        Text("Ausblenden", fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { albumToHide = null }) {
                        Text("Abbrechen", color = TextMuted)
                    }
                }
            )
        }

        // Album Context Action Menu Dialog
        contextMenuAlbum?.let { album ->
            AlbumActionDialog(
                album = album,
                isSdCardAvailable = isSdCardAvailable,
                onDismiss = { contextMenuAlbum = null },
                onRename = {
                    contextMenuAlbum = null
                    albumToRename = album
                },
                onMigrate = {
                    contextMenuAlbum = null
                    albumToMigrate = album
                },
                onHide = {
                    contextMenuAlbum = null
                    albumToHide = album
                },
                onDelete = {
                    contextMenuAlbum = null
                    albumToDelete = album
                }
            )
        }

        // Rename Album Dialog
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

        // Delete Album Confirmation Dialog
        albumToDelete?.let { album ->
            DeleteAlbumDialog(
                album = album,
                onDismiss = { albumToDelete = null },
                onDelete = {
                    albumToDelete = null
                    onDeleteAlbum(album)
                }
            )
        }

        // Migrate Storage Confirmation Dialog
        albumToMigrate?.let { album ->
            MigrateAlbumDialog(
                album = album,
                toSdCard = !album.isSdCard,
                onDismiss = { albumToMigrate = null },
                onMigrate = {
                    albumToMigrate = null
                    onMigrateAlbum(album, !album.isSdCard)
                }
            )
        }

        // Manage Hidden Albums Dialog
        if (showManageHiddenDialog) {
            ManageHiddenAlbumsDialog(
                hiddenAlbums = hiddenAlbums,
                onDismiss = { showManageHiddenDialog = false },
                onUnhideAlbum = onUnhideAlbum
            )
        }
    }
}

/**
 * Compact 1:1 square OneUI Album Card:
 * Scaled for 3-column layouts with smaller typography.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumCard(
    album: AlbumItem,
    onClick: () -> Unit,
    onMenuClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onMenuClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clipToBounds()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceDark),
            contentAlignment = Alignment.Center
        ) {
            if (album.coverUri != null) {
                AsyncImage(
                    model = album.coverUri,
                    contentDescription = album.name,
                    modifier = Modifier.fillMaxSize().aspectRatio(1f).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(36.dp)
                )
            }

            // SD Card badge
            if (album.isSdCard) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AmoledBlack.copy(alpha = 0.75f))
                        .padding(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SdCard,
                        contentDescription = "SD-Karte",
                        tint = PureWhite,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // 3-Dots Action Menu Button (Top-End)
            IconButton(
                onClick = { onMenuClick?.invoke() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AmoledBlack.copy(alpha = 0.65f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Optionen",
                    tint = PureWhite,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = album.name,
            color = PureWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val countText = buildString {
            append("${album.count}")
            if (album.subAlbumCount > 0) {
                append(" • ${album.subAlbumCount} Sub")
            }
        }

        Text(
            text = countText,
            color = TextMuted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Dialog to manage and unhide hidden albums.
 */
@Composable
private fun ManageHiddenAlbumsDialog(
    hiddenAlbums: List<AlbumItem>,
    onDismiss: () -> Unit,
    onUnhideAlbum: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceVariantDark,
        shape = RoundedCornerShape(LocalAppTheme.current.shapes.dialogCornerRadius),
        title = {
            Text(
                text = "Ausgeblendete Alben",
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            if (hiddenAlbums.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Keine ausgeblendeten Alben vorhanden",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(hiddenAlbums, key = { it.bucketId }) { album ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceDark)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceVariantDark),
                                contentAlignment = Alignment.Center
                            ) {
                                if (album.coverUri != null) {
                                    AsyncImage(
                                        model = album.coverUri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().aspectRatio(1f).clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Folder,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = album.name,
                                    color = PureWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${album.count} Elemente",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }

                            IconButton(
                                onClick = { onUnhideAlbum(album.bucketId) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Visibility,
                                    contentDescription = "Wieder einblenden",
                                    tint = PureWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PureWhite,
                    contentColor = AmoledBlack
                ),
                shape = RoundedCornerShape(LocalAppTheme.current.shapes.dialogCornerRadius)
            ) {
                Text("Fertig", fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun CreateSubAlbumDialog(
    parentAlbum: AlbumItem,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var subAlbumName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceVariantDark,
        title = {
            Text(
                text = "Neues Unteralbum erstellen",
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Übergeordnet: ${parentAlbum.name}",
                    color = PureWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Speicherort: " + if (parentAlbum.isSdCard) "SD-Karte (${parentAlbum.relativePath.ifBlank { parentAlbum.name }})" else "Intern (${parentAlbum.relativePath.ifBlank { parentAlbum.name }})",
                    color = TextMuted,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = subAlbumName,
                    onValueChange = { subAlbumName = it },
                    label = { Text("Name des Unteralbums", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        focusedBorderColor = PureWhite,
                        unfocusedBorderColor = OneUiCardBorder,
                        cursorColor = PureWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subAlbumName.isNotBlank()) {
                        onCreate(subAlbumName.trim())
                    }
                },
                enabled = subAlbumName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PureWhite,
                    contentColor = AmoledBlack,
                    disabledContainerColor = SurfaceDark,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(LocalAppTheme.current.shapes.dialogCornerRadius)
            ) {
                Text("Erstellen", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen", color = TextMuted)
            }
        }
    )
}

@Composable
fun CreateAlbumDialog(
    isSdCardAvailable: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, Boolean) -> Unit
) {
    var albumName by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf("internal") } // "internal" or "sd"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceVariantDark,
        title = {
            Text(
                text = "Neues Album erstellen",
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = albumName,
                    onValueChange = { albumName = it },
                    label = { Text("Albumname", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        focusedBorderColor = PureWhite,
                        unfocusedBorderColor = OneUiCardBorder,
                        cursorColor = PureWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Speicherort:",
                    color = TextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Option 1: Internal Storage
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedLocation == "internal",
                            onClick = { selectedLocation = "internal" }
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedLocation == "internal",
                        onClick = { selectedLocation = "internal" },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = PureWhite,
                            unselectedColor = TextMuted
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = "Interner Speicher", color = PureWhite, fontSize = 14.sp)
                        Text(text = "Pictures/$albumName", color = TextMuted, fontSize = 11.sp)
                    }
                }

                // Option 2: SD Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedLocation == "sd",
                            enabled = isSdCardAvailable,
                            onClick = { if (isSdCardAvailable) selectedLocation = "sd" }
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedLocation == "sd",
                        enabled = isSdCardAvailable,
                        onClick = { if (isSdCardAvailable) selectedLocation = "sd" },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = PureWhite,
                            unselectedColor = TextMuted,
                            disabledSelectedColor = TextMuted,
                            disabledUnselectedColor = Color(0xFF444444)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isSdCardAvailable) "SD-Karte" else "SD-Karte (Nicht verfügbar)",
                            color = if (isSdCardAvailable) PureWhite else TextMuted,
                            fontSize = 14.sp
                        )
                        if (isSdCardAvailable) {
                            Text(text = "Externer Speicher", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (albumName.isNotBlank()) {
                        onCreate(albumName.trim(), selectedLocation == "sd")
                    }
                },
                enabled = albumName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PureWhite,
                    contentColor = AmoledBlack,
                    disabledContainerColor = SurfaceDark,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(LocalAppTheme.current.shapes.dialogCornerRadius)
            ) {
                Text("Erstellen", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen", color = TextMuted)
            }
        }
    )
}

/**
 * OneUI action menu dialog for an album:
 * Rename, Storage Migration (SD <-> Internal), Hide, and Delete.
 */
@Composable
private fun AlbumActionDialog(
    album: AlbumItem,
    isSdCardAvailable: Boolean,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onMigrate: () -> Unit,
    onHide: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceVariantDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceDark),
                    contentAlignment = Alignment.Center
                ) {
                    if (album.coverThumbnail != null) {
                        AsyncImage(
                            model = album.coverThumbnail,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = album.name,
                        color = PureWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${album.count} Elemente • " + if (album.isSdCard) "SD-Karte" else "Intern",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 1. Rename Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onRename() }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Album umbenennen",
                        color = PureWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 2. Storage Migration Option (SD <-> Internal)
                val migrateLabel = if (album.isSdCard) "Auf internen Speicher verschieben" else "Auf SD-Karte verschieben"
                val canMigrate = if (album.isSdCard) true else isSdCardAvailable
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = canMigrate) { onMigrate() }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (album.isSdCard) Icons.Rounded.PhoneAndroid else Icons.Rounded.SdCard,
                        contentDescription = null,
                        tint = if (canMigrate) PureWhite else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = migrateLabel,
                            color = if (canMigrate) PureWhite else TextMuted,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (!album.isSdCard && !isSdCardAvailable) {
                            Text(
                                text = "Keine SD-Karte verfügbar",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // 3. Hide Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onHide() }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Album ausblenden",
                        color = PureWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 4. Delete Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onDelete() }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Album löschen",
                        color = Color(0xFFFF5252),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen", color = TextMuted)
            }
        }
    )
}

/**
 * Dialog to rename an album.
 */
@Composable
private fun RenameAlbumDialog(
    album: AlbumItem,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var newName by remember { mutableStateOf(album.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceVariantDark,
        title = {
            Text(
                text = "Album umbenennen",
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Neuer Albumname", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        focusedBorderColor = PureWhite,
                        unfocusedBorderColor = OneUiCardBorder,
                        cursorColor = PureWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.isNotBlank() && newName.trim() != album.name) {
                        onRename(newName.trim())
                    }
                },
                enabled = newName.isNotBlank() && newName.trim() != album.name,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PureWhite,
                    contentColor = AmoledBlack,
                    disabledContainerColor = SurfaceDark,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(LocalAppTheme.current.shapes.dialogCornerRadius)
            ) {
                Text("Umbenennen", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen", color = TextMuted)
            }
        }
    )
}

/**
 * Confirmation dialog to delete an album.
 */
@Composable
private fun DeleteAlbumDialog(
    album: AlbumItem,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceVariantDark,
        title = {
            Text(
                text = "Album '${album.name}' löschen?",
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Text(
                text = if (album.count > 0) {
                    "Alle ${album.count} Medien in diesem Album werden in den Papierkorb verschoben und der Ordner wird entfernt."
                } else {
                    "Der leere Ordner '${album.name}' wird dauerhaft gelöscht."
                },
                color = PureWhite,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF5252),
                    contentColor = PureWhite
                ),
                shape = RoundedCornerShape(LocalAppTheme.current.shapes.dialogCornerRadius)
            ) {
                Text("Löschen", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen", color = TextMuted)
            }
        }
    )
}

/**
 * Confirmation dialog to migrate an album's storage location between Internal and MicroSD.
 */
@Composable
private fun MigrateAlbumDialog(
    album: AlbumItem,
    toSdCard: Boolean,
    onDismiss: () -> Unit,
    onMigrate: () -> Unit
) {
    val destName = if (toSdCard) "die SD-Karte" else "den internen Speicher"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceVariantDark,
        title = {
            Text(
                text = "Speicherort ändern?",
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Text(
                text = "Möchtest du das Album '${album.name}' mit ${album.count} Element(en) auf $destName verschieben? Die Dateien werden physisch verschoben und der MediaStore-Index sofort aktualisiert.",
                color = PureWhite,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onMigrate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PureWhite,
                    contentColor = AmoledBlack
                ),
                shape = RoundedCornerShape(LocalAppTheme.current.shapes.dialogCornerRadius)
            ) {
                Text("Verschieben", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen", color = TextMuted)
            }
        }
    )
}

