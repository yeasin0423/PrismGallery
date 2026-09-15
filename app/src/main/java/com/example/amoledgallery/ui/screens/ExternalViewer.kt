package com.example.amoledgallery.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.exifinterface.media.ExifInterface
import com.example.amoledgallery.model.MediaItem

/** A photo or video another app asked to show ("Open with"). [item] is filled in once the file has been looked at. */
class ExternalViewRequest(val uri: Uri) {
    val item = mutableStateOf<MediaItem?>(null)
}

/** Shows only the handed-over file, in the viewer of the current design; closing returns to the other app. */
@Composable
fun ExternalMediaViewer(request: ExternalViewRequest, onClose: () -> Unit) {
    val item = request.item.value
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (item != null) {
            MediaDetailScreen(
                mediaList = remember(item) { listOf(item) },
                initialIndex = 0,
                onBack = onClose,
                onTrashItem = {},
                onRenameItem = { _, _ -> },
                viewOnly = true
            )
        }
    }
}

/**
 * Builds a [MediaItem] for a URI handed over by another app. It may come from MediaStore, a file manager's
 * FileProvider or a chat app, so every detail is optional: whatever can't be read stays at its default.
 * Runs blocking I/O; call it off the main thread.
 */
fun externalMediaItem(context: Context, uri: Uri, typeHint: String?): MediaItem {
    val resolver = context.contentResolver
    val mime = runCatching { resolver.getType(uri) }.getOrNull()?.takeIf { '/' in it && !it.endsWith("/*") }
        ?: typeHint?.takeIf { '/' in it }
        ?: ""
    val isVideo = mime.startsWith("video/")

    var name = uri.lastPathSegment?.substringAfterLast('/').orEmpty()
    var width = 0
    var height = 0
    var orientation = 0
    var durationMs = 0L
    var dateModified = 0L
    var dateTaken = 0L

    runCatching {
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                fun long(column: String): Long? =
                    cursor.getColumnIndex(column).takeIf { it >= 0 && !cursor.isNull(it) }?.let { cursor.getLong(it) }
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }
                    ?.let { cursor.getString(it) }?.takeIf { it.isNotBlank() }?.let { name = it }
                width = long(MediaStore.MediaColumns.WIDTH)?.toInt() ?: 0
                height = long(MediaStore.MediaColumns.HEIGHT)?.toInt() ?: 0
                orientation = long(MediaStore.MediaColumns.ORIENTATION)?.toInt() ?: 0
                durationMs = long(MediaStore.MediaColumns.DURATION) ?: 0L
                dateModified = long(MediaStore.MediaColumns.DATE_MODIFIED) ?: 0L
                dateTaken = (long(MediaStore.MediaColumns.DATE_TAKEN) ?: 0L) / 1000L
            }
        }
    }

    // Outside MediaStore the size and rotation have to come from the file itself; the zoom needs the size.
    if (!isVideo && (width <= 0 || height <= 0)) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            width = bounds.outWidth.coerceAtLeast(0)
            height = bounds.outHeight.coerceAtLeast(0)
        }
        runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                orientation = exif.rotationDegrees
                if (dateTaken <= 0L) {
                    exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.let { raw ->
                        runCatching { java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.US).parse(raw) }
                            .getOrNull()?.let { dateTaken = it.time / 1000L }
                    }
                }
            }
        }
    }

    return MediaItem(
        // Negative, so it can never collide with a MediaStore id.
        id = -1L - (uri.toString().hashCode().toLong() and 0xFFFFFFFFL),
        uri = uri,
        isVideo = isVideo,
        dateAdded = dateModified,
        durationMs = durationMs,
        displayName = name,
        dateModified = dateModified,
        mimeType = mime,
        dateTaken = dateTaken,
        width = width,
        height = height,
        orientation = orientation
    )
}
