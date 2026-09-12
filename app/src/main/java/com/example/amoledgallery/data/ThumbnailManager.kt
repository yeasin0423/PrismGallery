package com.example.amoledgallery.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Size
import com.example.amoledgallery.model.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.FileOutputStream

/**
 * Permanent internal thumbnail cache service.
 * Eliminates MicroSD I/O bus latency by caching downsampled thumbnails (250x250 px, WebP, 75% quality)
 * permanently on internal high-speed UFS storage (context.cacheDir.resolve("thumbnails")).
 *
 * Cache naming format: "${mediaItem.id}_${mediaItem.dateModified}.webp"
 * Includes a quiet background pre-indexer that step-by-step pre-caches missing thumbnails.
 */
class ThumbnailManager private constructor(private val context: Context) {

    val cacheDir: File = context.cacheDir.resolve("thumbnails").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    private val preindexScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(2))
    private var preindexJob: Job? = null

    @Volatile
    var isAggressivePerformanceMode: Boolean = false

    @Volatile
    private var isPreindexingPaused: Boolean = false

    /**
     * Resolves the internal cache file location for a given [MediaItem].
     */
    fun getThumbnailFile(item: MediaItem): File {
        return getThumbnailFile(item.id, item.dateModified)
    }

    /**
     * Resolves the internal cache file location for a given [id] and [dateModified].
     */
    fun getThumbnailFile(id: Long, dateModified: Long): File {
        val jpg = cacheDir.resolve("${id}_${dateModified}.jpg")
        if (jpg.exists()) return jpg
        val webp = cacheDir.resolve("${id}_${dateModified}.webp")
        if (webp.exists()) return webp
        return jpg
    }

    /**
     * Returns true if a non-empty cache file already exists on internal storage.
     */
    fun isCached(item: MediaItem): Boolean {
        return isCached(item.id, item.dateModified)
    }

    /**
     * Returns true if a non-empty cache file already exists on internal storage for [id] and [dateModified].
     */
    fun isCached(id: Long, dateModified: Long): Boolean {
        val file = getThumbnailFile(id, dateModified)
        return file.exists() && file.length() > 0L
    }

    /**
     * Retrieves existing cached thumbnail File, or decodes via ContentResolver.loadThumbnail(),
     * compresses to 160x160 JPEG with 75% quality, saves to internal storage, and returns the File.
     */
    suspend fun getOrCreateThumbnailFile(item: MediaItem): File? = withContext(Dispatchers.IO) {
        getOrCreateThumbnailFile(item.uri, item.isVideo, item.id, item.dateModified)
    }

    /**
     * Retrieves existing cached thumbnail File, or decodes via ContentResolver.loadThumbnail(),
     * compresses to 160x160 JPEG with 75% quality, saves to internal storage, and returns the File.
     */
    suspend fun getOrCreateThumbnailFile(
        uri: Uri,
        isVideo: Boolean,
        id: Long,
        dateModified: Long
    ): File? = withContext(Dispatchers.IO) {
        val targetFile = getThumbnailFile(id, dateModified)
        if (targetFile.exists() && targetFile.length() > 0L) {
            return@withContext targetFile
        }

        val bitmap = loadThumbnailBitmap(uri, isVideo, 160, 160) ?: return@withContext null
        if (saveBitmapToCache(bitmap, targetFile)) {
            cleanupOutdatedThumbnails(id, targetFile.name)
            targetFile
        } else {
            null
        }
    }

    /**
     * Decodes a downsampled thumbnail bitmap using ContentResolver.loadThumbnail (API 29+)
     * with fallbacks for older devices or decoding failures.
     */
    fun loadThumbnailBitmap(
        uri: Uri,
        isVideo: Boolean,
        targetWidth: Int = 160,
        targetHeight: Int = 160
    ): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                return context.contentResolver.loadThumbnail(uri, Size(targetWidth, targetHeight), null)
            } catch (_: Throwable) {
                // Fallback below
            }
        }

        return if (isVideo) {
            loadVideoThumbnailFallback(uri)
        } else {
            loadImageThumbnailFallback(uri, targetWidth, targetHeight)
        }
    }

    private fun loadImageThumbnailFallback(
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565

            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun loadVideoThumbnailFallback(uri: Uri): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val frame = retriever.frameAtTime
            retriever.release()
            if (frame != null && frame.config != Bitmap.Config.RGB_565) {
                val converted = frame.copy(Bitmap.Config.RGB_565, false)
                converted ?: frame
            } else {
                frame
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    /**
     * Compresses bitmap to JPEG format at 75% quality using fast SIMD libjpeg-turbo.
     */
    fun saveBitmapToCache(bitmap: Bitmap, targetFile: File): Boolean {
        return try {
            val tempFile = File.createTempFile("thumb_tmp_", ".tmp", cacheDir)
            val compressBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
            } else {
                bitmap
            }

            val quality = if (isAggressivePerformanceMode) 65 else 75
            FileOutputStream(tempFile).use { out ->
                compressBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.flush()
            }

            if (tempFile.length() > 0L) {
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                tempFile.renameTo(targetFile)
            } else {
                tempFile.delete()
                false
            }
        } catch (e: Exception) {
            Log.w("ThumbnailManager", "Failed to write cached thumbnail ${targetFile.name}", e)
            false
        }
    }

    /**
     * Asynchronously writes a decoded bitmap to internal UFS cache without blocking the caller.
     * Subsequent accesses will load the cached file directly at hardware speed.
     */
    fun saveBitmapAsync(bitmap: Bitmap, targetFile: File, id: Long) {
        if (targetFile.exists() && targetFile.length() > 0L) return
        preindexScope.launch {
            if (!targetFile.exists() || targetFile.length() == 0L) {
                if (saveBitmapToCache(bitmap, targetFile)) {
                    cleanupOutdatedThumbnails(id, targetFile.name)
                }
            }
        }
    }

    /**
     * Deletes older thumbnails for the same item ID when the media item was updated/modified.
     */
    private fun cleanupOutdatedThumbnails(id: Long, currentFileName: String) {
        try {
            val prefix = "${id}_"
            cacheDir.listFiles { _, name ->
                name.startsWith(prefix) && (name.endsWith(".jpg") || name.endsWith(".webp")) && name != currentFileName
            }?.forEach { it.delete() }
        } catch (_: Exception) {}
    }

    fun startPreindexing(items: List<MediaItem>) {
        // Disabled to eliminate device overheating, battery drain, and thermal throttling
    }

    fun pausePreindexing() {
        // No-op
    }

    fun resumePreindexing(items: List<MediaItem>) {
        // No-op
    }

    fun stopPreindexing() {
        // No-op
    }

    companion object {
        @Volatile
        private var instance: ThumbnailManager? = null

        fun getInstance(context: Context): ThumbnailManager {
            return instance ?: synchronized(this) {
                instance ?: ThumbnailManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
