package com.gallery.app.ui.components
import com.gallery.app.ui.i18n.strings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.gallery.app.model.MediaItem
import com.gallery.app.ui.theme.PureWhite

/**
 * The still a video shows inside the viewer's pager: the cached thumbnail, with the sharp first
 * frame decoded over it as soon as it is ready, plus a Play button that opens [VideoPlayerScreen].
 *
 * Nothing here creates an ExoPlayer or a MediaCodec. Scrolling onto a video therefore costs the
 * same as scrolling onto a photo, and pressing Play is what starts playback, on its own page.
 */
@Composable
fun AmoledVideoPlayer(
    item: MediaItem,
    isActivePage: Boolean,
    /** Play button follows the viewer's bars, so everything fades together. */
    controlsVisible: Boolean,
    onTap: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isActivePage) {
                if (isActivePage) detectTapGestures(onTap = { onTap() })
            },
        contentAlignment = Alignment.Center
    ) {
        // Instant, already-cached thumbnail; also the placeholder while the sharp frame decodes.
        AsyncImage(
            model = item.thumbnail ?: item.uri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // The video's own first frame at screen resolution, drawn over the soft thumbnail. Coil
        // caches it, so opening the player later starts from this same sharp frame with no blur.
        AsyncImage(
            model = item.uri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        if (isActivePage) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                // No scrim: a video page must look as bright as a photo.
                Box(modifier = Modifier.fillMaxSize()) {
                    IconButton(
                        onClick = onPlay,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = strings.play,
                            tint = PureWhite,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }
    }
}
