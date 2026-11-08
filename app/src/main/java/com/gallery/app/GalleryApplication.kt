package com.gallery.app

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import coil3.request.bitmapConfig
import com.gallery.app.data.MediaThumbnailFetcher
import com.gallery.app.data.MediaThumbnailKeyer
import kotlinx.coroutines.Dispatchers
import okio.Path.Companion.toOkioPath

/**
 * Manual graphics quality tiers (like a game's video settings).
 * ULTRA is never auto-assigned - it's an explicit opt-in for users who know their device can handle it.
 */
enum class PerformanceProfile { LOW, MEDIUM, HIGH, ULTRA }

private const val PERF_PREFS_NAME = "amoled_gallery_perf"
private const val KEY_GRAPHICS_QUALITY = "graphics_quality"

class GalleryApplication : Application(), SingletonImageLoader.Factory {

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
                val app = context.applicationContext as GalleryApplication
                coil3.SingletonImageLoader.setUnsafe(app.newImageLoader(app))
            } catch (_: Throwable) {
                // If the singleton can't be hot-swapped on this Coil version, the new
                // ImageLoader config takes effect on the next app start.
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val userManager = getSystemService(android.os.UserManager::class.java)
        if (userManager == null || userManager.isUserUnlocked) {
            loadSettings()
        } else {
            // Started before the phone was unlocked for the first time since a restart: only the live
            // wallpaper (direct-boot aware) runs then, and the app's settings are still locked away -
            // reading them threw, which crashed the process and made Android drop the wallpaper.
            // Defaults until the unlock, then the real settings for everything that follows.
            performanceProfile = calculateDefaultPerformanceProfile()
            androidx.core.content.ContextCompat.registerReceiver(
                this,
                object : android.content.BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: android.content.Intent) {
                        runCatching { unregisterReceiver(this) }
                        loadSettings()
                    }
                },
                android.content.IntentFilter(android.content.Intent.ACTION_USER_UNLOCKED),
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    private fun loadSettings() {
        performanceProfile = loadGraphicsQuality()
        com.gallery.app.ui.theme.ThemeState.load(this)
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

    override fun newImageLoader(context: Context): ImageLoader {
        // Bitmap depth, decode parallelism, animation and cache budgets scale with the manually
        // selected graphics quality tier (Low/Medium/High/Ultra) - see MenuScreen's quality picker.
        // Decode parallelism. Two threads was too few even for LOW: a single screenful is ~25 tiles
        // and each miss costs a JPEG decode, so the queue never drained during a fling.
        val maxParallelism = when (performanceProfile) {
            PerformanceProfile.LOW -> 3
            PerformanceProfile.MEDIUM -> 4
            PerformanceProfile.HIGH -> 6
            PerformanceProfile.ULTRA -> 8
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
        // Coil's own disk cache only ever serves the full-size viewer and video frames -
        // grid thumbnails come from MediaThumbnailFetcher's own cache dir and never touch it -
        // so the previous 1GB budget just cost journal I/O at startup for nothing.
        val diskCacheBytes = when (performanceProfile) {
            PerformanceProfile.LOW, PerformanceProfile.MEDIUM -> 128L * 1024 * 1024
            else -> 256L * 1024 * 1024
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
            // NOTE: coroutineContext is deliberately left at Coil's default (Dispatchers.Main.immediate).
            // It runs the request/interceptor chain, which is where the memory cache is consulted -
            // moving it onto a limitedParallelism IO dispatcher meant even a guaranteed memory-cache
            // hit had to queue behind in-flight JPEG decodes and then hop back to the main thread,
            // so already-decoded tiles still appeared late while scrolling. Only the actual fetch and
            // decode work belongs off the main thread.
            .fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(maxParallelism))
            .decoderCoroutineContext(Dispatchers.IO.limitedParallelism(maxParallelism))
            .crossfade(enableCrossfade)
            .build()
    }
}

