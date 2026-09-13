package com.example.amoledgallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amoledgallery.PerformanceProfile
import com.example.amoledgallery.ui.components.HsvColorPicker
import com.example.amoledgallery.ui.components.LargeHeader
import com.example.amoledgallery.ui.design.DialogButton
import com.example.amoledgallery.ui.design.DialogButtonStyle
import com.example.amoledgallery.ui.design.ExpressiveCookieShape
import com.example.amoledgallery.ui.design.ListRow
import com.example.amoledgallery.ui.design.ThemedCard
import com.example.amoledgallery.ui.design.ThemedDialog
import com.example.amoledgallery.ui.design.ThemedListSection
import com.example.amoledgallery.ui.design.ThemedProgressBar
import com.example.amoledgallery.ui.design.ThemedRadioIndicator
import com.example.amoledgallery.ui.design.ThemedSectionHeader
import com.example.amoledgallery.ui.design.ThemedSegmentedControl
import com.example.amoledgallery.ui.design.ThemedSheet
import com.example.amoledgallery.ui.design.bottomBarContentPadding
import com.example.amoledgallery.ui.design.pressScale
import com.example.amoledgallery.ui.theme.AppColorPalette
import com.example.amoledgallery.ui.theme.CUSTOM_PALETTE_ID
import com.example.amoledgallery.ui.theme.DesignLanguage
import com.example.amoledgallery.ui.theme.LocalAppTheme
import com.example.amoledgallery.ui.theme.ThemeState
import com.example.amoledgallery.ui.theme.isLight
import com.example.amoledgallery.ui.theme.onAccent
import com.example.amoledgallery.ui.theme.presetsFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

/** Sections of the settings sheet; every menu entry opens the sheet scrolled to its own section. */
enum class SettingsSection { DESIGN, SETTINGS, STORAGE, INFO }

/**
 * The "Menü" tab, built the way each OS would build it:
 * - One UI: huge centred title, two big shortcut tiles, a list card with blue value summaries.
 * - Material 3 Expressive: a tonal storage card with the wavy progress indicator, segmented lists with
 *   cookie-shaped icons.
 * - iOS: Settings-style inset grouped lists with colored icon squares and trailing values.
 * - HyperOS: light cards - shortcut cards with pastel icon circles, a storage card, a chevron list.
 *
 * Design, Einstellungen, Speicher and Info all live in one settings sheet, and each entry opens it
 * scrolled to that section.
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
    var settingsSection by remember { mutableStateOf<SettingsSection?>(null) }
    val theme = LocalAppTheme.current
    val palette = theme.palette
    val language = theme.language

    val favoritesText = if (favoriteCount == 1) "1 Element" else "$favoriteCount Elemente"
    val trashText = if (trashedCount == 1) "1 Element" else "$trashedCount Elemente"
    val storageText = if (internalTotalBytes > 0) {
        "${formatBytes(internalFreeBytes)} frei von ${formatBytes(internalTotalBytes)}"
    } else {
        "Unbekannt"
    }
    val usedFraction = if (internalTotalBytes > 0) {
        (1f - internalFreeBytes.toFloat() / internalTotalBytes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val openDesign = { settingsSection = SettingsSection.DESIGN }
    val openSettings = { settingsSection = SettingsSection.SETTINGS }
    val openStorage = { settingsSection = SettingsSection.STORAGE }
    val openInfo = { settingsSection = SettingsSection.INFO }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        LargeHeader(title = "Menü")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (language == DesignLanguage.GOOGLE_EXPRESSIVE || language == DesignLanguage.XIAOMI_HYPEROS) 12.dp else 16.dp)
                .padding(bottom = bottomBarContentPadding(24.dp)),
            verticalArrangement = Arrangement.spacedBy(if (language == DesignLanguage.IOS_LIQUID_GLASS) 26.dp else 16.dp)
        ) {
            when (language) {
                DesignLanguage.SAMSUNG_ONEUI -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OneUiShortcutTile(Icons.Rounded.Favorite, Color(0xFFFF5A7A), "Favoriten", favoritesText, Modifier.weight(1f), onOpenFavorites)
                        OneUiShortcutTile(Icons.Rounded.Delete, Color(0xFFFFA24C), "Papierkorb", trashText, Modifier.weight(1f), onOpenTrash)
                    }
                    ThemedListSection(
                        rows = listOf(
                            ListRow("Design", subtitle = "${ThemeState.designLanguage.displayName} · ${ThemeState.palette.label}", icon = Icons.Rounded.Palette, iconColor = Color(0xFFB39DFF), onClick = openDesign),
                            ListRow("Einstellungen", subtitle = "Grafikqualität", icon = Icons.Rounded.Settings, iconColor = palette.onBackground, onClick = openSettings),
                            ListRow("Speicher", subtitle = storageText, icon = Icons.Rounded.Storage, iconColor = Color(0xFF4FC3F7), onClick = openStorage),
                            ListRow("Info", subtitle = "Version ${appVersionName.ifBlank { "unbekannt" }}", icon = Icons.Rounded.Info, iconColor = palette.textMuted, onClick = openInfo)
                        )
                    )
                }

                DesignLanguage.GOOGLE_EXPRESSIVE -> {
                    val interaction = remember { MutableInteractionSource() }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScale(interaction, 0.98f)
                            .clip(RoundedCornerShape(30.dp))
                            .background(lerp(palette.surfaceVariant, palette.accent, 0.14f))
                            .clickable(interactionSource = interaction, indication = null, onClick = openStorage)
                            .padding(22.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(ExpressiveCookieShape)
                                    .background(palette.accent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Storage, null, tint = palette.onAccent, modifier = Modifier.size(26.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Gerätespeicher", color = palette.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                                Text(storageText, color = palette.textMuted, fontSize = 14.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        ThemedProgressBar(progress = usedFraction)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${(usedFraction * 100).roundToInt()} % belegt",
                            color = palette.textMuted,
                            fontSize = 13.sp
                        )
                    }
                    ThemedListSection(
                        header = "Bibliothek",
                        rows = listOf(
                            ListRow("Favoriten", subtitle = favoritesText, icon = Icons.Rounded.Favorite, iconColor = Color(0xFFFF8AB3), onClick = onOpenFavorites),
                            ListRow("Papierkorb", subtitle = trashText, icon = Icons.Rounded.Delete, iconColor = Color(0xFFFFB870), onClick = onOpenTrash)
                        )
                    )
                    ThemedListSection(
                        header = "App",
                        rows = listOf(
                            ListRow("Design & Farben", subtitle = ThemeState.designLanguage.displayName, icon = Icons.Rounded.Palette, iconColor = Color(0xFFB39DFF), onClick = openDesign),
                            ListRow("Einstellungen", subtitle = "Grafikqualität", icon = Icons.Rounded.Settings, iconColor = palette.accent, onClick = openSettings),
                            ListRow("Info", subtitle = "Version ${appVersionName.ifBlank { "unbekannt" }}", icon = Icons.Rounded.Info, iconColor = Color(0xFF90CAF9), onClick = openInfo)
                        )
                    )
                }

                DesignLanguage.IOS_LIQUID_GLASS -> {
                    ThemedListSection(
                        header = "Mediathek",
                        rows = listOf(
                            ListRow("Favoriten", icon = Icons.Rounded.Favorite, iconColor = Color(0xFFFF375F), value = "$favoriteCount", onClick = onOpenFavorites),
                            ListRow("Zuletzt gelöscht", icon = Icons.Rounded.Delete, iconColor = Color(0xFF8E8E93), value = "$trashedCount", onClick = onOpenTrash)
                        )
                    )
                    ThemedListSection(
                        header = "Allgemein",
                        rows = listOf(
                            ListRow("Darstellung", icon = Icons.Rounded.Palette, iconColor = Color(0xFF0A84FF), onClick = openDesign),
                            ListRow("Einstellungen", icon = Icons.Rounded.Settings, iconColor = Color(0xFF8E8E93), onClick = openSettings),
                            ListRow("Speicher", icon = Icons.Rounded.Storage, iconColor = Color(0xFF34C759), value = if (internalTotalBytes > 0) "${formatBytes(internalFreeBytes)} frei" else null, onClick = openStorage),
                            ListRow("Info", icon = Icons.Rounded.Info, iconColor = Color(0xFF5E5CE6), value = appVersionName.ifBlank { null }, onClick = openInfo)
                        )
                    )
                }

                DesignLanguage.XIAOMI_HYPEROS -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HyperOsShortcutCard(Icons.Rounded.Favorite, Color(0xFFFF5C7A), "Favoriten", favoritesText, Modifier.weight(1f), onOpenFavorites)
                        HyperOsShortcutCard(Icons.Rounded.Delete, Color(0xFF3482FF), "Papierkorb", trashText, Modifier.weight(1f), onOpenTrash)
                    }
                    ThemedCard(modifier = Modifier.clickable(onClick = openStorage)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Speicher", color = palette.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Text(storageText, color = palette.textMuted, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        ThemedProgressBar(progress = usedFraction)
                    }
                    ThemedListSection(
                        rows = listOf(
                            ListRow("Design", subtitle = ThemeState.designLanguage.displayName, icon = Icons.Rounded.Palette, iconColor = Color(0xFFFF9F0A), onClick = openDesign),
                            ListRow("Einstellungen", subtitle = "Grafikqualität", icon = Icons.Rounded.Settings, iconColor = Color(0xFF8A8A8E), onClick = openSettings),
                            ListRow("Über Gallery", icon = Icons.Rounded.Info, iconColor = Color(0xFF34C759), value = appVersionName.ifBlank { null }, onClick = openInfo)
                        )
                    )
                }
            }
        }
    }

    settingsSection?.let { section ->
        SettingsSheet(
            initialSection = section,
            internalFreeBytes = internalFreeBytes,
            internalTotalBytes = internalTotalBytes,
            sdCardFreeBytes = sdCardFreeBytes,
            sdCardTotalBytes = sdCardTotalBytes,
            appVersionName = appVersionName,
            graphicsQuality = graphicsQuality,
            onSetGraphicsQuality = onSetGraphicsQuality,
            onDismiss = { settingsSection = null }
        )
    }
}

@Composable
private fun OneUiShortcutTile(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val palette = LocalAppTheme.current.palette
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(palette.surface)
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(30.dp))
        Spacer(modifier = Modifier.height(26.dp))
        Text(title, color = palette.onBackground, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(2.dp))
        Text(subtitle, color = palette.accent, fontSize = 14.sp)
    }
}

@Composable
private fun HyperOsShortcutCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val palette = LocalAppTheme.current.palette
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(palette.surface)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(23.dp))
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(title, color = palette.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Text(subtitle, color = palette.textMuted, fontSize = 12.sp)
    }
}

@Composable
private fun SettingsSheet(
    initialSection: SettingsSection,
    internalFreeBytes: Long,
    internalTotalBytes: Long,
    sdCardFreeBytes: Long?,
    sdCardTotalBytes: Long?,
    appVersionName: String,
    graphicsQuality: PerformanceProfile,
    onSetGraphicsQuality: (PerformanceProfile) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showCustomEditor by remember { mutableStateOf(false) }
    val currentLanguage = ThemeState.designLanguage
    val currentPalette = ThemeState.palette
    val lightMode = currentPalette.isLight
    val palette = LocalAppTheme.current.palette

    val scrollState = rememberScrollState()
    val sectionOffsets = remember { mutableStateMapOf<SettingsSection, Int>() }
    fun Modifier.sectionAnchor(section: SettingsSection) = onGloballyPositioned {
        sectionOffsets[section] = it.positionInParent().y.roundToInt()
    }

    fun switchBrightness(light: Boolean) {
        if (currentPalette.isLight == light) return
        val target = presetsFor(currentLanguage, light)
        val next = if (currentPalette.id == CUSTOM_PALETTE_ID) {
            target.first().copy(id = CUSTOM_PALETTE_ID, label = "Eigene Palette", accent = currentPalette.accent)
        } else {
            // Keep the same accent slot (blue stays blue) when switching between light and dark.
            val slot = presetsFor(currentLanguage, currentPalette.isLight).indexOfFirst { it.id == currentPalette.id }
            target.getOrElse(slot) { target.first() }
        }
        ThemeState.apply(currentLanguage, next, context = context)
    }

    ThemedSheet(onDismissRequest = onDismiss, title = "Einstellungen") {
        LaunchedEffect(initialSection) {
            if (initialSection != SettingsSection.DESIGN) {
                val offset = snapshotFlow { sectionOffsets[initialSection] }.filterNotNull().first()
                delay(150)
                scrollState.animateScrollTo(offset)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Column(modifier = Modifier.sectionAnchor(SettingsSection.DESIGN)) {
                ThemedSectionHeader("Design")
                ThemedCard(inSheet = true) {
                    DesignLanguage.entries.forEachIndexed { index, language ->
                        if (index > 0) Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    // Keep the current light/dark mode when switching design language.
                                    val preset = presetsFor(language, lightMode).firstOrNull() ?: presetsFor(language).first()
                                    ThemeState.apply(language, preset, context = context)
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(language.displayName, color = palette.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(language.shortDescription, color = palette.textMuted, fontSize = 13.sp, lineHeight = 17.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            ThemedRadioIndicator(selected = language == currentLanguage)
                        }
                    }
                }
            }

            Column {
                ThemedSectionHeader("Farben")
                ThemedCard(inSheet = true) {
                    ThemedSegmentedControl(
                        options = listOf(true, false),
                        selected = lightMode,
                        label = { light -> if (light) "Hell" else "Dunkel" },
                        onSelect = { light -> switchBrightness(light) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PaletteRow(
                        presets = presetsFor(currentLanguage, lightMode),
                        currentPaletteId = currentPalette.id,
                        onSelectPreset = { preset -> ThemeState.apply(currentLanguage, preset, context = context) },
                        onOpenCustomEditor = { showCustomEditor = true }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(currentPalette.label, color = palette.textMuted, fontSize = 13.sp)
                }
            }

            Column(modifier = Modifier.sectionAnchor(SettingsSection.SETTINGS)) {
                ThemedSectionHeader("Grafikqualität")
                ThemedSegmentedControl(
                    options = PerformanceProfile.entries,
                    selected = graphicsQuality,
                    label = { graphicsQualityLabel(it) },
                    onSelect = onSetGraphicsQuality
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = graphicsQualityDescription(graphicsQuality),
                    color = palette.textMuted,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Column(modifier = Modifier.sectionAnchor(SettingsSection.STORAGE)) {
                ThemedSectionHeader("Speicher")
                ThemedCard(inSheet = true) {
                    StorageBlock("Interner Speicher", internalFreeBytes, internalTotalBytes, null)
                    Spacer(modifier = Modifier.height(18.dp))
                    StorageBlock("SD-Karte", sdCardFreeBytes, sdCardTotalBytes, "Nicht vorhanden")
                }
            }

            ThemedListSection(
                header = "Info",
                inSheet = true,
                modifier = Modifier.sectionAnchor(SettingsSection.INFO),
                rows = listOf(
                    ListRow("App-Version", icon = Icons.Rounded.Info, iconColor = Color(0xFF8E8E93), value = appVersionName.ifBlank { "Unbekannt" }, showChevron = false)
                )
            )
        }
    }

    if (showCustomEditor) {
        CustomAccentDialog(
            language = currentLanguage,
            lightMode = lightMode,
            initialAccent = currentPalette.accent,
            onConfirm = { accent ->
                // The custom palette keeps the active mode's base colors - light stays light.
                val base = presetsFor(currentLanguage, lightMode).first()
                val custom = base.copy(id = CUSTOM_PALETTE_ID, label = "Eigene Palette", accent = accent)
                ThemeState.apply(currentLanguage, custom, context = context)
                showCustomEditor = false
            },
            onDismiss = { showCustomEditor = false }
        )
    }
}

@Composable
private fun StorageBlock(label: String, freeBytes: Long?, totalBytes: Long?, notAvailableText: String?) {
    val palette = LocalAppTheme.current.palette
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = palette.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Text(
            text = if (freeBytes != null && totalBytes != null && totalBytes > 0) {
                "${formatBytes(freeBytes)} frei von ${formatBytes(totalBytes)}"
            } else {
                notAvailableText ?: "Unbekannt"
            },
            color = palette.textMuted,
            fontSize = 13.sp
        )
    }
    if (freeBytes != null && totalBytes != null && totalBytes > 0) {
        Spacer(modifier = Modifier.height(10.dp))
        ThemedProgressBar(progress = (1f - freeBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f))
    }
}

/** Accent swatches for the active mode, plus a trailing "custom" swatch. */
@Composable
private fun PaletteRow(
    presets: List<AppColorPalette>,
    currentPaletteId: String,
    onSelectPreset: (AppColorPalette) -> Unit,
    onOpenCustomEditor: () -> Unit
) {
    val theme = LocalAppTheme.current
    val palette = theme.palette
    val swatchShape = if (theme.language == DesignLanguage.GOOGLE_EXPRESSIVE) ExpressiveCookieShape else CircleShape
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        presets.forEach { preset ->
            val isSelected = preset.id == currentPaletteId
            // Each swatch shows the preset's own background with its accent inside.
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(swatchShape)
                    .background(preset.background)
                    .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) palette.accent else palette.onBackground.copy(alpha = 0.15f),
                        shape = swatchShape
                    )
                    .clickable { onSelectPreset(preset) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(preset.accent)
                )
            }
        }
        val isCustomSelected = currentPaletteId == CUSTOM_PALETTE_ID
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(swatchShape)
                .background(if (isCustomSelected) ThemeState.accent else palette.surface)
                .border(
                    width = if (isCustomSelected) 2.5.dp else 1.dp,
                    color = if (isCustomSelected) palette.onBackground else palette.textMuted,
                    shape = swatchShape
                )
                .clickable { onOpenCustomEditor() },
            contentAlignment = Alignment.Center
        ) {
            if (!isCustomSelected) {
                Icon(Icons.Rounded.Add, "Eigene Farbe", tint = palette.textMuted, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun CustomAccentDialog(
    language: DesignLanguage,
    lightMode: Boolean,
    initialAccent: Color,
    onConfirm: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var pickedColor by remember { mutableStateOf(initialAccent) }
    ThemedDialog(
        onDismissRequest = onDismiss,
        title = "Eigene Akzentfarbe",
        message = "Die ${if (lightMode) "hellen" else "dunklen"} Grundfarben von ${language.displayName} bleiben erhalten - nur der Akzent ändert sich.",
        buttons = listOf(
            DialogButton("Abbrechen", DialogButtonStyle.Cancel, onClick = onDismiss),
            DialogButton("Übernehmen", DialogButtonStyle.Primary) { onConfirm(pickedColor) }
        )
    ) {
        HsvColorPicker(
            initialColor = initialAccent,
            onColorChanged = { pickedColor = it }
        )
    }
}

private fun graphicsQualityLabel(tier: PerformanceProfile): String = when (tier) {
    PerformanceProfile.LOW -> "Low"
    PerformanceProfile.MEDIUM -> "Medium"
    PerformanceProfile.HIGH -> "High"
    PerformanceProfile.ULTRA -> "Ultra"
}

private fun graphicsQualityDescription(tier: PerformanceProfile): String = when (tier) {
    PerformanceProfile.LOW -> "Nur das Nötigste: keine Animationen, kein Glas, kleinere Vorschaubilder, geringster Speicher- & Akkuverbrauch."
    PerformanceProfile.MEDIUM -> "Ausgewogen: reduzierte Animationen, undurchsichtige statt gläserne Leisten, gute Bildqualität."
    PerformanceProfile.HIGH -> "Echtes Glas mit Hintergrundunschärfe, flüssige Animationen, hohe Bildqualität."
    PerformanceProfile.ULTRA -> "Die volle Effektpalette: Glas, bewegte Wellen, längste Animationen, höchste Bildqualität und größte Caches."
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    return java.util.Locale.getDefault().let { locale -> String.format(locale, "%.1f GB", gb) }
}
