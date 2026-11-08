package com.gallery.app.service

import android.content.Context
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.max

/**
 * The live wallpaper: the video or GIF the user picked in the viewer, playing on the home screen.
 *
 * Which file it is lives in [LiveWallpaperChoice]; the engine reads it whenever it becomes visible, so
 * picking another one takes effect straight away. Playback stops whenever the wallpaper is not visible
 * (another app in front, screen off), so nothing runs in the background.
 */
class LiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = MediaEngine()

    private inner class MediaEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private var player: MediaPlayer? = null
        private var gif: AnimatedImageDrawable? = null
        private var playing: Uri? = null
        /** The player only accepts start() once it has finished preparing. */
        private var prepared = false

        /** Redraws the GIF about 30 times a second while it is visible. */
        private val drawGif = object : Runnable {
            override fun run() {
                if (!isVisible) return
                drawGifFrame()
                handler.postDelayed(this, 33L)
            }
        }

        /**
         * Tries again while the file cannot be opened yet.
         *
         * Straight after a restart the wallpaper is drawn before the phone has been unlocked, and
         * nothing on the shared storage can be read until it is - the clip simply is not there yet.
         * Giving up at that point is what left a black screen behind until the wallpaper was set anew.
         */
        private val retry = object : Runnable {
            override fun run() {
                if (isVisible) start()
            }
        }

        /** The moment the phone is unlocked the clip becomes readable, so it is started right then. */
        private val onUnlocked = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: android.content.Intent?) {
                if (isVisible) start()
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            runCatching {
                androidx.core.content.ContextCompat.registerReceiver(
                    this@LiveWallpaperService,
                    onUnlocked,
                    android.content.IntentFilter().apply {
                        addAction(android.content.Intent.ACTION_USER_UNLOCKED)
                        addAction(android.content.Intent.ACTION_USER_PRESENT)
                    },
                    androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
                )
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) start() else pause()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            player?.let { runCatching { it.setSurface(holder.surface) } }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            player?.let { runCatching { it.setSurface(holder.surface) } }
            if (isVisible) start()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            pause()
        }


        private fun start() {
            handler.removeCallbacks(retry)
            val choice = LiveWallpaperChoice.read(this@LiveWallpaperService)
            if (choice == null) {
                // Nothing picked yet, or the setting is not readable. Either way: try again shortly. Not
                // painted black here: if a video comes next, the canvas would block it (see paintBlack).
                handler.postDelayed(retry, RETRY_MS)
                return
            }
            if (choice.uri == playing && (player != null || gif != null)) {
                player?.let {
                    runCatching { it.setSurface(surfaceHolder.surface) }
                    if (prepared) runCatching { it.start() }
                }
                gif?.let {
                    runCatching { it.start() }
                    handler.removeCallbacks(drawGif)
                    handler.post(drawGif)
                }
                return
            }
            release()
            playing = choice.uri
            prepared = false
            if (choice.isVideo) startVideo(choice.uri) else startGif(choice.uri)
            if (player == null && gif == null) {
                // The file could not be opened - the phone has most likely not been unlocked since it
                // was restarted. Come back to it in a moment. A GIF wallpaper is painted black meanwhile;
                // a video one is left alone, since its surface stays black until the player draws.
                playing = null
                if (!choice.isVideo) paintBlack()
                handler.postDelayed(retry, RETRY_MS)
            }
        }

        /**
         * Fills the wallpaper with black, so a failed GIF start shows nothing rather than stale pixels.
         *
         * Never for a video: drawing with a canvas claims the surface for the CPU for good, and the video
         * player can then no longer attach to it ("already connected"). After a restart that left the
         * wallpaper still and black until the app was opened, which created a fresh surface.
         */
        private fun paintBlack() {
            val holder = surfaceHolder
            if (holder.surface?.isValid != true) return
            val canvas = runCatching { holder.lockCanvas() }.getOrNull() ?: return
            runCatching { canvas.drawColor(Color.BLACK) }
            runCatching { holder.unlockCanvasAndPost(canvas) }
        }

        private fun startVideo(uri: Uri) {
            val created = MediaPlayer()
            player = runCatching {
                created.apply {
                    setDataSource(this@LiveWallpaperService, uri)
                    // setSurface, not setDisplay: a wallpaper surface refuses the "keep screen on" that setDisplay sets.
                    runCatching { setSurface(surfaceHolder.surface) }
                    isLooping = true
                    // A wallpaper stays silent; the video is there for its picture.
                    setVolume(0f, 0f)
                    setOnPreparedListener { mp ->
                        prepared = true
                        // Fill the screen and crop what does not fit, rather than stretching the video.
                        runCatching { mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING) }
                        if (isVisible) runCatching { mp.start() }
                    }
                    // A file that cannot be read yet fails here rather than when it was opened; the
                    // session is dropped so the next attempt starts cleanly.
                    setOnErrorListener { _, _, _ ->
                        // The engine's cleanup, not MediaPlayer.release(): inside this apply block a plain
                        // release() meant the player's own, which killed it while the engine still waited
                        // for it to finish preparing - the wallpaper then stayed black for good.
                        this@MediaEngine.release()
                        if (isVisible) handler.postDelayed(retry, RETRY_MS)
                        true
                    }
                    prepareAsync()
                }
            }.onFailure {
                // Half-built players hold on to a codec; let it go before trying again.
                runCatching { created.release() }
            }.getOrNull()
        }

        private fun startGif(uri: Uri) {
            gif = runCatching {
                val source = ImageDecoder.createSource(contentResolver, uri)
                (ImageDecoder.decodeDrawable(source) as? AnimatedImageDrawable)?.apply {
                    repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                    // Its own frame scheduling drives the redraws; the loop above only paints.
                    callback = object : Drawable.Callback {
                        override fun invalidateDrawable(who: Drawable) = Unit
                        override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
                            handler.postAtTime(what, who, `when`)
                        }
                        override fun unscheduleDrawable(who: Drawable, what: Runnable) {
                            handler.removeCallbacks(what, who)
                        }
                    }
                    start()
                }
            }.getOrNull()
            if (gif != null) handler.post(drawGif)
        }

        private fun drawGifFrame() {
            val drawable = gif ?: return
            val holder = surfaceHolder
            val canvas = runCatching { holder.lockCanvas() }.getOrNull() ?: return
            runCatching {
                canvas.drawColor(Color.BLACK)
                val width = canvas.width
                val height = canvas.height
                val imageWidth = drawable.intrinsicWidth.coerceAtLeast(1)
                val imageHeight = drawable.intrinsicHeight.coerceAtLeast(1)
                // Centre-cropped, like a photo wallpaper.
                val scale = max(width.toFloat() / imageWidth, height.toFloat() / imageHeight)
                val scaledWidth = (imageWidth * scale).toInt()
                val scaledHeight = (imageHeight * scale).toInt()
                val left = (width - scaledWidth) / 2
                val top = (height - scaledHeight) / 2
                drawable.setBounds(left, top, left + scaledWidth, top + scaledHeight)
                drawable.draw(canvas)
            }
            runCatching { holder.unlockCanvasAndPost(canvas) }
        }

        private fun pause() {
            handler.removeCallbacks(drawGif)
            handler.removeCallbacks(retry)
            // Only a prepared player may be paused: pausing one that is still preparing is an error (-38),
            // which happens whenever the wallpaper is hidden for a moment right after it started (a restart,
            // an app update, the preview). The prepared listener checks visibility before it starts.
            if (prepared) player?.let { runCatching { it.pause() } }
            gif?.let { runCatching { it.stop() } }
        }

        private fun release() {
            handler.removeCallbacksAndMessages(null)
            player?.let { runCatching { it.stop() }; runCatching { it.release() } }
            player = null
            prepared = false
            gif?.let { runCatching { it.stop() }; it.callback = null }
            gif = null
            playing = null
        }

        override fun onDestroy() {
            runCatching { unregisterReceiver(onUnlocked) }
            release()
            super.onDestroy()
        }
    }

    private companion object {
        /** How long to wait before looking for the clip again while the storage is still locked. */
        const val RETRY_MS = 2_000L
    }
}

/** The video or GIF shown as the live wallpaper, remembered between the viewer and the service. */
object LiveWallpaperChoice {

    /** Longer videos are not offered: a wallpaper loops, and a long clip would only drain the battery. */
    const val MAX_VIDEO_MS = 15_000L

    data class Choice(val uri: Uri, val isVideo: Boolean)

    /**
     * Device-protected storage, which is readable before the phone has been unlocked for the first
     * time after a restart. The app's ordinary settings are not: they are locked away until the user
     * types their PIN, and the wallpaper is already on screen by then. Reading them there did not
     * return "nothing", it threw - which took the wallpaper service down with it and left the home
     * screen black until the wallpaper was set again by hand.
     */
    private fun prefs(context: Context) =
        context.applicationContext.createDeviceProtectedStorageContext()
            .getSharedPreferences("prism_live_wallpaper", Context.MODE_PRIVATE)

    fun write(context: Context, uri: Uri, isVideo: Boolean) {
        runCatching {
            prefs(context).edit().putString("uri", uri.toString()).putBoolean("video", isVideo).apply()
        }
    }

    fun read(context: Context): Choice? = runCatching {
        val safe = prefs(context)
        val stored = safe.getString("uri", null)
            // Picked before this moved to storage that survives a restart: carried over once.
            ?: runCatching {
                context.applicationContext
                    .getSharedPreferences("prism_live_wallpaper", Context.MODE_PRIVATE)
                    .getString("uri", null)
                    ?.also { old ->
                        val video = context.applicationContext
                            .getSharedPreferences("prism_live_wallpaper", Context.MODE_PRIVATE)
                            .getBoolean("video", true)
                        safe.edit().putString("uri", old).putBoolean("video", video).apply()
                    }
            }.getOrNull()
            ?: return@runCatching null
        Choice(Uri.parse(stored), safe.getBoolean("video", true))
    }.getOrNull()
}
