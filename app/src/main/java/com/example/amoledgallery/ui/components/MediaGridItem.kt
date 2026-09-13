package com.example.amoledgallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import com.example.amoledgallery.AmoledGalleryApp
import com.example.amoledgallery.data.ThumbnailManager
import com.example.amoledgallery.model.MediaItem
import com.example.amoledgallery.ui.theme.OneUiBlue
import com.example.amoledgallery.ui.theme.PureWhite

/**
 * Ultra-performance Samsung OneUI AMOLED media grid tile:
 * - Downscaled Hardware Bitmaps sized by the current graphics quality tier
 * - Dark placeholder to avoid a white flash while the thumbnail decodes
 * - Selection checkbox (top-start), favorite heart (top-end) and video duration badge (bottom-end)
 */
private val DarkPlaceholder = ColorPainter(Color(0xFF101010))

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
    val quality = AmoledGalleryApp.performanceProfile
    val imageRequest = remember(item.id, item.dateModified, quality, context) {
        val px = ThumbnailManager.getInstance(context).thumbnailPx
        ImageRequest.Builder(context)
            .data(item.thumbnail)
            .size(coil3.size.Size(px, px))
            .precision(Precision.INEXACT)
            .crossfade(false)
            .build()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = DarkPlaceholder,
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick)
        )

        if (isSelectionMode && isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )
        }

        if (item.isVideo) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = item.formattedDuration(),
                    color = PureWhite,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (isFavorite) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFFF4D6D),
                    modifier = Modifier.size(11.dp)
                )
            }
        }

        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) OneUiBlue else Color.Black.copy(alpha = 0.35f))
                    .then(
                        if (!isSelected) {
                            Modifier.border(1.5.dp, PureWhite.copy(alpha = 0.85f), CircleShape)
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
