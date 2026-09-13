package com.example.amoledgallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.SdCard
import androidx.compose.material.icons.rounded.Settings
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
import com.example.amoledgallery.PerformanceProfile
import com.example.amoledgallery.ui.components.OneUiHeader
import com.example.amoledgallery.ui.theme.AmoledBlack
import com.example.amoledgallery.ui.theme.AppColorPalette
import com.example.amoledgallery.ui.theme.CUSTOM_PALETTE_ID
import com.example.amoledgallery.ui.theme.DesignLanguage
import com.example.amoledgallery.ui.theme.LocalAppTheme
import com.example.amoledgallery.ui.theme.OneUiBlue
import com.example.amoledgallery.ui.theme.OneUiCardBorder
import com.example.amoledgallery.ui.theme.PureWhite
import com.example.amoledgallery.ui.theme.SurfaceDark
import com.example.amoledgallery.ui.theme.SurfaceVariantDark
import com.example.amoledgallery.ui.theme.TextMuted
import com.example.amoledgallery.ui.theme.ThemeState
import com.example.amoledgallery.ui.theme.presetsFor
import com.example.amoledgallery.ui.themedBackgroundBlur

/**
 * Clean Samsung OneUI style "Menü" tab in pure AMOLED black:
 * - Favorites shortcut showing favorite count
 * - Trash shortcut showing trashed media count
 * - Settings & info modal with manual Graphics Quality picker (Low/Medium/High/Ultra)
 */
@Composable
fun MenuScreen(
    favoriteCount: Int,
    trashedCount: Int,
    internalFreeBytes: Long,
    internalTotalBytes: Long,
    sdCardFreeBytes: Long?,
    sdCardTotalBytes: Long?,
    appVersionName: String = "",
    graphicsQuality: PerformanceProfile = PerformanceProfile.MEDIUM,
    onSetGraphicsQuality: (PerformanceProfile) -> Unit = {},
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
                modifier = Modifier
                    .fillMaxWidth()
                    .themedBackgroundBlur(radiusDp = 20f),
                shape = RoundedCornerShape(LocalAppTheme.current.shapes.cardCornerRadius),
                colors = CardDefaults.cardColors(
                    containerColor = if (LocalAppTheme.current.shapes.useBlur) SurfaceDark.copy(alpha = 0.75f) else SurfaceDark
                ),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .themedBackgroundBlur(radiusDp = 20f),
                shape = RoundedCornerShape(LocalAppTheme.current.shapes.cardCornerRadius),
                colors = CardDefaults.cardColors(
                    containerColor = if (LocalAppTheme.current.shapes.useBlur) SurfaceDark.copy(alpha = 0.75f) else SurfaceDark
                ),
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
            internalFreeBytes = internalFreeBytes,
            internalTotalBytes = internalTotalBytes,
            sdCardFreeBytes = sdCardFreeBytes,
            sdCardTotalBytes = sdCardTotalBytes,
            appVersionName = appVersionName,
            graphicsQuality = graphicsQuality,
            onSetGraphicsQuality = onSetGraphicsQuality,
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SettingsInfoDialog(
    internalFreeBytes: Long,
    internalTotalBytes: Long,
    sdCardFreeBytes: Long?,
    sdCardTotalBytes: Long?,
    appVersionName: String,
    graphicsQuality: PerformanceProfile,
    onSetGraphicsQuality: (PerformanceProfile) -> Unit,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var designLanguage by remember { mutableStateOf(ThemeState.designLanguage) }
    var showCustomEditor by remember { mutableStateOf(false) }
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceVariantDark,
        contentColor = PureWhite,
        shape = RoundedCornerShape(
            topStart = LocalAppTheme.current.shapes.dialogCornerRadius,
            topEnd = LocalAppTheme.current.shapes.dialogCornerRadius
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                InfoBadgeRow(
                    icon = Icons.Rounded.Info,
                    label = "App-Version",
                    value = appVersionName.ifBlank { "Unbekannt" }
                )
                StorageSpaceRow(
                    icon = Icons.Rounded.PhoneAndroid,
                    label = "Interner Speicher",
                    freeBytes = internalFreeBytes,
                    totalBytes = internalTotalBytes,
                    notAvailableText = null
                )
                StorageSpaceRow(
                    icon = Icons.Rounded.SdCard,
                    label = "SD-Karte",
                    freeBytes = sdCardFreeBytes,
                    totalBytes = sdCardTotalBytes,
                    notAvailableText = "Nicht vorhanden"
                )

                HorizontalDivider(
                    color = Color(0xFF2A2A2A),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Column {
                    Text(
                        text = "Design",
                        color = PureWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DesignLanguageSelector(
                        selected = designLanguage,
                        onSelect = { language ->
                            designLanguage = language
                            val preset = presetsFor(language).first()
                            ThemeState.apply(language, preset, context = context)
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = designLanguage.shortDescription,
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PaletteRow(
                        language = designLanguage,
                        currentPaletteId = ThemeState.palette.id,
                        onSelectPreset = { preset -> ThemeState.apply(designLanguage, preset, context = context) },
                        onOpenCustomEditor = { showCustomEditor = true }
                    )
                }

                HorizontalDivider(
                    color = Color(0xFF2A2A2A),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Column {
                    Text(
                        text = "Grafikqualität",
                        color = PureWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    GraphicsQualitySelector(
                        selected = graphicsQuality,
                        onSelect = onSetGraphicsQuality
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = graphicsQualityDescription(graphicsQuality),
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Schließen", color = OneUiBlue, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showCustomEditor) {
        CustomPaletteEditorDialog(
            language = designLanguage,
            initialAccent = if (ThemeState.palette.id == CUSTOM_PALETTE_ID) ThemeState.palette.accent else presetsFor(designLanguage).first().accent,
            onConfirm = { accent ->
                val base = presetsFor(designLanguage).first()
                val custom = base.copy(id = CUSTOM_PALETTE_ID, label = "Eigene Palette", accent = accent)
                ThemeState.apply(designLanguage, custom, context = context)
                showCustomEditor = false
            },
            onDismiss = { showCustomEditor = false }
        )
    }
}

/** 4 design-language chips, always visible together so the switch is a single tap. */
@Composable
private fun DesignLanguageSelector(
    selected: DesignLanguage,
    onSelect: (DesignLanguage) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DesignLanguage.entries.forEach { language ->
            val isSelected = language == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) OneUiBlue.copy(alpha = 0.18f) else SurfaceDark)
                    .clickable { onSelect(language) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) OneUiBlue else Color.Transparent)
                        .border(1.5.dp, if (isSelected) OneUiBlue else TextMuted, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = language.displayName,
                    color = if (isSelected) PureWhite else TextMuted,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

/** Preset color swatches for the active design language, plus a trailing "custom" swatch. */
@Composable
private fun PaletteRow(
    language: DesignLanguage,
    currentPaletteId: String,
    onSelectPreset: (AppColorPalette) -> Unit,
    onOpenCustomEditor: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        presetsFor(language).forEach { preset ->
            val isSelected = preset.id == currentPaletteId
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(preset.accent)
                    .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) PureWhite else Color.White.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                    .clickable { onSelectPreset(preset) }
            )
        }
        // Custom palette swatch: shows the current custom accent (or a placeholder ring)
        val isCustomSelected = currentPaletteId == CUSTOM_PALETTE_ID
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isCustomSelected) ThemeState.accent else SurfaceDark)
                .border(
                    width = if (isCustomSelected) 2.5.dp else 1.dp,
                    color = if (isCustomSelected) PureWhite else TextMuted,
                    shape = CircleShape
                )
                .clickable { onOpenCustomEditor() },
            contentAlignment = Alignment.Center
        ) {
            if (!isCustomSelected) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Eigene Farbe",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CustomPaletteEditorDialog(
    language: DesignLanguage,
    initialAccent: Color,
    onConfirm: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var pickedColor by remember { mutableStateOf(initialAccent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Eigene Akzentfarbe",
                color = PureWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Der Hintergrund bleibt reines AMOLED-Schwarz - wähle deine persönliche Akzentfarbe für ${language.displayName}.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                com.example.amoledgallery.ui.components.HsvColorPicker(
                    initialColor = initialAccent,
                    onColorChanged = { pickedColor = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pickedColor) }) {
                Text("Übernehmen", color = OneUiBlue, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen", color = TextMuted)
            }
        },
        containerColor = SurfaceVariantDark,
        shape = RoundedCornerShape(LocalAppTheme.current.shapes.dialogCornerRadius)
    )
}

/**
 * Game-style 4-tier segmented picker for the manual graphics quality setting.
 */
@Composable
private fun GraphicsQualitySelector(
    selected: PerformanceProfile,
    onSelect: (PerformanceProfile) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        PerformanceProfile.entries.forEach { tier ->
            val isSelected = tier == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (isSelected) OneUiBlue else Color.Transparent)
                    .clickable { onSelect(tier) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = graphicsQualityLabel(tier),
                    color = if (isSelected) PureWhite else TextMuted,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

private fun graphicsQualityLabel(tier: PerformanceProfile): String = when (tier) {
    PerformanceProfile.LOW -> "Low"
    PerformanceProfile.MEDIUM -> "Medium"
    PerformanceProfile.HIGH -> "High"
    PerformanceProfile.ULTRA -> "Ultra"
}

private fun graphicsQualityDescription(tier: PerformanceProfile): String = when (tier) {
    PerformanceProfile.LOW -> "Nur das Nötigste: keine Animationen, kleinere Vorschaubilder, geringster Speicher- & Akkuverbrauch. Für ältere oder langsame Geräte."
    PerformanceProfile.MEDIUM -> "Ausgewogen: reduzierte Animationen, gute Bildqualität. Empfohlen für die meisten Mittelklasse-Geräte."
    PerformanceProfile.HIGH -> "Flüssige Animationen und Überblendungen, hohe Bildqualität. Für aktuelle Geräte."
    PerformanceProfile.ULTRA -> "Alle Animationseffekte, höchste Bildqualität und größte Caches – für sehr starke Geräte."
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

/**
 * Clean, simple free/total storage readout with a thin usage bar, for internal storage
 * (always available) and the SD card (shows [notAvailableText] instead when null).
 */
@Composable
private fun StorageSpaceRow(
    icon: ImageVector,
    label: String,
    freeBytes: Long?,
    totalBytes: Long?,
    notAvailableText: String?
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = TextMuted,
                fontSize = 12.sp
            )
            if (freeBytes != null && totalBytes != null && totalBytes > 0) {
                Text(
                    text = "${formatBytes(freeBytes)} frei von ${formatBytes(totalBytes)}",
                    color = PureWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                val usedFraction = (1f - (freeBytes.toFloat() / totalBytes.toFloat())).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(SurfaceDark)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(usedFraction)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(2.dp))
                            .background(OneUiBlue)
                    )
                }
            } else {
                Text(
                    text = notAvailableText ?: "Unbekannt",
                    color = PureWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    return java.util.Locale.getDefault().let { locale -> String.format(locale, "%.1f GB", gb) }
}
