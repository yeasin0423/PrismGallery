package com.gallery.app.ui.design
import com.gallery.app.ui.i18n.strings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.app.ui.theme.LocalAppTheme
import com.gallery.app.ui.theme.destructive
import com.gallery.app.ui.theme.isLight
import com.gallery.app.ui.theme.onAccent
import dev.chrisbanes.haze.HazeState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlin.math.min

// ---------------------------------------------------------------------------------------------
// Progress bar
// ---------------------------------------------------------------------------------------------

/**
 * Determinate progress: One UI's thick rounded bar in the accent color.
 */
@Composable
fun ThemedProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val palette = LocalAppTheme.current.palette
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 140f),
        label = "progress"
    )

    LinearBar(
        fraction = animated,
        height = 8.dp,
        track = palette.surfaceVariant,
        fill = Brush.horizontalGradient(listOf(palette.accent, palette.accent)),
        modifier = modifier
    )
}

@Composable
private fun LinearBar(
    fraction: Float,
    height: androidx.compose.ui.unit.Dp,
    track: Color,
    fill: Brush,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(track)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0.001f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(fill)
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Segmented control
// ---------------------------------------------------------------------------------------------

/**
 * A single-choice control: a sliding thumb inside a rounded track, like One UI's settings toggles.
 */
@Composable
fun <T> ThemedSegmentedControl(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppTheme.current.palette

    val trackHeight = 46.dp
    val trackPadding = 4.dp
    val trackShape = CircleShape
    val thumbShape = CircleShape
    val trackModifier = Modifier.clip(trackShape).background(palette.surfaceVariant)

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val trackPaddingPx = with(density) { trackPadding.toPx() }
    val latestOptions by rememberUpdatedState(options)
    val latestSelected by rememberUpdatedState(selected)
    val latestOnSelect by rememberUpdatedState(onSelect)
    // Hold or drag on the control: the thumb follows the finger and the hovered segment is selected live.
    var dragX by remember { mutableStateOf<Float?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
            .then(trackModifier)
            // The slide gesture lives on the track itself, the parent of the segments, so it sees every
            // touch. Once it becomes a slide it consumes the moves, which cancels the segment taps.
            .pointerInput(trackPaddingPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downTime = System.currentTimeMillis()
                    var dragging = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        if (!dragging) {
                            // A surrounding vertical scroll took the gesture: leave it alone.
                            if (change.isConsumed ||
                                abs(change.position.y - down.position.y) > viewConfiguration.touchSlop * 1.5f
                            ) break
                            if (abs(change.position.x - down.position.x) > viewConfiguration.touchSlop ||
                                System.currentTimeMillis() - downTime > 300
                            ) {
                                dragging = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                        if (dragging) {
                            change.consume()
                            dragX = change.position.x
                            val count = latestOptions.size.coerceAtLeast(1)
                            val segmentPx = (size.width - trackPaddingPx * 2) / count
                            val hovered = ((change.position.x - trackPaddingPx) / segmentPx).toInt().coerceIn(0, count - 1)
                            val option = latestOptions[hovered]
                            if (option != latestSelected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                latestOnSelect(option)
                            }
                        }
                    }
                    dragX = null
                }
            }
    ) {
        val count = options.size.coerceAtLeast(1)
        val segmentWidth = (maxWidth - trackPadding * 2) / count
        val index = options.indexOf(selected).coerceAtLeast(0)
        val segmentWidthPx = with(density) { segmentWidth.toPx() }
        val fingerOffset = dragX?.let {
            with(density) { (it - trackPaddingPx - segmentWidthPx / 2f).coerceIn(0f, segmentWidthPx * (count - 1)).toDp() }
        }
        val thumbOffset by animateDpAsState(
            targetValue = fingerOffset ?: (segmentWidth * index),
            animationSpec = if (fingerOffset != null) snap() else tween(260, easing = FastOutSlowInEasing),
            label = "segment_thumb"
        )
        val thumbScale by animateFloatAsState(
            targetValue = if (dragX != null) 1.04f else 1f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
            label = "segment_thumb_scale"
        )

        Box(
            modifier = Modifier
                .padding(trackPadding)
                .offset(x = thumbOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .clip(thumbShape)
                .background(if (palette.isLight) palette.surface else Color.White.copy(alpha = 0.16f))
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(trackPadding)
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()                        .clip(thumbShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(option) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label(option),
                        color = if (isSelected) palette.onBackground else palette.textMuted,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Lists & cards
// ---------------------------------------------------------------------------------------------

data class ListRow(
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector? = null,
    /** Brand color of this row's icon (a flat tint unless overridden). */
    val iconColor: Color = Color.Unspecified,
    val value: String? = null,
    val showChevron: Boolean = true,
    val onClick: (() -> Unit)? = null,
    /** Overrides the title color, e.g. [com.gallery.app.ui.theme.destructive] for delete rows. */
    val titleColor: Color = Color.Unspecified
)

@Composable
fun ThemedSectionHeader(text: String, modifier: Modifier = Modifier) {
    val palette = LocalAppTheme.current.palette
    Text(
        text = text,
        color = palette.textMuted,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier.padding(start = 24.dp, bottom = 8.dp, top = 4.dp)
    )
}

/** The grouped container for settings-style lists. */
@Composable
fun ThemedListSection(
    rows: List<ListRow>,
    modifier: Modifier = Modifier,
    header: String? = null,
    /** Use one step lighter containers when the list sits on a sheet or dialog surface. */
    inSheet: Boolean = false
) {
    val theme = LocalAppTheme.current
    val palette = theme.palette

    Column(modifier = modifier.fillMaxWidth()) {
        if (header != null) ThemedSectionHeader(header)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(theme.shapes.cardCornerRadius))
                .background(if (inSheet) palette.surfaceVariant else palette.surface)
        ) {
            rows.forEachIndexed { index, row ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .padding(start = if (row.icon != null) 64.dp else 22.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(palette.onBackground.copy(alpha = 0.07f))
                    )
                }
                ListRowContent(row)
            }
        }
    }
}

@Composable
private fun ListRowContent(row: ListRow) {
    val palette = LocalAppTheme.current.palette
    val iconColor = if (row.iconColor == Color.Unspecified) palette.accent else row.iconColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (row.onClick != null) Modifier.clickable(onClick = row.onClick) else Modifier)
            .heightIn(min = 60.dp)
            .padding(start = 22.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (row.icon != null) {
            Icon(row.icon, null, tint = iconColor, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.title,
                color = if (row.titleColor == Color.Unspecified) palette.onBackground else row.titleColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (row.subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = row.subtitle,
                    color = palette.accent,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (row.value != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = row.value,
                color = palette.textMuted,
                fontSize = 14.sp
            )
        }
    }
}

/** Plain grouped container in the card style, for custom content. */
@Composable
fun ThemedCard(
    modifier: Modifier = Modifier,
    inSheet: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalAppTheme.current
    val palette = theme.palette
    val shape = RoundedCornerShape(theme.shapes.cardCornerRadius)
    val base = if (inSheet) palette.surfaceVariant else palette.surface
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(base)
            .padding(22.dp),
        content = content
    )
}

// ---------------------------------------------------------------------------------------------
// Buttons, fields, indicators
// ---------------------------------------------------------------------------------------------

/** Full-size action button: a filled accent pill or a calm surface pill. */
@Composable
fun ThemedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: DialogButtonStyle = DialogButtonStyle.Primary,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val palette = LocalAppTheme.current.palette
    val interaction = remember { MutableInteractionSource() }

    val filled = style == DialogButtonStyle.Primary
    val container = if (filled) palette.accent else palette.surfaceVariant
    val content = when (style) {
        DialogButtonStyle.Primary -> palette.onAccent
        DialogButtonStyle.Destructive -> palette.destructive
        else -> palette.onBackground
    }
    val shape = RoundedCornerShape(26.dp)

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(if (enabled) container else container.copy(alpha = container.alpha * 0.4f))
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = if (enabled) content else content.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = if (enabled) content else content.copy(alpha = 0.5f),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ThemedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = false,
    /**
     * Above 1 a long value wraps onto further lines instead of scrolling sideways, so every position can
     * be reached by tapping or dragging the cursor handle. Line breaks are never entered.
     */
    maxLines: Int = 1
) {
    val palette = LocalAppTheme.current.palette
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    if (autoFocus) {
        LaunchedEffect(Unit) {
            delay(250)
            runCatching { focusRequester.requestFocus() }
        }
    }

    BasicTextField(
        value = value,
        onValueChange = { onValueChange(if (maxLines > 1) it.replace("\n", "") else it) },
        singleLine = maxLines == 1,
        maxLines = maxLines,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() }),
        textStyle = TextStyle(color = palette.onBackground, fontSize = 17.sp),
        cursorBrush = SolidColor(palette.accent),
        interactionSource = interaction,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        decorationBox = { inner ->
            val placeholderText: @Composable () -> Unit = {
                if (value.isEmpty()) Text(placeholder, color = palette.textMuted, fontSize = 17.sp, maxLines = 1)
            }
            Column {
                Box(modifier = Modifier.padding(vertical = 10.dp)) {
                    placeholderText()
                    inner()
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (focused) 2.dp else 1.dp)
                        .background(if (focused) palette.accent else palette.textMuted.copy(alpha = 0.5f))
                )
            }
        }
    )
}

/** Selection indicator for single-choice lists. */
@Composable
fun ThemedRadioIndicator(selected: Boolean, modifier: Modifier = Modifier) {
    val palette = LocalAppTheme.current.palette
    val dot by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(180),
        label = "radio_dot"
    )
    Box(
        modifier = modifier
            .size(22.dp)
            .border(2.dp, if (selected) palette.accent else palette.textMuted, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(11.dp)
                .graphicsLayer {
                    scaleX = dot
                    scaleY = dot
                }
                .clip(CircleShape)
                .background(palette.accent)
        )
    }
}

/** Plain toolbar icon button with a short tap pulse. */
@Composable
fun ThemedIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    enabled: Boolean = true,
    hazeState: HazeState? = LocalContentHaze.current
) {
    val palette = LocalAppTheme.current.palette
    val interaction = remember { MutableInteractionSource() }
    val resolvedTint = (if (tint == Color.Unspecified) palette.onBackground else tint)
        .let { if (enabled) it else it.copy(alpha = 0.35f) }

    // Tap pulse: a quick squeeze and springy bounce on every click, so even a fast tap is clearly
    // acknowledged (a pure press-scale only shows while the finger is held down).
    val pulse = remember { androidx.compose.animation.core.Animatable(1f) }
    val scope = rememberCoroutineScope()
    val pulseDepth = 0.82f
    val pulsingClick: () -> Unit = {
        scope.launch {
            pulse.animateTo(pulseDepth, tween(70))
            pulse.animateTo(1f, spring(dampingRatio = 0.38f, stiffness = 600f))
        }
        onClick()
    }
    val pulseModifier = Modifier.graphicsLayer {
        scaleX = pulse.value
        scaleY = pulse.value
    }

    Box(
        modifier = modifier
            .then(pulseModifier)
            .size(48.dp)
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = ripple(), enabled = enabled, onClick = pulsingClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription,
            tint = resolvedTint,
            modifier = Modifier.size(25.dp)
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Top bar
// ---------------------------------------------------------------------------------------------

/**
 * Compact top bar for sub-screens and scrolled states. Its background blurs [hazeState] (if given) and
 * fades with [backgroundAlpha] so it can appear only once content scrolls underneath.
 */
@Composable
fun ThemedTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    hazeState: HazeState? = null,
    backgroundAlpha: () -> Float = { 1f },
    /** Replaces the back arrow, e.g. with a close icon for selection mode. */
    navigationIcon: ImageVector? = null,
    /** Optional second row of compact actions under the title. */
    secondaryActions: (@Composable RowScope.() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val palette = LocalAppTheme.current.palette

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = backgroundAlpha() }
                .glass(
                    RectangleShape,
                    GlassSpec(palette.background.copy(alpha = 0.72f), palette.background.copy(alpha = 0.97f), 30.dp, GlassRim.None),
                    hazeState
                )
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(64.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    ThemedIconButton(
                        icon = navigationIcon ?: Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = strings.back,
                        onClick = onBack,
                        hazeState = hazeState
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                } else {
                    Spacer(modifier = Modifier.width(14.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = palette.onBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Text(subtitle, color = palette.textMuted, fontSize = 13.sp, maxLines = 1)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }

            if (secondaryActions != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    content = secondaryActions
                )
            }
        }
    }
}

/** Compact pill action for a top bar's secondary row. */
@Composable
fun ThemedChipAction(
    text: String,
    color: Color,
    onClick: () -> Unit,
    hazeState: HazeState? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = CircleShape
    Box(
        modifier = Modifier
            .pressScale(interaction, 0.93f)
            .height(34.dp)
            .clip(shape)
            .background(color.copy(alpha = 0.16f))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Centred empty state (no favorites, empty trash...). */
@Composable
fun ThemedEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppTheme.current.palette
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = palette.textMuted, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = title,
            color = palette.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = palette.textMuted,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center
        )
    }
}

/** Text used on small badges drawn straight on top of photos. */
val PhotoBadgeTextStyle = TextStyle(
    color = Color.White,
    fontSize = 12.sp,
    fontWeight = FontWeight.SemiBold,
    shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 1f), 6f)
)
