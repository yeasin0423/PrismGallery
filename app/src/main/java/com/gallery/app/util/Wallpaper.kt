package com.gallery.app.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlin.math.max
import kotlin.math.roundToInt

/** Which screen(s) a photo becomes the wallpaper of. */
enum class WallpaperTarget(val flags: Int) {
    HOME(WallpaperManager.FLAG_SYSTEM),
    LOCK(WallpaperManager.FLAG_LOCK),
    BOTH(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
}

/**
 * Sets the image at [uri] as wallpaper, directly through [WallpaperManager].
 *
 * The share-style "Set as" route (ACTION_ATTACH_DATA) depends on the phone having a wallpaper app
 * that answers it - on custom ROMs and GSIs there often is none, and nothing happened. Here the photo
 * is decoded at about screen size, turned upright (EXIF), centre-cropped to the screen's shape so it
 * fills the screen without stretching, and handed over as a bitmap.
 *
 * Blocking I/O: call it off the main thread. Returns false if the image could not be read or set.
 */
fun setWallpaper(context: Context, uri: Uri, target: WallpaperTarget): Boolean = runCatching {
    val manager = WallpaperManager.getInstance(context)
    if (!manager.isWallpaperSupported || !manager.isSetWallpaperAllowed) return false
    val resolver = context.contentResolver
    // The whole screen, system bars included: the wallpaper sits behind them too.
    val (screenW, screenH) = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        val bounds = context.getSystemService(android.view.WindowManager::class.java).maximumWindowMetrics.bounds
        bounds.width() to bounds.height()
    } else {
        context.resources.displayMetrics.let { it.widthPixels to it.heightPixels }
    }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false
    val rotation = runCatching {
        resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
    }.getOrDefault(0)

    // Decode no larger than needed: the short side still at least the screen's long side.
    val needed = max(screenW, screenH)
    var sample = 1
    while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= needed) sample *= 2
    val decoded = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return false

    val upright = if (rotation != 0) {
        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
            .also { if (it !== decoded) decoded.recycle() }
    } else {
        decoded
    }

    val screenAspect = screenW.toFloat() / screenH
    val cropped = if (upright.width.toFloat() / upright.height > screenAspect) {
        val w = (upright.height * screenAspect).roundToInt()
        Bitmap.createBitmap(upright, (upright.width - w) / 2, 0, w, upright.height)
    } else {
        val h = (upright.width / screenAspect).roundToInt()
        Bitmap.createBitmap(upright, 0, (upright.height - h) / 2, upright.width, h)
    }

    manager.setBitmap(cropped, null, true, target.flags)
    true
}.getOrDefault(false)

/**
 * Makes the video or GIF at [uri] the live wallpaper: the choice is stored for
 * [com.gallery.app.service.LiveWallpaperService], and Android's own wallpaper preview opens
 * so the user confirms it (an app cannot switch the live wallpaper on its own). Falls back to the
 * general live-wallpaper list on phones without the direct preview.
 */
fun setLiveWallpaper(context: Context, uri: Uri, isVideo: Boolean) {
    com.gallery.app.service.LiveWallpaperChoice.write(context, uri, isVideo)
    val component = android.content.ComponentName(context, com.gallery.app.service.LiveWallpaperService::class.java)
    val preview = android.content.Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
    val opened = runCatching { context.startActivity(preview); true }.getOrDefault(false)
    if (!opened) {
        runCatching { context.startActivity(android.content.Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)) }
    }
}
