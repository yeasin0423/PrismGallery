package com.example.amoledgallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.amoledgallery.model.MediaItem
import com.example.amoledgallery.ui.design.DialogButton
import com.example.amoledgallery.ui.design.DialogButtonStyle
import com.example.amoledgallery.ui.design.ThemedButton
import com.example.amoledgallery.ui.design.ThemedChipAction
import com.example.amoledgallery.ui.theme.DesignLanguage
import com.example.amoledgallery.ui.design.ThemedDialog
import com.example.amoledgallery.ui.design.ThemedEmptyState
import com.example.amoledgallery.ui.design.ThemedSheet
import com.example.amoledgallery.ui.design.ThemedTopBar
import com.example.amoledgallery.ui.theme.LocalAppTheme
import com.example.amoledgallery.ui.theme.destructive
import dev.chrisbanes.haze.HazeState
import com.example.amoledgallery.ui.design.HazeSource

/**
 * MediaStore trash: restore or permanently delete single items (via the language's action sheet) or
 * everything at once (confirmed through the language's dialog).
 */
@Composable
fun TrashScreen(
    trashedItems: List<MediaItem>,
    onBack: () -> Unit,
    onRestoreItem: (MediaItem) -> Unit,
    onRestoreAll: () -> Unit,
    onDeletePermanently: (MediaItem) -> Unit,
    onEmptyTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedItem by remember { mutableStateOf<MediaItem?>(null) }
    var showEmptyConfirmDialog by remember { mutableStateOf(false) }
    val theme = LocalAppTheme.current
    val palette = theme.palette
    val compactActions = trashedItems.isNotEmpty() &&
        (theme.language == DesignLanguage.GOOGLE_EXPRESSIVE || theme.language == DesignLanguage.IOS_LIQUID_GLASS)
    val trashHaze = remember { HazeState() }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = modifier.fillMaxSize()) {
        if (trashedItems.isEmpty()) {
            ThemedEmptyState(
                icon = Icons.Rounded.DeleteOutline,
                title = "Papierkorb ist leer",
                message = "Gelöschte Medien werden hier 30 Tage aufbewahrt."
            )
        } else {
            HazeSource(trashHaze, Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = theme.shapes.gridHorizontalPadding,
                        end = theme.shapes.gridHorizontalPadding,
                        top = statusBarTop + if (compactActions) 112.dp else 64.dp,
                        bottom = navigationBottom + 24.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(theme.shapes.gridSpacing),
                    verticalArrangement = Arrangement.spacedBy(theme.shapes.gridSpacing)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "Elemente im Papierkorb werden nach 30 Tagen endgültig gelöscht.",
                            color = palette.textMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }

                    items(trashedItems, key = { it.id }) { item ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(theme.shapes.gridItemCornerRadius))
                                .background(palette.surfaceVariant)
                                .clickable { selectedItem = item }
                        ) {
                            AsyncImage(
                                model = item.uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        ThemedTopBar(
            title = "Papierkorb",
            subtitle = if (trashedItems.isEmpty()) null else "${trashedItems.size} Elemente",
            onBack = onBack,
            hazeState = trashHaze,
            modifier = Modifier.align(Alignment.TopCenter),
            // iOS and Material center/size their title so that two text actions beside it would leave
            // no room for "Papierkorb"; they get a compact second row instead.
            secondaryActions = if (compactActions) {
                {
                    ThemedChipAction("Wiederherstellen", palette.accent, onRestoreAll, trashHaze)
                    ThemedChipAction("Leeren", palette.destructive, { showEmptyConfirmDialog = true }, trashHaze)
                }
            } else {
                null
            }
        ) {
            if (trashedItems.isNotEmpty() && !compactActions) {
                TopBarTextAction("Wiederherstellen", palette.accent, onRestoreAll)
                TopBarTextAction("Leeren", palette.destructive) { showEmptyConfirmDialog = true }
            }
        }
    }

    selectedItem?.let { item ->
        ThemedSheet(
            onDismissRequest = { selectedItem = null },
            title = "Gelöschtes Element"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(theme.shapes.cardCornerRadius))
                        .background(palette.surfaceVariant)
                ) {
                    AsyncImage(
                        model = item.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = item.displayName.ifBlank { "Medium" },
                    color = palette.textMuted,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                ThemedButton(
                    text = "Wiederherstellen",
                    icon = Icons.Rounded.Restore,
                    style = DialogButtonStyle.Primary,
                    onClick = {
                        selectedItem = null
                        onRestoreItem(item)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                ThemedButton(
                    text = "Endgültig löschen",
                    icon = Icons.Rounded.DeleteForever,
                    style = DialogButtonStyle.Destructive,
                    onClick = {
                        selectedItem = null
                        onDeletePermanently(item)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showEmptyConfirmDialog) {
        ThemedDialog(
            onDismissRequest = { showEmptyConfirmDialog = false },
            title = "Papierkorb leeren?",
            message = "Alle ${trashedItems.size} Elemente im Papierkorb werden dauerhaft gelöscht.",
            buttons = listOf(
                DialogButton("Abbrechen", DialogButtonStyle.Cancel) { showEmptyConfirmDialog = false },
                DialogButton("Leeren", DialogButtonStyle.Destructive) {
                    showEmptyConfirmDialog = false
                    onEmptyTrash()
                }
            )
        )
    }
}

@Composable
private fun TopBarTextAction(text: String, color: Color, onClick: () -> Unit) {
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
