package com.example.amoledgallery.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amoledgallery.AmoledGalleryApp
import com.example.amoledgallery.ui.theme.AmoledBlack
import com.example.amoledgallery.ui.theme.LocalAppTheme
import com.example.amoledgallery.ui.theme.designTween
import com.example.amoledgallery.ui.theme.PureWhite
import com.example.amoledgallery.ui.theme.SurfaceVariantDark
import com.example.amoledgallery.ui.theme.TextMuted
import com.example.amoledgallery.ui.viewmodel.MediaFilter

/**
 * OneUI-style horizontal media filter chips: "Alle" | "Bilder" | "Videos" | "GIFs"
 */
@Composable
fun FilterChips(
    selectedFilter: MediaFilter,
    onFilterSelected: (MediaFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val theme = LocalAppTheme.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MediaFilter.entries.forEach { filter ->
            val isSelected = filter == selectedFilter
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) PureWhite else SurfaceVariantDark,
                animationSpec = designTween(theme.language, AmoledGalleryApp.performanceProfile, 220),
                label = "chip_bg"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) AmoledBlack else TextMuted,
                animationSpec = designTween(theme.language, AmoledGalleryApp.performanceProfile, 220),
                label = "chip_content"
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(theme.shapes.pillCornerRadius))
                    .background(bgColor)
                    .clickable { onFilterSelected(filter) }
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filter.label,
                    color = contentColor,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
