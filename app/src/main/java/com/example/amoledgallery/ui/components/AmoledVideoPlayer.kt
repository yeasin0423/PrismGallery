package com.example.amoledgallery.ui.components

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.amoledgallery.model.MediaItem
import com.example.amoledgallery.ui.theme.AmoledBlack
import com.example.amoledgallery.ui.theme.PureWhite
import com.example.amoledgallery.ui.theme.TextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

/**
 * Native Media3 ExoPlayer video viewer in pure AMOLED black:
 * - Direct hardware decoding via Media3
 * - Minimalist center Play/Pause control
 * - Minimalist bottom scrubber and timestamp overlay
 * - Auto-hides controls after 2 seconds of playback
 * - Proper lifecycle cleanup
 */
@OptIn(UnstableApi::class)
@Composable
fun AmoledVideoPlayer(
    item: MediaItem,
    isActivePage: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isActivePage) {
        // Offscreen page in HorizontalPager: Render static thumbnail without allocating ExoPlayer or MediaCodec
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(AmoledBlack),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = item.thumbnail ?: item.uri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    ActiveVideoPlayer(
        item = item,
        onTap = onTap,
        modifier = modifier
    )
}

@OptIn(UnstableApi::class)
@Composable
private fun ActiveVideoPlayer(
    item: MediaItem,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var isPlaying by remember { mutableStateOf(false) }
    var isFirstFrameRendered by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(item.durationMs) }
    var showPlayerControls by remember { mutableStateOf(true) }
    var isScrubbing by remember { mutableStateOf(false) }

    val exoPlayer = remember(item.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(Media3Item.fromUri(item.uri))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
        }
    }

    // Attach listener for playback state changes
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onRenderedFirstFrame() {
                isFirstFrameRendered = true
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val d = exoPlayer.duration
                    if (d > 0) durationMs = d
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Continuous progress tracking while playing
    LaunchedEffect(isPlaying, isScrubbing) {
        while (isActive && isPlaying && !isScrubbing) {
            currentPositionMs = exoPlayer.currentPosition
            delay(150)
        }
    }

    // Auto-hide controls after 2 seconds of active playback
    LaunchedEffect(showPlayerControls, isPlaying, isScrubbing) {
        if (showPlayerControls && isPlaying && !isScrubbing) {
            delay(2000)
            showPlayerControls = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showPlayerControls = !showPlayerControls
                        onTap()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Media3 Player Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
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
            modifier = Modifier.fillMaxSize()
        )

        // Instant preview thumbnail before first frame is rendered
        if (!isFirstFrameRendered) {
            AsyncImage(
                model = item.thumbnail ?: item.uri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Overlay Controls (Center Play/Pause + Bottom Scrubber)
        AnimatedVisibility(
            visible = showPlayerControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            ) {
                // Center Play/Pause Button
                IconButton(
                    onClick = {
                        if (isPlaying) {
                            exoPlayer.pause()
                            showPlayerControls = true
                        } else {
                            exoPlayer.play()
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
                        contentDescription = if (isPlaying) "Pause" else "Abspielen",
                        tint = PureWhite,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Bottom Scrubber Bar & Time Info (Positioned above Detail Bar)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
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
                            exoPlayer.seekTo(currentPositionMs)
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
