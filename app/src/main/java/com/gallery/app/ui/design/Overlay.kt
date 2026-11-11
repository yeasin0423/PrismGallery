package com.gallery.app.ui.design

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.app.ui.theme.AppColorPalette
import com.gallery.app.ui.theme.LocalAppTheme
import com.gallery.app.ui.theme.destructive
import com.gallery.app.ui.theme.frost
import com.gallery.app.ui.theme.isLight
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/*
 * Dialogs, sheets and popup menus are rendered INSIDE the activity window by an [OverlayHost] rather
 * than as separate platform windows. A separate window can only get a blurred backdrop through
 * FLAG_BLUR_BEHIND, which many devices (including the Galaxy A15) do not support - rendered in-window
 * they can blur the real app content with Haze on every API 31+ device.
 */

@Stable
class OverlayHostState {
    internal val entries = mutableStateListOf<OverlayEntry>()
}

internal class OverlayEntry(content: @Composable (Transition<Boolean>) -> Unit) {
    val id: Long = nextOverlayId++
    var content by mutableStateOf(content)
    val visibility = MutableTransitionState(false).apply { targetState = true }
    var removed by mutableStateOf(false)
}

private var nextOverlayId = 0L

val LocalOverlayHost = staticCompositionLocalOf<OverlayHostState?> { null }

/** Renders every open overlay. Place it last inside a full-screen Box so it draws above the app. */
@Composable
fun OverlayHost(state: OverlayHostState) {
    state.entries.toList().forEach { entry ->
        key(entry.id) {
            val transition = rememberTransition(entry.visibility, label = "overlay")
            entry.content(transition)
            LaunchedEffect(entry.removed) {
                if (entry.removed) {
                    // Keep the entry composed until its exit animation has fully played.
                    snapshotFlow { entry.visibility.isIdle && !entry.visibility.currentState }.first { it }
                    state.entries.remove(entry)
                }
            }
        }
    }
}

/**
 * Shows [content] in the overlay host for as long as this call stays in composition, then plays the
 * exit transition. [content] receives the visibility transition to drive its own animation.
 */
@Composable
fun OverlayWindow(content: @Composable (Transition<Boolean>) -> Unit) {
    val host = LocalOverlayHost.current ?: error("OverlayWindow needs an OverlayHost higher up")
    val entry = remember { OverlayEntry(content) }
    SideEffect { entry.content = content }
    DisposableEffect(host, entry) {
        host.entries.add(entry)
        onDispose {
            entry.removed = true
            entry.visibility.targetState = false
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Scrim
// ---------------------------------------------------------------------------------------------

@Composable
private fun OverlayScrim(
    transition: Transition<Boolean>,
    onTap: (() -> Unit)?,
    blur: Boolean
) {
    val theme = LocalAppTheme.current
    val alpha by transition.animateFloat(
        transitionSpec = { tween(if (targetState) 240 else 180) },
        label = "scrim_alpha"
    ) { if (it) 1f else 0f }
    val rootHaze = LocalRootHaze.current
    val dim = Color.Black.copy(alpha = 0.45f)
    val useBlur = blur && glassEnabled()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .then(
                if (useBlur && rootHaze != null) {
                    Modifier.hazeChild(
                        rootHaze,
                        HazeStyle(
                            backgroundColor = theme.palette.background,
                            tint = HazeTint(dim),
                            blurRadius = 18.dp,
                            fallbackTint = HazeTint(dim)
                        )
                    )
                } else {
                    Modifier.background(dim)
                }
            )
            .then(
                // Block touches to the app underneath while open; stop blocking during the exit.
                if (transition.targetState) {
                    Modifier.pointerInput(onTap) { detectTapGestures { onTap?.invoke() } }
                } else {
                    Modifier
                }
            )
    )
}

// ---------------------------------------------------------------------------------------------
// Dialog
// ---------------------------------------------------------------------------------------------

enum class DialogButtonStyle { Default, Primary, Destructive, Cancel }

data class DialogButton(
    val text: String,
    val style: DialogButtonStyle = DialogButtonStyle.Default,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

/**
 * A One UI dialog: frosted dark card, start-aligned bold title, split text buttons at the bottom.
 *
 * Pass [buttons] in visual order, dismiss action first and confirm action last.
 */
@Composable
fun ThemedDialog(
    onDismissRequest: () -> Unit,
    title: String? = null,
    message: String? = null,
    buttons: List<DialogButton> = emptyList(),
    dismissible: Boolean = true,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    OverlayWindow { transition ->
        DialogFrame(transition, onDismissRequest, title, message, buttons, dismissible, content)
    }
}

@Composable
private fun DialogFrame(
    transition: Transition<Boolean>,
    onDismissRequest: () -> Unit,
    title: String?,
    message: String?,
    buttons: List<DialogButton>,
    dismissible: Boolean,
    content: (@Composable ColumnScope.() -> Unit)?
) {
    val theme = LocalAppTheme.current
    val palette = theme.palette

    BackHandler(enabled = transition.targetState && dismissible) { onDismissRequest() }

    val progress by transition.animateFloat(
        transitionSpec = {
            if (targetState) spring(dampingRatio = 0.86f, stiffness = 520f) else tween(160)
        },
        label = "dialog_progress"
    ) { if (it) 1f else 0f }

    Box(modifier = Modifier.fillMaxSize()) {
        OverlayScrim(
            transition = transition,
            onTap = if (dismissible) onDismissRequest else null,
            blur = true
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            val shape = RoundedCornerShape(theme.shapes.dialogCornerRadius)
            Column(
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        val p = progress
                        alpha = p.coerceIn(0f, 1f)
                        val s = 0.92f + 0.08f * p
                        scaleX = s
                        scaleY = s
                    }
                    .dialogSurface(shape)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .padding(PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp)),
                horizontalAlignment = Alignment.Start
            ) {
                val textAlign = TextAlign.Start
                if (title != null) {
                    Text(
                        text = title,
                        color = palette.onBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp,
                        textAlign = textAlign,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (message != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        color = palette.onBackground.copy(alpha = 0.86f),
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        textAlign = textAlign,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (content != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    content()
                }
                if (buttons.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    DialogButtons(buttons)
                }
            }
        }
    }
}

@Composable
private fun Modifier.dialogSurface(shape: Shape): Modifier {
    val theme = LocalAppTheme.current
    val palette = theme.palette
    val rootHaze = LocalRootHaze.current
    return this.glass(
        shape,
        GlassSpec(palette.frost(Color(0xFF282828), 0.80f), palette.frost(Color(0xFF282828), 1f), 44.dp, GlassRim.Subtle),
        rootHaze
    )
}

private fun dialogButtonTextColor(button: DialogButton, palette: AppColorPalette): Color = when (button.style) {
    DialogButtonStyle.Primary -> palette.accent
    DialogButtonStyle.Destructive -> palette.destructive
    DialogButtonStyle.Default, DialogButtonStyle.Cancel -> palette.onBackground
}

@Composable
private fun DialogButtons(buttons: List<DialogButton>) {
    val palette = LocalAppTheme.current.palette
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        buttons.forEachIndexed { index, button ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(18.dp)
                        .background(palette.onBackground.copy(alpha = 0.16f))
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(enabled = button.enabled, onClick = button.onClick)
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                val color = dialogButtonTextColor(button, palette)
                Text(
                    text = button.text,
                    color = if (button.enabled) color else color.copy(alpha = 0.38f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Sheet
// ---------------------------------------------------------------------------------------------

/**
 * A One UI bottom sheet: a frosted dark card that slides up from the bottom edge. Dragging the header
 * down dismisses it. Scrollable content should use `Modifier.weight(1f, fill = false)`.
 */
@Composable
fun ThemedSheet(
    onDismissRequest: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    OverlayWindow { transition ->
        SheetFrame(transition, onDismissRequest, title, content)
    }
}

@Composable
private fun SheetFrame(
    transition: Transition<Boolean>,
    onDismissRequest: () -> Unit,
    title: String?,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalAppTheme.current

    BackHandler(enabled = transition.targetState) { onDismissRequest() }

    val progress by transition.animateFloat(
        transitionSpec = {
            if (targetState) spring(dampingRatio = 1f, stiffness = 360f) else tween(210)
        },
        label = "sheet_progress"
    ) { if (it) 1f else 0f }

    var dragOffset by remember { mutableFloatStateOf(0f) }
    val dismissThresholdPx = with(LocalDensity.current) { 120.dp.toPx() }
    val dragState = rememberDraggableState { delta -> dragOffset = (dragOffset + delta).coerceAtLeast(0f) }
    val dragScope = rememberCoroutineScope()

    fun settleDrag(velocity: Float) {
        if (dragOffset > dismissThresholdPx || velocity > 2000f) {
            onDismissRequest()
        } else {
            dragScope.launch {
                animate(dragOffset, 0f, animationSpec = spring(stiffness = 500f)) { value, _ -> dragOffset = value }
            }
        }
    }

    // The sheet can be pulled down from anywhere: non-scrolling content drags it directly (draggable on
    // the whole panel), scrolling content hands over the rest of a downward drag once it is at its top.
    val sheetNestedScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f && dragOffset > 0f) {
                    val consumed = maxOf(available.y, -dragOffset)
                    dragOffset += consumed
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f && source == NestedScrollSource.UserInput) {
                    dragOffset += available.y
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (dragOffset > 0f) {
                    settleDrag(available.y)
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OverlayScrim(transition, onDismissRequest, blur = true)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            val corner = theme.shapes.dialogCornerRadius
            val shape = RoundedCornerShape(topStart = corner, topEnd = corner)
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .heightIn(max = maxHeight * 0.92f)
                    .graphicsLayer { translationY = (1f - progress) * size.height + dragOffset }
                    .sheetSurface(shape)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .nestedScroll(sheetNestedScroll)
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity -> settleDrag(velocity) }
                    )
                    .navigationBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SheetHeader(title, onDismissRequest)
                }
                content()
            }
        }
    }
}

@Composable
private fun Modifier.sheetSurface(shape: Shape): Modifier {
    val theme = LocalAppTheme.current
    val palette = theme.palette
    val rootHaze = LocalRootHaze.current
    return this.glass(
        shape,
        GlassSpec(palette.frost(Color(0xFF1C1C1C), 0.84f), palette.frost(Color(0xFF1C1C1C), 1f), 44.dp, GlassRim.Subtle),
        rootHaze
    )
}

@Composable
private fun SheetHeader(title: String?, onDismissRequest: () -> Unit) {
    val palette = LocalAppTheme.current.palette

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(CircleShape)
                .background(palette.onBackground.copy(alpha = 0.22f))
        )
    }

    if (title == null) {
        Spacer(modifier = Modifier.height(12.dp))
        return
    }

    Text(
        text = title,
        color = palette.onBackground,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 12.dp)
    )
}

// ---------------------------------------------------------------------------------------------
// Popup menu
// ---------------------------------------------------------------------------------------------

data class MenuEntry(
    val text: String,
    val icon: ImageVector? = null,
    val checked: Boolean = false,
    val destructive: Boolean = false,
    val onClick: () -> Unit
)

/**
 * A popup menu anchored below [anchorBounds] (root coordinates, right-aligned to the anchor), drawn as
 * a solid rounded One UI card.
 */
@Composable
fun ThemedPopupMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    anchorBounds: Rect?,
    entries: List<MenuEntry>
) {
    if (!expanded) return
    OverlayWindow { transition ->
        PopupMenuFrame(transition, onDismissRequest, anchorBounds, entries)
    }
}

@Composable
private fun PopupMenuFrame(
    transition: Transition<Boolean>,
    onDismissRequest: () -> Unit,
    anchorBounds: Rect?,
    entries: List<MenuEntry>
) {
    val palette = LocalAppTheme.current.palette

    BackHandler(enabled = transition.targetState) { onDismissRequest() }

    val progress by transition.animateFloat(
        transitionSpec = {
            if (targetState) spring(dampingRatio = 0.85f, stiffness = 800f) else tween(130)
        },
        label = "menu_progress"
    ) { if (it) 1f else 0f }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (transition.targetState) Modifier.pointerInput(Unit) { detectTapGestures { onDismissRequest() } } else Modifier
            )
    ) {
        Layout(
            modifier = Modifier.fillMaxSize(),
            content = {
                val shape = RoundedCornerShape(26.dp)
                val popIn = Modifier.graphicsLayer {
                    val p = progress
                    alpha = p.coerceIn(0f, 1f)
                    val s = 0.82f + 0.18f * p
                    scaleX = s
                    scaleY = s
                    transformOrigin = TransformOrigin(1f, 0f)
                }
                Column(
                    modifier = popIn
                        .widthIn(min = 196.dp, max = 290.dp)
                        .width(IntrinsicSize.Max)
                        .menuSurface(shape)
                        .padding(vertical = 6.dp)
                ) {
                    val anyChecked = entries.any { it.checked }
                    entries.forEach { entry ->
                        MenuRow(entry, anyChecked) {
                            onDismissRequest()
                            entry.onClick()
                        }
                    }
                }
            }
        ) { measurables, constraints ->
            val placeable = measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
            val margin = 12.dp.roundToPx()
            val gap = 6.dp.roundToPx()
            val anchor = anchorBounds ?: Rect(
                constraints.maxWidth.toFloat() - margin, 0f, constraints.maxWidth.toFloat() - margin, 0f
            )
            val x = (anchor.right.roundToInt() - placeable.width)
                .coerceIn(margin, (constraints.maxWidth - placeable.width - margin).coerceAtLeast(margin))
            var y = anchor.bottom.roundToInt() + gap
            if (y + placeable.height > constraints.maxHeight - margin) {
                y = (anchor.top.roundToInt() - placeable.height - gap).coerceAtLeast(margin)
            }
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeable.place(x, y)
            }
        }
    }
}

@Composable
private fun Modifier.menuSurface(shape: Shape): Modifier {
    val palette = LocalAppTheme.current.palette
    // One UI popups are a solid grey card.
    return this
        .shadow(16.dp, shape, ambientColor = Color.Black.copy(alpha = 0.3f), spotColor = Color.Black.copy(alpha = 0.3f))
        .clip(shape)
        .background(if (palette.isLight) Color.White else Color(0xFF3A3A3C))
}

@Composable
private fun MenuRow(entry: MenuEntry, anyChecked: Boolean, onClick: () -> Unit) {
    val palette = LocalAppTheme.current.palette
    val textColor = when {
        entry.destructive -> palette.destructive
        entry.checked -> palette.accent
        else -> palette.onBackground
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 30.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            entry.text,
            color = textColor,
            fontSize = 19.sp,
            fontWeight = if (entry.checked) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
    }
}
