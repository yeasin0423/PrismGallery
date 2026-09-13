package com.example.amoledgallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.SdCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.amoledgallery.model.AlbumItem
import com.example.amoledgallery.ui.design.ExpressiveCookieShape
import com.example.amoledgallery.ui.design.ThemedSheet
import com.example.amoledgallery.ui.theme.DesignLanguage
import com.example.amoledgallery.ui.theme.LocalAppTheme
import com.example.amoledgallery.ui.theme.onAccent

/**
 * Sheet for moving the current selection into an album (or a new one), drawn as the active design
 * language's own sheet.
 */
@Composable
fun MoveToAlbumBottomSheet(
    albums: List<AlbumItem>,
    onDismiss: () -> Unit,
    onSelectAlbum: (AlbumItem) -> Unit,
    onCreateNewAlbum: () -> Unit
) {
    ThemedSheet(onDismissRequest = onDismiss, title = "In Album verschieben") {
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item(key = "create_new_album_option") {
                MoveTargetRow(
                    title = "Neues Album erstellen",
                    subtitle = "Zielordner anlegen und verschieben",
                    cover = null,
                    icon = Icons.Rounded.Add,
                    highlighted = true,
                    isSdCard = false,
                    onClick = {
                        onDismiss()
                        onCreateNewAlbum()
                    }
                )
            }
            items(albums, key = { it.bucketId }) { album ->
                MoveTargetRow(
                    title = album.name,
                    subtitle = "${if (album.isSdCard) "SD-Karte" else "Intern"} · ${album.count} Elemente",
                    cover = album.coverThumbnail,
                    icon = Icons.Rounded.Folder,
                    highlighted = false,
                    isSdCard = album.isSdCard,
                    onClick = { onSelectAlbum(album) }
                )
            }
        }
    }
}

@Composable
private fun MoveTargetRow(
    title: String,
    subtitle: String,
    cover: Any?,
    icon: ImageVector,
    highlighted: Boolean,
    isSdCard: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    val palette = theme.palette
    val language = theme.language
    val coverShape: Shape = when (language) {
        DesignLanguage.GOOGLE_EXPRESSIVE -> if (highlighted) ExpressiveCookieShape else RoundedCornerShape(16.dp)
        DesignLanguage.IOS_LIQUID_GLASS -> RoundedCornerShape(10.dp)
        DesignLanguage.SAMSUNG_ONEUI -> RoundedCornerShape(14.dp)
        DesignLanguage.XIAOMI_HYPEROS -> RoundedCornerShape(12.dp)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(coverShape)
                .background(if (highlighted) palette.accent else palette.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (cover != null) {
                AsyncImage(model = cover, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(icon, null, tint = if (highlighted) palette.onAccent else palette.textMuted, modifier = Modifier.size(26.dp))
            }
            if (isSdCard) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.SdCard, "SD-Karte", tint = Color.White, modifier = Modifier.size(10.dp))
                }
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (highlighted && language != DesignLanguage.SAMSUNG_ONEUI) palette.accent else palette.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = subtitle, color = palette.textMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (language == DesignLanguage.IOS_LIQUID_GLASS || language == DesignLanguage.XIAOMI_HYPEROS) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = palette.textMuted.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
        }
    }
}
