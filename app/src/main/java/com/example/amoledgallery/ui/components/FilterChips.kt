package com.example.amoledgallery.ui.components
import com.example.amoledgallery.ui.i18n.displayLabel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amoledgallery.AmoledGalleryApp
import com.example.amoledgallery.ui.design.ThemedSegmentedControl
import com.example.amoledgallery.ui.theme.DesignLanguage
import com.example.amoledgallery.ui.theme.LocalAppTheme
import com.example.amoledgallery.ui.theme.designTween
import com.example.amoledgallery.ui.viewmodel.MediaFilter

/**
 * Media type filter ("Alle" | "Bilder" | "Videos" | "GIFs"), in each OS's own control:
 * - Material 3 Expressive: filter chips with a check that springs in.
 * - iOS: a segmented control (GalleryScreen floats it as a glass capsule above the tab bar).
 * - HyperOS: text tabs with a short accent underline.
 * - One UI: rounded tab pills (the Bilder tab itself uses the ⋮ popup menu instead).
 */
@Composable
fun FilterChips(
    selectedFilter: MediaFilter,
    onFilterSelected: (MediaFilter) -> Unit,
    modifier: Modifier = Modifier,
    floatingGlass: Boolean = false
) {
    val theme = LocalAppTheme.current
    val palette = theme.palette

    when (theme.language) {
        DesignLanguage.IOS_LIQUID_GLASS -> ThemedSegmentedControl(
            options = MediaFilter.entries,
            selected = selectedFilter,
            label = { it.displayLabel },
            onSelect = onFilterSelected,
            floatingGlass = floatingGlass,
            modifier = modifier
        )

        DesignLanguage.GOOGLE_EXPRESSIVE -> Row(
            modifier = modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaFilter.entries.forEach { filter ->
                val selected = filter == selectedFilter
                val container by animateColorAsState(
                    targetValue = if (selected) lerp(palette.surfaceVariant, palette.accent, 0.38f) else palette.background.copy(alpha = 0f),
                    label = "chip_container"
                )
                val shape = RoundedCornerShape(10.dp)
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(shape)
                        .background(container)
                        .then(if (!selected) Modifier.border(1.dp, palette.cardBorder, shape) else Modifier)
                        .clickable { onFilterSelected(filter) }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedVisibility(
                        visible = selected,
                        enter = fadeIn() + expandHorizontally(spring(dampingRatio = 0.6f, stiffness = 500f)),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Check, null, tint = palette.onBackground, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                    }
                    Text(
                        text = filter.displayLabel,
                        color = palette.onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        DesignLanguage.XIAOMI_HYPEROS -> Row(
            modifier = modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaFilter.entries.forEach { filter ->
                val selected = filter == selectedFilter
                val underline by animateDpAsState(
                    targetValue = if (selected) 16.dp else 0.dp,
                    animationSpec = designTween(theme.language, AmoledGalleryApp.performanceProfile, 200),
                    label = "hyperos_underline"
                )
                Column(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onFilterSelected(filter) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = filter.displayLabel,
                        color = if (selected) palette.onBackground else palette.textMuted,
                        fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(underline)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(palette.accent)
                    )
                }
            }
        }

        DesignLanguage.SAMSUNG_ONEUI -> Row(
            modifier = modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaFilter.entries.forEach { filter ->
                val selected = filter == selectedFilter
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (selected) palette.surfaceVariant else palette.background)
                        .clickable { onFilterSelected(filter) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = filter.displayLabel,
                        color = if (selected) palette.onBackground else palette.textMuted,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
