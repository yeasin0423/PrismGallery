package com.example.amoledgallery.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amoledgallery.model.MediaItem
import com.example.amoledgallery.ui.components.MediaGridItem
import com.example.amoledgallery.ui.theme.AmoledBlack
import com.example.amoledgallery.ui.theme.PureWhite
import com.example.amoledgallery.ui.theme.TextMuted

/**
 * Dedicated Favorites Screen:
 * - 5-column grid layout (GridCells.Fixed(5))
 * - High-speed hardware bitmap rendering with MediaGridItem
 * - Fullscreen MediaDetailScreen overlay with intact scroll state
 * - Pure AMOLED Black (#000000)
 */
@Composable
fun FavoritesScreen(
    favoriteItems: List<MediaItem>,
    onBack: () -> Unit,
    onTrashItem: (MediaItem) -> Unit,
    onRenameItem: (MediaItem, String) -> Unit,
    isFavorite: (Long) -> Boolean,
    onToggleFavorite: (Long) -> Unit,
    modifier: Modifier = Modifier,
    columnCount: Int = 5
) {
    var selectedDetailIndex by remember { mutableStateOf<Int?>(null) }
    val gridState = rememberLazyGridState()

    BackHandler {
        if (selectedDetailIndex != null) {
            selectedDetailIndex = null
        } else {
            onBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // OneUI Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AmoledBlack)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (selectedDetailIndex != null) {
                        selectedDetailIndex = null
                    } else {
                        onBack()
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Zurück",
                        tint = PureWhite
                    )
                }

                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = "Favoriten",
                        color = PureWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (favoriteItems.size == 1) "1 Element" else "${favoriteItems.size} Elemente",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            if (favoriteItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.FavoriteBorder,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Keine Favoriten vorhanden",
                            color = TextMuted,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnCount),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = 3.dp,
                        end = 3.dp,
                        top = 4.dp,
                        bottom = 32.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    itemsIndexed(
                        items = favoriteItems,
                        key = { _, item -> item.id },
                        contentType = { _, _ -> "media_tile" }
                    ) { index, item ->
                        MediaGridItem(
                            item = item,
                            isSelectionMode = false,
                            isSelected = false,
                            isFavorite = isFavorite(item.id),
                            onClick = { selectedDetailIndex = index },
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clipToBounds()
                                .animateItem()
                        )
                    }
                }
            }
        }

        // Fullscreen Overlay Detail Viewer
        AnimatedVisibility(
            visible = selectedDetailIndex != null,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            selectedDetailIndex?.let { index ->
                MediaDetailScreen(
                    mediaList = favoriteItems,
                    initialIndex = index,
                    onBack = { selectedDetailIndex = null },
                    onTrashItem = onTrashItem,
                    onRenameItem = onRenameItem,
                    isFavorite = isFavorite,
                    onToggleFavorite = onToggleFavorite
                )
            }
        }
    }
}
