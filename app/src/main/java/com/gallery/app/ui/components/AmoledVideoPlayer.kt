package com.gallery.app.ui.components
import com.gallery.app.ui.i18n.strings

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.gallery.app.model.MediaItem
import com.gallery.app.ui.design.ThemedButton
import com.gallery.app.ui.theme.PureWhite
import com.gallery.app.ui.theme.TextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

/**
 * Prefers the device's hardware decoders for video.
 *
 * Some devices (notably MediaTek ones) report no profile/level support for a format that their
 * hardware decoder plays perfectly well. Media3 then ranks the software decoder first - and the
 * software H.264 decoder crashes in `media.swcodec` on those devices. Hardware decoders are
 * therefore used exclusively whenever one exists for the MIME type; formats without any hardware
 * decoder (and all audio) keep the default list, so nothing loses support.
 */
@OptIn(UnstableApi::class)
private val preferHardwareVideoCodecSelector = MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
    val all = MediaCodecSelector.DEFAULT.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
    if (!mimeType.startsWith("video/")) {
        all
    } else {
        all.filter { it.hardwareAccelerated }.ifEmpty { all }
    }
}

/**
 * Native Media3 ExoPlayer video viewer in pure AMOLED black:
 * - Direct hardware decoding via Media3
 * - Minimalist center Play/Pause control
 * - Minimalist bottom scrubber and timestamp overlay
 * - Opens on its thumbnail; playback starts only with the Play button
 * - Stays paused also after coming back to the app
 * - Controls that belong to the viewer's: they appear and fade with its top and bottom bars
 * - Proper lifecycle cleanup
 *
 * The page is one stable composition whether it is being scrolled or has settled: the cached
 * thumbnail is the base layer in both cases, and the player (and its decoder) is only created on
 * the first Play press. That is what keeps swiping onto a video from flashing black.
 */
@OptIn(UnstableApi::class)
@Composable
fun AmoledVideoPlayer(
    item: MediaItem,
    isActivePage: Boolean,
    /** Play button and scrubber follow the viewer's bars, so everything fades together. */
    controlsVisible: Boolean,
    onTap: () -> Unit,
    /** Pausing brings the controls back, however they were hidden. */
    onShowControls: () -> Unit,
    onPlayingChange: (Boolean) -> Unit,
    onScrubbingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var isPlaying by remember(item.uri) { mutableStateOf(false) }
    var isFirstFrameRendered by remember(item.uri) { mutableStateOf(false) }
    var currentPositionMs by remember(item.uri) { mutableLongStateOf(0L) }
    var durationMs by remember(item.uri) { mutableLongStateOf(item.durationMs) }
    var isScrubbing by remember(item.uri) { mutableStateOf(false) }
    /** Paused until the user presses Play; returning to the app does not start playback. */
    var pausedByUser by remember(item.uri) { mutableStateOf(true) }
    var playbackError by remember(item.uri) { mutableStateOf<PlaybackException?>(null) }

    /**
     * False until the user presses Play. Until then no player, media item or decoder exists, so a
     * video page is just its thumbnail - swiping past one costs the same as swiping past a photo.
     */
    var playbackStarted by remember(item.uri) { mutableStateOf(false) }
    var exoPlayer by remember(item.uri) { mutableStateOf<ExoPlayer?>(null) }

    fun createPlayer(): ExoPlayer {
        val renderersFactory = DefaultRenderersFactory(context)
            // Without this, Media3 can pick a software H.264 decoder that crashes on some devices.
            .setMediaCodecSelector(preferHardwareVideoCodecSelector)
            .setEnableDecoderFallback(true)
        return ExoPlayer.Builder(context, renderersFactory).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = false
            val self = this
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onRenderedFirstFrame() {
                    isFirstFrameRendered = true
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        val d = self.duration
                        if (d > 0) durationMs = d
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    playbackError = error
                    isPlaying = false
                }
            })
        }
    }

    // Loads and prepares the video on the first Play press, then starts it.
    fun startPlayback() {
        val fresh = exoPlayer == null
        val player = exoPlayer ?: createPlayer().also { exoPlayer = it }
        if (fresh) {
            player.setMediaItem(Media3Item.fromUri(item.uri))
            player.prepare()
        }
        if (!playbackStarted) {
            playbackStarted = true
            // A scrub before Play already moved the handle; start from there.
            if (currentPositionMs > 0L) player.seekTo(currentPositionMs)
        }
        player.play()
    }

    fun retryPlayback() {
        playbackError = null
        isFirstFrameRendered = false
        pausedByUser = false
        currentPositionMs = 0L
        val player = exoPlayer ?: createPlayer().also { exoPlayer = it }
        player.setMediaItem(Media3Item.fromUri(item.uri))
        player.prepare()
        playbackStarted = true
        player.play()
    }

    LaunchedEffect(isPlaying) { onPlayingChange(isPlaying) }
    LaunchedEffect(isScrubbing) { onScrubbingChange(isScrubbing) }
    val reportStopped by rememberUpdatedState(onPlayingChange)
    DisposableEffect(item.uri) {
        onDispose {
            exoPlayer?.release()
            exoPlayer = null
            reportStopped(false)
        }
    }

    // Pause when the app goes to the background; the player otherwise kept playing (and looping audio)
    // after leaving the app until the viewer was closed.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> exoPlayer?.pause()
                // Back in the app: carry on, unless the user had paused it themselves.
                androidx.lifecycle.Lifecycle.Event.ON_RESUME ->
                    if (!pausedByUser && playbackStarted) exoPlayer?.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Swiping away from the page ends playback and frees the decoder; the page goes back to a still.
    LaunchedEffect(isActivePage) {
        if (!isActivePage) {
            exoPlayer?.release()
            exoPlayer = null
            playbackStarted = false
            pausedByUser = true
            isPlaying = false
            isFirstFrameRendered = false
            currentPositionMs = 0L
            playbackError = null
        }
    }

    // Continuous progress tracking while playing
    LaunchedEffect(isPlaying, isScrubbing) {
        while (isActive && isPlaying && !isScrubbing) {
            currentPositionMs = exoPlayer?.currentPosition ?: 0L
            delay(150)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isActivePage) {
                if (isActivePage) detectTapGestures(onTap = { onTap() })
            },
        contentAlignment = Alignment.Center
    ) {
        // The player surface only exists after Play. Before that the page is pure Compose, so landing
        // on a video never flashes a freshly created black surface or decoder.
        if (playbackStarted) {
            exoPlayer?.let { player ->
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            setBackgroundColor(android.graphics.Color.BLACK)
                            setShutterBackgroundColor(android.graphics.Color.BLACK)
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { it.player = player },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // The cached thumbnail is always drawn, and keeps covering the player until its first frame.
        // It is the same image in the same slot before and after the page becomes active, so the
        // switch from scrolling to settled has nothing to reload - hence no blink.
        if (!isFirstFrameRendered) {
            AsyncImage(
                model = item.thumbnail ?: item.uri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        // A sharper first frame for the page being viewed. The soft thumbnail above stays visible
        // until this has loaded, so it never leaves a blank frame either.
        if (isActivePage && !playbackStarted) {
            AsyncImage(
                model = item.uri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isActivePage) {
            // Overlay Controls (Center Play/Pause + Bottom Scrubber)
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                // No scrim: a video page must look as bright as a photo. The play button has its own
                // backing circle, and the scrubber sits over the viewer's bottom gradient.
                Box(modifier = Modifier.fillMaxSize()) {
                    // Center Play/Pause Button
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                exoPlayer?.pause()
                                pausedByUser = true
                                onShowControls()
                            } else {
                                pausedByUser = false
                                startPlayback()
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) strings.pause else strings.play,
                            tint = PureWhite,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Bottom Scrubber Bar & Time Info (Positioned above Detail Bar). Capped in width so it
                    // does not stretch across the whole screen in landscape.
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .widthIn(max = 560.dp)
                            .fillMaxWidth()
                            .padding(bottom = 76.dp)
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTimestamp(currentPositionMs),
                                color = PureWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = formatTimestamp(durationMs),
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        val maxVal = durationMs.toFloat().coerceAtLeast(1f)
                        val sliderVal = currentPositionMs.toFloat().coerceIn(0f, maxVal)

                        Slider(
                            value = sliderVal,
                            onValueChange = { newPos ->
                                isScrubbing = true
                                currentPositionMs = newPos.toLong()
                            },
                            onValueChangeFinished = {
                                if (playbackStarted) exoPlayer?.seekTo(currentPositionMs)
                                isScrubbing = false
                            },
                            valueRange = 0f..maxVal,
                            colors = SliderDefaults.colors(
                                thumbColor = PureWhite,
                                activeTrackColor = PureWhite,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // A black screen is indistinguishable from a frozen player: say what happened and offer a retry.
        if (playbackError != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = strings.videoPlaybackFailed,
                    color = PureWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(18.dp))
                ThemedButton(
                    text = strings.retry,
                    icon = Icons.Rounded.Refresh,
                    onClick = { retryPlayback() }
                )
            }
        }
    }
}

private fun formatTimestamp(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
