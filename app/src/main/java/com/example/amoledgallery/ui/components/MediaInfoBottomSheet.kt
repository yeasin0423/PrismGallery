package com.example.amoledgallery.ui.components

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.amoledgallery.ui.theme.LocalAppTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.exifinterface.media.ExifInterface
import com.example.amoledgallery.model.MediaItem
import com.example.amoledgallery.ui.theme.OneUiBlue
import com.example.amoledgallery.ui.theme.OneUiCardBorder
import com.example.amoledgallery.ui.theme.PureWhite
import com.example.amoledgallery.ui.theme.SurfaceDark
import com.example.amoledgallery.ui.theme.SurfaceVariantDark
import com.example.amoledgallery.ui.theme.TextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Extracted detailed file metadata.
 */
data class MediaMetadata(
    val formattedDate: String = "",
    val fileName: String = "",
    val fileSizeFormatted: String = "",
    val resolutionFormatted: String = "",
    val megapixelsFormatted: String = "",
    val storagePath: String = "",
    val albumName: String = "",
    val mimeType: String = "",
    // Camera EXIF
    val cameraDevice: String? = null,
    val cameraSettings: String? = null,
    val focalLength: String? = null,
    val iso: String? = null,
    val flashUsed: Boolean? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    // Video
    val durationFormatted: String? = null,
    val bitrateFormatted: String? = null
)

/**
 * Samsung OneUI style Details & Metadata bottom sheet for fullscreen media viewer.
 * Slides up when scrolling down / swiping up on media detail screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaInfoBottomSheet(
    item: MediaItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var metadata by remember(item.id) { mutableStateOf<MediaMetadata?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(item.id, item.uri) {
        withContext(Dispatchers.IO) {
            metadata = extractMediaMetadata(context, item)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        contentColor = PureWhite,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(
            topStart = LocalAppTheme.current.shapes.dialogCornerRadius,
            topEnd = LocalAppTheme.current.shapes.dialogCornerRadius
        ),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            )
        }
    ) {
        com.example.amoledgallery.ui.WindowBackgroundBlur()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Details",
                color = PureWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val meta = metadata
            if (meta == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Metadaten werden geladen...", color = TextMuted, fontSize = 14.sp)
                }
            } else {
                // Section 1: Date and Time
                MetadataCard {
                    MetadataRow(
                        icon = Icons.Rounded.CalendarToday,
                        title = "Datum & Uhrzeit",
                        value = meta.formattedDate
                    )
                    HorizontalDivider(color = OneUiCardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                    MetadataRow(
                        icon = Icons.Rounded.Description,
                        title = "Dateiname",
                        value = meta.fileName
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 2: Technical Details (Resolution, Size, MIME)
                MetadataCard {
                    if (meta.resolutionFormatted.isNotBlank()) {
                        val resSubtitle = if (meta.megapixelsFormatted.isNotBlank()) {
                            "${meta.resolutionFormatted} • ${meta.megapixelsFormatted}"
                        } else {
                            meta.resolutionFormatted
                        }
                        MetadataRow(
                            icon = Icons.Rounded.AspectRatio,
                            title = "Auflösung",
                            value = resSubtitle
                        )
                        HorizontalDivider(color = OneUiCardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                    }

                    if (meta.fileSizeFormatted.isNotBlank()) {
                        MetadataRow(
                            icon = Icons.Rounded.Storage,
                            title = "Dateigröße",
                            value = meta.fileSizeFormatted
                        )
                        HorizontalDivider(color = OneUiCardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                    }

                    if (meta.durationFormatted != null) {
                        MetadataRow(
                            icon = Icons.Rounded.Movie,
                            title = "Videodauer",
                            value = meta.durationFormatted
                        )
                        HorizontalDivider(color = OneUiCardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                    }

                    if (meta.bitrateFormatted != null) {
                        MetadataRow(
                            icon = Icons.Rounded.Tune,
                            title = "Bitrate",
                            value = meta.bitrateFormatted
                        )
                        HorizontalDivider(color = OneUiCardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                    }

                    MetadataRow(
                        icon = Icons.Rounded.Folder,
                        title = "Dateipfad",
                        value = meta.storagePath.ifBlank { "Pictures/${meta.albumName}/${meta.fileName}" }
                    )
                }

                // Section 3: Camera EXIF (if available)
                if (meta.cameraDevice != null || meta.cameraSettings != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    MetadataCard {
                        if (meta.cameraDevice != null) {
                            MetadataRow(
                                icon = Icons.Rounded.CameraAlt,
                                title = "Kameramodell",
                                value = meta.cameraDevice
                            )
                            if (meta.cameraSettings != null) {
                                HorizontalDivider(color = OneUiCardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }

                        if (meta.cameraSettings != null) {
                            MetadataRow(
                                icon = Icons.Rounded.Tune,
                                title = "Aufnahmeparameter",
                                value = meta.cameraSettings
                            )
                        }
                    }
                }

                // Section 4: Geolocation / GPS (if available)
                if (meta.latitude != null && meta.longitude != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    MetadataCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MetadataRow(
                                icon = Icons.Rounded.LocationOn,
                                title = "Aufnahmeort",
                                value = "${String.format(Locale.US, "%.5f", meta.latitude)}, ${String.format(Locale.US, "%.5f", meta.longitude)}",
                                modifier = Modifier.weight(1f)
                            )

                            TextButton(
                                onClick = {
                                    val geoUri = Uri.parse("geo:${meta.latitude},${meta.longitude}?q=${meta.latitude},${meta.longitude}(${Uri.encode(meta.fileName)})")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                    try {
                                        context.startActivity(mapIntent)
                                    } catch (_: Exception) {}
                                }
                            ) {
                                Text(text = "Karte öffnen", color = OneUiBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun MetadataCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceVariantDark)
            .padding(14.dp)
    ) {
        content()
    }
}

@Composable
private fun MetadataRow(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SurfaceDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PureWhite,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = value,
                color = PureWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Extracts comprehensive metadata from file, ContentResolver and ExifInterface.
 */
private fun extractMediaMetadata(context: Context, item: MediaItem): MediaMetadata {
    // 1. Date formatting
    val millis = if (item.dateModified > 0) item.dateModified * 1000L else item.dateAdded * 1000L
    val dateStr = if (millis > 0) {
        val sdf = SimpleDateFormat("d. MMMM yyyy, HH:mm 'Uhr'", Locale.GERMAN)
        sdf.format(Date(millis))
    } else ""

    // 2. File size
    var sizeBytes = 0L
    if (item.dataPath.isNotBlank()) {
        try {
            val f = File(item.dataPath)
            if (f.exists()) sizeBytes = f.length()
        } catch (_: Exception) {}
    }
    if (sizeBytes <= 0L) {
        try {
            context.contentResolver.openFileDescriptor(item.uri, "r")?.use {
                sizeBytes = it.statSize
            }
        } catch (_: Exception) {}
    }
    val sizeStr = formatFileSize(sizeBytes)

    // 3. Media info (Dimensions, Camera EXIF or Video Info)
    var resStr = ""
    var mpStr = ""
    var camDevice: String? = null
    var camSettings: String? = null
    var focal: String? = null
    var isoStr: String? = null
    var flashUsed: Boolean? = null
    var lat: Double? = null
    var lon: Double? = null
    var videoDuration: String? = null
    var videoBitrate: String? = null

    if (item.isVideo) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, item.uri)
            val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            if (w > 0 && h > 0) {
                resStr = "$w × $h"
                val mp = (w.toDouble() * h) / 1_000_000.0
                mpStr = "${DecimalFormat("#.#").format(mp)} MP"
            }
            val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: item.durationMs
            if (durMs > 0) {
                val totalSec = durMs / 1000
                val m = (totalSec % 3600) / 60
                val s = totalSec % 60
                val hrs = totalSec / 3600
                videoDuration = if (hrs > 0) "$hrs Std. $m Min. $s Sek." else "$m Min. $s Sek."
            }
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull() ?: 0L
            if (bitrate > 0) {
                val mbps = bitrate.toDouble() / 1_000_000.0
                videoBitrate = "${DecimalFormat("#.#").format(mbps)} Mbps"
            }
        } catch (_: Exception) {
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    } else {
        // Image EXIF
        try {
            context.contentResolver.openInputStream(item.uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val w = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                val h = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
                if (w > 0 && h > 0) {
                    resStr = "$w × $h"
                    val mp = (w.toDouble() * h) / 1_000_000.0
                    mpStr = "${DecimalFormat("#.#").format(mp)} MP"
                }

                val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim()
                val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim()
                if (!model.isNullOrBlank()) {
                    camDevice = if (!make.isNullOrBlank() && !model.startsWith(make, ignoreCase = true)) {
                        "$make $model"
                    } else {
                        model
                    }
                }

                val fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
                val exposure = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
                val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                val focalLen = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)

                val settingsParts = mutableListOf<String>()
                if (!fNumber.isNullOrBlank()) {
                    settingsParts.add("f/$fNumber")
                }
                if (!exposure.isNullOrBlank()) {
                    val expDouble = exposure.toDoubleOrNull()
                    if (expDouble != null && expDouble < 1.0 && expDouble > 0.0) {
                        val fraction = (1.0 / expDouble).roundToInt()
                        settingsParts.add("1/${fraction}s")
                    } else {
                        settingsParts.add("${exposure}s")
                    }
                }
                if (!iso.isNullOrBlank()) {
                    settingsParts.add("ISO $iso")
                    isoStr = iso
                }
                if (!focalLen.isNullOrBlank()) {
                    val fVal = focalLen.toDoubleOrNull()
                    if (fVal != null) {
                        settingsParts.add("${DecimalFormat("#.#").format(fVal)}mm")
                        focal = "${DecimalFormat("#.#").format(fVal)}mm"
                    }
                }

                if (settingsParts.isNotEmpty()) {
                    camSettings = settingsParts.joinToString(" • ")
                }

                val flash = exif.getAttributeInt(ExifInterface.TAG_FLASH, -1)
                if (flash != -1) {
                    flashUsed = (flash and 1) != 0
                }

                val latLong = exif.latLong
                if (latLong != null && latLong.size >= 2) {
                    lat = latLong[0]
                    lon = latLong[1]
                }
            }
        } catch (_: Exception) {}
    }

    return MediaMetadata(
        formattedDate = dateStr,
        fileName = item.displayName.ifBlank { "media_${item.id}" },
        fileSizeFormatted = sizeStr,
        resolutionFormatted = resStr,
        megapixelsFormatted = mpStr,
        storagePath = item.dataPath.ifBlank { item.relativePath },
        albumName = item.bucketDisplayName.ifBlank { "Galerie" },
        mimeType = item.mimeType,
        cameraDevice = camDevice,
        cameraSettings = camSettings,
        focalLength = focal,
        iso = isoStr,
        flashUsed = flashUsed,
        latitude = lat,
        longitude = lon,
        durationFormatted = videoDuration,
        bitrateFormatted = videoBitrate
    )
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return ""
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    val df = DecimalFormat("#.##")
    return when {
        gb >= 1.0 -> "${df.format(gb)} GB"
        mb >= 1.0 -> "${df.format(mb)} MB"
        else -> "${df.format(kb)} KB"
    }
}
