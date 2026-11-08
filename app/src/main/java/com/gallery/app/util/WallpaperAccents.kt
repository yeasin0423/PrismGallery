package com.gallery.app.util

import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * The colours Android has extracted from the home-screen wallpaper.
 *
 * This reads the system's own palette, not the picture, so it needs no storage permission. Some
 * ROMs (and any device still on a live wallpaper that does not report colours) hand back nothing,
 * in which case the accent picker leaves the wallpaper row out entirely rather than showing an
 * empty one.
 */
object WallpaperAccents {

    /** Up to a handful of colours from the wallpaper, with unusable ones dropped. */
    fun read(context: Context, light: Boolean): List<Color> =
        (extracted(context) + dynamic(context, light))
            // A near-black or near-white wallpaper colour cannot carry an accent; the palette's own
            // readability pass would only drag it back to the text colour anyway.
            .filter { it.luminance() in 0.04f..0.92f }
            .distinct()
            .take(5)

    /** What the wallpaper itself reports. Live wallpapers and some ROMs answer with nothing. */
    private fun extracted(context: Context): List<Color> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return emptyList()
        val colors = runCatching {
            WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        }.getOrNull() ?: return emptyList()
        return listOfNotNull(colors.primaryColor, colors.secondaryColor, colors.tertiaryColor)
            .map { Color(it.toArgb()) }
    }

    /**
     * The system's Material You palette, which Android derives from the same wallpaper. This is the
     * one that actually answers on most modern phones, and it is already tuned to be usable as an
     * accent, so it fills in wherever the raw wallpaper colours come back empty.
     */
    private fun dynamic(context: Context, light: Boolean): List<Color> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return emptyList()
        val scheme = runCatching {
            if (light) dynamicLightColorScheme(context) else dynamicDarkColorScheme(context)
        }.getOrNull() ?: return emptyList()
        return listOf(scheme.primary, scheme.tertiary, scheme.secondary)
    }
}
