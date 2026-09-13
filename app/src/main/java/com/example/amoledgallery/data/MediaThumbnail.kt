package com.example.amoledgallery.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.amoledgallery.PerformanceProfile
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Model representing a thumbnail request for Coil 3.
 * Incorporates MediaStore ID and dateModified to target permanent internal fast storage thumbnails.
 */
data class MediaThumbnail(
    val uri: Uri,
    val isVideo: Boolean = false,
    val id: Long = 0L,
    val dateModified: Long = 0L
)

/**
 * Keyer for caching thumbnails in Coil's MemoryCache.
 */
class MediaThumbnailKeyer : Keyer<MediaThumbnail> {
    override fun key(data: MediaThumbnail, options: Options): String {
        return if (data.id != 0L) {
            "thumb_${data.id}_${data.dateModified}_${data.isVideo}"
        } else {
            "thumb_${data.uri}_${data.isVideo}"
        }
    }
}

/**
 * High-performance thumbnail fetcher utilizing [ThumbnailManager] internal permanent cache.
 * 1. Checks if internal cached WebP file exists (loads directly from fast UFS internal storage).
 * 2. If not, decodes via ContentResolver.loadThumbnail(250x250) on Dispatchers.IO,
 *    compresses to WebP 75%, saves permanently in internal cache, and returns image.
 */
class MediaThumbnailFetcher(
    private val context: Context,
    private val data: MediaThumbnail
) : Fetcher {

    override suspend fun fetch(): FetchResult = withContext(Dispatchers.IO) {
        val thumbnailManager = ThumbnailManager.getInstance(context)

        // HARDWARE bitmaps decode directly into GPU-resident memory, skipping the CPU->GPU
        // texture upload that a software (RGB_565/ARGB_8888) bitmap needs on every draw - but
        // that upload-avoidance only pays off on devices with a mature hardware-bitmap
        // compositing path; on weaker/older GPUs, promoting every single clipped (rounded-
        // corner) grid tile to a hardware layer can cost MORE than it saves. So this follows
        // the same tier philosophy as the rest of the app: only HIGH/ULTRA (stronger, newer
        // devices) get HARDWARE bitmaps here; LOW/MEDIUM decode straight to cheap RGB_565.
        val quality = com.example.amoledgallery.AmoledGalleryApp.performanceProfile
        val useHardwareBitmap = quality == PerformanceProfile.HIGH || quality == PerformanceProfile.ULTRA
        val decodeOptions = BitmapFactory.Options().apply {
            inPreferredConfig = if (useHardwareBitmap) {
                android.graphics.Bitmap.Config.HARDWARE
            } else {
                android.graphics.Bitmap.Config.RGB_565
            }
        }

        // 1. Fast path: Check existing cache file on internal UFS storage
        if (data.id != 0L) {
            val cachedFile = thumbnailManager.getThumbnailFile(data.id, data.dateModified)
            if (cachedFile.exists() && cachedFile.length() > 0L) {
                val bitmap = BitmapFactory.decodeFile(cachedFile.absolutePath, decodeOptions)
                if (bitmap != null) {
                    return@withContext ImageFetchResult(
                        image = bitmap.asImage(),
                        isSampled = true,
                        dataSource = DataSource.DISK
                    )
                }
            }
        }

        // 2. Direct native decoding via ContentResolver.loadThumbnail (Zero WebP compression lag)
        val px = thumbnailManager.thumbnailPx
        val bitmap = thumbnailManager.loadThumbnailBitmap(data.uri, data.isVideo, px, px)
            ?: throw IllegalStateException("Unable to decode thumbnail for ${data.uri}")

        // Asynchronously cache on internal fast storage so all future scrolls read directly from disk
        if (data.id != 0L) {
            val targetFile = thumbnailManager.getThumbnailFile(data.id, data.dateModified)
            thumbnailManager.saveBitmapAsync(bitmap, targetFile, data.id)
        }

        ImageFetchResult(
            image = bitmap.asImage(),
            isSampled = true,
            dataSource = DataSource.MEMORY
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<MediaThumbnail> {
        override fun create(data: MediaThumbnail, options: Options, imageLoader: ImageLoader): Fetcher {
            return MediaThumbnailFetcher(context, data)
        }
    }
}
