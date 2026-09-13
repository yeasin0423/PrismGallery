package com.example.amoledgallery.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
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
import com.example.amoledgallery.ui.design.GlassRim
import com.example.amoledgallery.ui.design.glass
import com.example.amoledgallery.ui.design.glassRim
import com.example.amoledgallery.ui.design.pressScale
import com.example.amoledgallery.ui.theme.DesignLanguage
import com.example.amoledgallery.ui.theme.LocalAppTheme
import com.example.amoledgallery.ui.theme.destructive
import com.example.amoledgallery.ui.theme.isLight
import com.example.amoledgallery.ui.theme.onAccent
import com.example.amoledgallery.ui.viewmodel.GalleryTab
import kotlin.math.abs

private const val HOLD_TO_SLIDE_TIMEOUT_MS = 350L

private data class NavTab(
    val tab: GalleryTab,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector
)

private val NAV_TABS = listOf(
    NavTab(GalleryTab.BILDER, "Bilder", Icons.Rounded.Image, Icons.Outlined.Image),
    NavTab(GalleryTab.ALBEN, "Alben", Icons.Rounded.Collections, Icons.Outlined.Collections),
    NavTab(GalleryTab.MENU, "Menü", Icons.Rounded.Menu, Icons.Rounded.Menu)
)

/**
 * Bottom navigation whose whole *structure* changes per [DesignLanguage]:
 * - One UI (Samsung Gallery): a floating frosted pill spanning the width, icon above label, with a
 *   lighter capsule sliding behind the active tab.
 * - Material 3 Expressive: an attached tonal bar; the active indicator springs open around the icon.
 * - iOS 26 Photos: a Liquid Glass capsule for the content tabs with a selection bubble that stretches as
 *   it glides, plus a detached glass circle. The glass swells slightly while touched.
 * - HyperOS: a small frosted icon-only pill centred above the gesture bar.
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
                var lastX = down.position.x
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id }
                    if (change == null || !change.pressed) {
                        if (slideActive) latestOnTabSelected(NAV_TABS[indexForRootX(gestureRootX + lastX)].tab)
                        slideRootX = null
                        pressed = false
                        break
                    }
                    lastX = change.position.x
                    if (!slideActive && System.currentTimeMillis() - downTime >= HOLD_TO_SLIDE_TIMEOUT_MS) {
                        slideActive = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    if (slideActive) {
                        slideRootX = gestureRootX + lastX
                        change.consume()
                    }
                }
            }
        }

    fun reportCenter(index: Int): Modifier = Modifier.onGloballyPositioned {
        val bounds = it.boundsInRoot()
        tabCenters[index] = bounds.left + bounds.width / 2f
    }

    val palette = theme.palette

    when (theme.language) {
        DesignLanguage.SAMSUNG_ONEUI -> Box(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 12.dp)
        ) {
            val barScale by animateFloatAsState(if (pressed) 0.985f else 1f, label = "oneui_bar_press")
            val pill = RoundedCornerShape(percent = 50)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .graphicsLayer {
                        scaleX = barScale
                        scaleY = barScale
                    }
                    .glass(pill)
                    .then(holdSlideModifier)
            ) {
                val inset = 6.dp
                val itemWidth = (maxWidth - inset * 2) / NAV_TABS.size
                val indicatorX by animateDpAsState(
                    targetValue = itemWidth * highlighted,
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
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
                                    indication = null
                                ) { onTabSelected(tab.tab) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.icon,
                                contentDescription = tab.label,
                                tint = palette.onBackground,
                                modifier = Modifier.size(25.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = tab.label,
                                color = palette.onBackground,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Material 3 Expressive floating navigation bar: a frosted tonal pill that only holds icons,
        // except the active tab, which springs open into an accent capsule with its label.
        DesignLanguage.GOOGLE_EXPRESSIVE -> Box(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            val pill = RoundedCornerShape(percent = 50)
            val barScale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "expressive_bar_press")
            Row(
                modifier = Modifier
                    .height(68.dp)
                    .graphicsLayer {
                        scaleX = barScale
                        scaleY = barScale
                    }
                    .glass(pill)
                    .then(holdSlideModifier)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NAV_TABS.forEachIndexed { index, tab ->
                    val selected = index == highlighted
                    val container by animateColorAsState(
                        targetValue = if (selected) palette.accent else palette.accent.copy(alpha = 0f),
                        animationSpec = spring(stiffness = 700f),
                        label = "expressive_tab_container"
                    )
                    val content by animateColorAsState(
                        targetValue = if (selected) palette.onAccent else palette.onBackground.copy(alpha = 0.8f),
                        label = "expressive_tab_content"
                    )
                    Row(
                        modifier = Modifier
                            .then(reportCenter(index))
                            .height(52.dp)
                            .clip(pill)
                            .background(container)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(tab.tab) }
                            .animateContentSize(spring(dampingRatio = 0.62f, stiffness = 500f))
                            .padding(horizontal = if (selected) 20.dp else 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.icon,
                            contentDescription = tab.label,
                            tint = content,
                            modifier = Modifier.size(24.dp)
                        )
                        if (selected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tab.label,
                                color = content,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        DesignLanguage.IOS_LIQUID_GLASS -> Row(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val swell by animateFloatAsState(
                targetValue = if (pressed) 1.045f else 1f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = 480f),
                label = "ios_swell"
            )
            val pill = RoundedCornerShape(percent = 50)
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .graphicsLayer {
                        scaleX = swell
                        scaleY = swell
                    }
                    .glass(pill)
                    .then(holdSlideModifier)
            ) {
                val inset = 4.dp
                val contentTabs = 2
                val itemWidth = (maxWidth - inset * 2) / contentTabs
                val itemWidthPx = with(LocalDensity.current) { itemWidth.toPx() }
                val bubbleIndex = highlighted.coerceAtMost(contentTabs - 1)
                val bubbleX = remember { Animatable(bubbleIndex * itemWidthPx) }
                LaunchedEffect(bubbleIndex, itemWidthPx) {
                    bubbleX.animateTo(bubbleIndex * itemWidthPx, spring(dampingRatio = 0.62f, stiffness = 300f))
                }
                val bubbleAlpha by animateFloatAsState(
                    targetValue = if (highlighted < contentTabs) 1f else 0f,
                    label = "ios_bubble_alpha"
                )
                Box(
                    modifier = Modifier
                        .padding(inset)
                        .width(itemWidth)
                        .fillMaxHeight()
                        .graphicsLayer {
                            translationX = bubbleX.value
                            // Liquid stretch: the bubble elongates with its speed and settles back round.
                            val stretch = (abs(bubbleX.velocity) / 3800f).coerceAtMost(0.32f)
                            scaleX = 1f + stretch
                            scaleY = 1f - stretch * 0.3f
                            alpha = bubbleAlpha
                        }
                        .clip(pill)
                        .background(if (palette.isLight) Color.Black.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.16f))
                        .glassRim(pill, GlassRim.Liquid, palette.isLight)
                )
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inset)
                ) {
                    NAV_TABS.take(contentTabs).forEachIndexed { index, tab ->
                        val selected = index == highlighted
                        val tint by animateColorAsState(
                            targetValue = if (selected) palette.accent else palette.onBackground,
                            label = "ios_tab_tint"
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .then(reportCenter(index))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onTabSelected(tab.tab) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (index == 0) Icons.Rounded.PhotoLibrary else tab.selectedIcon,
                                contentDescription = tab.label,
                                tint = tint,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tab.label,
                                color = tint,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            val menuTab = NAV_TABS[2]
            val menuSelected = highlighted == 2
            val menuInteraction = remember { MutableInteractionSource() }
            val menuTint by animateColorAsState(
                targetValue = if (menuSelected) palette.accent else palette.onBackground,
                label = "ios_menu_tint"
            )
            Box(
                modifier = Modifier
                    .then(reportCenter(2))
                    .pressScale(menuInteraction, 1.08f)
                    .size(64.dp)
                    .glass(CircleShape)
                    .clickable(interactionSource = menuInteraction, indication = null) { onTabSelected(menuTab.tab) },
                contentAlignment = Alignment.Center
            ) {
                val innerAlpha by animateFloatAsState(if (menuSelected) 1f else 0f, label = "ios_menu_bubble")
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .graphicsLayer { alpha = innerAlpha }
                        .clip(CircleShape)
                        .background(if (palette.isLight) Color.Black.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.16f))
                )
                Icon(menuTab.selectedIcon, menuTab.label, tint = menuTint, modifier = Modifier.size(24.dp))
            }
        }

        DesignLanguage.XIAOMI_HYPEROS -> Box(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 10.dp, bottom = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            val pill = RoundedCornerShape(percent = 50)
            Row(
                modifier = Modifier
                    .glass(pill)
                    .then(holdSlideModifier)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NAV_TABS.forEachIndexed { index, tab ->
                    val selected = index == highlighted
                    val tint by animateColorAsState(
                        targetValue = if (selected) palette.onBackground else palette.textMuted.copy(alpha = 0.6f),
                        animationSpec = tween(120),
                        label = "hyperos_tint"
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.08f else 1f,
                        animationSpec = tween(140),
                        label = "hyperos_scale"
                    )
                    Box(
                        modifier = Modifier
                            .then(reportCenter(index))
                            .size(width = 62.dp, height = 44.dp)
                            .clip(pill)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(tab.tab) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.icon,
                            contentDescription = tab.label,
                            tint = tint,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Replaces the tab bar while items are selected, in each OS's own form: One UI's frosted action pill,
 * Material's floating toolbar with a separate FAB, the iOS glass toolbar with a centred count, and
 * HyperOS's full-width frosted action bar.
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

    when (theme.language) {
        DesignLanguage.SAMSUNG_ONEUI -> Box(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .glass(RoundedCornerShape(percent = 50))
                    .padding(6.dp)
            ) {
                LabeledAction(Icons.Outlined.Share, "Teilen", enabled, palette.onBackground, Modifier.weight(1f), onShare)
                LabeledAction(Icons.AutoMirrored.Rounded.DriveFileMove, "Verschieben", enabled, palette.onBackground, Modifier.weight(1f), onMove)
                LabeledAction(Icons.Outlined.Delete, "Löschen", enabled, palette.onBackground, Modifier.weight(1f), onDelete)
            }
        }

        DesignLanguage.GOOGLE_EXPRESSIVE -> Row(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val pill = RoundedCornerShape(percent = 50)
            Row(
                modifier = Modifier
                    .glass(pill)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ToolbarIcon(Icons.Rounded.Share, "Teilen", enabled, onShare)
                ToolbarIcon(Icons.AutoMirrored.Rounded.DriveFileMove, "Verschieben", enabled, onMove)
            }
            val fabInteraction = remember { MutableInteractionSource() }
            val fabShape = RoundedCornerShape(18.dp)
            Box(
                modifier = Modifier
                    .pressScale(fabInteraction, 0.9f)
                    .shadow(8.dp, fabShape)
                    .size(60.dp)
                    .clip(fabShape)
                    .background(if (enabled) palette.accent else palette.accent.copy(alpha = 0.4f))
                    .clickable(interactionSource = fabInteraction, indication = ripple(), enabled = enabled, onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Delete, "Löschen", tint = palette.onAccent, modifier = Modifier.size(26.dp))
            }
        }

        DesignLanguage.IOS_LIQUID_GLASS -> Row(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassCircleAction(Icons.Rounded.Share, "Teilen", enabled, palette.onBackground, onShare)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .glass(RoundedCornerShape(percent = 50)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedCount == 1) "1 Objekt ausgewählt" else "$selectedCount Objekte ausgewählt",
                    color = palette.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            GlassCircleAction(Icons.AutoMirrored.Rounded.DriveFileMove, "Verschieben", enabled, palette.onBackground, onMove)
            GlassCircleAction(Icons.Rounded.Delete, "Löschen", enabled, palette.destructive, onDelete)
        }

        DesignLanguage.XIAOMI_HYPEROS -> Row(
            modifier = modifier
                .fillMaxWidth()
                .glass(RectangleShape)
                .navigationBarsPadding()
                .height(66.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LabeledAction(Icons.Outlined.Share, "Senden", enabled, palette.onBackground, Modifier.weight(1f), onShare)
            LabeledAction(Icons.AutoMirrored.Rounded.DriveFileMove, "Verschieben", enabled, palette.onBackground, Modifier.weight(1f), onMove)
            LabeledAction(Icons.Outlined.Delete, "Löschen", enabled, palette.onBackground, Modifier.weight(1f), onDelete)
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

@Composable
private fun ToolbarIcon(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val palette = LocalAppTheme.current.palette
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            label,
            tint = if (enabled) palette.onBackground else palette.onBackground.copy(alpha = 0.35f),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun GlassCircleAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    tint: Color,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .pressScale(interaction, 1.08f)
            .size(56.dp)
            .glass(CircleShape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = if (enabled) tint else tint.copy(alpha = 0.35f), modifier = Modifier.size(23.dp))
    }
}
