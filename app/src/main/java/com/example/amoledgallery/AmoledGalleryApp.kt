package com.example.amoledgallery

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import coil3.request.bitmapConfig
import com.example.amoledgallery.data.MediaThumbnailFetcher
import com.example.amoledgallery.data.MediaThumbnailKeyer
import kotlinx.coroutines.Dispatchers
import okio.Path.Companion.toOkioPath

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

/**
 * Manual graphics quality tiers (like a game's video settings).
 * ULTRA is never auto-assigned - it's an explicit opt-in for users who know their device can handle it.
 */
enum class PerformanceProfile { LOW, MEDIUM, HIGH, ULTRA }

private const val PERF_PREFS_NAME = "amoled_gallery_perf"
private const val KEY_GRAPHICS_QUALITY = "graphics_quality"

class AmoledGalleryApp : Application(), SingletonImageLoader.Factory {

    companion object {
        lateinit var performanceProfile: PerformanceProfile
            private set

        /**
         * Rebuilds and installs a fresh Coil [ImageLoader] reflecting the current
         * [performanceProfile]. Called whenever the user changes graphics quality at runtime
         * so image-pipeline settings (bitmap depth, parallelism, cache sizes) apply immediately.
         */
        @OptIn(coil3.annotation.DelicateCoilApi::class)
        fun applyGraphicsQuality(context: Context, quality: PerformanceProfile) {
            performanceProfile = quality
            context.getSharedPreferences(PERF_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_GRAPHICS_QUALITY, quality.name)
                .apply()
            try {
                val app = context.applicationContext as AmoledGalleryApp
                coil3.SingletonImageLoader.setUnsafe(app.newImageLoader(app))
            } catch (_: Throwable) {
                // If the singleton can't be hot-swapped on this Coil version, the new
                // ImageLoader config takes effect on the next app start.
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        performanceProfile = loadGraphicsQuality()
        createNotificationChannels()
    }

    private fun loadGraphicsQuality(): PerformanceProfile {
        val prefs = getSharedPreferences(PERF_PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_GRAPHICS_QUALITY, null)
        return saved?.let { name -> runCatching { PerformanceProfile.valueOf(name) }.getOrNull() }
            ?: calculateDefaultPerformanceProfile()
    }

    private fun calculateDefaultPerformanceProfile(): PerformanceProfile {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val totalRam = memoryInfo.totalMem
        val gb = 1024L * 1024L * 1024L
        return when {
            totalRam >= 6 * gb -> PerformanceProfile.HIGH
            totalRam >= 3 * gb -> PerformanceProfile.MEDIUM
            else -> PerformanceProfile.LOW
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "move_media_channel",
                "Medien verschieben",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Fortschrittsanzeige beim Verschieben von Medien"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    override fun newImageLoader(context: Context): ImageLoader {
        // Bitmap depth, decode parallelism, animation and cache budgets scale with the manually
        // selected graphics quality tier (Low/Medium/High/Ultra) - see MenuScreen's quality picker.
        val maxParallelism = when (performanceProfile) {
            PerformanceProfile.LOW -> 2
            PerformanceProfile.MEDIUM -> 3
            PerformanceProfile.HIGH -> 4
            PerformanceProfile.ULTRA -> 6
        }
        val enableCrossfade = performanceProfile == PerformanceProfile.HIGH || performanceProfile == PerformanceProfile.ULTRA
        val bitmapConf = if (enableCrossfade) {
            android.graphics.Bitmap.Config.ARGB_8888
        } else {
            // LOW/MEDIUM: RGB_565 halves per-pixel memory footprint on weaker/low-RAM SoCs.
            android.graphics.Bitmap.Config.RGB_565
        }
        val memoryCachePercent = when (performanceProfile) {
            PerformanceProfile.LOW -> 0.15
            PerformanceProfile.MEDIUM -> 0.20
            PerformanceProfile.HIGH -> 0.25
            PerformanceProfile.ULTRA -> 0.30
        }
        val diskCacheBytes = when (performanceProfile) {
            // LOW/ULTRA both favor a larger disk cache: LOW to avoid re-decoding on weak CPUs,
            // ULTRA because the user explicitly opted into using more device resources.
            PerformanceProfile.LOW, PerformanceProfile.ULTRA -> 1024L * 1024 * 1024
            else -> 512L * 1024 * 1024
        }

        return ImageLoader.Builder(context)
            .bitmapConfig(bitmapConf)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, memoryCachePercent)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(diskCacheBytes)
                    .build()
            }
            .components {
                add(MediaThumbnailKeyer())
                add(MediaThumbnailFetcher.Factory(context))
                add(VideoFrameDecoder.Factory())
                add(coil3.gif.AnimatedImageDecoder.Factory())
            }
            .coroutineContext(Dispatchers.IO.limitedParallelism(maxParallelism))
            .fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(maxParallelism))
            .decoderCoroutineContext(Dispatchers.IO.limitedParallelism(maxParallelism))
            .crossfade(enableCrossfade)
            .build()
    }
}

