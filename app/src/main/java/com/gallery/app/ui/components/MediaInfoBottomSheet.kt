package com.gallery.app.ui.components
import com.gallery.app.ui.i18n.LanguageState
import com.gallery.app.ui.i18n.strings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.exifinterface.media.ExifInterface
import com.gallery.app.model.MediaItem
import com.gallery.app.ui.theme.LocalAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
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
    val bitrateFormatted: String? = null,
    // Individual capture values as One UI lists them ("ƒ1,6", "1/43 s", "0 ev", "26 mm")
    val fNumberText: String? = null,
    val exposureTimeText: String? = null,
    val exposureBiasText: String? = null,
    val focal35Text: String? = null,
    /** File format as a short label ("JPEG", "HEIC", "MP4"). */
    val formatLabel: String = "",
    /** "Samstag, 18. April 2026 um 12:10" */
    val fullDate: String = ""
)

/** Loads [item]'s metadata off the main thread once a page swipe has settled. */
@Composable
private fun rememberMediaMetadata(item: MediaItem): MediaMetadata? {
    val context = LocalContext.current
    var metadata by remember(item.id) { mutableStateOf<MediaMetadata?>(null) }
    LaunchedEffect(item.id, item.uri, LanguageState.language) {
        // Let a page swipe settle first; the result is then applied on the main thread in one go.
        kotlinx.coroutines.delay(200)
        val loaded = withContext(Dispatchers.IO) { extractMediaMetadata(context, item) }
        metadata = loaded
    }
    return metadata
}

/**
 * Details & metadata of [item] as One UI cards. The fullscreen viewer shows it in the panel that opens
 * underneath the photo; the caller provides scrolling.
 */
@Composable
fun MediaInfoContent(
    item: MediaItem,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null
) {
    OneUiMediaInfoContent(item, onEdit, modifier)
}

/**
 * Samsung Gallery's details under the photo: the weekday, date and time in bold with "Bearbeiten" on the right,
 * the file name and folder in grey, then "Bild-Info" with size, resolution and megapixels split by hairlines,
 * and the camera with its capture values.
 */
@Composable
private fun OneUiMediaInfoContent(item: MediaItem, onEdit: (() -> Unit)?, modifier: Modifier) {
    val context = LocalContext.current
    val palette = LocalAppTheme.current.palette
    val meta = rememberMediaMetadata(item)
    val dateMillis = item.date * 1000L
    val locale = LanguageState.locale
    val dateLine = remember(dateMillis, locale) {
        SimpleDateFormat("EEEE, ${strings.longDatePattern} · HH:mm", locale).format(Date(dateMillis))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(dateLine, color = palette.onBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (onEdit != null) {
                Text(
                    text = strings.edit,
                    color = palette.accent,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onEdit)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
        if (meta == null) {
            Text(strings.loadingMetadata, color = palette.textMuted, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp))
            return@Column
        }
        Text(meta.fileName, color = palette.textMuted, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
        Text(meta.storagePath.ifBlank { "Pictures/${meta.albumName}" }, color = palette.textMuted, fontSize = 15.sp)

        Spacer(modifier = Modifier.height(26.dp))
        Text(strings.imageInfo, color = palette.onBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        OneUiValueLine(
            listOfNotNull(
                meta.fileSizeFormatted.ifBlank { null },
                meta.resolutionFormatted.ifBlank { null }?.replace(" ", "")?.replace("×", "x"),
                meta.megapixelsFormatted.ifBlank { null }?.replace(" ", ""),
                meta.durationFormatted,
                meta.bitrateFormatted
            )
        )

        val capture = listOfNotNull(
            meta.fNumberText,
            meta.exposureTimeText,
            meta.iso?.let { if (it.startsWith("ISO", ignoreCase = true)) it else "ISO $it" },
            meta.focalLength
        )
        if (meta.cameraDevice != null || capture.isNotEmpty()) {
            Spacer(modifier = Modifier.height(22.dp))
            Text(meta.cameraDevice ?: strings.cameraModel, color = palette.onBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            OneUiValueLine(capture)
        }

        if (meta.latitude != null && meta.longitude != null) {
            Spacer(modifier = Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.captureLocation, color = palette.onBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${String.format(Locale.US, "%.5f", meta.latitude)}, ${String.format(Locale.US, "%.5f", meta.longitude)}",
                        color = palette.textMuted,
                        fontSize = 15.sp
                    )
                }
                Text(
                    text = strings.map,
                    color = palette.accent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { openMap(context, meta) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun OneUiValueLine(values: List<String>) {
    if (values.isEmpty()) return
    val palette = LocalAppTheme.current.palette
    Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        values.forEachIndexed { index, value ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .width(1.dp)
                        .height(16.dp)
                        .background(palette.onBackground.copy(alpha = 0.2f))
                )
            }
            Text(value, color = palette.textMuted, fontSize = 15.sp, maxLines = 1)
        }
    }
}

private fun openMap(context: Context, meta: MediaMetadata) {
    val geoUri = Uri.parse("geo:${meta.latitude},${meta.longitude}?q=${meta.latitude},${meta.longitude}(${Uri.encode(meta.fileName)})")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, geoUri))
    } catch (_: Exception) {
    }
}


/**
 * Opens a photo with its location tags intact. Since Android 10 the system strips GPS data from files the app did not
 * create, unless the app holds ACCESS_MEDIA_LOCATION and asks MediaStore for the original. Without the permission, or
 * for files that are not in the media library ("Open with" from another app), the normal stream is used.
 */
private fun openWithLocation(context: Context, uri: Uri): java.io.InputStream? {
    if (uri.authority == MediaStore.AUTHORITY &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED
    ) {
        try {
            context.contentResolver.openInputStream(MediaStore.setRequireOriginal(uri))?.let { return it }
        } catch (_: Exception) {
            // Not available for this file: fall back to the normal stream below.
        }
    }
    return context.contentResolver.openInputStream(uri)
}

/**
 * Extracts comprehensive metadata from file, ContentResolver and ExifInterface.
 */
private fun extractMediaMetadata(context: Context, item: MediaItem): MediaMetadata {
    val locale = LanguageState.locale
    // Decimal separator of the app language: "ƒ1,6" / "ƒ1.6".
    val decimal = DecimalFormat("#.#", DecimalFormatSymbols(locale))

    // 1. Date formatting
    val millis = item.date * 1000L
    val dateStr = if (millis > 0) {
        val sdf = SimpleDateFormat(strings.metadataDatePattern, locale)
        sdf.format(Date(millis))
    } else ""
    val fullDate = if (millis > 0) SimpleDateFormat(strings.infoDatePattern, locale).format(Date(millis)) else ""

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
    val sizeStr = formatFileSize(sizeBytes, decimal)

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
    var fNumberText: String? = null
    var exposureTimeText: String? = null
    var exposureBiasText: String? = null
    var focal35Text: String? = null

    if (item.isVideo) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, item.uri)
            val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            if (w > 0 && h > 0) {
                resStr = "$w × $h"
                val mp = (w.toDouble() * h) / 1_000_000.0
                mpStr = "${decimal.format(mp)} MP"
            }
            val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: item.durationMs
            if (durMs > 0) {
                val totalSec = durMs / 1000
                val m = (totalSec % 3600) / 60
                val s = totalSec % 60
                val hrs = totalSec / 3600
                videoDuration = strings.durationText(hrs, m, s)
            }
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull() ?: 0L
            if (bitrate > 0) {
                val mbps = bitrate.toDouble() / 1_000_000.0
                videoBitrate = "${decimal.format(mbps)} Mbps"
            }
        } catch (_: Exception) {
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    } else {
        // Image EXIF
        try {
            openWithLocation(context, item.uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val w = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                val h = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
                if (w > 0 && h > 0) {
                    resStr = "$w × $h"
                    val mp = (w.toDouble() * h) / 1_000_000.0
                    mpStr = "${decimal.format(mp)} MP"
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
                    exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0).takeIf { it > 0.0 }?.let {
                        fNumberText = "ƒ${decimal.format(it)}"
                    }
                }
                if (!exposure.isNullOrBlank()) {
                    val expDouble = exposure.toDoubleOrNull()
                    if (expDouble != null && expDouble < 1.0 && expDouble > 0.0) {
                        val fraction = (1.0 / expDouble).roundToInt()
                        settingsParts.add("1/${fraction}s")
                        exposureTimeText = "1/$fraction s"
                    } else {
                        settingsParts.add("${exposure}s")
                        exposureTimeText = expDouble?.let { "${decimal.format(it)} s" }
                    }
                }
                if (!iso.isNullOrBlank()) {
                    settingsParts.add("ISO $iso")
                    isoStr = iso
                }
                if (!focalLen.isNullOrBlank()) {
                    val fVal = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
                    if (fVal > 0.0) {
                        settingsParts.add("${decimal.format(fVal)}mm")
                        focal = "${decimal.format(fVal)} mm"
                    }
                }
                if (exif.getAttribute(ExifInterface.TAG_EXPOSURE_BIAS_VALUE) != null) {
                    exposureBiasText = "${decimal.format(exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_BIAS_VALUE, 0.0))} ev"
                }
                exif.getAttributeInt(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, 0).takeIf { it > 0 }?.let {
                    focal35Text = "$it mm"
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

    // Files without size EXIF (PNG, screenshots) still have MediaStore's dimensions.
    if (resStr.isBlank() && item.width > 0 && item.height > 0) {
        val rotated = item.orientation % 180 != 0
        val w = if (rotated) item.height else item.width
        val h = if (rotated) item.width else item.height
        resStr = "$w × $h"
        mpStr = "${decimal.format((w.toDouble() * h) / 1_000_000.0)} MP"
    }

    return MediaMetadata(
        formattedDate = dateStr,
        fileName = item.displayName.ifBlank { "media_${item.id}" },
        fileSizeFormatted = sizeStr,
        resolutionFormatted = resStr,
        megapixelsFormatted = mpStr,
        storagePath = item.dataPath.ifBlank { item.relativePath },
        albumName = item.bucketDisplayName.ifBlank { strings.gallery },
        mimeType = item.mimeType,
        cameraDevice = camDevice,
        cameraSettings = camSettings,
        focalLength = focal,
        iso = isoStr,
        flashUsed = flashUsed,
        latitude = lat,
        longitude = lon,
        durationFormatted = videoDuration,
        bitrateFormatted = videoBitrate,
        fNumberText = fNumberText,
        exposureTimeText = exposureTimeText,
        exposureBiasText = exposureBiasText,
        focal35Text = focal35Text,
        formatLabel = item.mimeType.substringAfter('/', "").substringAfterLast('.').uppercase(Locale.ROOT)
            .removePrefix("X-").ifBlank { item.displayName.substringAfterLast('.', "").uppercase(Locale.ROOT) },
        fullDate = fullDate
    )
}

private fun formatFileSize(bytes: Long, decimal: DecimalFormat): String {
    if (bytes <= 0L) return ""
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "${decimal.format(gb)} GB"
        mb >= 1.0 -> "${decimal.format(mb)} MB"
        else -> "${decimal.format(kb)} KB"
    }
}
