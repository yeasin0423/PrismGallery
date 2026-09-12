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

enum class PerformanceProfile { HIGH, MID, LOW }

class AmoledGalleryApp : Application(), SingletonImageLoader.Factory {

    companion object {
        lateinit var performanceProfile: PerformanceProfile
            private set
    }

    override fun onCreate() {
        super.onCreate()
        calculatePerformanceProfile()
        createNotificationChannels()
    }
    
    private fun calculatePerformanceProfile() {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val totalRam = memoryInfo.totalMem
        val gb = 1024L * 1024L * 1024L
        performanceProfile = when {
            totalRam >= 6 * gb -> PerformanceProfile.HIGH
            totalRam >= 3 * gb -> PerformanceProfile.MID
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
        val maxParallelism = if (performanceProfile == PerformanceProfile.MID) 2 else 4
        val enableCrossfade = performanceProfile == PerformanceProfile.HIGH
        val bitmapConf = if (performanceProfile == PerformanceProfile.HIGH) {
            android.graphics.Bitmap.Config.ARGB_8888
        } else {
            // "MID: Coil OHNE Crossfade, Bitmap.Config.RGB_565, Hardware-Bitmaps"
            // We use RGB_565, and HARDWARE bitmaps are enabled by default for Coil when appropriate.
            android.graphics.Bitmap.Config.RGB_565
        }

        return ImageLoader.Builder(context)
            .bitmapConfig(bitmapConf)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(if (performanceProfile == PerformanceProfile.LOW) 1024L * 1024 * 1024 else 512L * 1024 * 1024)
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

