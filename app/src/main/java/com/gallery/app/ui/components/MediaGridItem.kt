package com.gallery.app.ui.components
import com.gallery.app.ui.i18n.strings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import com.gallery.app.GalleryApplication
import com.gallery.app.data.ThumbnailManager
import com.gallery.app.model.MediaItem
import com.gallery.app.ui.design.LocalMediaOpenOrigin
import com.gallery.app.ui.design.PhotoBadgeTextStyle
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onPlaced
import com.gallery.app.ui.theme.LocalAppTheme
import com.gallery.app.ui.theme.isLight

/**
 * Media grid tile. Thumbnails are downscaled hardware bitmaps sized by the graphics quality tier; the
 * placeholder matches the palette brightness so nothing flashes while decoding. Selection, video and
 * favorite badges follow One UI: a large white ring top-left that fills white with a dark check, and
 * the tile lightens.
 */
private val DarkPlaceholder = ColorPainter(Color(0xFF141414))
private val LightPlaceholder = ColorPainter(Color(0xFFE4E4E4))

/**
 * The grid tile's thumbnail request. Shared, so screens can load thumbnails into Coil's memory cache
 * ahead of time (e.g. before an album page animates in) under exactly the key the tiles look up.
 */
fun thumbnailRequest(context: android.content.Context, item: MediaItem): ImageRequest {
    val px = ThumbnailManager.getInstance(context).thumbnailPx
    return ImageRequest.Builder(context)
        .data(item.thumbnail)
        .size(coil3.size.Size(px, px))
        .precision(Precision.INEXACT)
        .crossfade(false)
        .build()
}

@Composable
fun MediaGridItem(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    isFavorite: Boolean = false
) {
    val context = LocalContext.current

    // Zero Main-Thread I/O: item.thumbnail is resolved directly by Coil's MediaThumbnailFetcher on
    // Dispatchers.IO. Note: bitmapConfig() is NOT set here - MediaThumbnailFetcher decodes the
    // bitmap itself (bypassing Coil's own decoder) and picks the config by quality tier, so a
    // request-level override here would have no effect anyway.
    //
    // Precision must stay INEXACT: ContentResolver.loadThumbnail() returns aspect-ratio-preserving
    // bitmaps (a 4:3 photo asked for at 120x120 comes back ~160x120), and Coil validates a memory
    // cache entry by comparing its dimensions against the request. Under Precision.EXACT every
    // single lookup was therefore rejected as "wrong size", so the memory cache never served a hit
    // and every tile re-decoded its JPEG from disk on each scroll-in - the main cause of scroll jank.
    val quality = GalleryApplication.performanceProfile
    val imageRequest = remember(item.id, item.dateModified, quality, context) { thumbnailRequest(context, item) }

    val theme = LocalAppTheme.current
    val placeholder = if (theme.palette.isLight) LightPlaceholder else DarkPlaceholder
    val selected = isSelectionMode && isSelected

    // Remember where the tile sits so the fullscreen viewer can zoom out of it. Kept in a plain holder
    // (not state) and only resolved to bounds on click, so scrolling costs nothing extra.
    val openOrigin = LocalMediaOpenOrigin.current
    val coordinatesHolder = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val openAwareClick: () -> Unit = {
        coordinatesHolder[0]?.takeIf { it.isAttached }?.let { openOrigin.record(it.boundsInRoot()) }
        onClick()
    }

    Box(
        modifier = modifier
            .onPlaced { coordinatesHolder[0] = it }
            .fillMaxSize()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(theme.shapes.gridItemCornerRadius))
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = placeholder,
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = openAwareClick)
        )

        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            )
        }

        if (item.isVideo) VideoBadge(item)
        if (isFavorite) FavoriteBadge()

        if (isSelectionMode) {
            // The check pops in on a spring when a tile gets selected.
            val checkScale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.45f, stiffness = 700f),
                label = "check_pop"
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.12f))
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (checkScale > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = checkScale
                                scaleY = checkScale
                            }
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Check, null, tint = Color.Black, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.VideoBadge(item: MediaItem) {
    Row(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(2.dp))
        Text(item.formattedDuration(), style = PhotoBadgeTextStyle)
    }
}

@Composable
private fun BoxScope.FavoriteBadge() {
    val alignment = Alignment.TopEnd
    // A plain white heart disappears on bright photos, so it sits on a small dark disc in a saturated pink.
    Box(
        modifier = Modifier
            .align(alignment)
            .padding(5.dp)
            .size(22.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.42f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Favorite,
            contentDescription = strings.favorite,
            tint = Color(0xFFFF4D6D),
            modifier = Modifier.size(14.dp)
        )
    }
}
