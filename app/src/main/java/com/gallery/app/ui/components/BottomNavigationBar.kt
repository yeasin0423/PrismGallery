package com.gallery.app.ui.components
import com.gallery.app.ui.i18n.strings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.app.ui.design.glass
import com.gallery.app.ui.design.selectionPop
import com.gallery.app.ui.design.SamsungGalleryIcons
import androidx.compose.animation.core.snap
import com.gallery.app.ui.theme.LocalAppTheme
import com.gallery.app.ui.theme.isLight
import com.gallery.app.ui.viewmodel.GalleryTab
import kotlin.math.abs

private const val HOLD_TO_SLIDE_TIMEOUT_MS = 250L

private data class NavTab(
    val tab: GalleryTab,
    val selectedIcon: ImageVector,
    val icon: ImageVector
) {
    val label: String
        get() = when (tab) {
            GalleryTab.BILDER -> strings.tabPictures
            GalleryTab.ALBEN -> strings.tabAlbums
        }
}

/** One UI uses Samsung Gallery's own album glyph. */
private fun NavTab.oneUiIcon(selected: Boolean): ImageVector = when (tab) {
    GalleryTab.ALBEN -> if (selected) SamsungGalleryIcons.AlbumsSelected else SamsungGalleryIcons.Albums
    else -> if (selected) selectedIcon else icon
}

private val NAV_TABS = listOf(
    NavTab(GalleryTab.BILDER, Icons.Rounded.Image, Icons.Outlined.Image),
    NavTab(GalleryTab.ALBEN, Icons.Rounded.Collections, Icons.Outlined.Collections)
)

/**
 * Bottom navigation: a floating One UI frosted pill with the content tabs.
 * - One UI (Samsung Gallery): a floating frosted pill spanning the width, icon above label, with a
 *   lighter capsule sliding behind the active tab.
 *
 * Holding a tab for [HOLD_TO_SLIDE_TIMEOUT_MS] and dragging slides the selection across tabs; the
 * highlight follows the finger and the tab under it is selected on release.
 */
@Composable
fun BottomNavigationBar(
    currentTab: GalleryTab,
    onTabSelected: (GalleryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val haptic = LocalHapticFeedback.current

    // Root-space horizontal centre of each tab, reported by the tabs themselves.
    val tabCenters = remember { mutableStateMapOf<Int, Float>() }
    var gestureRootX by remember { mutableFloatStateOf(0f) }
    var slideRootX by remember { mutableStateOf<Float?>(null) }
    var pressed by remember { mutableStateOf(false) }
    val latestTab by rememberUpdatedState(currentTab)
    val latestOnTabSelected by rememberUpdatedState(onTabSelected)

    fun indexForRootX(rootX: Float): Int =
        tabCenters.entries.minByOrNull { abs(it.value - rootX) }?.key
            ?: NAV_TABS.indexOfFirst { it.tab == latestTab }.coerceAtLeast(0)

    val highlighted = slideRootX?.let { indexForRootX(it) }
        ?: NAV_TABS.indexOfFirst { it.tab == currentTab }.coerceAtLeast(0)

    val holdSlideModifier = Modifier
        .onGloballyPositioned { gestureRootX = it.positionInRoot().x }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                pressed = true
                val downTime = System.currentTimeMillis()
                var slideActive = false
                var hoveredIndex = -1
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id }
                    if (change == null || !change.pressed) {
                        slideRootX = null
                        pressed = false
                        break
                    }
                    // Sliding starts after a short hold, or as soon as the finger clearly moves sideways.
                    if (!slideActive && (System.currentTimeMillis() - downTime >= HOLD_TO_SLIDE_TIMEOUT_MS ||
                            abs(change.position.x - down.position.x) > viewConfiguration.touchSlop)
                    ) {
                        slideActive = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    if (slideActive) {
                        val rootX = gestureRootX + change.position.x
                        slideRootX = rootX
                        change.consume()
                        // Live switching: the hovered tab opens immediately, without waiting for release.
                        val index = indexForRootX(rootX)
                        if (index != hoveredIndex) {
                            if (hoveredIndex != -1) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            hoveredIndex = index
                            if (NAV_TABS[index].tab != latestTab) latestOnTabSelected(NAV_TABS[index].tab)
                        }
                    }
                }
            }
        }

    fun reportCenter(index: Int): Modifier = Modifier.onGloballyPositioned {
        val bounds = it.boundsInRoot()
        tabCenters[index] = bounds.left + bounds.width / 2f
    }

    val palette = theme.palette

    Box(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 8.dp)
        ) {
            val barScale by animateFloatAsState(if (pressed) 0.985f else 1f, label = "oneui_bar_press")
            val pill = RoundedCornerShape(percent = 50)
            // Proportions measured from Samsung Gallery: a slim pill whose active capsule nearly fills it.
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .graphicsLayer {
                        scaleX = barScale
                        scaleY = barScale
                    }
                    .glass(pill)
                    .then(holdSlideModifier)
            ) {
                val inset = 4.dp
                val itemWidth = (maxWidth - inset * 2) / NAV_TABS.size
                // While sliding, the capsule sits exactly under the finger; afterwards it glides to the tab.
                val fingerOffset = slideRootX?.let { rootX ->
                    with(LocalDensity.current) { (rootX - gestureRootX).toDp() } - inset - itemWidth / 2
                }?.coerceIn(0.dp, itemWidth * (NAV_TABS.size - 1))
                val indicatorX by animateDpAsState(
                    targetValue = fingerOffset ?: (itemWidth * highlighted),
                    animationSpec = if (fingerOffset != null) snap() else tween(280, easing = FastOutSlowInEasing),
                    label = "oneui_indicator"
                )
                Box(
                    modifier = Modifier
                        .padding(inset)
                        .offset(x = indicatorX)
                        .width(itemWidth)
                        .fillMaxHeight()
                        .clip(pill)
                        .background(if (palette.isLight) Color.Black.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.15f))
                )
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inset)
                ) {
                    NAV_TABS.forEachIndexed { index, tab ->
                        val selected = index == highlighted
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .then(reportCenter(index))
                                .clip(pill)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = palette.onBackground)
                                ) { onTabSelected(tab.tab) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.oneUiIcon(selected),
                                contentDescription = tab.label,
                                tint = palette.onBackground,
                                modifier = Modifier
                                    .size(23.dp)
                                    .selectionPop(selected, fromScale = 0.78f, dampingRatio = 0.5f, stiffness = 650f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tab.label,
                                color = palette.onBackground,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
}

/**
 * Replaces the tab bar while items are selected: a floating One UI action card with the count and the
 * actions that apply to the selection.
 */
@Composable
fun SelectionActionBar(
    selectedCount: Int,
    onShare: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val palette = theme.palette
    val enabled = selectedCount > 0

    // One UI 8: a compact floating frosted capsule with labelled actions.
    Box(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 6.dp, bottom = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .height(66.dp)
                    .glass(RoundedCornerShape(percent = 50))
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                LabeledAction(Icons.Outlined.Share, strings.send, enabled, palette.onBackground, Modifier.width(92.dp), onShare)
                LabeledAction(Icons.AutoMirrored.Rounded.DriveFileMove, strings.move, enabled, palette.onBackground, Modifier.width(92.dp), onMove)
                LabeledAction(Icons.Outlined.Delete, strings.delete, enabled, palette.onBackground, Modifier.width(92.dp), onDelete)
            }
        }

}

/**
 * Shown at the bottom of the trash while items are selected: restore the selection or delete it
 * permanently, in the same floating frosted capsule as the album selection bar.
 */
@Composable
fun TrashSelectionActionBar(
    selectedCount: Int,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppTheme.current.palette
    val enabled = selectedCount > 0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(top = 6.dp, bottom = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .height(66.dp)
                .glass(RoundedCornerShape(percent = 50))
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            LabeledAction(Icons.Rounded.Restore, strings.restore, enabled, palette.onBackground, Modifier.width(92.dp), onRestore)
            LabeledAction(Icons.Outlined.DeleteForever, strings.delete, enabled, palette.onBackground, Modifier.width(92.dp), onDelete)
        }
    }
}

@Composable
private fun LabeledAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    tint: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val color = if (enabled) tint else tint.copy(alpha = 0.35f)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(percent = 50))
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, label, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(3.dp))
        Text(label, color = color, fontSize = 12.sp, maxLines = 1)
    }
}
