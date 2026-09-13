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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amoledgallery.AmoledGalleryApp
import com.example.amoledgallery.ui.theme.AmoledBlack
import com.example.amoledgallery.ui.theme.LocalAppTheme
import com.example.amoledgallery.ui.theme.OneUiBlue
import com.example.amoledgallery.ui.theme.PureWhite
import com.example.amoledgallery.ui.theme.TextMuted
import com.example.amoledgallery.ui.theme.designTween
import com.example.amoledgallery.ui.themedBackgroundBlur
import com.example.amoledgallery.ui.viewmodel.GalleryTab

/**
 * Modern Samsung OneUI AMOLED Bottom Navigation Bar:
 * - Pitch black background (#000000) with 0.dp tonal elevation
 * - Active tab with smoothly animated rounded capsule/pill background (Color(0xFF1E1E1E))
 * - Slim modern vector icons
 * - Smooth fade in/out and expansion for active text label
 */
@Composable
fun BottomNavigationBar(
    currentTab: GalleryTab,
    onTabSelected: (GalleryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val shapes = LocalAppTheme.current.shapes

    Column(
        modifier = modifier
            .fillMaxWidth()
            .themedBackgroundBlur(radiusDp = 30f)
            .background(if (shapes.useBlur) AmoledBlack.copy(alpha = 0.72f) else AmoledBlack)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = Color(0xFF161616)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OneUiNavItem(
                title = "Bilder",
                icon = Icons.Rounded.Image,
                isSelected = currentTab == GalleryTab.BILDER,
                onClick = { onTabSelected(GalleryTab.BILDER) }
            )

            OneUiNavItem(
                title = "Alben",
                icon = Icons.Rounded.Collections,
                isSelected = currentTab == GalleryTab.ALBEN,
                onClick = { onTabSelected(GalleryTab.ALBEN) }
            )

            OneUiNavItem(
                title = "Menü",
                icon = Icons.Rounded.Menu,
                isSelected = currentTab == GalleryTab.MENU,
                onClick = { onTabSelected(GalleryTab.MENU) }
            )
        }
    }
}

@Composable
private fun OneUiNavItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val pillCornerRadius = theme.shapes.pillCornerRadius

    val pillBgColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF1E1E1E) else Color.Transparent,
        animationSpec = designTween(theme.language, AmoledGalleryApp.performanceProfile, 240),
        label = "pill_bg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) PureWhite else TextMuted,
        animationSpec = designTween(theme.language, AmoledGalleryApp.performanceProfile, 200),
        label = "pill_content"
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
                modifier = Modifier.size(22.dp)
            )

            val useSpring = theme.shapes.motionStyle != com.example.amoledgallery.ui.theme.MotionStyle.FAST_CUT
            val sizeAnimSpec: FiniteAnimationSpec<IntSize> = if (useSpring) {
                spring(stiffness = Spring.StiffnessMediumLow)
            } else {
                tween(durationMillis = 120)
            }

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
