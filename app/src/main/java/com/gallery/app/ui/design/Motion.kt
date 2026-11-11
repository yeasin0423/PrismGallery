package com.gallery.app.ui.design

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Indication
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.gallery.app.GalleryApplication
import com.gallery.app.PerformanceProfile
import com.gallery.app.ui.theme.LocalAppTheme
import kotlin.math.abs

/*
 * Motion and touch effects: the animation clock, press feedback, and the tab switch / push / media zoom
 * transitions.
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

// ---------------------------------------------------------------------------------------------
// Press feedback
// ---------------------------------------------------------------------------------------------

/**
 * Press feedback for every clickable without an explicit indication (list rows, tiles, cards): the
 * One UI ripple.
 */
@Composable
fun rememberThemedIndication(): Indication {
    val palette = LocalAppTheme.current.palette
    return ripple(color = palette.onBackground)
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

fun tabSwitchSpec(): AnimationSpec<Float> = tween(380, easing = EmphasizedDecelerate)

/**
 * Places a tab during a switch. [relative] is -1..1: 0 = fully shown, ±1 = fully gone towards that side.
 * One UI: a short slide with a fade.
 */
fun GraphicsLayerScope.tabSwitchTransition(relative: Float) {
    val r = relative.coerceIn(-1f, 1f)
    val a = abs(r)
    translationX = r * size.width * 0.16f
    alpha = (1f - a * 1.35f).coerceIn(0f, 1f)
}

// ---------------------------------------------------------------------------------------------
// Push (Favoriten, Papierkorb, albums)
// ---------------------------------------------------------------------------------------------

fun pushSpec(): AnimationSpec<Float> = tween(400, easing = EmphasizedDecelerate)

/**
 * Push navigation. [progress] 0 = the pushed page is off screen, 1 = it fully covers the page below.
 * The pushed page ([isTop]) arrives from [direction] (1 = from the right, -1 = from the left); the page
 * below fades out and shifts slightly the other way.
 */
fun Modifier.pushTransition(
    progress: () -> Float,
    isTop: Boolean,
    direction: () -> Int = { 1 }
): Modifier {
    val layer = this.graphicsLayer {
        val p = progress()
        val w = size.width
        val d = direction().toFloat()
        if (isTop) {
            translationX = (1f - p) * w * 0.22f * d
            alpha = p.coerceIn(0f, 1f)
        } else {
            val c = p.coerceIn(0f, 1f)
            translationX = -c * w * 0.08f
            alpha = 1f - c
        }
    }
    return layer
}

// ---------------------------------------------------------------------------------------------
// Media zoom (grid tile -> fullscreen viewer)
// ---------------------------------------------------------------------------------------------

// ---------------------------------------------------------------------------------------------
// Album navigation (opening an album, going back up)
// ---------------------------------------------------------------------------------------------

fun albumNavSpec(): AnimationSpec<Float> = tween(460, easing = EmphasizedDecelerate)

/**
 * The album page arriving ([progress] 0 -> 1) from [direction] (1 = deeper, -1 = back up): a One UI
 * slide with a fade and a slight grow.
 */
fun Modifier.albumNavTransition(
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
        translationX = (1f - p) * w * 0.12f * d
        alpha = (c * 1.35f).coerceIn(0f, 1f)
        val s = 0.965f + 0.035f * c
        scaleX = s
        scaleY = s
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

private fun openSpec(opening: Boolean): AnimationSpec<Float> =
    if (opening) tween(360, easing = EmphasizedDecelerate) else tween(290, easing = FastOutSlowInEasing)

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
        animationSpec = openSpec(visible),
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
