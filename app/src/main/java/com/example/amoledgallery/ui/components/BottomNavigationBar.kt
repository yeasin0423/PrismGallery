package com.example.amoledgallery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FiniteAnimationSpec
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amoledgallery.AmoledGalleryApp
import com.example.amoledgallery.ui.theme.AmoledBlack
import com.example.amoledgallery.ui.theme.AppTheme
import com.example.amoledgallery.ui.theme.DesignLanguage
import com.example.amoledgallery.ui.theme.LocalAppTheme
import com.example.amoledgallery.ui.theme.OneUiBlue
import com.example.amoledgallery.ui.theme.PureWhite
import com.example.amoledgallery.ui.theme.TextMuted
import com.example.amoledgallery.ui.theme.designTween
import com.example.amoledgallery.ui.viewmodel.GalleryTab

/**
 * Bottom navigation bar whose structure - not just its colors - changes per [DesignLanguage]
 * so switching design in Settings genuinely feels like switching OS:
 * - SAMSUNG_ONEUI: edge-to-edge bar, dark neutral capsule behind the selected item.
 * - GOOGLE_EXPRESSIVE: edge-to-edge bar, tonal ACCENT-colored capsule + accent icon/text and a
 *   springy icon bounce on selection.
 * - IOS_LIQUID_GLASS: a floating inset pill bar (margin on all sides, fully rounded), no divider,
 *   translucent surface with a light rim highlight instead of a dark capsule.
 * - XIAOMI_HYPEROS: no capsule at all - just an accent color change on the icon/label plus a
 *   thin underline dash, fast linear transitions.
 */
@Composable
fun BottomNavigationBar(
    currentTab: GalleryTab,
    onTabSelected: (GalleryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val isLiquidGlass = theme.language == DesignLanguage.IOS_LIQUID_GLASS

    val items = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavItem(
                title = "Bilder",
                icon = Icons.Rounded.Image,
                isSelected = currentTab == GalleryTab.BILDER,
                onClick = { onTabSelected(GalleryTab.BILDER) }
            )
            NavItem(
                title = "Alben",
                icon = Icons.Rounded.Collections,
                isSelected = currentTab == GalleryTab.ALBEN,
                onClick = { onTabSelected(GalleryTab.ALBEN) }
            )
            NavItem(
                title = "Menü",
                icon = Icons.Rounded.Menu,
                isSelected = currentTab == GalleryTab.MENU,
                onClick = { onTabSelected(GalleryTab.MENU) }
            )
        }
    }

    if (isLiquidGlass) {
        // Floating inset glass pill, detached from the screen edges.
        Box(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFF1C1C1E).copy(alpha = 0.78f))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(32.dp))
            ) {
                items()
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(AmoledBlack)
                .navigationBarsPadding()
        ) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = Color(0xFF161616)
            )
            items()
        }
    }
}

@Composable
private fun NavItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    when (theme.language) {
        DesignLanguage.XIAOMI_HYPEROS -> HyperOsNavItem(title, icon, isSelected, onClick, theme, modifier)
        else -> CapsuleNavItem(title, icon, isSelected, onClick, theme, modifier)
    }
}

/** Shared capsule-style item used by OneUI, Expressive and Liquid Glass (differ only in color/motion). */
@Composable
private fun CapsuleNavItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    theme: AppTheme,
    modifier: Modifier = Modifier
) {
    val pillCornerRadius = theme.shapes.pillCornerRadius
    val isExpressive = theme.language == DesignLanguage.GOOGLE_EXPRESSIVE
    val isGlass = theme.language == DesignLanguage.IOS_LIQUID_GLASS

    val pillTargetColor = when {
        !isSelected -> Color.Transparent
        isExpressive -> theme.palette.accent.copy(alpha = 0.28f)
        isGlass -> Color.White.copy(alpha = 0.16f)
        else -> Color(0xFF1E1E1E)
    }
    val contentTargetColor = when {
        !isSelected -> TextMuted
        isExpressive -> theme.palette.accent
        else -> PureWhite
    }

    val pillBgColor by animateColorAsState(
        targetValue = pillTargetColor,
        animationSpec = designTween(theme.language, AmoledGalleryApp.performanceProfile, 240),
        label = "pill_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = contentTargetColor,
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
            .clip(RoundedCornerShape(pillCornerRadius))
            .background(pillBgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 48.dp, color = OneUiBlue.copy(alpha = 0.25f)),
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier
                    .size(22.dp)
                    .scale(iconScale)
            )

            val sizeAnimSpec: FiniteAnimationSpec<IntSize> = spring(stiffness = Spring.StiffnessMediumLow)

            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(animationSpec = tween(220, delayMillis = 40)) +
                        expandHorizontally(animationSpec = sizeAnimSpec),
                exit = fadeOut(animationSpec = tween(150)) +
                        shrinkHorizontally(animationSpec = sizeAnimSpec)
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

/** HyperOS: no capsule - a plain accent color change plus a thin underline dash, fast/no-spring. */
@Composable
private fun HyperOsNavItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    theme: AppTheme,
    modifier: Modifier = Modifier
) {
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) theme.palette.accent else TextMuted,
        animationSpec = designTween(theme.language, AmoledGalleryApp.performanceProfile, 120),
        label = "hyperos_content"
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(if (isSelected) 16.dp else 0.dp)
                .clip(CircleShape)
                .background(theme.palette.accent)
        )
    }
}
