package com.gallery.app.ui.design

import android.os.Build
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.gallery.app.GalleryApplication
import com.gallery.app.PerformanceProfile
import com.gallery.app.ui.theme.DesignLanguage
import com.gallery.app.ui.theme.LocalAppTheme
import com.gallery.app.ui.theme.isLight
import kotlinx.coroutines.launch
import kotlin.math.abs

/*
 * Motion and touch effects of the four design languages: the animation clock, press feedback, and the
 * tab switch / push / media zoom transitions.
 */

/**
 * Animation clock scale for the whole app. Compose normally multiplies every animation by the system
 * "animator duration scale" developer option, so at 0x all motion is skipped. The app has its own
 * graphics quality setting instead: LOW turns animations off, every other tier plays them at full
 * length regardless of the system option. Installed through the activity's window recomposer.
 */
object AppMotionDurationScale : MotionDurationScale {
    override val scaleFactor: Float
        get() = if (GalleryApplication.performanceProfile == PerformanceProfile.LOW) 0f else 1f
}

private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val EmphasizedStandard = CubicBezierEasing(0.2f, 0f, 0f, 1f)

// ---------------------------------------------------------------------------------------------
// Press feedback
// ---------------------------------------------------------------------------------------------

/**
 * Press feedback for every clickable without an explicit indication (list rows, tiles, cards):
 * One UI and Material ripple, iOS a quick grey highlight like a table cell, HyperOS a light highlight
 * with a slight press-in.
 */
@Composable
fun rememberThemedIndication(): Indication {
    val theme = LocalAppTheme.current
    val palette = theme.palette
    return when (theme.language) {
        DesignLanguage.SAMSUNG_ONEUI -> ripple(color = palette.onBackground)
        DesignLanguage.GOOGLE_EXPRESSIVE -> ripple(color = palette.accent)
        DesignLanguage.IOS_LIQUID_GLASS -> remember(palette.isLight) {
            HighlightIndication(if (palette.isLight) Color.Black.copy(alpha = 0.09f) else Color.White.copy(alpha = 0.11f), 1f)
        }
        DesignLanguage.XIAOMI_HYPEROS -> remember(palette.isLight) {
            HighlightIndication(if (palette.isLight) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.08f), 0.975f)
        }
    }
}

private data class HighlightIndication(val color: Color, val pressedScale: Float) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        HighlightIndicationNode(interactionSource, color, pressedScale)
}

private class HighlightIndicationNode(
    private val interactionSource: InteractionSource,
    private val color: Color,
    private val pressedScale: Float
) : Modifier.Node(), DrawModifierNode {
    private val progress = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            var activePresses = 0
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> activePresses++
                    is PressInteraction.Release, is PressInteraction.Cancel ->
                        activePresses = (activePresses - 1).coerceAtLeast(0)
                    else -> return@collect
                }
                val pressed = activePresses > 0
                launch {
                    if (pressed) {
                        progress.animateTo(1f, tween(70)) { invalidateDraw() }
                    } else {
                        // A quick tap still flashes fully before fading out.
                        if (progress.value < 1f) progress.animateTo(1f, tween(50)) { invalidateDraw() }
                        progress.animateTo(0f, tween(280)) { invalidateDraw() }
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val p = progress.value
        if (pressedScale != 1f && p > 0f) {
            scale(1f + (pressedScale - 1f) * p) { this@draw.drawContent() }
        } else {
            drawContent()
        }
        if (p > 0f) drawRect(color.copy(alpha = color.alpha * p))
    }
}

/**
 * Pops the element when it becomes selected (not on first composition): scales from [fromScale] back
 * to 1 on a spring.
 */
@Composable
fun Modifier.selectionPop(
    selected: Boolean,
    fromScale: Float = 0.8f,
    dampingRatio: Float = 0.45f,
    stiffness: Float = 600f
): Modifier {
    val scale = remember { Animatable(1f) }
    val initialized = remember { booleanArrayOf(false) }
    LaunchedEffect(selected) {
        if (initialized[0] && selected) {
            scale.snapTo(fromScale)
            scale.animateTo(1f, spring(dampingRatio = dampingRatio, stiffness = stiffness))
        }
        initialized[0] = true
    }
    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/** Registers as a touch target without consuming anything, so siblings drawn below receive no touches. */
fun Modifier.blockTouchesBelow(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) awaitPointerEvent()
    }
}

// ---------------------------------------------------------------------------------------------
// Tab switch
// ---------------------------------------------------------------------------------------------

/** Animated switch between two values (e.g. tabs): which one is leaving, which is arriving, progress. */
@Stable
class SwitchTransitionState<T>(initial: T) {
    var from by mutableStateOf(initial)
        private set
    var to by mutableStateOf(initial)
        private set
    val progress = Animatable(1f)

    suspend fun animateTo(target: T, spec: AnimationSpec<Float>) {
        if (target == to) return
        from = to
        to = target
        progress.snapTo(0f)
        progress.animateTo(1f, spec)
    }
}

fun tabSwitchSpec(language: DesignLanguage): AnimationSpec<Float> = when (language) {
    DesignLanguage.SAMSUNG_ONEUI -> tween(380, easing = EmphasizedDecelerate)
    DesignLanguage.GOOGLE_EXPRESSIVE -> spring(dampingRatio = 0.8f, stiffness = 300f)
    DesignLanguage.IOS_LIQUID_GLASS -> spring(dampingRatio = 1f, stiffness = 360f)
    DesignLanguage.XIAOMI_HYPEROS -> tween(270, easing = EmphasizedStandard)
}

/**
 * Places a tab during a switch. [relative] is -1..1: 0 = fully shown, ±1 = fully gone towards that side.
 * One UI: short slide + fade. Material: fade through with a slight shrink. iOS: cross-dissolve with a
 * subtle zoom and (on ULTRA) a blur on the outgoing/incoming tab. HyperOS: wide, fast slide.
 */
fun GraphicsLayerScope.tabSwitchTransition(language: DesignLanguage, relative: Float, blur: Boolean) {
    val r = relative.coerceIn(-1f, 1f)
    val a = abs(r)
    when (language) {
        DesignLanguage.SAMSUNG_ONEUI -> {
            translationX = r * size.width * 0.16f
            alpha = (1f - a * 1.35f).coerceIn(0f, 1f)
        }
        DesignLanguage.GOOGLE_EXPRESSIVE -> {
            translationX = r * size.width * 0.08f
            alpha = (1f - a * 1.8f).coerceIn(0f, 1f)
            val s = 1f - 0.06f * a
            scaleX = s
            scaleY = s
        }
        DesignLanguage.IOS_LIQUID_GLASS -> {
            alpha = (1f - a).coerceIn(0f, 1f)
            val s = 1f - 0.035f * a
            scaleX = s
            scaleY = s
            renderEffect = if (blur && a > 0.01f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val radius = a * 26.dp.toPx()
                BlurEffect(radius, radius, TileMode.Decal)
            } else {
                null
            }
        }
        DesignLanguage.XIAOMI_HYPEROS -> {
            translationX = r * size.width * 0.3f
            alpha = (1f - a * 1.25f).coerceIn(0f, 1f)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Push (Favoriten, Papierkorb, albums)
// ---------------------------------------------------------------------------------------------

fun pushSpec(language: DesignLanguage): AnimationSpec<Float> = when (language) {
    DesignLanguage.SAMSUNG_ONEUI -> tween(400, easing = EmphasizedDecelerate)
    DesignLanguage.GOOGLE_EXPRESSIVE -> spring(dampingRatio = 0.84f, stiffness = 260f)
    DesignLanguage.IOS_LIQUID_GLASS -> spring(dampingRatio = 1f, stiffness = 250f)
    DesignLanguage.XIAOMI_HYPEROS -> tween(330, easing = EmphasizedStandard)
}

/**
 * Push navigation. [progress] 0 = the pushed page is off screen, 1 = it fully covers the page below.
 * The pushed page ([isTop]) arrives from [direction] (1 = from the right, -1 = from the left); the page
 * below reacts:
 * - One UI: short slide + fade; the page below fades out.
 * - Material: the page scales up out of the one below.
 * - iOS: full-width slide, the page below parallaxes left and dims.
 * - HyperOS: full-width slide, the page below shifts left and dims slightly.
 */
fun Modifier.pushTransition(
    language: DesignLanguage,
    progress: () -> Float,
    isTop: Boolean,
    direction: () -> Int = { 1 }
): Modifier {
    val layer = this.graphicsLayer {
        val p = progress()
        val w = size.width
        val d = direction().toFloat()
        if (isTop) {
            when (language) {
                DesignLanguage.SAMSUNG_ONEUI -> {
                    translationX = (1f - p) * w * 0.22f * d
                    alpha = p.coerceIn(0f, 1f)
                }
                DesignLanguage.GOOGLE_EXPRESSIVE -> {
                    val s = 0.9f + 0.1f * p
                    scaleX = s
                    scaleY = s
                    translationX = (1f - p) * w * 0.05f * d
                    alpha = (p * 1.5f).coerceIn(0f, 1f)
                }
                DesignLanguage.IOS_LIQUID_GLASS, DesignLanguage.XIAOMI_HYPEROS -> {
                    translationX = (1f - p) * w * d
                }
            }
        } else {
            val c = p.coerceIn(0f, 1f)
            when (language) {
                DesignLanguage.SAMSUNG_ONEUI -> {
                    translationX = -c * w * 0.08f
                    alpha = 1f - c
                }
                DesignLanguage.GOOGLE_EXPRESSIVE -> {
                    val s = 1f - 0.05f * c
                    scaleX = s
                    scaleY = s
                    alpha = (1f - c * 1.2f).coerceIn(0f, 1f)
                }
                DesignLanguage.IOS_LIQUID_GLASS -> translationX = -c * w * 0.3f
                DesignLanguage.XIAOMI_HYPEROS -> translationX = -c * w * 0.25f
            }
        }
    }
    val dims = !isTop && (language == DesignLanguage.IOS_LIQUID_GLASS || language == DesignLanguage.XIAOMI_HYPEROS)
    return if (dims) {
        val maxDim = if (language == DesignLanguage.IOS_LIQUID_GLASS) 0.3f else 0.18f
        layer.drawWithContent {
            drawContent()
            val c = progress().coerceIn(0f, 1f)
            if (c > 0f) drawRect(Color.Black.copy(alpha = maxDim * c))
        }
    } else {
        layer
    }
}

// ---------------------------------------------------------------------------------------------
// Media zoom (grid tile -> fullscreen viewer)
// ---------------------------------------------------------------------------------------------

// ---------------------------------------------------------------------------------------------
// Album navigation (opening an album, going back up)
// ---------------------------------------------------------------------------------------------

fun albumNavSpec(language: DesignLanguage): AnimationSpec<Float> = when (language) {
    DesignLanguage.SAMSUNG_ONEUI -> tween(460, easing = EmphasizedDecelerate)
    DesignLanguage.GOOGLE_EXPRESSIVE -> spring(dampingRatio = 0.9f, stiffness = 300f)
    DesignLanguage.IOS_LIQUID_GLASS -> spring(dampingRatio = 1f, stiffness = 280f)
    DesignLanguage.XIAOMI_HYPEROS -> tween(400, easing = EmphasizedDecelerate)
}

/**
 * The album page arriving ([progress] 0 -> 1) from [direction] (1 = deeper, -1 = back up). Quick, but
 * with enough travel, fade and depth to read as a real page change:
 * - One UI: slide + fade with a slight grow.
 * - Material: grows out of the page below with a fade.
 * - iOS: full-width slide.
 * - HyperOS: wide slide + fade.
 */
fun Modifier.albumNavTransition(
    language: DesignLanguage,
    progress: () -> Float,
    direction: () -> Int,
    /**
     * Overrides [progress] while a navigation has happened but its animation has not started yet, so the
     * new page is hidden from its very first frame instead of flashing up fully shown.
     */
    forcedProgress: Float? = null
): Modifier =
    graphicsLayer {
        val p = forcedProgress ?: progress()
        val c = p.coerceIn(0f, 1f)
        val w = size.width
        val d = direction().toFloat()
        when (language) {
            DesignLanguage.SAMSUNG_ONEUI -> {
                translationX = (1f - p) * w * 0.12f * d
                alpha = (c * 1.35f).coerceIn(0f, 1f)
                val s = 0.965f + 0.035f * c
                scaleX = s
                scaleY = s
            }
            DesignLanguage.GOOGLE_EXPRESSIVE -> {
                translationX = (1f - p) * w * 0.05f * d
                alpha = (c * 1.6f).coerceIn(0f, 1f)
                val s = 0.92f + 0.08f * p
                scaleX = s
                scaleY = s
            }
            DesignLanguage.IOS_LIQUID_GLASS -> translationX = (1f - p) * w * d
            DesignLanguage.XIAOMI_HYPEROS -> {
                translationX = (1f - p) * w * 0.3f * d
                alpha = (c * 1.25f).coerceIn(0f, 1f)
            }
        }
    }

/** Where the last tapped media tile sits on screen, so the viewer can grow out of it. */
class MediaOpenOrigin {
    private var bounds: Rect? = null
    private var recordedAt = 0L

    fun record(rect: Rect) {
        bounds = rect
        recordedAt = SystemClock.uptimeMillis()
    }

    /** The tile bounds if a tile was tapped just now (not a stale one from an earlier open). */
    fun consume(): Rect? {
        val fresh = bounds?.takeIf { SystemClock.uptimeMillis() - recordedAt < 700 }
        bounds = null
        return fresh
    }
}

val LocalMediaOpenOrigin = staticCompositionLocalOf { MediaOpenOrigin() }

private fun openSpec(language: DesignLanguage, opening: Boolean): AnimationSpec<Float> = when (language) {
    DesignLanguage.SAMSUNG_ONEUI -> if (opening) tween(360, easing = EmphasizedDecelerate) else tween(290, easing = FastOutSlowInEasing)
    DesignLanguage.GOOGLE_EXPRESSIVE -> if (opening) spring(dampingRatio = 0.76f, stiffness = 280f) else spring(dampingRatio = 1f, stiffness = 420f)
    DesignLanguage.IOS_LIQUID_GLASS -> spring(dampingRatio = 0.88f, stiffness = 300f)
    DesignLanguage.XIAOMI_HYPEROS -> tween(if (opening) 260 else 220, easing = EmphasizedStandard)
}

private class InsetRoundedShape(private val insetY: Float, private val corner: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rounded(RoundRect(0f, insetY, size.width, size.height - insetY, CornerRadius(corner)))
}

/**
 * Shows [content] (the fullscreen viewer) growing out of the tapped grid tile and shrinking back into
 * it when closed, with each language's curve. Falls back to a scale-fade when no tile was tapped.
 */
@Composable
fun MediaZoomTransition(
    visible: Boolean,
    /**
     * Reports whether the viewer fully covers the screen, so the caller can stop drawing the grid
     * underneath (it was otherwise rendered behind the opaque viewer on every frame).
     */
    onCoveringChange: ((Boolean) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val theme = LocalAppTheme.current
    val origin = LocalMediaOpenOrigin.current
    val progress = animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = openSpec(theme.language, visible),
        label = "media_zoom"
    )
    val stillAnimating by remember { derivedStateOf { progress.value > 0.001f } }
    val fullyOpen by remember { derivedStateOf { progress.value >= 0.999f } }
    if (onCoveringChange != null) {
        val covering = visible && fullyOpen
        SideEffect { onCoveringChange(covering) }
    }

    // Capture the tile once per open; closing shrinks back into the same tile.
    val boundsHolder = remember { arrayOfNulls<Rect>(1) }
    val wasVisible = remember { booleanArrayOf(false) }
    if (visible && !wasVisible[0]) boundsHolder[0] = origin.consume()
    wasVisible[0] = visible

    if (!visible && !stillAnimating) return

    val tileCornerPx = with(LocalDensity.current) { theme.shapes.gridItemCornerRadius.toPx() }
    val bounds = boundsHolder[0]
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = progress.value
                if (bounds == null || size.width <= 0f || size.height <= 0f) {
                    alpha = p.coerceIn(0f, 1f)
                    val s = 0.92f + 0.08f * p
                    scaleX = s
                    scaleY = s
                    return@graphicsLayer
                }
                val startScale = (bounds.width / size.width).coerceIn(0.05f, 1f)
                val s = startScale + (1f - startScale) * p
                scaleX = s
                scaleY = s
                translationX = (bounds.center.x - size.width / 2f) * (1f - p)
                translationY = (bounds.center.y - size.height / 2f) * (1f - p)
                // Clip to the tile's aspect at the start and open up to the full screen.
                val startHeight = bounds.height / startScale
                val visibleHeight = startHeight + (size.height - startHeight) * p.coerceIn(0f, 1f)
                val inset = ((size.height - visibleHeight) / 2f).coerceAtLeast(0f)
                val corner = (tileCornerPx / startScale) * (1f - p).coerceIn(0f, 1f)
                shape = InsetRoundedShape(inset, corner)
                clip = true
                alpha = (p * 5f).coerceIn(0f, 1f)
            }
    ) {
        content()
    }
}
