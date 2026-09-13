package com.example.amoledgallery.ui.components

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import com.example.amoledgallery.AmoledGalleryApp
import com.example.amoledgallery.data.ThumbnailManager
import com.example.amoledgallery.model.MediaItem
import com.example.amoledgallery.ui.design.PhotoBadgeTextStyle
import com.example.amoledgallery.ui.theme.AppTheme
import com.example.amoledgallery.ui.theme.DesignLanguage
import com.example.amoledgallery.ui.theme.LocalAppTheme
import com.example.amoledgallery.ui.theme.isLight
import com.example.amoledgallery.ui.theme.onAccent

/**
 * Media grid tile. Thumbnails are downscaled hardware bitmaps sized by the graphics quality tier; the
 * placeholder matches the palette brightness so nothing flashes while decoding. Selection, video and
 * favorite badges follow the active design language:
 * - One UI: large white ring top-left that fills white with a dark check; the tile lightens.
 * - Material 3 Expressive: the selected photo shrinks into a more rounded shape on a tonal backing.
 * - iOS: blue check badge bottom-right, nothing on unselected tiles.
 * - HyperOS: accent check circle top-right, a light dim on the tile.
 */
private val DarkPlaceholder = ColorPainter(Color(0xFF141414))
private val LightPlaceholder = ColorPainter(Color(0xFFE4E4E4))

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

    val theme = LocalAppTheme.current
    val placeholder = if (theme.palette.isLight) LightPlaceholder else DarkPlaceholder
    val selected = isSelectionMode && isSelected

    if (theme.language == DesignLanguage.GOOGLE_EXPRESSIVE) {
        ExpressiveTile(item, imageRequest, placeholder, theme, isSelectionMode, selected, isFavorite, onClick, modifier)
        return
    }

    Box(
        modifier = modifier
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
                .clickable(onClick = onClick)
        )

        if (selected) {
            val scrim = when (theme.language) {
                DesignLanguage.SAMSUNG_ONEUI -> Color.White.copy(alpha = 0.30f)
                DesignLanguage.XIAOMI_HYPEROS -> Color.Black.copy(alpha = 0.18f)
                else -> Color.White.copy(alpha = 0.12f)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrim)
            )
        }

        if (item.isVideo) VideoBadge(item, theme.language)
        if (isFavorite) FavoriteBadge(theme.language)

        if (isSelectionMode) {
            when (theme.language) {
                DesignLanguage.SAMSUNG_ONEUI -> Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else Color.Black.copy(alpha = 0.22f))
                        .then(if (!isSelected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Rounded.Check, null, tint = Color.Black, modifier = Modifier.size(17.dp))
                    }
                }

                DesignLanguage.IOS_LIQUID_GLASS -> if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(1.5.dp)
                            .clip(CircleShape)
                            .background(theme.palette.accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                }

                else -> Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) theme.palette.accent else Color.Black.copy(alpha = 0.22f))
                        .then(if (!isSelected) Modifier.border(1.5.dp, Color.White, CircleShape) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpressiveTile(
    item: MediaItem,
    imageRequest: ImageRequest,
    placeholder: ColorPainter,
    theme: AppTheme,
    isSelectionMode: Boolean,
    selected: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val palette = theme.palette
    val baseRadius = theme.shapes.gridItemCornerRadius
    val scale by animateFloatAsState(
        targetValue = if (selected) 0.82f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 480f),
        label = "expressive_tile_scale"
    )
    val corner by animateDpAsState(
        targetValue = if (selected) 28.dp else baseRadius,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 480f),
        label = "expressive_tile_corner"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(baseRadius))
            .background(if (selected) lerp(palette.surfaceVariant, palette.accent, 0.28f) else Color.Transparent)
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = placeholder,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    shape = RoundedCornerShape(corner)
                    clip = true
                }
                .clickable(onClick = onClick)
        )

        if (item.isVideo) VideoBadge(item, theme.language)
        if (isFavorite) FavoriteBadge(theme.language)

        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (selected) palette.accent else Color.Black.copy(alpha = 0.25f))
                    .then(if (!selected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(Icons.Rounded.Check, null, tint = palette.onAccent, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun BoxScope.VideoBadge(item: MediaItem, language: DesignLanguage) {
    when (language) {
        DesignLanguage.IOS_LIQUID_GLASS -> {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))))
            )
            Text(
                text = item.formattedDuration(),
                style = PhotoBadgeTextStyle,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 6.dp, bottom = 4.dp)
            )
        }

        DesignLanguage.XIAOMI_HYPEROS -> Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(5.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.42f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(11.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Text(item.formattedDuration(), style = PhotoBadgeTextStyle.copy(fontSize = PhotoBadgeTextStyle.fontSize * 0.85f))
        }

        DesignLanguage.GOOGLE_EXPRESSIVE -> Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(horizontal = 7.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(item.formattedDuration(), style = PhotoBadgeTextStyle)
            Spacer(modifier = Modifier.width(2.dp))
            Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(14.dp))
        }

        DesignLanguage.SAMSUNG_ONEUI -> Row(
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
}

@Composable
private fun BoxScope.FavoriteBadge(language: DesignLanguage) {
    val alignment = if (language == DesignLanguage.GOOGLE_EXPRESSIVE || language == DesignLanguage.IOS_LIQUID_GLASS) {
        Alignment.BottomStart
    } else {
        Alignment.TopEnd
    }
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
            contentDescription = "Favorit",
            tint = Color(0xFFFF4D6D),
            modifier = Modifier.size(14.dp)
        )
    }
}
