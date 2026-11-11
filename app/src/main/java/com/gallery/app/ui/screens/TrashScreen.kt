package com.gallery.app.ui.screens
import com.gallery.app.ui.i18n.strings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.app.model.MediaItem
import com.gallery.app.ui.components.MediaGridItem
import androidx.compose.ui.draw.clipToBounds
import com.gallery.app.ui.components.SelectionTopBar
import com.gallery.app.ui.components.TrashSelectionActionBar
import com.gallery.app.ui.components.detectDragSelect
import com.gallery.app.ui.design.BottomEdgeFade
import com.gallery.app.ui.design.DialogButton
import com.gallery.app.ui.design.DialogButtonStyle
import com.gallery.app.ui.design.ThemedDialog
import com.gallery.app.ui.design.ThemedEmptyState
import com.gallery.app.ui.design.ThemedTopBar
import com.gallery.app.ui.theme.LocalAppTheme
import com.gallery.app.ui.theme.destructive
import dev.chrisbanes.haze.HazeState
import com.gallery.app.ui.design.HazeSource

/**
 * MediaStore trash. Tapping an item opens it in the fullscreen viewer (in its reduced trash mode, with
 * restore and delete permanently); everything can be restored or deleted at once through the top bar.
 * Long-pressing a tile (or tapping Select) enters the same selection mode the album grids use, with
 * drag-to-select and batch Restore / Delete.
 */
@Composable
fun TrashScreen(
    trashedItems: List<MediaItem>,
    onBack: () -> Unit,
    onItemClick: (MediaItem, Int) -> Unit,
    onRestoreAll: () -> Unit,
    onEmptyTrash: () -> Unit,
    onRestoreItems: (List<MediaItem>) -> Unit,
    onDeleteItems: (List<MediaItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEmptyConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteSelectedConfirmDialog by remember { mutableStateOf(false) }

    // Selection is kept local instead of in the ViewModel: the trash is a separate page, so it must
    // not share (or clear) the selection of the Photos/Albums grids behind it.
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val selectedItems = remember(trashedItems, selectedIds) {
        trashedItems.filter { selectedIds.contains(it.id) }
    }

    val theme = LocalAppTheme.current
    val palette = theme.palette
    val trashHaze = remember { HazeState() }
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Same trailing-click guard as the album grids: the long-press gesture selects the tile and then
    // the tile's own tap detector also reports a completed click, which would toggle it back off.
    var lastLongPressId by remember { mutableStateOf<Long?>(null) }
    var lastLongPressTime by remember { mutableLongStateOf(0L) }
    var isLongPressGestureActive by remember { mutableStateOf(false) }

    val exitSelection = {
        isSelectionMode = false
        selectedIds = emptySet()
    }

    // Restored/deleted rows leave the list: drop them from the selection and leave selection mode
    // once nothing selectable is left.
    LaunchedEffect(trashedItems) {
        val available = trashedItems.map { it.id }.toSet()
        val pruned = selectedIds.filter { it in available }.toSet()
        if (pruned != selectedIds) selectedIds = pruned
        if (isSelectionMode && pruned.isEmpty()) isSelectionMode = false
    }

    BackHandler(enabled = isSelectionMode) { exitSelection() }

    Box(modifier = modifier.fillMaxSize()) {
        if (trashedItems.isEmpty()) {
            ThemedEmptyState(
                icon = Icons.Rounded.DeleteOutline,
                title = strings.trashEmpty,
                message = strings.trashEmptyMessage
            )
        } else {
            HazeSource(trashHaze, Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .detectDragSelect(
                            items = trashedItems,
                            gridState = gridState,
                            isSelectionMode = { isSelectionMode },
                            onStartSelection = { id ->
                                isSelectionMode = true
                                selectedIds = setOf(id)
                            },
                            onSetSelection = { id, selected ->
                                selectedIds = if (selected) selectedIds + id else selectedIds - id
                            },
                            onUpdateSelectedIds = { selectedIds = it },
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
                        start = theme.shapes.gridHorizontalPadding,
                        end = theme.shapes.gridHorizontalPadding,
                        top = statusBarTop + 64.dp,
                        bottom = navigationBottom + if (isSelectionMode) 96.dp else 24.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(theme.shapes.gridSpacing),
                    verticalArrangement = Arrangement.spacedBy(theme.shapes.gridSpacing)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = strings.trashInfo,
                            color = palette.textMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }

                    // The regular media tile: the small cached thumbnail, cropped to a square, with the video
                    // badge. It records its position, so the viewer grows out of it.
                    items(trashedItems, key = { it.id }, contentType = { "media_tile" }) { item ->
                        MediaGridItem(
                            item = item,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedIds.contains(item.id),
                            onClick = {
                                val isTrailingClickFromLongPress = item.id == lastLongPressId &&
                                    (isLongPressGestureActive || (System.currentTimeMillis() - lastLongPressTime) < 600)
                                if (isTrailingClickFromLongPress) {
                                    // Same press-release the long-press gesture already handled.
                                } else if (isSelectionMode) {
                                    val updated = if (selectedIds.contains(item.id)) {
                                        selectedIds - item.id
                                    } else {
                                        selectedIds + item.id
                                    }
                                    selectedIds = updated
                                    if (updated.isEmpty()) isSelectionMode = false
                                } else {
                                    onItemClick(item, trashedItems.indexOf(item))
                                }
                            },
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clipToBounds()
                        )
                    }
                }
            }
        }

        // Fades the grid into the background behind the bottom bar. Drawn here (instead of by the
        // caller) so the selection action bar can sit above it, exactly like the album grids.
        BottomEdgeFade(Modifier.align(Alignment.BottomCenter))

        if (isSelectionMode) {
            SelectionTopBar(
                selectedCount = selectedIds.size,
                totalCount = trashedItems.size,
                onToggleSelectAll = {
                    selectedIds = if (trashedItems.isNotEmpty() && selectedIds.size == trashedItems.size) {
                        emptySet()
                    } else {
                        trashedItems.map { it.id }.toSet()
                    }
                },
                onClose = exitSelection,
                hazeState = trashHaze,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        } else {
            ThemedTopBar(
                title = strings.trash,
                subtitle = if (trashedItems.isEmpty()) null else strings.items(trashedItems.size),
                onBack = onBack,
                hazeState = trashHaze,
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                if (trashedItems.isNotEmpty()) {
                    TopBarTextAction(strings.select, palette.accent) { isSelectionMode = true }
                    TopBarTextAction(strings.restore, palette.accent, onRestoreAll)
                    TopBarTextAction(strings.emptyAction, palette.destructive) { showEmptyConfirmDialog = true }
                }
            }
        }

        if (isSelectionMode) {
            TrashSelectionActionBar(
                selectedCount = selectedIds.size,
                onRestore = {
                    if (selectedItems.isNotEmpty()) {
                        onRestoreItems(selectedItems)
                    }
                },
                onDelete = {
                    if (selectedItems.isNotEmpty()) showDeleteSelectedConfirmDialog = true
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    if (showEmptyConfirmDialog) {
        ThemedDialog(
            onDismissRequest = { showEmptyConfirmDialog = false },
            title = strings.emptyTrashTitle,
            message = strings.emptyTrashMessage(trashedItems.size),
            buttons = listOf(
                DialogButton(strings.cancel, DialogButtonStyle.Cancel) { showEmptyConfirmDialog = false },
                DialogButton(strings.emptyAction, DialogButtonStyle.Destructive) {
                    showEmptyConfirmDialog = false
                    onEmptyTrash()
                }
            )
        )
    }

    if (showDeleteSelectedConfirmDialog) {
        ThemedDialog(
            onDismissRequest = { showDeleteSelectedConfirmDialog = false },
            title = strings.deleteSelectedTitle,
            message = strings.deleteSelectedMessage(selectedItems.size),
            buttons = listOf(
                DialogButton(strings.cancel, DialogButtonStyle.Cancel) { showDeleteSelectedConfirmDialog = false },
                DialogButton(strings.delete, DialogButtonStyle.Destructive) {
                    showDeleteSelectedConfirmDialog = false
                    if (selectedItems.isNotEmpty()) {
                        onDeleteItems(selectedItems)
                    }
                }
            )
        )
    }
}

@Composable
internal fun TopBarTextAction(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text = text,
        color = color,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    )
}
