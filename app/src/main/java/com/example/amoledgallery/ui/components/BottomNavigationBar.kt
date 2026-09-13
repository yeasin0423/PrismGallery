package com.example.amoledgallery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amoledgallery.AmoledGalleryApp
import com.example.amoledgallery.ui.theme.AmoledBlack
import com.example.amoledgallery.ui.theme.AppTheme
import com.example.amoledgallery.ui.theme.DesignLanguage
import com.example.amoledgallery.ui.theme.LocalAppTheme
import com.example.amoledgallery.ui.theme.NavStyle
import com.example.amoledgallery.ui.theme.OneUiBlue
import com.example.amoledgallery.ui.theme.PureWhite
import com.example.amoledgallery.ui.theme.TextMuted
import com.example.amoledgallery.ui.theme.designTween
import com.example.amoledgallery.ui.viewmodel.GalleryTab
import kotlin.math.abs
import kotlin.math.roundToInt

private val TAB_ORDER = listOf(GalleryTab.BILDER, GalleryTab.ALBEN, GalleryTab.MENU)
private const val HOLD_TO_SLIDE_TIMEOUT_MS = 350L

/**
 * Bottom navigation whose whole *structure* changes per [DesignLanguage], because the shape of the
 * nav bar is what makes each OS recognisable at a glance:
 * - SAMSUNG_ONEUI: edge-to-edge bar, neutral capsule with an animated label behind the active tab.
 * - GOOGLE_EXPRESSIVE: edge-to-edge bar, tonal accent capsule plus a springy icon bounce.
 * - IOS_LIQUID_GLASS: a narrow floating glass pill for the content tabs and a separate detached
 *   round glass button - the Library/Collections + search layout from iOS Photos.
 * - XIAOMI_HYPEROS: a small compact icon-only floating pill centred above the gesture bar.
 *
 * Holding a tab for [HOLD_TO_SLIDE_TIMEOUT_MS] and dragging switches tabs by sliding; the target is
 * resolved against the tabs' measured on-screen centres, so it stays correct even for the split
 * iOS layout where the third tab sits outside the pill.
 */
@Composable
fun BottomNavigationBar(
    currentTab: GalleryTab,
    onTabSelected: (GalleryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    // Root-space horizontal centre of each tab, reported by the tabs themselves.
    val tabCenters = remember { mutableStateMapOf<Int, Float>() }
    var gestureRootX by remember { mutableFloatStateOf(0f) }
    var slideIndicatorX by remember { mutableStateOf<Float?>(null) }

    fun indexForLocalX(localX: Float): Int {
        if (tabCenters.isEmpty()) return TAB_ORDER.indexOf(currentTab).coerceAtLeast(0)
        val absoluteX = gestureRootX + localX
        return tabCenters.entries.minByOrNull { abs(it.value - absoluteX) }?.key ?: 0
    }

    val holdSlideModifier = Modifier
        .onGloballyPositioned { gestureRootX = it.positionInRoot().x }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val downTime = System.currentTimeMillis()
                var slideActive = false
                var lastX = down.position.x
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id }
                    if (change == null || !change.pressed) {
                        if (slideActive) onTabSelected(TAB_ORDER[indexForLocalX(lastX)])
                        slideIndicatorX = null
                        break
                    }
                    lastX = change.position.x
                    if (!slideActive && System.currentTimeMillis() - downTime >= HOLD_TO_SLIDE_TIMEOUT_MS) {
                        slideActive = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    if (slideActive) {
                        slideIndicatorX = lastX
                        change.consume()
                    }
                }
            }
        }

    val slideCircle = @Composable { scope: androidx.compose.foundation.layout.BoxScope ->
        slideIndicatorX?.let { x ->
            val circleSizeDp = 52.dp
            val circleSizePx = with(density) { circleSizeDp.toPx() }
            with(scope) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset { IntOffset(x = (x - circleSizePx / 2f).roundToInt(), y = 0) }
                        .size(circleSizeDp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.38f))
                )
            }
        }
    }

    fun reportCenter(index: Int): Modifier = Modifier.onGloballyPositioned {
        val bounds = it.boundsInRoot()
        tabCenters[index] = bounds.left + bounds.width / 2f
    }

    when (theme.shapes.navStyle) {
        NavStyle.GLASS_SPLIT_PILL -> Box(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .then(holdSlideModifier)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color(0xFF2C2C2E).copy(alpha = 0.72f))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(percent = 50))
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassTabItem("Bilder", Icons.Rounded.Image, currentTab == GalleryTab.BILDER, theme,
                        reportCenter(0)) { onTabSelected(GalleryTab.BILDER) }
                    Spacer(modifier = Modifier.width(4.dp))
                    GlassTabItem("Alben", Icons.Rounded.Collections, currentTab == GalleryTab.ALBEN, theme,
                        reportCenter(1)) { onTabSelected(GalleryTab.ALBEN) }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .then(reportCenter(2))
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            if (currentTab == GalleryTab.MENU) Color.White.copy(alpha = 0.22f)
                            else Color(0xFF2C2C2E).copy(alpha = 0.72f)
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(GalleryTab.MENU) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Menu, "Menü", tint = PureWhite, modifier = Modifier.size(24.dp))
                }
            }
            slideCircle(this)
        }

        NavStyle.HYPEROS_COMPACT_PILL -> Box(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.then(holdSlideModifier)) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF1A1A1A))
                        .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HyperOsTabItem(Icons.Rounded.Image, "Bilder", currentTab == GalleryTab.BILDER, theme,
                        reportCenter(0)) { onTabSelected(GalleryTab.BILDER) }
                    HyperOsTabItem(Icons.Rounded.Collections, "Alben", currentTab == GalleryTab.ALBEN, theme,
                        reportCenter(1)) { onTabSelected(GalleryTab.ALBEN) }
                    HyperOsTabItem(Icons.Rounded.Menu, "Menü", currentTab == GalleryTab.MENU, theme,
                        reportCenter(2)) { onTabSelected(GalleryTab.MENU) }
                }
                slideCircle(this)
            }
        }

        else -> Column(
            modifier = modifier
                .fillMaxWidth()
                .background(AmoledBlack)
                .navigationBarsPadding()
        ) {
            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF161616))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 24.dp)
                    .then(holdSlideModifier)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CapsuleNavItem("Bilder", Icons.Rounded.Image, currentTab == GalleryTab.BILDER, theme,
                        reportCenter(0)) { onTabSelected(GalleryTab.BILDER) }
                    CapsuleNavItem("Alben", Icons.Rounded.Collections, currentTab == GalleryTab.ALBEN, theme,
                        reportCenter(1)) { onTabSelected(GalleryTab.ALBEN) }
                    CapsuleNavItem("Menü", Icons.Rounded.Menu, currentTab == GalleryTab.MENU, theme,
                        reportCenter(2)) { onTabSelected(GalleryTab.MENU) }
                }
                slideCircle(this)
            }
        }
    }
}

/** One UI / Expressive: capsule behind the active tab with an animated label. */
@Composable
private fun CapsuleNavItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    theme: AppTheme,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isExpressive = theme.language == DesignLanguage.GOOGLE_EXPRESSIVE

    val pillBgColor by animateColorAsState(
        targetValue = when {
            !isSelected -> Color.Transparent
            isExpressive -> theme.palette.accent.copy(alpha = 0.28f)
            else -> Color(0xFF1E1E1E)
        },
        animationSpec = designTween(theme.language, AmoledGalleryApp.performanceProfile, 240),
        label = "pill_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            !isSelected -> TextMuted
            isExpressive -> theme.palette.accent
            else -> PureWhite
        },
        animationSpec = designTween(theme.language, AmoledGalleryApp.performanceProfile, 200),
        label = "pill_content"
    )
    val iconScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isSelected && isExpressive) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "icon_scale"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(theme.shapes.pillCornerRadius))
            .background(pillBgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 48.dp, color = OneUiBlue.copy(alpha = 0.25f)),
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(22.dp).scale(iconScale)
            )
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(animationSpec = tween(220, delayMillis = 40)) +
                    expandHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut(animationSpec = tween(150)) +
                    shrinkHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        color = contentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** iOS: rounded tab inside the glass pill, white-frosted when active. */
@Composable
private fun GlassTabItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    theme: AppTheme,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (isSelected) Color.White.copy(alpha = 0.22f) else Color.Transparent,
        animationSpec = designTween(theme.language, AmoledGalleryApp.performanceProfile, 220),
        label = "glass_tab_bg"
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isSelected) PureWhite else PureWhite.copy(alpha = 0.72f),
            modifier = Modifier.size(21.dp)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = title,
            color = if (isSelected) PureWhite else PureWhite.copy(alpha = 0.72f),
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

/** HyperOS: compact icon-only button, accent tint when active, no capsule. */
@Composable
private fun HyperOsTabItem(
    icon: ImageVector,
    title: String,
    isSelected: Boolean,
    theme: AppTheme,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tint by animateColorAsState(
        targetValue = if (isSelected) theme.palette.accent else TextMuted,
        animationSpec = designTween(theme.language, AmoledGalleryApp.performanceProfile, 120),
        label = "hyperos_tab_tint"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 22.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = tint, modifier = Modifier.size(22.dp))
    }
}
