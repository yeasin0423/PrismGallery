package com.example.amoledgallery.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.amoledgallery.ui.viewmodel.GalleryTab

/**
 * Delegating OneUI style Bottom Navigation Bar in AMOLED pure black.
 * Directs to modern [BottomNavigationBar].
 */
@Composable
fun OneUiBottomBar(
    currentTab: GalleryTab,
    onTabSelected: (GalleryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    BottomNavigationBar(
        currentTab = currentTab,
        onTabSelected = onTabSelected,
        modifier = modifier
    )
}
