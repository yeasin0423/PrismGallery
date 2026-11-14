package com.gallery.app.ui.editor

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.app.ui.i18n.strings
import com.gallery.app.ui.theme.LocalAppTheme
import com.gallery.app.ui.theme.onAccent
import kotlin.math.min
import kotlin.math.roundToInt

/** The four bottom sections of the editor. Crop keeps the existing rotate/mirror/ratio tools. */
internal enum class EditorTab { Crop, Adjust, Filters, Draw }

@Composable
internal fun EditorTabs(selected: EditorTab, enabled: Boolean, onSelect: (EditorTab) -> Unit) {
    val palette = LocalAppTheme.current.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(CircleShape)
            .background(palette.surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        EditorTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (isSelected) palette.accent else Color.Transparent)
                    .clickable(enabled = enabled) { onSelect(tab) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tabLabel(tab),
                    color = if (isSelected) palette.onAccent else palette.onBackground,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

private fun tabLabel(tab: EditorTab): String = when (tab) {
    EditorTab.Crop -> strings.crop
    EditorTab.Adjust -> strings.adjust
    EditorTab.Filters -> strings.filters
    EditorTab.Draw -> strings.draw
}

/** One slider plus a scrollable row of the eight adjustment names. */
@Composable
internal fun AdjustPanel(
    adjustments: EditorAdjustments,
    selected: EditorAdjustment,
    enabled: Boolean,
    onSelect: (EditorAdjustment) -> Unit,
    onChange: (EditorAdjustment, Float) -> Unit,
    onChangeFinished: () -> Unit,
    onNudge: (EditorAdjustment, Float) -> Unit
) {
    val palette = LocalAppTheme.current.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(top = 8.dp, bottom = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = adjustmentLabel(selected),
                color = palette.onBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = adjustmentValue(adjustments.valueOf(selected)),
                color = palette.textMuted,
                fontSize = 13.sp
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AdjustmentStepButton(
                icon = Icons.Rounded.Remove,
                contentDescription = strings.decrease,
                enabled = enabled,
                onClick = { onNudge(selected, -AdjustmentStep) }
            )
            Slider(
                value = adjustments.valueOf(selected),
                onValueChange = { onChange(selected, it) },
                onValueChangeFinished = onChangeFinished,
                valueRange = -1f..1f,
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = palette.accent,
                    activeTrackColor = palette.accent,
                    inactiveTrackColor = palette.surfaceVariant,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                ),
                modifier = Modifier.weight(1f)
            )
            AdjustmentStepButton(
                icon = Icons.Rounded.Add,
                contentDescription = strings.increase,
                enabled = enabled,
                onClick = { onNudge(selected, AdjustmentStep) }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorAdjustment.entries.forEach { adjustment ->
                AdjustmentChip(
                    label = adjustmentLabel(adjustment),
                    selected = adjustment == selected,
                    onClick = { onSelect(adjustment) }
                )
            }
        }
    }
}

/** How much the +/- buttons move a slider; small enough to fine-tune and big enough to feel. */
private const val AdjustmentStep = 0.05f

@Composable
private fun AdjustmentStepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalAppTheme.current.palette
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(palette.surfaceVariant)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = palette.accent),
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) palette.onBackground else palette.textMuted,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun AdjustmentChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalAppTheme.current.palette
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) palette.accent else palette.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) palette.onAccent else palette.onBackground,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

private fun adjustmentLabel(adjustment: EditorAdjustment): String = when (adjustment) {
    EditorAdjustment.Brightness -> strings.brightness
    EditorAdjustment.Contrast -> strings.contrast
    EditorAdjustment.Saturation -> strings.saturation
    EditorAdjustment.Warmth -> strings.warmth
    EditorAdjustment.Tint -> strings.tint
    EditorAdjustment.BlackPoint -> strings.blackPoint
    EditorAdjustment.WhitePoint -> strings.whitePoint
    EditorAdjustment.Highlights -> strings.highlights
}

private fun adjustmentValue(value: Float): String {
    val percent = (value * 100f).roundToInt()
    return if (percent > 0) "+$percent" else percent.toString()
}

/** The 21 presets as live thumbnails; each thumbnail already includes the current adjustments. */
@Composable
internal fun FilterPanel(
    preview: ImageBitmap,
    filters: List<EditorFilter>,
    selected: EditorFilter,
    enabled: Boolean,
    matrixFor: (EditorFilter) -> FloatArray,
    onSelect: (EditorFilter) -> Unit
) {
    val palette = LocalAppTheme.current.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        filters.forEach { filter ->
            val isSelected = filter.id == selected.id
            Column(
                modifier = Modifier
                    .width(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = enabled) { onSelect(filter) }
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Canvas(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .then(
                            if (isSelected) {
                                Modifier.border(2.dp, palette.accent, RoundedCornerShape(13.dp))
                            } else {
                                Modifier
                            }
                        )
                ) {
                    val side = min(preview.width, preview.height)
                    drawImage(
                        image = preview,
                        srcOffset = IntOffset((preview.width - side) / 2, (preview.height - side) / 2),
                        srcSize = IntSize(side, side),
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                        colorFilter = ColorFilter.colorMatrix(ColorMatrix(values = matrixFor(filter)))
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = filter.name,
                    color = if (isSelected) palette.accent else palette.onBackground,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

/** Colour swatches and a brush-size slider for the freehand drawing tab. */
@Composable
internal fun DrawPanel(
    color: Color,
    brushWidth: Float,
    enabled: Boolean,
    onSelectColor: (Color) -> Unit,
    onBrushWidthChange: (Float) -> Unit
) {
    val palette = LocalAppTheme.current.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(top = 8.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorDrawColors.forEach { swatch ->
                val selected = swatch == color
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (selected) palette.accent else Color.Transparent)
                        .clickable(enabled = enabled) { onSelectColor(swatch) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (selected) 28.dp else 32.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .border(1.dp, palette.onBackground.copy(alpha = 0.16f), CircleShape)
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.brushSize,
                color = palette.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            // A dot that shows the current brush thickness.
            Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size((brushWidth / MaxDrawWidth * 18f).coerceIn(4f, 18f).dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, palette.onBackground.copy(alpha = 0.2f), CircleShape)
                )
            }
        }
        Slider(
            value = brushWidth,
            onValueChange = onBrushWidthChange,
            valueRange = MinDrawWidth..MaxDrawWidth,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = palette.accent,
                activeTrackColor = palette.accent,
                inactiveTrackColor = palette.surfaceVariant,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            ),
            modifier = Modifier.padding(horizontal = 14.dp)
        )
    }
}
