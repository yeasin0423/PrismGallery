package com.example.amoledgallery.ui.screens
import com.example.amoledgallery.ui.i18n.LanguageState
import com.example.amoledgallery.ui.i18n.strings

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.amoledgallery.model.MediaItem
import com.example.amoledgallery.ui.components.AmoledVideoPlayer
import com.example.amoledgallery.ui.components.MediaInfoBottomSheet
import com.example.amoledgallery.ui.components.ZoomableBox
import com.example.amoledgallery.ui.design.DialogButton
import com.example.amoledgallery.ui.design.DialogButtonStyle
import com.example.amoledgallery.ui.design.ThemedDialog
import com.example.amoledgallery.ui.design.ThemedTextField
import com.example.amoledgallery.ui.design.barGlass
import com.example.amoledgallery.ui.design.glass
import com.example.amoledgallery.ui.design.pressScale
import com.example.amoledgallery.ui.theme.DesignLanguage
import com.example.amoledgallery.ui.theme.LocalAppTheme
import com.example.amoledgallery.ui.theme.destructive
import com.example.amoledgallery.ui.theme.isLight
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.launch
import dev.chrisbanes.haze.HazeState
import com.example.amoledgallery.ui.design.HazeSource
import com.example.amoledgallery.ui.design.OverlayWindow
import com.example.amoledgallery.ui.editor.MediaEditorScreen
import androidx.compose.animation.core.animateFloat
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fullscreen media viewer: pager, pinch/double-tap zoom, swipe down to close, swipe up for details.
 * The chrome follows the design language:
 * - One UI: date/time title and icon-only action row on soft black gradients.
 * - Material 3 Expressive: tonal floating circles on top, a floating toolbar pill at the bottom.
 * - iOS: glass circle buttons, a glass date capsule and a glass toolbar blurring the photo.
 * - HyperOS: white chrome with labelled actions while controls are shown; tapping hides the chrome
 *   and turns the background black.
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
    onPageChanged: (Int) -> Unit = {},
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
    val context = LocalContext.current

    androidx.compose.runtime.LaunchedEffect(pagerState.currentPage) {
        isZoomed = false
        dragOffsetY.snapTo(0f)
        // Report the page back so a delete acts on the item actually on screen and the index is
        // left pointing at the following one.
        onPageChanged(pagerState.currentPage)
    }

    var showControls by remember { mutableStateOf(true) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<MediaItem?>(null) }

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

    val theme = LocalAppTheme.current
    val palette = theme.palette
    val language = theme.language
    val detailHaze = remember { HazeState() }
    val scope = rememberCoroutineScope()

    // Pull-down-to-close: the photo follows the finger and shrinks, the background fades; released past
    // the threshold it closes (zooming back into the grid), otherwise it springs back.
    val dismissShrink = when (language) {
        DesignLanguage.IOS_LIQUID_GLASS -> 0.45f
        DesignLanguage.GOOGLE_EXPRESSIVE -> 0.3f
        DesignLanguage.SAMSUNG_ONEUI -> 0.25f
        DesignLanguage.XIAOMI_HYPEROS -> 0.2f
    }
    val favoriteBurst = remember { androidx.compose.animation.core.Animatable(1f) }

    // HyperOS shows photos on white while the controls are visible, black in immersive mode.
    val lightChrome = language == DesignLanguage.XIAOMI_HYPEROS && palette.isLight
    val backgroundColor by animateColorAsState(
        targetValue = if (lightChrome && showControls) palette.background else Color.Black,
        animationSpec = tween(220),
        label = "viewer_background"
    )

    val dateMillis = if (currentItem.dateModified > 0) currentItem.dateModified * 1000L else currentItem.dateAdded * 1000L
    val dateTitle = remember(dateMillis, LanguageState.language) { SimpleDateFormat(strings.longDatePattern, LanguageState.locale).format(Date(dateMillis)) }
    val timeText = remember(dateMillis, LanguageState.language) { strings.timeLabel(SimpleDateFormat("HH:mm", LanguageState.locale).format(Date(dateMillis))) }

    val actions = DetailActions(
        isFavorite = isFav,
        favoriteScale = { favScale },
        favoriteBurst = { favoriteBurst.value },
        onBack = onBack,
        onToggleFavorite = {
            bounceTrigger = true
            if (!isFav) {
                scope.launch {
                    favoriteBurst.snapTo(0f)
                    favoriteBurst.animateTo(1f, tween(520))
                }
            }
            onToggleFavorite(currentItem.id)
        },
        onShare = {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (currentItem.isVideo) "video/*" else "image/*"
                putExtra(Intent.EXTRA_STREAM, currentItem.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, strings.shareItem))
        },
        onRename = { showRenameDialog = true },
        onEdit = { editingItem = currentItem },
        onInfo = { showInfoSheet = true },
        onDelete = { onTrashItem(currentItem) }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val fade = (dragOffsetY.value / size.height * 2.2f).coerceIn(0f, 0.9f)
                drawRect(backgroundColor.copy(alpha = backgroundColor.alpha * (1f - fade)))
            }
    ) {
        HazeSource(
            detailHaze,
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val offset = dragOffsetY.value
                    val fraction = (offset / size.height).coerceIn(0f, 1f)
                    translationY = offset
                    val s = 1f - fraction * dismissShrink
                    scaleX = s
                    scaleY = s
                    if (language == DesignLanguage.GOOGLE_EXPRESSIVE && fraction > 0f) {
                        shape = RoundedCornerShape((fraction * 120f).dp)
                        clip = true
                    }
                }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isZoomed) {
                        if (isZoomed) return@pointerInput
                        var totalDragY = 0f
                        var infoTriggered = false
                        detectVerticalDragGestures(
                            onDragStart = {
                                totalDragY = 0f
                                infoTriggered = false
                            },
                            onVerticalDrag = { change, dragAmount ->
                                totalDragY += dragAmount
                                if (totalDragY > 0f) {
                                    change.consume()
                                    scope.launch { dragOffsetY.snapTo(totalDragY) }
                                } else if (!infoTriggered && totalDragY < -80f) {
                                    change.consume()
                                    infoTriggered = true
                                    showInfoSheet = true
                                }
                            },
                            onDragEnd = {
                                if (dragOffsetY.value > 170f) {
                                    onBack()
                                } else {
                                    scope.launch { dragOffsetY.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 500f)) }
                                }
                            },
                            onDragCancel = {
                                scope.launch { dragOffsetY.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 500f)) }
                            }
                        )
                    },
                key = { index -> mediaList[index].id }
            ) { page ->
                val currentMedia = mediaList[page]

                if (currentMedia.isVideo) {
                    AmoledVideoPlayer(
                        item = currentMedia,
                        isActivePage = pagerState.currentPage == page,
                        onTap = { showControls = !showControls },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
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
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(160)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer { alpha = 1f - (dragOffsetY.value / 220f).coerceIn(0f, 1f) }
        ) {
            DetailTopBar(language, dateTitle, timeText, lightChrome, detailHaze, actions)
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(160)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer { alpha = 1f - (dragOffsetY.value / 220f).coerceIn(0f, 1f) }
        ) {
            DetailBottomBar(language, lightChrome, detailHaze, actions)
        }
    }

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

    // File Metadata Info Sheet (slides up on swipe-up or Details tap)
    if (showInfoSheet) {
        MediaInfoBottomSheet(
            item = currentItem,
            onDismiss = { showInfoSheet = false }
        )
    }

    // Crop / rotate / mirror editor, shown above everything (bars included) in the overlay host.
    editingItem?.let { item ->
        OverlayWindow { transition ->
            val progress by transition.animateFloat(
                transitionSpec = { tween(220) },
                label = "editor_enter"
            ) { visible -> if (visible) 1f else 0f }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = progress
                        val s = 0.94f + 0.06f * progress
                        scaleX = s
                        scaleY = s
                    }
            ) {
                MediaEditorScreen(item = item, onClose = { editingItem = null })
            }
        }
    }
}

private class DetailActions(
    val isFavorite: Boolean,
    val favoriteScale: () -> Float,
    /** 0..1 progress of the ring that bursts around the heart when an item is favorited (1 = idle). */
    val favoriteBurst: () -> Float,
    val onBack: () -> Unit,
    val onToggleFavorite: () -> Unit,
    val onShare: () -> Unit,
    val onRename: () -> Unit,
    val onEdit: () -> Unit,
    val onInfo: () -> Unit,
    val onDelete: () -> Unit
)

@Composable
private fun DetailTopBar(
    language: DesignLanguage,
    dateTitle: String,
    timeText: String,
    lightChrome: Boolean,
    hazeState: HazeState,
    actions: DetailActions
) {
    val palette = LocalAppTheme.current.palette
    when (language) {
        DesignLanguage.SAMSUNG_ONEUI -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)))
                .statusBarsPadding()
                .height(64.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChromeIcon(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, Color.White, actions.onBack)
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text(dateTitle, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(timeText, color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
            }
            ChromeIcon(Icons.Outlined.DriveFileRenameOutline, strings.rename, Color.White, actions.onRename)
        }

        DesignLanguage.GOOGLE_EXPRESSIVE -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TonalCircle(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, actions.onBack)
            // Balances the two circles on the right so the date pill stays centred.
            Spacer(modifier = Modifier.width(56.dp))
            Spacer(modifier = Modifier.weight(1f))
            Column(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(lerp(palette.surfaceVariant, palette.accent, 0.08f).copy(alpha = 0.9f))
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(dateTitle, color = palette.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.weight(1f))
            TonalCircle(Icons.Outlined.DriveFileRenameOutline, strings.rename, actions.onRename)
            Spacer(modifier = Modifier.width(8.dp))
            TonalCircle(Icons.Outlined.Info, strings.details, actions.onInfo)
        }

        DesignLanguage.IOS_LIQUID_GLASS -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            GlassCircle(Icons.AutoMirrored.Rounded.ArrowBackIos, strings.back, palette.onBackground, hazeState, 48.dp, Modifier.align(Alignment.CenterStart), actions.onBack)
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .glass(RoundedCornerShape(percent = 50), barGlass(), hazeState)
                    .padding(horizontal = 20.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(dateTitle, color = palette.onBackground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(timeText, color = palette.onBackground.copy(alpha = 0.7f), fontSize = 12.sp)
            }
            GlassCircle(Icons.Outlined.DriveFileRenameOutline, strings.rename, palette.onBackground, hazeState, 48.dp, Modifier.align(Alignment.CenterEnd), actions.onRename)
        }

        DesignLanguage.XIAOMI_HYPEROS -> {
            val chrome = if (lightChrome) palette.onBackground else Color.White
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (lightChrome) Modifier.background(palette.background)
                        else Modifier.background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)))
                    )
                    .statusBarsPadding()
                    .height(60.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChromeIcon(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, chrome, actions.onBack)
                Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(dateTitle, color = chrome, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text(timeText, color = chrome.copy(alpha = 0.6f), fontSize = 12.sp)
                }
                ChromeIcon(Icons.Outlined.DriveFileRenameOutline, strings.rename, chrome, actions.onRename)
            }
        }
    }
}

@Composable
private fun DetailBottomBar(
    language: DesignLanguage,
    lightChrome: Boolean,
    hazeState: HazeState,
    actions: DetailActions
) {
    val palette = LocalAppTheme.current.palette
    val favoriteIcon = if (actions.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder
    val burstColor = if (language == DesignLanguage.GOOGLE_EXPRESSIVE) palette.accent else Color(0xFFFF4D6D)
    val favoriteModifier = Modifier
        .drawBehind {
            val b = actions.favoriteBurst()
            if (b < 1f) {
                drawCircle(
                    color = burstColor.copy(alpha = (1f - b) * 0.85f),
                    radius = size.minDimension * (0.45f + b * 0.5f),
                    style = Stroke(width = 3.dp.toPx() * (1f - b))
                )
            }
        }
        .graphicsLayer {
            val s = actions.favoriteScale()
            scaleX = s
            scaleY = s
        }

    when (language) {
        DesignLanguage.SAMSUNG_ONEUI -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))))
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ChromeIcon(favoriteIcon, strings.favorite, if (actions.isFavorite) Color(0xFFFF4D6D) else Color.White, actions.onToggleFavorite, favoriteModifier)
            ChromeIcon(Icons.Outlined.Edit, strings.edit, Color.White, actions.onEdit)
            ChromeIcon(Icons.Outlined.Share, strings.share, Color.White, actions.onShare)
            ChromeIcon(Icons.Outlined.Info, strings.details, Color.White, actions.onInfo)
            ChromeIcon(Icons.Outlined.Delete, strings.delete, Color.White, actions.onDelete)
        }

        DesignLanguage.GOOGLE_EXPRESSIVE -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            val pill = RoundedCornerShape(percent = 50)
            Row(
                modifier = Modifier
                    .glass(pill, barGlass(), hazeState)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ChromeIcon(Icons.Outlined.Share, strings.share, palette.onBackground, actions.onShare)
                ChromeIcon(Icons.Outlined.Edit, strings.edit, palette.onBackground, actions.onEdit)
                ChromeIcon(favoriteIcon, strings.favorite, if (actions.isFavorite) palette.accent else palette.onBackground, actions.onToggleFavorite, favoriteModifier)
                ChromeIcon(Icons.Outlined.Delete, strings.delete, palette.onBackground, actions.onDelete)
            }
        }

        DesignLanguage.IOS_LIQUID_GLASS -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassCircle(Icons.Outlined.Share, strings.share, palette.onBackground, hazeState, 56.dp, Modifier, actions.onShare)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .glass(RoundedCornerShape(percent = 50), barGlass(), hazeState),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChromeIcon(favoriteIcon, strings.favorite, if (actions.isFavorite) Color(0xFFFF375F) else palette.onBackground, actions.onToggleFavorite, favoriteModifier)
                ChromeIcon(Icons.Outlined.Info, strings.details, palette.onBackground, actions.onInfo)
                ChromeIcon(Icons.Outlined.Edit, strings.edit, palette.onBackground, actions.onEdit)
            }
            GlassCircle(Icons.Outlined.Delete, strings.delete, palette.destructive, hazeState, 56.dp, Modifier, actions.onDelete)
        }

        DesignLanguage.XIAOMI_HYPEROS -> {
            val chrome = if (lightChrome) palette.onBackground else Color.White
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (lightChrome) Modifier.background(palette.background)
                        else Modifier.background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                    )
                    .navigationBarsPadding()
                    .height(72.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LabeledChromeAction(Icons.Outlined.Share, strings.send, chrome, actions.onShare)
                LabeledChromeAction(favoriteIcon, strings.favorite, if (actions.isFavorite) Color(0xFFFF5C7A) else chrome, actions.onToggleFavorite, favoriteModifier)
                LabeledChromeAction(Icons.Outlined.Edit, strings.edit, chrome, actions.onEdit)
                LabeledChromeAction(Icons.Outlined.Info, strings.details, chrome, actions.onInfo)
                LabeledChromeAction(Icons.Outlined.Delete, strings.delete, chrome, actions.onDelete)
            }
        }
    }
}

@Composable
private fun ChromeIcon(
    icon: ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit,
    iconModifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 24.dp),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = tint, modifier = iconModifier.size(25.dp))
    }
}

@Composable
private fun TonalCircle(icon: ImageVector, description: String, onClick: () -> Unit) {
    val palette = LocalAppTheme.current.palette
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .pressScale(interaction, 0.9f)
            .size(48.dp)
            .clip(CircleShape)
            .background(lerp(palette.surfaceVariant, palette.accent, 0.08f).copy(alpha = 0.9f))
            .clickable(interactionSource = interaction, indication = ripple(), onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = palette.onBackground, modifier = Modifier.size(23.dp))
    }
}

@Composable
private fun GlassCircle(
    icon: ImageVector,
    description: String,
    tint: Color,
    hazeState: HazeState,
    size: Dp,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interaction, 1.08f)
            .size(size)
            .glass(CircleShape, barGlass(), hazeState)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = tint, modifier = Modifier.size(23.dp))
    }
}

@Composable
private fun LabeledChromeAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    iconModifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, label, tint = tint, modifier = iconModifier.size(24.dp))
        Spacer(modifier = Modifier.height(3.dp))
        Text(label, color = tint, fontSize = 11.sp)
    }
}

@Composable
private fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textValue by remember { mutableStateOf(currentName) }
    ThemedDialog(
        onDismissRequest = onDismiss,
        title = strings.renameFile,
        buttons = listOf(
            DialogButton(strings.cancel, DialogButtonStyle.Cancel, onClick = onDismiss),
            DialogButton(strings.rename, DialogButtonStyle.Primary, enabled = textValue.isNotBlank()) {
                if (textValue.isNotBlank()) onConfirm(textValue.trim())
            }
        )
    ) {
        ThemedTextField(
            value = textValue,
            onValueChange = { textValue = it },
            placeholder = strings.name,
            autoFocus = true
        )
    }
}
