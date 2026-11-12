package com.gallery.app.ui.screens
import com.gallery.app.ui.i18n.LanguageState
import com.gallery.app.ui.i18n.strings

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import com.gallery.app.util.WallpaperAccents
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.app.PerformanceProfile
import com.gallery.app.ui.components.HsvColorPicker
import com.gallery.app.ui.design.DialogButton
import com.gallery.app.ui.design.DialogButtonStyle
import com.gallery.app.ui.design.HazeSource
import com.gallery.app.ui.design.ThemedDialog
import com.gallery.app.ui.design.ThemedTopBar
import com.gallery.app.ui.design.bottomBarContentPadding
import com.gallery.app.ui.design.selectionPop
import com.gallery.app.ui.design.pushSpec
import com.gallery.app.ui.design.pushTransition
import com.gallery.app.ui.design.screenBackground
import com.gallery.app.ui.design.blockTouchesBelow
import com.gallery.app.ui.theme.AppColorPalette
import com.gallery.app.ui.theme.CUSTOM_PALETTE_ID
import com.gallery.app.ui.theme.DesignLanguage
import com.gallery.app.ui.theme.LocalAppTheme
import com.gallery.app.ui.theme.ThemeState
import com.gallery.app.ui.theme.isLight
import com.gallery.app.ui.theme.presetsFor
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.flow.first

/** The app's full name, shown on the info pages; the launcher label (launcher_name) is "Gallery". */
internal const val APP_FULL_NAME = "YS Gallery"

/** Sections of the settings sheet; every menu entry opens the sheet scrolled to its own section. */
enum class SettingsSection { DESIGN, SETTINGS, STORAGE, INFO }

/**
 * The One UI settings hub, pushed from the ⋮ menu: a compact top bar titled "Settings", then grouped
 * cards with coloured circle icons.
 *
 * Appearance, General, Storage and Info each open as their own pushed page.
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
    modifier: Modifier = Modifier,
    // Settings pages
    loadStorageStats: suspend () -> com.gallery.app.data.MediaRepository.MediaStorageStats? = { null },
    onEmptyTrash: () -> Unit = {},
    onOpenLibrary: () -> Unit = {},
    /** The settings hub is a pushed page without the tab bar, so it needs its own way back. */
    onBack: () -> Unit = {}
) {
    val theme = LocalAppTheme.current
    val palette = theme.palette

    val storageText = if (internalTotalBytes > 0) {
        strings.freeOf(formatBytes(internalFreeBytes), formatBytes(internalTotalBytes))
    } else {
        strings.unknown
    }
    // Each entry is pushed as its own page, like Samsung Settings.
    var page by remember { mutableStateOf<SettingsSection?>(null) }
    val openSection: (SettingsSection) -> Unit = { page = it }
    val openDesign = { openSection(SettingsSection.DESIGN) }
    val openSettings = { openSection(SettingsSection.SETTINGS) }
    val openStorage = { openSection(SettingsSection.STORAGE) }
    val openInfo = { openSection(SettingsSection.INFO) }
    // The last page stays shown while it slides back out.
    val lastPage = remember { arrayOf(SettingsSection.DESIGN) }
    page?.let { lastPage[0] = it }
    val pageProgress = androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (page != null) 1f else 0f,
        animationSpec = pushSpec(),
        label = "settings_page_push"
    )
    val pageShown by remember { androidx.compose.runtime.derivedStateOf { pageProgress.value > 0.001f } }
    // Composed only while a page is open: a handler added later takes priority over the tab's own back handler,
    // which merely enabling an always-present one would not.
    if (page != null) {
        androidx.activity.compose.BackHandler { page = null }
    }

    val menuScroll = rememberScrollState()
    // Backdrop for the pinned title bar, which blurs the list scrolling underneath it.
    val menuHaze = remember { HazeState() }

    Box(modifier = modifier.fillMaxSize()) {
        HazeSource(menuHaze, Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(menuScroll)
            ) {
                // Pinned compact bar: the title sits at the very top, like Samsung Settings.
                Spacer(modifier = Modifier.statusBarsPadding().height(64.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = bottomBarContentPadding(24.dp)),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Samsung Settings' grouped cards with coloured circle icons.
                    OneUiMenuContent(
                        favoriteCount = favoriteCount,
                        trashedCount = trashedCount,
                        storageValue = storageText,
                        appVersionName = appVersionName,
                        onOpenFavorites = onOpenFavorites,
                        onOpenTrash = onOpenTrash,
                        onOpenDesign = openDesign,
                        onOpenSettings = openSettings,
                        onOpenStorage = openStorage,
                        onOpenInfo = openInfo
                    )
                }
            }
        }

        ThemedTopBar(
            title = strings.settings,
            onBack = onBack,
            hazeState = menuHaze,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    if (page != null || pageShown) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pushTransition({ pageProgress.value }, isTop = true)
                .screenBackground()
                .blockTouchesBelow()
        ) {
            OneUiSettingsPage(
                section = lastPage[0],
                onBack = { page = null },
                internalFreeBytes = internalFreeBytes,
                internalTotalBytes = internalTotalBytes,
                sdCardFreeBytes = sdCardFreeBytes,
                sdCardTotalBytes = sdCardTotalBytes,
                appVersionName = appVersionName,
                trashedCount = trashedCount,
                graphicsQuality = graphicsQuality,
                onSetGraphicsQuality = onSetGraphicsQuality,
                loadStorageStats = loadStorageStats,
                onEmptyTrash = onEmptyTrash,
                onOpenLibrary = onOpenLibrary,
                onOpenTrash = onOpenTrash
            )
        }
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


/** Accent swatches for the active mode, plus a trailing "custom" swatch. */
@Composable
internal fun PaletteRow(
    presets: List<AppColorPalette>,
    currentPaletteId: String,
    onSelectPreset: (AppColorPalette) -> Unit,
    onOpenCustomEditor: () -> Unit
) {
    val theme = LocalAppTheme.current
    val palette = theme.palette
    val swatchShape = CircleShape
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        presets.forEach { preset ->
            val isSelected = preset.id == currentPaletteId
            // Each swatch shows the preset's own background with its accent inside.
            Box(
                modifier = Modifier
                    .selectionPop(isSelected, fromScale = 0.72f, dampingRatio = 0.4f, stiffness = 600f)
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
        val context = LocalContext.current
        // A wallpaper colour is stored as a custom accent too; then its swatch below is the selected one.
        val isCustomSelected = currentPaletteId == CUSTOM_PALETTE_ID && pickedWallpaperAccent(context) == null
        Box(
            modifier = Modifier
                .selectionPop(isCustomSelected, fromScale = 0.72f, dampingRatio = 0.4f, stiffness = 600f)
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
                Icon(Icons.Rounded.Add, strings.customColor, tint = palette.textMuted, modifier = Modifier.size(20.dp))
            }
        }
    }
    WallpaperAccentRow(currentPaletteId)
    }
}

@Composable
internal fun CustomAccentDialog(
    lightMode: Boolean,
    initialAccent: Color,
    onConfirm: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var pickedColor by remember { mutableStateOf(initialAccent) }
    ThemedDialog(
        onDismissRequest = onDismiss,
        title = strings.customAccentTitle,
        message = strings.customAccentMessage(lightMode, DesignLanguage.SAMSUNG_ONEUI.displayName),
        buttons = listOf(
            DialogButton(strings.cancel, DialogButtonStyle.Cancel, onClick = onDismiss),
            DialogButton(strings.apply, DialogButtonStyle.Primary) { onConfirm(pickedColor) }
        )
    ) {
        HsvColorPicker(
            initialColor = initialAccent,
            onColorChanged = { pickedColor = it }
        )
    }
}

/** Switches the active palette between light and dark, keeping the accent slot (blue stays blue) or custom accent. */
internal fun switchPaletteBrightness(context: android.content.Context, light: Boolean) {
    val currentLanguage = ThemeState.designLanguage
    val currentPalette = ThemeState.palette
    if (currentPalette.isLight == light) return
    val target = presetsFor(currentLanguage, light)
    val next = if (currentPalette.id == CUSTOM_PALETTE_ID) {
        target.first().copy(id = CUSTOM_PALETTE_ID, label = "Custom palette", accent = currentPalette.accent)
    } else {
        val slot = presetsFor(currentLanguage, currentPalette.isLight).indexOfFirst { it.id == currentPalette.id }
        target.getOrElse(slot) { target.first() }
    }
    ThemeState.apply(next, context = context)
}

/** Applies a custom accent on the active mode's base colors - light stays light. */
internal fun applyCustomAccent(context: android.content.Context, lightMode: Boolean, accent: Color) {
    val base = presetsFor(DesignLanguage.SAMSUNG_ONEUI, lightMode).first()
    val custom = base.copy(id = CUSTOM_PALETTE_ID, label = "Custom palette", accent = accent)
    ThemeState.apply(custom, context = context)
    wallpaperAccentPrefs(context).edit().remove(KEY_WALLPAPER_ACCENT).apply()
}

private const val KEY_WALLPAPER_ACCENT = "wallpaper_accent"

private fun wallpaperAccentPrefs(context: android.content.Context) =
    context.getSharedPreferences("prism_wallpaper_accent", android.content.Context.MODE_PRIVATE)

/** The wallpaper colour the current custom accent was taken from, or null if it was picked by hand. */
internal fun pickedWallpaperAccent(context: android.content.Context): Color? =
    wallpaperAccentPrefs(context).takeIf { it.contains(KEY_WALLPAPER_ACCENT) }
        ?.let { Color(it.getInt(KEY_WALLPAPER_ACCENT, 0)) }

/** Takes one of the wallpaper's colours as the accent (stored as a custom accent, remembered as from the wallpaper). */
internal fun applyWallpaperAccent(context: android.content.Context, lightMode: Boolean, color: Color) {
    applyCustomAccent(context, lightMode, color)
    wallpaperAccentPrefs(context).edit().putInt(KEY_WALLPAPER_ACCENT, color.toArgb()).apply()
}

/**
 * The colours Android took from the home-screen wallpaper, as accent swatches (as in Prism Camera).
 * Left out entirely when the phone reports none.
 */
@Composable
private fun WallpaperAccentRow(currentPaletteId: String) {
    val context = LocalContext.current
    val palette = LocalAppTheme.current.palette
    val lightMode = palette.isLight
    val colors = remember(lightMode) { WallpaperAccents.read(context, lightMode) }
    if (colors.isEmpty()) return
    val picked = if (currentPaletteId == CUSTOM_PALETTE_ID) pickedWallpaperAccent(context) else null
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(strings.fromWallpaper, color = palette.textMuted, fontSize = 14.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            colors.forEach { color ->
                val isSelected = picked == color
                Box(
                    modifier = Modifier
                        .selectionPop(isSelected, fromScale = 0.72f, dampingRatio = 0.4f, stiffness = 600f)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(palette.background)
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) palette.accent else palette.onBackground.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                        .clickable { applyWallpaperAccent(context, lightMode, color) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }
    }
}

internal fun graphicsQualityLabel(tier: PerformanceProfile): String = when (tier) {
    PerformanceProfile.LOW -> "Low"
    PerformanceProfile.MEDIUM -> "Medium"
    PerformanceProfile.HIGH -> "High"
    PerformanceProfile.ULTRA -> "Ultra"
}

private fun graphicsQualityDescription(tier: PerformanceProfile): String = strings.qualityDescription(tier)

private fun formatBytes(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    // App language decides the decimal separator (1,5 GB / 1.5 GB), like every other text.
    return String.format(LanguageState.locale, "%.1f GB", gb)
}
