package com.example.amoledgallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SdCard
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amoledgallery.ui.components.OneUiHeader
import com.example.amoledgallery.ui.theme.AmoledBlack
import com.example.amoledgallery.ui.theme.OneUiBlue
import com.example.amoledgallery.ui.theme.OneUiCardBorder
import com.example.amoledgallery.ui.theme.PureWhite
import com.example.amoledgallery.ui.theme.SurfaceDark
import com.example.amoledgallery.ui.theme.SurfaceVariantDark
import com.example.amoledgallery.ui.theme.TextMuted

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults

/**
 * Clean Samsung OneUI style "Menü" tab in pure AMOLED black:
 * - Favorites shortcut showing favorite count
 * - Trash shortcut showing trashed media count
 * - Settings & system diagnostic info modal
 * - Aggressive Performance Mode switch for older SoCs (Exynos 9810)
 */
@Composable
fun MenuScreen(
    favoriteCount: Int,
    trashedCount: Int,
    isSdCardAvailable: Boolean,
    isAggressivePerformance: Boolean = true,
    onToggleAggressivePerformance: (Boolean) -> Unit = {},
    onOpenFavorites: () -> Unit,
    onOpenTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSettingsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        OneUiHeader(
            mediaCount = 0,
            title = "Menü",
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section: Bibliothek
            Text(
                text = "BIBLIOTHEK",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 12.dp, top = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OneUiCardBorder))
            ) {
                Column {
                    MenuRowItem(
                        icon = Icons.Rounded.Favorite,
                        iconTint = PureWhite,
                        iconBg = Color(0xFF2C161C),
                        title = "Favoriten",
                        subtitle = if (favoriteCount == 1) "1 Element" else "$favoriteCount Elemente",
                        onClick = onOpenFavorites
                    )

                    HorizontalDivider(
                        color = Color(0xFF1F1F1F),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 64.dp)
                    )

                    MenuRowItem(
                        icon = Icons.Rounded.Delete,
                        iconTint = PureWhite,
                        iconBg = Color(0xFF28201A),
                        title = "Papierkorb",
                        subtitle = if (trashedCount == 1) "1 Element" else "$trashedCount Elemente",
                        onClick = onOpenTrash
                    )
                }
            }

            // Section: Einstellungen & System
            Text(
                text = "SYSTEM & EINSTELLUNGEN",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 12.dp, top = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OneUiCardBorder))
            ) {
                Column {
                    MenuRowItem(
                        icon = Icons.Rounded.Settings,
                        iconTint = PureWhite,
                        iconBg = Color(0xFF1A2230),
                        title = "Einstellungen & Info",
                        subtitle = "Speicher, Performance & Systemstatus",
                        onClick = { showSettingsDialog = true }
                    )
                }
            }
        }
    }

    if (showSettingsDialog) {
        SettingsInfoDialog(
            isSdCardAvailable = isSdCardAvailable,
            isAggressivePerformance = isAggressivePerformance,
            onToggleAggressivePerformance = onToggleAggressivePerformance,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun MenuRowItem(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = PureWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 13.sp
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsInfoDialog(
    isSdCardAvailable: Boolean,
    isAggressivePerformance: Boolean,
    onToggleAggressivePerformance: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "AmoledGallery Info",
                    color = PureWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                InfoBadgeRow(
                    icon = Icons.Rounded.Palette,
                    label = "Display-Profil",
                    value = "100% Pure AMOLED (#000000)"
                )
                InfoBadgeRow(
                    icon = Icons.Rounded.Speed,
                    label = "Scroll-Engine",
                    value = if (isAggressivePerformance) "GPU Hardware Bitmaps (160px WebP 65%)" else "GPU Hardware Bitmaps (160px WebP 70%)"
                )
                InfoBadgeRow(
                    icon = Icons.Rounded.SdCard,
                    label = "MicroSD-Speicher",
                    value = if (isSdCardAvailable) "Erkannt & Verfügbar" else "Nicht vorhanden"
                )

                HorizontalDivider(
                    color = Color(0xFF2A2A2A),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Aggressive Performance Mode Switch (Exynos 9810 Boost)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Aggressiver Performance-Modus",
                            color = PureWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Begrenzt Coil auf 2 CPU-Worker & pausiert Indexer beim Scrollen (ideal für Exynos 9810).",
                            color = TextMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = isAggressivePerformance,
                        onCheckedChange = onToggleAggressivePerformance,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PureWhite,
                            checkedTrackColor = OneUiBlue,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceDark
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen", color = OneUiBlue, fontWeight = FontWeight.SemiBold)
            }
        },
        containerColor = SurfaceVariantDark,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun InfoBadgeRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                color = TextMuted,
                fontSize = 12.sp
            )
            Text(
                text = value,
                color = PureWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
