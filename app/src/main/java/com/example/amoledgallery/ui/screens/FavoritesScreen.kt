package com.example.amoledgallery.ui.screens
import com.example.amoledgallery.ui.i18n.strings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.amoledgallery.model.MediaItem
import com.example.amoledgallery.ui.components.MediaGridItem
import com.example.amoledgallery.ui.design.ThemedEmptyState
import com.example.amoledgallery.ui.design.ThemedTopBar
import com.example.amoledgallery.ui.theme.LocalAppTheme
import dev.chrisbanes.haze.HazeState
import com.example.amoledgallery.ui.design.HazeSource
import com.example.amoledgallery.ui.design.MediaZoomTransition

/**
 * Favorites: the grid scrolls underneath the language's frosted top bar; tapping opens the
 * fullscreen viewer as an overlay so the scroll position survives.
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
    columnCount: Int = 4
) {
    var selectedDetailIndex by remember { mutableStateOf<Int?>(null) }
    val gridState = rememberLazyGridState()
    val theme = LocalAppTheme.current
    val favoritesHaze = remember { HazeState() }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // While the fullscreen viewer covers everything, the grid below it is not drawn at all.
    var gridCoveredByViewer by remember { mutableStateOf(false) }

    BackHandler {
        if (selectedDetailIndex != null) {
            selectedDetailIndex = null
        } else {
            onBack()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (favoriteItems.isEmpty()) {
            ThemedEmptyState(
                icon = Icons.Rounded.FavoriteBorder,
                title = strings.noFavorites,
                message = strings.noFavoritesMessage
            )
        } else {
            HazeSource(
                favoritesHaze,
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (gridCoveredByViewer) 0f else 1f }
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnCount),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = theme.shapes.gridHorizontalPadding,
                        end = theme.shapes.gridHorizontalPadding,
                        top = statusBarTop + 72.dp,
                        bottom = navigationBottom + 24.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(theme.shapes.gridSpacing),
                    verticalArrangement = Arrangement.spacedBy(theme.shapes.gridSpacing)
                ) {
                    itemsIndexed(
                        items = favoriteItems,
                        key = { _, item -> item.id },
                        contentType = { _, _ -> "media_tile" }
                    ) { index, item ->
                        MediaGridItem(
                            item = item,
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

        ThemedTopBar(
            title = strings.favorites,
            subtitle = strings.items(favoriteItems.size),
            onBack = onBack,
            hazeState = favoritesHaze,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        com.example.amoledgallery.ui.design.BottomEdgeFade(Modifier.align(Alignment.BottomCenter))

        // Fullscreen Overlay Detail Viewer
        val detailIndexHolder = remember { IntArray(1) }
        selectedDetailIndex?.let { detailIndexHolder[0] = it }
        MediaZoomTransition(
            visible = selectedDetailIndex != null,
            onCoveringChange = { gridCoveredByViewer = it }
        ) {
                MediaDetailScreen(
                    mediaList = favoriteItems,
                    initialIndex = detailIndexHolder[0],
                    onBack = { selectedDetailIndex = null },
                    onTrashItem = onTrashItem,
                    onRenameItem = onRenameItem,
                    isFavorite = isFavorite,
                    onToggleFavorite = onToggleFavorite
                )
        }
    }
}
