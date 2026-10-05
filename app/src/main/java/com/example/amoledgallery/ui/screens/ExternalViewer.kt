package com.example.amoledgallery.ui.screens

import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.exifinterface.media.ExifInterface
import com.example.amoledgallery.model.MediaItem

/**
 * Photos or videos another app asked to show ("Open with"). Usually one file; a camera app can hand
 * over every shot of its session (as the intent's ClipData) so they can be swiped through, starting
 * at [uri]. [items] is filled in once the files have been looked at.
 */
class ExternalViewRequest(val uri: Uri, val uris: List<Uri> = listOf(uri)) {
    val items = mutableStateOf<List<MediaItem>>(emptyList())
    val initialIndex = mutableStateOf(0)

    /** Takes a deleted file out of the list; the viewer closes by itself once none is left. */
    fun remove(item: MediaItem) {
        items.value = items.value.filter { it.id != item.id }
    }
}

/**
 * Shows the handed-over files in the viewer of the current design; closing returns to the other app.
 *
 * Files that are in the phone's media library (a camera's shots) get the same viewer as inside the
 * gallery - favourite, edit, share and delete (to the trash, confirmed by the system). Files from
 * elsewhere (a chat app's attachment) cannot be deleted or kept as favourites, so they get the simple
 * share / details bar.
 */
@Composable
fun ExternalMediaViewer(
    request: ExternalViewRequest,
    onClose: () -> Unit,
    favoriteIds: Set<Long> = emptySet(),
    onToggleFavorite: (Long) -> Unit = {}
) {
    val items = request.items.value
    val context = LocalContext.current
    val inLibrary = items.isNotEmpty() && items.all { it.uri.authority == MediaStore.AUTHORITY }

    // The item waiting for the system's "move to trash?" answer.
    var trashing by remember { mutableStateOf<MediaItem?>(null) }
    var deletedAny by remember { mutableStateOf(false) }
    fun removed(item: MediaItem) {
        deletedAny = true
        request.remove(item)
    }
    val trashLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val item = trashing
        trashing = null
        if (result.resultCode == Activity.RESULT_OK && item != null) removed(item)
    }
    // The last file was deleted: nothing left to show, back to the app that sent them.
    if (deletedAny && items.isEmpty()) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onClose() }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (items.isNotEmpty()) {
            MediaDetailScreen(
                mediaList = items,
                initialIndex = request.initialIndex.value.coerceIn(0, items.lastIndex),
                onBack = onClose,
                onTrashItem = { item ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val pending = runCatching {
                            MediaStore.createTrashRequest(context.contentResolver, listOf(item.uri), true)
                        }.getOrNull()
                        if (pending != null) {
                            trashing = item
                            trashLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                        }
                    } else if (runCatching { context.contentResolver.delete(item.uri, null, null) > 0 }.getOrDefault(false)) {
                        removed(item)
                    }
                },
                onRenameItem = { _, _ -> },
                isFavorite = { it in favoriteIds },
                onToggleFavorite = onToggleFavorite,
                viewOnly = !inLibrary,
                allowRename = false
            )
        }
    }
}

/**
 * Loads every file of [request] (blocking I/O; call it off the main thread). Files that no longer
 * exist - a shot deleted since the camera handed the list over - are left out rather than shown blank.
 */
fun loadExternalRequest(context: Context, request: ExternalViewRequest, typeHint: String?) {
    val readable = request.uris.filter { uri ->
        uri == request.uri || runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
        }.getOrDefault(false)
    }
    val items = readable.map { uri -> externalMediaItem(context, uri, typeHint.takeIf { uri == request.uri }) }
    request.initialIndex.value = readable.indexOf(request.uri).coerceAtLeast(0)
    request.items.value = items
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
    var dataPath = ""
    var relativePath = ""
    var bucketName = ""

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
                // Where it lives (library files only): the details show the folder.
                fun text(column: String): String? =
                    cursor.getColumnIndex(column).takeIf { it >= 0 && !cursor.isNull(it) }?.let { cursor.getString(it) }
                text(MediaStore.MediaColumns.DATA)?.let { dataPath = it }
                text(MediaStore.MediaColumns.RELATIVE_PATH)?.let { relativePath = it }
                text(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)?.let { bucketName = it }
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
        // A library file keeps its MediaStore id (so favourites match the gallery's); anything else gets
        // a negative one, which can never collide with a MediaStore id.
        id = if (uri.authority == MediaStore.AUTHORITY) {
            runCatching { ContentUris.parseId(uri) }.getOrNull()?.takeIf { it >= 0 }
        } else {
            null
        } ?: (-1L - (uri.toString().hashCode().toLong() and 0xFFFFFFFFL)),
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
        orientation = orientation,
        dataPath = dataPath,
        relativePath = relativePath,
        bucketDisplayName = bucketName
    )
}
