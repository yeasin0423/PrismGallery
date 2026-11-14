@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.gallery.app.ui.editor

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import androidx.media3.common.Effect
import androidx.media3.effect.Crop
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.gallery.app.data.ThumbnailManager
import com.gallery.app.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.roundToInt
import androidx.media3.common.MediaItem as ExoMediaItem

/** Crop rectangle in 0..1 coordinates of the rotated/flipped frame (y grows downwards). */
data class NormalizedRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    val isFull: Boolean
        get() = left <= 0.001f && top <= 0.001f && right >= 0.999f && bottom >= 0.999f

    /** The same region after the frame is rotated 90° clockwise. */
    fun rotateClockwise() = NormalizedRect(1f - bottom, left, 1f - top, right)

    /** The same region after the frame is rotated 90° counter-clockwise. */
    fun rotateCounterClockwise() = NormalizedRect(top, 1f - right, bottom, 1f - left)

    /** The same region after the frame is mirrored left-right. */
    fun flipHorizontal() = NormalizedRect(1f - right, top, 1f - left, bottom)

    /** The same region after the frame is mirrored top-bottom. */
    fun flipVertical() = NormalizedRect(left, 1f - bottom, right, 1f - top)

    companion object {
        val Full = NormalizedRect(0f, 0f, 1f, 1f)
    }
}

/**
 * An edit: mirror first ([flipHorizontal]/[flipVertical] on the source frame), then rotate clockwise by
 * [rotation] degrees, then crop to [crop] (expressed in the resulting frame).
 */
data class EditSpec(
    val rotation: Int,
    val flipHorizontal: Boolean,
    val flipVertical: Boolean,
    val crop: NormalizedRect
)

/** Result of saving an edited image. */
sealed interface ImageSaveResult {
    /** The edited image was stored, either as a new copy or over the original. */
    data class Saved(val uri: Uri, val overwritten: Boolean) : ImageSaveResult

    /**
     * The original is owned by another app and Android must ask the user for write access first.
     * [tempFile] holds the rendered image and is deleted by [MediaEditor.completeOverwrite] or
     * [MediaEditor.discardOverwrite].
     */
    data class NeedsWritePermission(val pendingIntent: PendingIntent, val tempFile: File) : ImageSaveResult
}

/** A rendered edit waiting in the cache directory. */
private data class RenderedImage(val file: File, val extension: String, val mimeType: String)

/** Loading previews and exporting edited copies of images and videos. */
object MediaEditor {

    /** Largest side of a saved image; keeps full-resolution decodes of 50 MP photos within memory. */
    private const val MAX_EXPORT_SIDE = 4096

    suspend fun loadPreview(context: Context, item: MediaItem, maxSize: Int = 1600): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            if (item.isVideo) loadVideoFrame(context, item.uri, maxSize) else decodeImage(context, item.uri, maxSize)
        }.getOrNull()
    }

    /** Applies mirroring and then a clockwise rotation (the preview and export share this order). */
    fun transform(source: Bitmap, rotation: Int, flipHorizontal: Boolean, flipVertical: Boolean): Bitmap {
        if (rotation % 360 == 0 && !flipHorizontal && !flipVertical) return source
        val matrix = Matrix().apply {
            postScale(if (flipHorizontal) -1f else 1f, if (flipVertical) -1f else 1f, source.width / 2f, source.height / 2f)
            postRotate(rotation.toFloat())
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun decodeImage(context: Context, uri: Uri, maxSize: Int): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        // ImageDecoder already applies the EXIF orientation.
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = max(info.size.width, info.size.height)
            if (longest > maxSize) {
                val scale = maxSize.toFloat() / longest
                decoder.setTargetSize(
                    (info.size.width * scale).roundToInt().coerceAtLeast(1),
                    (info.size.height * scale).roundToInt().coerceAtLeast(1)
                )
            }
        }
    }

    private fun loadVideoFrame(context: Context, uri: Uri, maxSize: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val videoWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val videoHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val frame = retriever.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, maxSize, maxSize)
                ?: return null
            // Some retrievers return frames without the container rotation applied; detect that by
            // comparing the frame's orientation with the displayed orientation and rotate if needed.
            if (rotation % 180 != 0 && videoWidth > 0 && videoHeight > 0) {
                val displayedLandscape = videoHeight >= videoWidth
                val frameLandscape = frame.width >= frame.height
                if (displayedLandscape != frameLandscape) return transform(frame, rotation, false, false)
            }
            return frame
        } finally {
            runCatching { retriever.release() }
        }
    }

    /**
     * Saves an edited image. With [overwrite] false the result is stored as a new copy next to the
     * original. With [overwrite] true the original file is replaced in place; if Android does not let
     * this app write it, [ImageSaveResult.NeedsWritePermission] carries the request to show.
     */
    suspend fun saveImage(
        context: Context,
        item: MediaItem,
        spec: EditSpec,
        overwrite: Boolean = false
    ): ImageSaveResult = withContext(Dispatchers.IO) {
        if (overwrite) {
            val rendered = renderToCache(context, item, spec, overwrite = true)
            try {
                writeToOriginal(context, item.uri, rendered.file)
                rendered.file.delete()
                invalidateCaches(context, item)
                ImageSaveResult.Saved(item.uri, overwritten = true)
            } catch (e: SecurityException) {
                val pending = writeRequest(context, item.uri)
                if (pending == null) {
                    rendered.file.delete()
                    throw e
                }
                ImageSaveResult.NeedsWritePermission(pending, rendered.file)
            } catch (e: FileNotFoundException) {
                val pending = writeRequest(context, item.uri)
                if (pending == null) {
                    rendered.file.delete()
                    throw e
                }
                ImageSaveResult.NeedsWritePermission(pending, rendered.file)
            } catch (e: Exception) {
                rendered.file.delete()
                throw e
            }
        } else {
            val rendered = renderToCache(context, item, spec, overwrite = false)
            try {
                ImageSaveResult.Saved(insertCopy(context, item, rendered), overwritten = false)
            } finally {
                rendered.file.delete()
            }
        }
    }

    /**
     * True when an edited image can replace the original in place. JPEG and PNG always; WebP only on
     * Android 11+, where the lossy encoder exists.
     */
    fun canOverwrite(item: MediaItem): Boolean {
        if (item.isVideo) return false
        val mime = item.mimeType.lowercase()
        val extension = item.displayName.substringAfterLast('.', "").lowercase()
        val isJpeg = mime.contains("jpeg") || mime.contains("jpg") || extension == "jpg" || extension == "jpeg"
        val isPng = mime.contains("png") || extension == "png"
        val isWebp = mime.contains("webp") || extension == "webp"
        return isJpeg || isPng || (isWebp && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
    }

    /** Writes the rendered image into the original after the user granted write access. */
    suspend fun completeOverwrite(context: Context, item: MediaItem, tempFile: File): Uri = withContext(Dispatchers.IO) {
        try {
            writeToOriginal(context, item.uri, tempFile)
            invalidateCaches(context, item)
            item.uri
        } finally {
            tempFile.delete()
        }
    }

    /** Deletes the rendered image when the user declined the overwrite request. */
    fun discardOverwrite(tempFile: File) {
        runCatching { tempFile.delete() }
    }

    /** Decodes, rotates, mirrors, crops and compresses the edit into a cache file. */
    private fun renderToCache(
        context: Context,
        item: MediaItem,
        spec: EditSpec,
        overwrite: Boolean
    ): RenderedImage {
        val decoded = decodeImage(context, item.uri, MAX_EXPORT_SIDE)
        val transformed = transform(decoded, spec.rotation, spec.flipHorizontal, spec.flipVertical)
        val crop = spec.crop
        val x = (crop.left * transformed.width).roundToInt().coerceIn(0, transformed.width - 1)
        val y = (crop.top * transformed.height).roundToInt().coerceIn(0, transformed.height - 1)
        val width = ((crop.right - crop.left) * transformed.width).roundToInt().coerceIn(1, transformed.width - x)
        val height = ((crop.bottom - crop.top) * transformed.height).roundToInt().coerceIn(1, transformed.height - y)
        val result = if (crop.isFull) transformed else Bitmap.createBitmap(transformed, x, y, width, height)

        val (format, extension, mimeType) = outputFormat(item, overwrite)
        val file = File(context.cacheDir, "edit_${System.currentTimeMillis()}.$extension")
        try {
            file.outputStream().use { stream ->
                if (!result.compress(format, 95, stream)) error("compress failed")
            }
            // Keep the camera metadata when replacing the original. The pixels are already upright,
            // so the orientation tag is reset instead of copied.
            if (overwrite && format == Bitmap.CompressFormat.JPEG) copyExif(context, item.uri, file)
            return RenderedImage(file, extension, mimeType)
        } catch (e: Exception) {
            file.delete()
            throw e
        } finally {
            if (result !== transformed) result.recycle()
            if (transformed !== decoded) transformed.recycle()
            decoded.recycle()
        }
    }

    /** The encoded format used for a copy, or for an overwrite (which must keep the original format). */
    private fun outputFormat(item: MediaItem, overwrite: Boolean): Triple<Bitmap.CompressFormat, String, String> {
        val mime = item.mimeType.lowercase()
        val extension = item.displayName.substringAfterLast('.', "").lowercase()
        val isJpeg = mime.contains("jpeg") || mime.contains("jpg") || extension == "jpg" || extension == "jpeg"
        val isPng = mime.contains("png") || extension == "png"
        val isWebp = mime.contains("webp") || extension == "webp"

        if (overwrite) {
            return when {
                isJpeg -> Triple(Bitmap.CompressFormat.JPEG, "jpg", "image/jpeg")
                isPng -> Triple(Bitmap.CompressFormat.PNG, "png", "image/png")
                isWebp -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Triple(Bitmap.CompressFormat.WEBP_LOSSY, "webp", "image/webp")
                } else {
                    @Suppress("DEPRECATION")
                    val webp = Bitmap.CompressFormat.WEBP
                    Triple(webp, "webp", "image/webp")
                }
                else -> error("This format can't be overwritten")
            }
        }

        return when {
            isPng -> Triple(Bitmap.CompressFormat.PNG, "png", "image/png")
            isWebp && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                Triple(Bitmap.CompressFormat.WEBP_LOSSY, "webp", "image/webp")
            else -> Triple(Bitmap.CompressFormat.JPEG, "jpg", "image/jpeg")
        }
    }

    /** Stores a rendered image as a new copy next to the original and returns its uri. */
    private fun insertCopy(context: Context, item: MediaItem, rendered: RenderedImage): Uri {
        val volume = volumeNameOf(context, item)
        val collection = MediaStore.Images.Media.getContentUri(volume)
        val uri = insertPending(
            context, collection, editedName(item, rendered.extension), rendered.mimeType, item.relativePath,
            allowedDirectories = listOf(Environment.DIRECTORY_DCIM, Environment.DIRECTORY_PICTURES),
            fallbackDirectory = Environment.DIRECTORY_PICTURES
        )
        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                rendered.file.inputStream().use { it.copyTo(out) }
            } ?: error("no output stream")
            publish(context, uri)
        } catch (e: Exception) {
            context.contentResolver.delete(uri, null, null)
            throw e
        }
        return uri
    }

    /** Writes the rendered bytes over the original MediaStore file. */
    private fun writeToOriginal(context: Context, uri: Uri, file: File) {
        val stream = try {
            context.contentResolver.openOutputStream(uri, "wt")
        } catch (_: IllegalArgumentException) {
            context.contentResolver.openOutputStream(uri, "w")
        } ?: error("no output stream")

        stream.use { out ->
            file.inputStream().use { it.copyTo(out) }
        }

        // Some providers keep DATE_MODIFIED at the old value. Updating it makes the gallery reload
        // and the thumbnail cache (keyed by id + dateModified) pick up the new pixels.
        runCatching {
            context.contentResolver.update(
                uri,
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis() / 1000)
                },
                null,
                null
            )
        }
    }

    /** Android's "allow this app to modify this photo" request, available from Android 11. */
    private fun writeRequest(context: Context, uri: Uri): PendingIntent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return runCatching {
            MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
        }.getOrNull()
    }

    /**
     * Drops the caches that may still hold the old pixels after an overwrite: the permanent
     * thumbnail file and Coil's in-memory/on-disk copies of the full image.
     */
    private suspend fun invalidateCaches(context: Context, item: MediaItem) {
        runCatching { ThumbnailManager.getInstance(context).getThumbnailFile(item).delete() }
        runCatching { coil3.SingletonImageLoader.get(context).memoryCache?.clear() }
        runCatching { coil3.SingletonImageLoader.get(context).diskCache?.clear() }
    }

    /**
     * Copies the camera metadata from the original to the rendered JPEG. Without this an overwrite
     * would drop DATE_TAKEN, GPS and camera details, and the photo could lose its place in the timeline.
     */
    private fun copyExif(context: Context, source: Uri, target: File) {
        runCatching {
            val original = context.contentResolver.openInputStream(source)?.use { ExifInterface(it) } ?: return
            val edited = ExifInterface(target.absolutePath)
            val tags = listOf(
                ExifInterface.TAG_DATETIME_ORIGINAL,
                ExifInterface.TAG_DATETIME_DIGITIZED,
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_F_NUMBER,
                ExifInterface.TAG_EXPOSURE_TIME,
                ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
                ExifInterface.TAG_FOCAL_LENGTH,
                ExifInterface.TAG_FLASH,
                ExifInterface.TAG_WHITE_BALANCE,
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_GPS_ALTITUDE_REF,
                ExifInterface.TAG_GPS_TIMESTAMP,
                ExifInterface.TAG_GPS_DATESTAMP
            )
            tags.forEach { tag ->
                original.getAttribute(tag)?.let { edited.setAttribute(tag, it) }
            }
            edited.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            edited.saveAttributes()
        }
    }

    /**
     * Exports an edited copy of a video with Media3 Transformer (rotation/mirroring and crop as GPU
     * effects), then stores it next to the original. [onProgress] reports 0..1.
     */
    suspend fun saveVideo(context: Context, item: MediaItem, spec: EditSpec, onProgress: (Float) -> Unit): Uri {
        val output = File(context.cacheDir, "edit_${System.currentTimeMillis()}.mp4")
        try {
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    val effects = mutableListOf<Effect>()
                    if (spec.rotation % 360 != 0 || spec.flipHorizontal || spec.flipVertical) {
                        effects += ScaleAndRotateTransformation.Builder()
                            .setScale(if (spec.flipHorizontal) -1f else 1f, if (spec.flipVertical) -1f else 1f)
                            // Media3 rotates counter-clockwise; the editor rotates clockwise.
                            .setRotationDegrees(((360 - spec.rotation % 360) % 360).toFloat())
                            .build()
                    }
                    if (!spec.crop.isFull) {
                        val c = spec.crop
                        // Normalized device coordinates: x and y in -1..1, y pointing up.
                        effects += Crop(2f * c.left - 1f, 2f * c.right - 1f, 1f - 2f * c.bottom, 1f - 2f * c.top)
                    }
                    val edited = EditedMediaItem.Builder(ExoMediaItem.fromUri(item.uri))
                        .setEffects(Effects(emptyList(), effects))
                        .build()

                    val handler = Handler(Looper.getMainLooper())
                    val transformer = Transformer.Builder(context)
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                                if (continuation.isActive) continuation.resume(Unit)
                            }

                            override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                                if (continuation.isActive) continuation.resumeWithException(exportException)
                            }
                        })
                        .build()
                    transformer.start(edited, output.absolutePath)

                    val holder = ProgressHolder()
                    val poll = object : Runnable {
                        override fun run() {
                            if (!continuation.isActive) return
                            if (transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                                onProgress(holder.progress / 100f)
                            }
                            handler.postDelayed(this, 150)
                        }
                    }
                    handler.post(poll)
                    continuation.invokeOnCancellation {
                        handler.removeCallbacks(poll)
                        transformer.cancel()
                    }
                }
            }
            onProgress(1f)

            return withContext(Dispatchers.IO) {
                val volume = volumeNameOf(context, item)
                val collection = MediaStore.Video.Media.getContentUri(volume)
                val uri = insertPending(
                    context, collection, editedName(item, "mp4"), "video/mp4", item.relativePath,
                    allowedDirectories = listOf(Environment.DIRECTORY_DCIM, Environment.DIRECTORY_MOVIES, Environment.DIRECTORY_PICTURES),
                    fallbackDirectory = Environment.DIRECTORY_MOVIES
                )
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        output.inputStream().use { it.copyTo(out) }
                    } ?: error("no output stream")
                    publish(context, uri)
                } catch (e: Exception) {
                    context.contentResolver.delete(uri, null, null)
                    throw e
                }
                uri
            }
        } finally {
            output.delete()
        }
    }

    /**
     * The storage volume the original lives on, so the copy lands next to it. Gallery uris use the
     * merged "external" volume, which inserts into internal storage - SD card originals then got their
     * copy in a same-named album on internal storage.
     */
    private fun volumeNameOf(context: Context, item: MediaItem): String {
        val fromRow = runCatching {
            context.contentResolver.query(item.uri, arrayOf(MediaStore.MediaColumns.VOLUME_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
        return fromRow?.takeIf { it.isNotBlank() && it != MediaStore.VOLUME_EXTERNAL }
            ?: MediaStore.VOLUME_EXTERNAL_PRIMARY
    }

    private fun editedName(item: MediaItem, extension: String): String {
        val base = item.displayName.substringBeforeLast('.').ifBlank { "media" }
        return "${base}_edited_${System.currentTimeMillis() / 1000}.$extension"
    }

    private fun insertPending(
        context: Context,
        collection: Uri,
        displayName: String,
        mimeType: String,
        relativePath: String,
        allowedDirectories: List<String>,
        fallbackDirectory: String
    ): Uri {
        // MediaStore only accepts certain top-level folders per media type (images: DCIM/Pictures,
        // videos: DCIM/Movies/Pictures). Originals elsewhere - the storage root, Download, app
        // folders - get their copy in the default folder instead.
        val cleanPath = relativePath.trim('/')
        val topDirectory = cleanPath.substringBefore('/')
        val targetPath = if (cleanPath.isNotEmpty() && allowedDirectories.any { it.equals(topDirectory, ignoreCase = true) }) {
            "$cleanPath/"
        } else {
            "$fallbackDirectory/"
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, targetPath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        return context.contentResolver.insert(collection, values) ?: error("MediaStore insert failed")
    }

    private fun publish(context: Context, uri: Uri) {
        val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
        context.contentResolver.update(uri, values, null, null)
    }
}
