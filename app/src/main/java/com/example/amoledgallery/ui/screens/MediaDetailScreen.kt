package com.example.amoledgallery.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.amoledgallery.model.MediaItem
import com.example.amoledgallery.ui.components.AmoledVideoPlayer
import com.example.amoledgallery.ui.components.CircularIconButton
import com.example.amoledgallery.ui.components.MediaInfoBottomSheet
import com.example.amoledgallery.ui.components.ZoomableBox
import com.example.amoledgallery.ui.theme.AmoledBlack
import com.example.amoledgallery.ui.theme.OneUiCardBorder
import com.example.amoledgallery.ui.theme.OverlayScrim
import com.example.amoledgallery.ui.theme.PureWhite
import com.example.amoledgallery.ui.theme.SurfaceVariantDark
import com.example.amoledgallery.ui.theme.TextMuted
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Fullscreen AMOLED media viewer supporting:
 * - HorizontalPager swipe between items
 * - Double tap and pinch-to-zoom for photos
 * - Swipe-down-to-dismiss gesture and System Back
 * - Swipe-up / scroll-down to open file metadata details info sheet
 * - Video playback action
 * - Rename file with OneUI dialog
 * - Trash / Delete action
 * - Share action
 */
@Composable
fun MediaDetailScreen(
    mediaList: List<MediaItem>,
    initialIndex: Int,
    onBack: () -> Unit,
    onTrashItem: (MediaItem) -> Unit,
    onRenameItem: (MediaItem, String) -> Unit,
    isFavorite: (Long) -> Boolean = { false },
    onToggleFavorite: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (mediaList.isEmpty()) {
        onBack()
        return
    }

    // Intercept system back button
    BackHandler(onBack = onBack)

    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { mediaList.size })

    var isZoomed by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(initialIndex) {
        val targetPage = initialIndex.coerceIn(0, (mediaList.size - 1).coerceAtLeast(0))
        if (pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
    }
    val dragOffsetY = remember { androidx.compose.animation.core.Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val dismissThreshold = 220f
    
    androidx.compose.runtime.LaunchedEffect(pagerState.currentPage) {
        isZoomed = false
        dragOffsetY.snapTo(0f)
    }

    var showControls by remember { mutableStateOf(true) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }

    val currentItem = mediaList.getOrNull(pagerState.currentPage) ?: mediaList[0]
    val isFav = isFavorite(currentItem.id)
    var bounceTrigger by remember { mutableStateOf(false) }
    val favScale by animateFloatAsState(
        targetValue = if (bounceTrigger) 1.3f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        finishedListener = {
            if (bounceTrigger) bounceTrigger = false
        },
        label = "fav_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        // Horizontal Pager for swiping between media items
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isZoomed) {
                    if (isZoomed) return@pointerInput
                    var totalDragY = 0f
                    var actionTriggered = false
                    detectVerticalDragGestures(
                        onDragStart = { 
                            totalDragY = 0f 
                            actionTriggered = false
                        },
                        onVerticalDrag = { change, dragAmount ->
                            if (actionTriggered) return@detectVerticalDragGestures
                            totalDragY += dragAmount
                            if (totalDragY > 80f) {
                                change.consume()
                                actionTriggered = true
                                onBack()
                            } else if (totalDragY < -80f) {
                                change.consume()
                                actionTriggered = true
                                showInfoSheet = true
                            }
                        }
                    )
                },
            key = { index -> mediaList[index].id }
        ) { page ->
            val currentMedia = mediaList[page] // <-- DAS HAT GEFEHLT!

            if (currentMedia.isVideo) {
                // Native Media3 ExoPlayer Video Viewer
                AmoledVideoPlayer(
                    item = currentMedia,
                    isActivePage = pagerState.currentPage == page,
                    onTap = { showControls = !showControls },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Image Viewer with double tap & pinch to zoom
                ZoomableBox(
                    onSingleTap = { showControls = !showControls },
                    onZoomChange = { zoomed ->
                        if (pagerState.currentPage == page) {
                            isZoomed = zoomed
                        }
                    }
                ) {
                    AsyncImage(
                        model = currentMedia.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Top Bar Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OverlayScrim)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                CircularIconButton(
                    onClick = onBack,
                    contentDescription = "Zurück",
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    tint = Color.White,
                    backgroundColor = Color.Black.copy(alpha = 0.35f),
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                Text(
                    text = "${pagerState.currentPage + 1} / ${mediaList.size}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.Center)
                )

                CircularIconButton(
                    onClick = { showInfoSheet = true },
                    contentDescription = "Details",
                    icon = Icons.Rounded.Info,
                    tint = Color.White,
                    backgroundColor = Color.Black.copy(alpha = 0.35f),
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }

        // Bottom Bar Overlay with OneUI Actions: Favorite, Share, Edit, Details, Trash
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OverlayScrim)
                    .navigationBarsPadding()
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DetailActionButton(
                    icon = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    label = "Favorit",
                    iconModifier = Modifier.graphicsLayer {
                        scaleX = favScale
                        scaleY = favScale
                    },
                    onClick = {
                        bounceTrigger = true
                        onToggleFavorite(currentItem.id)
                    }
                )

                DetailActionButton(
                    icon = Icons.Rounded.Share,
                    label = "Teilen",
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = if (currentItem.isVideo) "video/*" else "image/*"
                            putExtra(Intent.EXTRA_STREAM, currentItem.uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Medium teilen"))
                    }
                )

                DetailActionButton(
                    icon = Icons.Rounded.Edit,
                    label = "Umbenennen",
                    onClick = { showRenameDialog = true }
                )

                DetailActionButton(
                    icon = Icons.Rounded.Info,
                    label = "Details",
                    onClick = { showInfoSheet = true }
                )

                DetailActionButton(
                    icon = Icons.Rounded.Delete,
                    label = "Löschen",
                    onClick = { onTrashItem(currentItem) }
                )
            }
        }

        // Rename Dialog
        if (showRenameDialog) {
            RenameDialog(
                currentName = currentItem.displayName,
                onDismiss = { showRenameDialog = false },
                onConfirm = { newName ->
                    showRenameDialog = false
                    onRenameItem(currentItem, newName)
                }
            )
        }

        // File Metadata Info Bottom Sheet (Slides up on swipe-up / scroll-down or Details tap)
        if (showInfoSheet) {
            MediaInfoBottomSheet(
                item = currentItem,
                onDismiss = { showInfoSheet = false }
            )
        }
    }
}

@Composable
private fun DetailActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = PureWhite,
            modifier = iconModifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = PureWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textValue by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceVariantDark,
        shape = RoundedCornerShape(com.example.amoledgallery.ui.theme.LocalAppTheme.current.shapes.dialogCornerRadius),
        title = {
            Text(
                text = "Datei umbenennen",
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                com.example.amoledgallery.ui.WindowBackgroundBlur()
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text("Name", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        focusedBorderColor = PureWhite,
                        unfocusedBorderColor = OneUiCardBorder,
                        cursorColor = PureWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (textValue.isNotBlank()) {
                        onConfirm(textValue.trim())
                    }
                },
                enabled = textValue.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PureWhite,
                    contentColor = AmoledBlack
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Umbenennen", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen", color = TextMuted)
            }
        }
    )
}
