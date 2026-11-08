package com.gallery.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gallery.app.data.MediaRepository
import com.gallery.app.ui.i18n.strings
import com.gallery.app.ui.theme.LocalAppTheme

/** What one storage volume holds, for the storage pages. Media sizes are 0 while the stats are loading. */
data class VolumeUsage(
    val label: String,
    val isSdCard: Boolean,
    val freeBytes: Long,
    val totalBytes: Long,
    val photos: Long,
    val videos: Long,
    val trash: Long,
    /** SD card or USB storage (for the external volumes); INTERNAL for the phone's own storage. */
    val kind: com.gallery.app.util.StorageKind =
        if (isSdCard) com.gallery.app.util.StorageKind.SD_CARD else com.gallery.app.util.StorageKind.INTERNAL
) {
    val total: Long get() = totalBytes.coerceAtLeast(1L)
    val used: Long get() = (totalBytes - freeBytes).coerceIn(0L, total)
    val free: Long get() = (total - used).coerceAtLeast(0L)
    val other: Long get() = (used - photos - videos - trash).coerceAtLeast(0L)
    val usedFraction: Float get() = used.toFloat() / total
}

/** Internal storage, plus the SD card when one is mounted, each with its own media sizes. */
internal fun storageVolumes(
    stats: MediaRepository.MediaStorageStats?,
    internalFreeBytes: Long,
    internalTotalBytes: Long,
    sdCardFreeBytes: Long?,
    sdCardTotalBytes: Long?
): List<VolumeUsage> {
    fun volume(label: String, sd: Boolean, free: Long, total: Long, photos: Long, videos: Long, trash: Long): VolumeUsage {
        val used = (total - free).coerceIn(0L, total.coerceAtLeast(1L))
        val p = photos.coerceIn(0L, used)
        val v = videos.coerceIn(0L, used - p)
        val t = trash.coerceIn(0L, used - p - v)
        return VolumeUsage(label, sd, free, total, p, v, t)
    }
    val list = ArrayList<VolumeUsage>()
    list += volume(
        strings.internalStorage, false, internalFreeBytes, internalTotalBytes,
        stats?.photoBytes ?: 0L, stats?.videoBytes ?: 0L, stats?.trashBytes ?: 0L
    )
    // Every SD card and USB drive on its own page once the stats have counted them; before that the
    // external volume the app writes to, as one page.
    val externals = stats?.externals.orEmpty().filter { it.totalBytes > 0L }
    if (externals.isNotEmpty()) {
        externals.forEach { ext ->
            list += volume(
                com.gallery.app.ui.i18n.storageLabel(ext.kind), true, ext.freeBytes, ext.totalBytes,
                ext.photoBytes, ext.videoBytes, ext.trashBytes
            ).copy(kind = ext.kind)
        }
    } else if (sdCardFreeBytes != null && sdCardTotalBytes != null && sdCardTotalBytes > 0L) {
        list += volume(
            strings.sdCard, true, sdCardFreeBytes, sdCardTotalBytes,
            stats?.sdPhotoBytes ?: 0L, stats?.sdVideoBytes ?: 0L, stats?.sdTrashBytes ?: 0L
        )
    }
    return list
}

/**
 * Shows each storage volume as its own page: swiping sideways moves from internal storage to the SD card. With
 * only internal storage it is a single page without dots.
 */
@Composable
internal fun StorageVolumePager(
    volumes: List<VolumeUsage>,
    modifier: Modifier = Modifier,
    pageSpacing: Dp = 12.dp,
    page: @Composable (VolumeUsage) -> Unit
) {
    if (volumes.size <= 1) {
        Box(modifier = modifier) { volumes.firstOrNull()?.let { page(it) } }
        return
    }
    val palette = LocalAppTheme.current.palette
    val pagerState = rememberPagerState(pageCount = { volumes.size })
    // A page off screen is dropped and built again when it comes back, so its fill animation plays
    // from 0 on every visit by itself. It must not be rebuilt on top of that (it used to be keyed on
    // a visit counter): the swipe builds the page, the counter then rebuilt it once the swipe settled,
    // and the fill restarted halfway through - the bar appeared to fill twice.
    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = pageSpacing,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) { index ->
            page(volumes[index])
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            volumes.indices.forEach { index ->
                val selected = pagerState.currentPage == index
                val width by animateDpAsState(if (selected) 18.dp else 7.dp, label = "storage_dot")
                val color by animateColorAsState(
                    if (selected) palette.onBackground else palette.onBackground.copy(alpha = 0.3f),
                    label = "storage_dot_color"
                )
                Box(
                    modifier = Modifier
                        .width(width)
                        .height(7.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}
