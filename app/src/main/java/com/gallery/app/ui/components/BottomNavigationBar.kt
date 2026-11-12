package com.gallery.app.ui.components
import com.gallery.app.ui.i18n.strings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.app.ui.design.glass
import com.gallery.app.ui.design.SamsungGalleryIcons
import com.gallery.app.ui.theme.LocalAppTheme
import com.gallery.app.ui.viewmodel.GalleryTab

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
 * Traditional full-width bottom bar with the Photos and Albums content tabs.
 * A solid background with a hairline top divider; the selected tab is tinted with the accent colour.
 */
@Composable
fun BottomNavigationBar(
    currentTab: GalleryTab,
    onTabSelected: (GalleryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppTheme.current.palette

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.background)
            .navigationBarsPadding()
    ) {
        // Hairline divider between the content and the bar.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.cardBorder)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            NAV_TABS.forEach { tab ->
                val selected = tab.tab == currentTab
                val tint = if (selected) palette.accent else palette.textMuted
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = palette.accent)
                        ) { onTabSelected(tab.tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.oneUiIcon(selected),
                        contentDescription = tab.label,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        color = tint,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
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
