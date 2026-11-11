package com.gallery.app.ui.components
import com.gallery.app.ui.i18n.displayLabel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.app.ui.theme.LocalAppTheme
import com.gallery.app.ui.viewmodel.MediaFilter

/**
 * Media type filter ("All" | "Images" | "Videos") as One UI rounded tab pills. The Bilder tab also
 * exposes the same choice through the ⋮ popup menu.
 */
@Composable
fun FilterChips(
    selectedFilter: MediaFilter,
    onFilterSelected: (MediaFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppTheme.current.palette

    Row(
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
