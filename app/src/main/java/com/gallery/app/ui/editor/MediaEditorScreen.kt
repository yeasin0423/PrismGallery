package com.gallery.app.ui.editor

import android.app.Activity
import android.app.PendingIntent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Rotate90DegreesCcw
import androidx.compose.material.icons.rounded.Rotate90DegreesCw
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.app.model.MediaItem
import com.gallery.app.ui.design.DialogButton
import com.gallery.app.ui.design.DialogButtonStyle
import com.gallery.app.ui.design.MenuEntry
import com.gallery.app.ui.design.ThemedDialog
import com.gallery.app.ui.design.ThemedPopupMenu
import com.gallery.app.ui.design.ThemedProgressBar
import com.gallery.app.ui.design.ThemedToolItem
import com.gallery.app.ui.design.blockTouchesBelow
import com.gallery.app.ui.design.pressScale
import com.gallery.app.ui.i18n.strings
import com.gallery.app.ui.theme.LocalAppTheme
import com.gallery.app.ui.theme.onAccent
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Called after an edited copy was saved, so the gallery can reload. Provided by MainActivity. */
val LocalOnMediaEdited = staticCompositionLocalOf<() -> Unit> { {} }

private enum class CropHandle { NONE, MOVE, LEFT, TOP, RIGHT, BOTTOM, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

/** The inline panel under the preview. Only one is open at a time. */
private enum class EditorPanel { None, Rotate, Ratio }

/** Everything an undo step restores. The selected tab and open panel are UI state, not image state. */
private data class EditorSnapshot(
    val rotation: Int,
    val flipHorizontal: Boolean,
    val flipVertical: Boolean,
    val crop: NormalizedRect,
    val ratio: CropRatio,
    val adjustments: EditorAdjustments,
    val filter: EditorFilter,
    val strokes: List<DrawStroke>
)

private const val MaxHistory = 60

private const val MIN_CROP = 0.1f

/**
 * A crop ratio: free, the whole frame, or a fixed width:height measured in pixels of the photo as it
 * is currently shown (after rotation and mirroring).
 */
private sealed interface CropRatio {
    data object Free : CropRatio
    data object Original : CropRatio
    data class Fixed(val width: Float, val height: Float) : CropRatio

    /**
     * The crop's width/height in normalized frame coordinates, or null for a free crop. A frame that
     * is wider than it is tall needs a narrower normalized width for the same 3:2 pixel crop.
     */
    fun normalizedAspect(frameAspect: Float): Float? = when (this) {
        Free -> null
        Original -> 1f
        is Fixed -> (width / height) / frameAspect
    }
}

/**
 * Photo editor: free or fixed-ratio crop, 90° rotation and horizontal/vertical mirroring. Saving keeps
 * the original untouched by default; the overflow menu can overwrite it instead, after a confirmation
 * and, for photos owned by other apps, Android's write-access request.
 */
@Composable
fun MediaEditorScreen(item: MediaItem, onClose: () -> Unit) {
    val context = LocalContext.current
    val palette = LocalAppTheme.current.palette
    val onEdited = LocalOnMediaEdited.current
    val scope = rememberCoroutineScope()

    var source by remember(item.id) { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember(item.id) { mutableStateOf(false) }
    LaunchedEffect(item.id) {
        source = MediaEditor.loadPreview(context, item)
        loadFailed = source == null
    }

    var rotation by remember { mutableIntStateOf(0) }
    var flipHorizontal by remember { mutableStateOf(false) }
    var flipVertical by remember { mutableStateOf(false) }
    var crop by remember { mutableStateOf(NormalizedRect.Full) }
    var ratio by remember { mutableStateOf<CropRatio>(CropRatio.Free) }
    var tab by remember { mutableStateOf(EditorTab.Crop) }
    var selectedAdjustment by remember { mutableStateOf(EditorAdjustment.Brightness) }
    var adjustments by remember { mutableStateOf(EditorAdjustments()) }
    var filter by remember { mutableStateOf(EditorFilterNone) }
    var drawColor by remember { mutableStateOf(EditorDrawColors.first()) }
    var drawWidth by remember { mutableFloatStateOf(DefaultDrawWidth) }
    var strokes by remember { mutableStateOf<List<DrawStroke>>(emptyList()) }
    var activeStroke by remember { mutableStateOf<DrawStroke?>(null) }
    // Live points of the stroke under the finger; a snapshot list keeps long strokes cheap to extend.
    val activePoints = remember { mutableStateListOf<Offset>() }
    var saving by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    var undoStack by remember { mutableStateOf<List<EditorSnapshot>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<EditorSnapshot>>(emptyList()) }
    // One snapshot per gesture, so dragging a slider or a crop handle is a single undo step.
    var adjustGestureStart by remember { mutableStateOf<EditorSnapshot?>(null) }
    var cropGestureStart by remember { mutableStateOf<EditorSnapshot?>(null) }
    var strokeGestureStart by remember { mutableStateOf<EditorSnapshot?>(null) }

    var openPanel by remember { mutableStateOf(EditorPanel.None) }
    // Kept after closing so the collapse animation still has content to draw.
    var shownPanel by remember { mutableStateOf(EditorPanel.Rotate) }
    var showSaveMenu by remember { mutableStateOf(false) }
    var saveMenuAnchor by remember { mutableStateOf<Rect?>(null) }
    var showOverwriteDialog by remember { mutableStateOf(false) }
    var pendingOverwrite by remember { mutableStateOf<Pair<PendingIntent, File>?>(null) }

    val rotateAnimation = remember { Animatable(0f) }
    val flipXAnimation = remember { Animatable(1f) }
    val flipYAnimation = remember { Animatable(1f) }

    val preview: ImageBitmap? = remember(source, rotation, flipHorizontal, flipVertical) {
        source?.let { MediaEditor.transform(it, rotation, flipHorizontal, flipVertical).asImageBitmap() }
    }
    val frameAspect = preview?.let { it.width.toFloat() / it.height.toFloat() } ?: 1f
    val cropAspect = ratio.normalizedAspect(frameAspect)
    val colorMatrix = remember(adjustments, filter) { editorColorMatrix(adjustments, filter) }
    val previewColorFilter = remember(colorMatrix) {
        ColorFilter.colorMatrix(ColorMatrix(values = colorMatrix))
    }
    val changed = rotation != 0 || flipHorizontal || flipVertical || !crop.isFull ||
            !adjustments.isNeutral || filter.id != EditorFilterNone.id || strokes.isNotEmpty()
    val canOverwrite = item.isImage && MediaEditor.canOverwrite(item)
    val canUndo = undoStack.isNotEmpty()
    val canRedo = redoStack.isNotEmpty()

    fun snapshot() = EditorSnapshot(
        rotation = rotation,
        flipHorizontal = flipHorizontal,
        flipVertical = flipVertical,
        crop = crop,
        ratio = ratio,
        adjustments = adjustments,
        filter = filter,
        strokes = strokes
    )

    /** Pushes [before] onto the undo stack and drops any redo branch. */
    fun pushHistory(before: EditorSnapshot = snapshot()) {
        undoStack = (undoStack + before).takeLast(MaxHistory)
        redoStack = emptyList()
    }

    fun applySnapshot(state: EditorSnapshot) {
        rotation = state.rotation
        flipHorizontal = state.flipHorizontal
        flipVertical = state.flipVertical
        crop = state.crop
        ratio = state.ratio
        adjustments = state.adjustments
        filter = state.filter
        strokes = state.strokes
        activeStroke = null
        adjustGestureStart = null
        cropGestureStart = null
        strokeGestureStart = null
        openPanel = EditorPanel.None
    }

    fun undo() {
        val previous = undoStack.lastOrNull() ?: return
        redoStack = (redoStack + snapshot()).takeLast(MaxHistory)
        undoStack = undoStack.dropLast(1)
        applySnapshot(previous)
    }

    fun redo() {
        val next = redoStack.lastOrNull() ?: return
        undoStack = (undoStack + snapshot()).takeLast(MaxHistory)
        redoStack = redoStack.dropLast(1)
        applySnapshot(next)
    }

    fun onSaved(saved: ImageSaveResult.Saved) {
        saving = false
        Toast.makeText(
            context,
            if (saved.overwritten) strings.overwritten else strings.editSaved,
            Toast.LENGTH_SHORT
        ).show()
        onEdited()
        onClose()
    }

    fun onSaveFailed(error: Throwable) {
        saving = false
        Toast.makeText(context, strings.editFailed(error.localizedMessage), Toast.LENGTH_LONG).show()
    }

    // Android 11+ asks the user to let this app modify a photo owned by another app.
    val writeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val pending = pendingOverwrite
        pendingOverwrite = null
        if (pending != null) {
            if (result.resultCode == Activity.RESULT_OK) {
                scope.launch {
                    runCatching { MediaEditor.completeOverwrite(context, item, pending.second) }
                        .onSuccess {
                            Toast.makeText(context, strings.overwritten, Toast.LENGTH_SHORT).show()
                            saving = false
                            onEdited()
                            onClose()
                        }
                        .onFailure {
                            saving = false
                            Toast.makeText(context, strings.editFailed(it.localizedMessage), Toast.LENGTH_LONG).show()
                        }
                }
            } else {
                MediaEditor.discardOverwrite(pending.second)
                saving = false
                Toast.makeText(context, strings.overwriteCancelled, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Rotates the frame a quarter turn; the inline panel's two buttons pick the direction. */
    fun rotate(clockwise: Boolean) {
        pushHistory()
        rotation = (rotation + if (clockwise) 90 else 270) % 360
        // The frame's width and height swap, so the same pixel ratio gets a different normalized one.
        val locked = ratio.normalizedAspect(1f / frameAspect)
        crop = if (locked == null) {
            if (clockwise) crop.rotateClockwise() else crop.rotateCounterClockwise()
        } else {
            fitCrop(crop, locked)
        }
        strokes = strokes.map { if (clockwise) it.rotateClockwise() else it.rotateCounterClockwise() }
        scope.launch {
            // A positive angle runs clockwise, so the preview spins in from the opposite side.
            rotateAnimation.snapTo(if (clockwise) -90f else 90f)
            rotateAnimation.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 380f))
        }
    }

    // Mirroring always follows the screen axes: after a 90°/270° rotation the source's other axis flips.
    fun mirrorHorizontally() {
        pushHistory()
        if (rotation % 180 == 0) flipHorizontal = !flipHorizontal else flipVertical = !flipVertical
        crop = crop.flipHorizontal()
        strokes = strokes.map { it.flipHorizontal() }
        scope.launch {
            flipXAnimation.snapTo(-1f)
            flipXAnimation.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 350f))
        }
    }

    fun mirrorVertically() {
        pushHistory()
        if (rotation % 180 == 0) flipVertical = !flipVertical else flipHorizontal = !flipHorizontal
        crop = crop.flipVertical()
        strokes = strokes.map { it.flipVertical() }
        scope.launch {
            flipYAnimation.snapTo(-1f)
            flipYAnimation.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 350f))
        }
    }

    fun reset() {
        pushHistory()
        rotation = 0
        flipHorizontal = false
        flipVertical = false
        crop = NormalizedRect.Full
        ratio = CropRatio.Free
        adjustments = EditorAdjustments()
        selectedAdjustment = EditorAdjustment.Brightness
        filter = EditorFilterNone
        strokes = emptyList()
        activeStroke = null
        openPanel = EditorPanel.None
    }

    fun chooseRatio(newRatio: CropRatio) {
        if (newRatio == ratio) return
        pushHistory()
        ratio = newRatio
        newRatio.normalizedAspect(frameAspect)?.let { crop = fitCrop(crop, it) }
    }

    fun togglePanel(panel: EditorPanel) {
        if (openPanel == panel) {
            openPanel = EditorPanel.None
        } else {
            shownPanel = panel
            openPanel = panel
        }
    }

    fun normalizedPoint(position: Offset, rect: Rect) = Offset(
        ((position.x - rect.left) / rect.width).coerceIn(0f, 1f),
        ((position.y - rect.top) / rect.height).coerceIn(0f, 1f)
    )

    fun beginStroke(position: Offset, rect: Rect) {
        if (!rect.contains(position)) return
        strokeGestureStart = snapshot()
        activePoints.clear()
        activePoints.add(normalizedPoint(position, rect))
        activeStroke = DrawStroke(activePoints, drawColor, drawWidth)
    }

    fun extendStroke(position: Offset, rect: Rect) {
        val current = activeStroke ?: return
        val point = normalizedPoint(position, rect)
        val last = activePoints.lastOrNull() ?: current.points.last()
        // Skip sub-pixel moves; they only bloat the stroke and the undo snapshots.
        if (abs((point.x - last.x) * rect.width) < 1f && abs((point.y - last.y) * rect.height) < 1f) return
        activePoints.add(point)
    }

    fun finishStroke() {
        val finished = activeStroke ?: return
        val before = strokeGestureStart
        activeStroke = null
        strokeGestureStart = null
        strokes = strokes + finished.copy(points = activePoints.toList())
        activePoints.clear()
        if (before != null) pushHistory(before)
    }

    fun saveCopy() {
        if (saving || !changed) return
        saving = true
        progress = 0f
        val spec = EditSpec(rotation, flipHorizontal, flipVertical, crop, colorMatrix, strokes)
        scope.launch {
            if (item.isVideo) {
                runCatching { MediaEditor.saveVideo(context, item, spec) { progress = it } }
                    .onSuccess {
                        saving = false
                        Toast.makeText(context, strings.editSaved, Toast.LENGTH_SHORT).show()
                        onEdited()
                        onClose()
                    }
                    .onFailure { onSaveFailed(it) }
                return@launch
            }
            runCatching { MediaEditor.saveImage(context, item, spec, overwrite = false) }
                .onSuccess { result ->
                    when (result) {
                        is ImageSaveResult.Saved -> onSaved(result)
                        is ImageSaveResult.NeedsWritePermission -> {
                            MediaEditor.discardOverwrite(result.tempFile)
                            onSaveFailed(IllegalStateException())
                        }
                    }
                }
                .onFailure { onSaveFailed(it) }
        }
    }

    fun overwrite() {
        if (saving || !changed || !canOverwrite) return
        saving = true
        progress = 0f
        val spec = EditSpec(rotation, flipHorizontal, flipVertical, crop, colorMatrix, strokes)
        scope.launch {
            runCatching { MediaEditor.saveImage(context, item, spec, overwrite = true) }
                .onSuccess { result ->
                    when (result) {
                        is ImageSaveResult.Saved -> onSaved(result)
                        is ImageSaveResult.NeedsWritePermission -> {
                            pendingOverwrite = result.pendingIntent to result.tempFile
                            writeLauncher.launch(
                                IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                            )
                        }
                    }
                }
                .onFailure { onSaveFailed(it) }
        }
    }

    // A rendered overwrite waiting for the system prompt is deleted if the editor goes away.
    DisposableEffect(Unit) {
        onDispose { pendingOverwrite?.let { MediaEditor.discardOverwrite(it.second) } }
    }

    BackHandler {
        if (saving) return@BackHandler
        when {
            openPanel != EditorPanel.None -> openPanel = EditorPanel.None
            tab != EditorTab.Crop -> tab = EditorTab.Crop
            else -> onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .blockTouchesBelow()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            EditorTopBar(
                closeEnabled = !saving,
                saveEnabled = changed && !saving && preview != null,
                canOverwrite = canOverwrite,
                undoEnabled = !saving && canUndo,
                redoEnabled = !saving && canRedo,
                resetEnabled = changed && !saving && preview != null,
                onUndo = ::undo,
                onRedo = ::redo,
                onReset = ::reset,
                onClose = onClose,
                onSaveCopy = ::saveCopy,
                onSaveOptions = { showSaveMenu = true },
                onOptionsBounds = { saveMenuAnchor = it }
            )

            // Preview with the crop frame drawn on top.
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                val image = preview
                if (image == null) {
                    if (loadFailed) {
                        Text(strings.unknown, color = palette.textMuted)
                    } else {
                        CircularProgressIndicator(color = palette.accent)
                    }
                    return@BoxWithConstraints
                }
                val areaWidth = constraints.maxWidth.toFloat()
                val areaHeight = constraints.maxHeight.toFloat()
                val scale = min(areaWidth / image.width, areaHeight / image.height)
                val drawWidth = image.width * scale
                val drawHeight = image.height * scale
                val imageRect = Rect(
                    Offset((areaWidth - drawWidth) / 2f, (areaHeight - drawHeight) / 2f),
                    Size(drawWidth, drawHeight)
                )
                var activeHandle by remember { mutableStateOf(CropHandle.NONE) }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ = rotateAnimation.value
                            scaleX = flipXAnimation.value
                            scaleY = flipYAnimation.value
                        }
                        .pointerInput(imageRect, cropAspect, tab) {
                            if (tab == EditorTab.Draw) {
                                detectDragGestures(
                                    onDragStart = { position -> beginStroke(position, imageRect) },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        extendStroke(change.position, imageRect)
                                    },
                                    onDragEnd = { finishStroke() },
                                    onDragCancel = { finishStroke() }
                                )
                                return@pointerInput
                            }
                            if (tab != EditorTab.Crop) return@pointerInput
                            val touchSlop = 28.dp.toPx()
                            detectDragGestures(
                                onDragStart = { position ->
                                    val handle = hitTest(
                                        position,
                                        cropRectIn(imageRect, crop),
                                        touchSlop,
                                        cornersOnly = cropAspect != null
                                    )
                                    activeHandle = handle
                                    cropGestureStart = if (handle != CropHandle.NONE) snapshot() else null
                                },
                                onDrag = { change, drag ->
                                    if (activeHandle != CropHandle.NONE) {
                                        change.consume()
                                        crop = applyDrag(
                                            crop,
                                            activeHandle,
                                            drag.x / imageRect.width,
                                            drag.y / imageRect.height,
                                            cropAspect
                                        )
                                    }
                                },
                                onDragEnd = {
                                    activeHandle = CropHandle.NONE
                                    cropGestureStart?.takeIf { it != snapshot() }?.let { pushHistory(it) }
                                    cropGestureStart = null
                                },
                                onDragCancel = {
                                    activeHandle = CropHandle.NONE
                                    cropGestureStart?.takeIf { it != snapshot() }?.let { pushHistory(it) }
                                    cropGestureStart = null
                                }
                            )
                        }
                ) {
                    drawImage(
                        image = image,
                        dstOffset = IntOffset(imageRect.left.roundToInt(), imageRect.top.roundToInt()),
                        dstSize = IntSize(imageRect.width.roundToInt(), imageRect.height.roundToInt()),
                        colorFilter = previewColorFilter
                    )
                    val frameMin = min(imageRect.width, imageRect.height)
                    val inkBounds = cropRectIn(imageRect, crop)
                    // Ink is clipped to the crop so the preview hides what the export would cut off.
                    clipRect(inkBounds.left, inkBounds.top, inkBounds.right, inkBounds.bottom) {
                        strokes.forEach { stroke ->
                            drawEditorStroke(
                                stroke = stroke,
                                left = imageRect.left,
                                top = imageRect.top,
                                width = imageRect.width,
                                height = imageRect.height,
                                strokeWidth = stroke.width * frameMin
                            )
                        }
                        activeStroke?.let { stroke ->
                            drawEditorStroke(
                                stroke = stroke,
                                left = imageRect.left,
                                top = imageRect.top,
                                width = imageRect.width,
                                height = imageRect.height,
                                strokeWidth = stroke.width * frameMin
                            )
                        }
                    }
                    // Ink is clipped to the crop; show its edge in Draw so nothing vanishes unexplained.
                    if (tab == EditorTab.Draw && !crop.isFull) {
                        drawRect(
                            color = Color.White.copy(alpha = 0.35f),
                            topLeft = inkBounds.topLeft,
                            size = inkBounds.size,
                            style = Stroke(1.dp.toPx())
                        )
                    }
                    if (tab == EditorTab.Crop) {
                        val cropRect = cropRectIn(imageRect, crop)
                        val shade = Color.Black.copy(alpha = 0.55f)
                        // Dim everything outside the crop.
                        drawRect(shade, Offset(imageRect.left, imageRect.top), Size(imageRect.width, cropRect.top - imageRect.top))
                        drawRect(shade, Offset(imageRect.left, cropRect.bottom), Size(imageRect.width, imageRect.bottom - cropRect.bottom))
                        drawRect(shade, Offset(imageRect.left, cropRect.top), Size(cropRect.left - imageRect.left, cropRect.height))
                        drawRect(shade, Offset(cropRect.right, cropRect.top), Size(imageRect.right - cropRect.right, cropRect.height))
                        // Rule-of-thirds grid while adjusting.
                        if (activeHandle != CropHandle.NONE) {
                            for (i in 1..2) {
                                val gx = cropRect.left + cropRect.width * i / 3f
                                val gy = cropRect.top + cropRect.height * i / 3f
                                drawLine(Color.White.copy(alpha = 0.4f), Offset(gx, cropRect.top), Offset(gx, cropRect.bottom), 1.dp.toPx())
                                drawLine(Color.White.copy(alpha = 0.4f), Offset(cropRect.left, gy), Offset(cropRect.right, gy), 1.dp.toPx())
                            }
                        }
                        drawRect(Color.White, cropRect.topLeft, cropRect.size, style = Stroke(1.5.dp.toPx()))
                        // Corner handles
                        val handleLength = 20.dp.toPx()
                        val handleStroke = 3.5.dp.toPx()
                        val handleColor = Color.White
                        listOf(
                            Triple(cropRect.topLeft, 1f, 1f),
                            Triple(Offset(cropRect.right, cropRect.top), -1f, 1f),
                            Triple(Offset(cropRect.left, cropRect.bottom), 1f, -1f),
                            Triple(cropRect.bottomRight, -1f, -1f)
                        ).forEach { (corner, dx, dy) ->
                            drawLine(handleColor, corner, Offset(corner.x + handleLength * dx, corner.y), handleStroke)
                            drawLine(handleColor, corner, Offset(corner.x, corner.y + handleLength * dy), handleStroke)
                        }
                    }
                }
            }

            EditorTabs(
                selected = tab,
                enabled = !saving,
                onSelect = { selected ->
                    tab = selected
                    openPanel = EditorPanel.None
                }
            )

            when (tab) {
                EditorTab.Crop -> {
                    AnimatedVisibility(
                        visible = openPanel != EditorPanel.None,
                        enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                        exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
                    ) {
                        EditorInlinePanel(
                            panel = shownPanel,
                            enabled = !saving && preview != null,
                            ratio = ratio,
                            onRotateLeft = { rotate(clockwise = false) },
                            onRotateRight = { rotate(clockwise = true) },
                            onSelectRatio = ::chooseRatio
                        )
                    }

                    EditorToolbar(
                        enabled = !saving && preview != null,
                        openPanel = openPanel,
                        onToggleRotate = { togglePanel(EditorPanel.Rotate) },
                        // Any one-shot tool closes whatever inline panel was open first.
                        onMirrorHorizontally = {
                            openPanel = EditorPanel.None
                            mirrorHorizontally()
                        },
                        onMirrorVertically = {
                            openPanel = EditorPanel.None
                            mirrorVertically()
                        },
                        onToggleRatio = { togglePanel(EditorPanel.Ratio) }
                    )
                }

                EditorTab.Adjust -> AdjustPanel(
                    adjustments = adjustments,
                    selected = selectedAdjustment,
                    enabled = !saving && preview != null,
                    onSelect = { selectedAdjustment = it },
                    onChange = { adjustment, value ->
                        // A drag is one undo step: remember where it started, commit when it ends.
                        if (adjustGestureStart == null) adjustGestureStart = snapshot()
                        adjustments = adjustments.with(adjustment, value)
                    },
                    onChangeFinished = {
                        adjustGestureStart?.takeIf { it != snapshot() }?.let { pushHistory(it) }
                        adjustGestureStart = null
                    },
                    onNudge = { adjustment, delta ->
                        val current = adjustments.valueOf(adjustment)
                        val next = (current + delta).coerceIn(-1f, 1f)
                        if (next != current) {
                            pushHistory()
                            adjustments = adjustments.with(adjustment, next)
                        }
                    }
                )

                EditorTab.Filters -> {
                    val image = preview
                    if (image != null) {
                        FilterPanel(
                            preview = image,
                            filters = EditorFilters,
                            selected = filter,
                            enabled = !saving,
                            matrixFor = { candidate -> editorColorMatrix(adjustments, candidate) },
                            onSelect = { candidate ->
                                if (candidate.id != filter.id) {
                                    pushHistory()
                                    filter = candidate
                                }
                            }
                        )
                    }
                }

                EditorTab.Draw -> DrawPanel(
                    color = drawColor,
                    brushWidth = drawWidth,
                    enabled = !saving && preview != null,
                    onSelectColor = { drawColor = it },
                    onBrushWidthChange = { drawWidth = it }
                )
            }
        }

        if (saving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.overlayScrim)
                    .blockTouchesBelow(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (item.isVideo) {
                        ThemedProgressBar(progress = progress, modifier = Modifier.width(220.dp))
                    } else {
                        CircularProgressIndicator(color = palette.accent)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(strings.saving, color = Color.White, fontSize = 15.sp)
                }
            }
        }
    }

    ThemedPopupMenu(
        expanded = showSaveMenu,
        onDismissRequest = { showSaveMenu = false },
        anchorBounds = saveMenuAnchor,
        entries = buildList {
            // Saving a copy is the main button; this menu only offers the destructive alternative.
            if (canOverwrite) {
                add(MenuEntry(strings.overwriteOriginal, destructive = true) { showOverwriteDialog = true })
            }
        }
    )

    if (showOverwriteDialog) {
        ThemedDialog(
            onDismissRequest = { showOverwriteDialog = false },
            title = strings.overwriteTitle,
            message = strings.overwriteMessage,
            buttons = listOf(
                DialogButton(strings.cancel, style = DialogButtonStyle.Cancel) {
                    showOverwriteDialog = false
                },
                DialogButton(strings.overwriteAction, style = DialogButtonStyle.Destructive) {
                    showOverwriteDialog = false
                    overwrite()
                }
            )
        )
    }
}

@Composable
private fun EditorTopBar(
    closeEnabled: Boolean,
    saveEnabled: Boolean,
    canOverwrite: Boolean,
    undoEnabled: Boolean,
    redoEnabled: Boolean,
    resetEnabled: Boolean,
    onClose: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onReset: () -> Unit,
    onSaveCopy: () -> Unit,
    onSaveOptions: () -> Unit,
    onOptionsBounds: (Rect) -> Unit
) {
    val palette = LocalAppTheme.current.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(60.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val closeInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .size(44.dp)
                .pressScale(closeInteraction, 0.9f)
                .clip(CircleShape)
                .background(palette.surface)
                .clickable(
                    interactionSource = closeInteraction,
                    indication = ripple(color = palette.onBackground),
                    enabled = closeEnabled,
                    onClick = onClose
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = strings.close,
                tint = if (closeEnabled) palette.onBackground else palette.textMuted,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = strings.edit,
            color = palette.onBackground,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 10.dp, end = 2.dp)
        )
        TopBarIconButton(
            icon = Icons.AutoMirrored.Rounded.Undo,
            contentDescription = strings.undo,
            enabled = undoEnabled,
            iconSize = 21.dp,
            onClick = onUndo
        )
        TopBarIconButton(
            icon = Icons.AutoMirrored.Rounded.Redo,
            contentDescription = strings.redo,
            enabled = redoEnabled,
            iconSize = 21.dp,
            onClick = onRedo
        )
        Spacer(modifier = Modifier.weight(1f))
        TopBarIconButton(
            icon = Icons.Rounded.Restore,
            contentDescription = strings.reset,
            enabled = resetEnabled,
            iconSize = 20.dp,
            onClick = onReset
        )
        Spacer(modifier = Modifier.width(4.dp))
        SavePill(
            enabled = saveEnabled,
            canOverwrite = canOverwrite,
            onSaveCopy = onSaveCopy,
            onSaveOptions = onSaveOptions,
            onOptionsBounds = onOptionsBounds
        )
    }
}

/** A compact borderless icon button for the top bar; disabled steps fade to the muted text colour. */
@Composable
private fun TopBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    iconSize: Dp,
    onClick: () -> Unit
) {
    val palette = LocalAppTheme.current.palette
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(36.dp)
            .pressScale(interaction, 0.9f)
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = palette.onBackground),
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) palette.onBackground else palette.textMuted.copy(alpha = 0.5f),
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * The save button. With an overwritable original it is split: the main half saves a copy, the chevron
 * opens the destructive "Overwrite original" option. Other formats show a single-pill save-copy button.
 */
@Composable
private fun SavePill(
    enabled: Boolean,
    canOverwrite: Boolean,
    onSaveCopy: () -> Unit,
    onSaveOptions: () -> Unit,
    onOptionsBounds: (Rect) -> Unit
) {
    val palette = LocalAppTheme.current.palette
    val background = if (enabled) palette.accent else palette.surfaceVariant
    val content = if (enabled) palette.onAccent else palette.textMuted
    val copyInteraction = remember { MutableInteractionSource() }
    val optionsInteraction = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(background),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .pressScale(copyInteraction, 0.94f)
                .clip(CircleShape)
                .clickable(
                    interactionSource = copyInteraction,
                    indication = ripple(color = content),
                    enabled = enabled,
                    onClick = onSaveCopy
                )
                .padding(start = 14.dp, end = if (canOverwrite) 10.dp else 14.dp, top = 10.dp, bottom = 10.dp)
        ) {
            Text(
                text = strings.save,
                color = content,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        if (canOverwrite) {
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(content.copy(alpha = 0.3f))
            )
            Box(
                modifier = Modifier
                    .pressScale(optionsInteraction, 0.9f)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = optionsInteraction,
                        indication = ripple(color = content),
                        enabled = enabled,
                        onClick = onSaveOptions
                    )
                    .onGloballyPositioned { onOptionsBounds(it.boundsInRoot()) }
                    .padding(horizontal = 7.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = strings.saveOptions,
                    tint = content,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/** Rotate / mirror / ratio tools in the One UI tool-row style. */
@Composable
private fun EditorToolbar(
    enabled: Boolean,
    openPanel: EditorPanel,
    onToggleRotate: () -> Unit,
    onMirrorHorizontally: () -> Unit,
    onMirrorVertically: () -> Unit,
    onToggleRatio: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ThemedToolItem(
            icon = Icons.Rounded.Rotate90DegreesCw,
            label = strings.rotate,
            onClick = onToggleRotate,
            enabled = enabled,
            selected = openPanel == EditorPanel.Rotate
        )
        ThemedToolItem(
            icon = Icons.Rounded.Flip,
            label = strings.flipHorizontal,
            onClick = onMirrorHorizontally,
            enabled = enabled
        )
        ThemedToolItem(
            icon = Icons.Rounded.Flip,
            label = strings.flipVertical,
            onClick = onMirrorVertically,
            enabled = enabled,
            iconRotation = 90f
        )
        ThemedToolItem(
            icon = Icons.Rounded.Crop,
            label = strings.ratio,
            onClick = onToggleRatio,
            enabled = enabled,
            selected = openPanel == EditorPanel.Ratio
        )
    }
}

/** The panel that slides in between the preview and the tool row; only one is open at a time. */
@Composable
private fun EditorInlinePanel(
    panel: EditorPanel,
    enabled: Boolean,
    ratio: CropRatio,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onSelectRatio: (CropRatio) -> Unit
) {
    val palette = LocalAppTheme.current.palette
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(palette.surface)
    ) {
        when (panel) {
            EditorPanel.Rotate -> RotatePanel(enabled, onRotateLeft, onRotateRight)
            EditorPanel.Ratio -> RatioPicker(ratio, onSelectRatio)
            EditorPanel.None -> Unit
        }
    }
}

/** Rotate left / rotate right, picked explicitly instead of rotating on every toolbar tap. */
@Composable
private fun RotatePanel(enabled: Boolean, onRotateLeft: () -> Unit, onRotateRight: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PanelAction(
            icon = Icons.Rounded.Rotate90DegreesCcw,
            label = strings.rotateLeft,
            onClick = onRotateLeft,
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )
        PanelAction(
            icon = Icons.Rounded.Rotate90DegreesCw,
            label = strings.rotateRight,
            onClick = onRotateRight,
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PanelAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppTheme.current.palette
    val content = if (enabled) palette.onBackground else palette.textMuted
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surfaceVariant)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = palette.accent),
                enabled = enabled,
                onClick = onClick
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = content, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = content, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/** The crop ratios as a horizontally scrollable row of One UI chips below the photo. */
@Composable
private fun RatioPicker(selected: CropRatio, onSelect: (CropRatio) -> Unit) {
    val palette = LocalAppTheme.current.palette
    val entries = listOf(
        CropRatio.Free to strings.ratioFree,
        CropRatio.Original to strings.ratioOriginal,
        CropRatio.Fixed(1f, 1f) to strings.ratioSquare,
        CropRatio.Fixed(3f, 2f) to strings.ratio3x2,
        CropRatio.Fixed(2f, 3f) to strings.ratio2x3,
        CropRatio.Fixed(4f, 3f) to strings.ratio4x3,
        CropRatio.Fixed(3f, 4f) to strings.ratio3x4,
        CropRatio.Fixed(16f, 9f) to strings.ratio16x9,
        CropRatio.Fixed(9f, 16f) to strings.ratio9x16
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        entries.forEach { (ratio, label) ->
            val isSelected = ratio == selected
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) palette.accent else palette.surfaceVariant)
                    .clickable(onClick = { onSelect(ratio) })
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) palette.onAccent else palette.onBackground,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

private fun cropRectIn(imageRect: Rect, crop: NormalizedRect) = Rect(
    imageRect.left + crop.left * imageRect.width,
    imageRect.top + crop.top * imageRect.height,
    imageRect.left + crop.right * imageRect.width,
    imageRect.top + crop.bottom * imageRect.height
)

/** The largest crop with [aspect] (normalized width/height) around the current crop's center. */
private fun fitCrop(current: NormalizedRect, aspect: Float): NormalizedRect {
    var width = 1f
    var height = width / aspect
    if (height > 1f) {
        height = 1f
        width = height * aspect
    }
    val centerX = (current.left + current.right) / 2f
    val centerY = (current.top + current.bottom) / 2f
    val left = (centerX - width / 2f).coerceIn(0f, 1f - width)
    val top = (centerY - height / 2f).coerceIn(0f, 1f - height)
    return NormalizedRect(left, top, left + width, top + height)
}

private fun hitTest(position: Offset, rect: Rect, slop: Float, cornersOnly: Boolean): CropHandle {
    val nearLeft = abs(position.x - rect.left) < slop
    val nearRight = abs(position.x - rect.right) < slop
    val nearTop = abs(position.y - rect.top) < slop
    val nearBottom = abs(position.y - rect.bottom) < slop
    val withinX = position.x > rect.left - slop && position.x < rect.right + slop
    val withinY = position.y > rect.top - slop && position.y < rect.bottom + slop
    val corner = when {
        nearLeft && nearTop -> CropHandle.TOP_LEFT
        nearRight && nearTop -> CropHandle.TOP_RIGHT
        nearLeft && nearBottom -> CropHandle.BOTTOM_LEFT
        nearRight && nearBottom -> CropHandle.BOTTOM_RIGHT
        else -> CropHandle.NONE
    }
    if (corner != CropHandle.NONE) return corner
    // With a locked ratio only the corners can change the size; the whole rect still moves.
    if (cornersOnly) return if (rect.contains(position)) CropHandle.MOVE else CropHandle.NONE
    return when {
        nearLeft && withinY -> CropHandle.LEFT
        nearRight && withinY -> CropHandle.RIGHT
        nearTop && withinX -> CropHandle.TOP
        nearBottom && withinX -> CropHandle.BOTTOM
        rect.contains(position) -> CropHandle.MOVE
        else -> CropHandle.NONE
    }
}

/**
 * Applies a drag in normalized units. A free crop moves every edge; a locked one keeps [aspect] by
 * moving the dragged corner and the two edges that meet there.
 */
private fun applyDrag(
    crop: NormalizedRect,
    handle: CropHandle,
    dx: Float,
    dy: Float,
    aspect: Float?
): NormalizedRect {
    if (handle == CropHandle.NONE) return crop
    if (handle == CropHandle.MOVE) {
        val width = crop.width
        val height = crop.height
        val left = (crop.left + dx).coerceIn(0f, 1f - width)
        val top = (crop.top + dy).coerceIn(0f, 1f - height)
        return NormalizedRect(left, top, left + width, top + height)
    }
    if (aspect != null) return dragCorner(crop, handle, dx, dy, aspect)

    var left = crop.left
    var top = crop.top
    var right = crop.right
    var bottom = crop.bottom
    if (handle == CropHandle.LEFT || handle == CropHandle.TOP_LEFT || handle == CropHandle.BOTTOM_LEFT) {
        left = (left + dx).coerceIn(0f, right - MIN_CROP)
    }
    if (handle == CropHandle.RIGHT || handle == CropHandle.TOP_RIGHT || handle == CropHandle.BOTTOM_RIGHT) {
        right = (right + dx).coerceIn(left + MIN_CROP, 1f)
    }
    if (handle == CropHandle.TOP || handle == CropHandle.TOP_LEFT || handle == CropHandle.TOP_RIGHT) {
        top = (top + dy).coerceIn(0f, bottom - MIN_CROP)
    }
    if (handle == CropHandle.BOTTOM || handle == CropHandle.BOTTOM_LEFT || handle == CropHandle.BOTTOM_RIGHT) {
        bottom = (bottom + dy).coerceIn(top + MIN_CROP, 1f)
    }
    return NormalizedRect(left, top, right, bottom)
}

private fun dragCorner(
    crop: NormalizedRect,
    handle: CropHandle,
    dx: Float,
    dy: Float,
    aspect: Float
): NormalizedRect {
    val fromLeft = handle == CropHandle.TOP_LEFT || handle == CropHandle.BOTTOM_LEFT
    val fromTop = handle == CropHandle.TOP_LEFT || handle == CropHandle.TOP_RIGHT
    // The corner opposite the dragged one stays where it is.
    val anchorX = if (fromLeft) crop.right else crop.left
    val anchorY = if (fromTop) crop.bottom else crop.top
    val movingX = (if (fromLeft) crop.left else crop.right) + dx
    val movingY = (if (fromTop) crop.top else crop.bottom) + dy

    val availableWidth = if (fromLeft) anchorX else 1f - anchorX
    val availableHeight = if (fromTop) anchorY else 1f - anchorY
    val limit = min(availableWidth, availableHeight * aspect)
    val minWidth = max(MIN_CROP, MIN_CROP * aspect)
    val desired = max(abs(movingX - anchorX), abs(movingY - anchorY) * aspect)
    val width = desired.coerceIn(min(minWidth, limit), limit)
    val height = width / aspect

    val left = if (fromLeft) anchorX - width else anchorX
    val top = if (fromTop) anchorY - height else anchorY
    return NormalizedRect(left, top, left + width, top + height)
}
