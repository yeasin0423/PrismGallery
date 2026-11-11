package com.gallery.app.ui.editor

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Rotate90DegreesCw
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.app.model.MediaItem
import com.gallery.app.ui.design.ThemedProgressBar
import com.gallery.app.ui.design.blockTouchesBelow
import com.gallery.app.ui.design.pressScale
import com.gallery.app.ui.i18n.strings
import com.gallery.app.ui.theme.LocalAppTheme
import com.gallery.app.ui.theme.onAccent
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/** Called after an edited copy was saved, so the gallery can reload. Provided by MainActivity. */
val LocalOnMediaEdited = staticCompositionLocalOf<() -> Unit> { {} }

private enum class CropHandle { NONE, MOVE, LEFT, TOP, RIGHT, BOTTOM, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

private const val MIN_CROP = 0.1f

/**
 * Simple editor for images and videos: free crop with draggable corners/edges, 90° rotation and
 * horizontal/vertical mirroring. The result is saved as a new copy next to the original, so the
 * original is never overwritten. Videos are edited on a still frame and exported with Media3 Transformer.
 */
@Composable
fun MediaEditorScreen(item: MediaItem, onClose: () -> Unit) {
    val context = LocalContext.current
    val theme = LocalAppTheme.current
    val palette = theme.palette
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
    var saving by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    val rotateAnimation = remember { Animatable(0f) }
    val flipXAnimation = remember { Animatable(1f) }
    val flipYAnimation = remember { Animatable(1f) }

    BackHandler { if (!saving) onClose() }

    val preview: ImageBitmap? = remember(source, rotation, flipHorizontal, flipVertical) {
        source?.let { MediaEditor.transform(it, rotation, flipHorizontal, flipVertical).asImageBitmap() }
    }
    val changed = rotation != 0 || flipHorizontal || flipVertical || !crop.isFull

    fun rotate() {
        rotation = (rotation + 90) % 360
        crop = crop.rotateClockwise()
        scope.launch {
            rotateAnimation.snapTo(-90f)
            rotateAnimation.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 380f))
        }
    }

    // Mirroring always follows the screen axes: after a 90°/270° rotation the source's other axis flips.
    fun mirrorHorizontally() {
        if (rotation % 180 == 0) flipHorizontal = !flipHorizontal else flipVertical = !flipVertical
        crop = crop.flipHorizontal()
        scope.launch {
            flipXAnimation.snapTo(-1f)
            flipXAnimation.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 350f))
        }
    }

    fun mirrorVertically() {
        if (rotation % 180 == 0) flipVertical = !flipVertical else flipHorizontal = !flipHorizontal
        crop = crop.flipVertical()
        scope.launch {
            flipYAnimation.snapTo(-1f)
            flipYAnimation.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 350f))
        }
    }

    fun reset() {
        rotation = 0
        flipHorizontal = false
        flipVertical = false
        crop = NormalizedRect.Full
    }

    fun save() {
        if (saving || !changed) return
        saving = true
        progress = 0f
        val spec = EditSpec(rotation, flipHorizontal, flipVertical, crop)
        scope.launch {
            val result = runCatching {
                if (item.isVideo) {
                    MediaEditor.saveVideo(context, item, spec) { progress = it }
                } else {
                    MediaEditor.saveImage(context, item, spec)
                }
            }
            saving = false
            result.onSuccess {
                Toast.makeText(context, strings.editSaved, Toast.LENGTH_SHORT).show()
                onEdited()
                onClose()
            }.onFailure {
                Toast.makeText(context, strings.editFailed(it.localizedMessage), Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .blockTouchesBelow()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar: close, title, save
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(60.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !saving, onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, strings.close, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Text(
                    text = strings.edit,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                )
                val saveInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .pressScale(saveInteraction, 0.93f)
                        .clip(CircleShape)
                        .background(if (changed) palette.accent else palette.accent.copy(alpha = 0.35f))
                        .clickable(
                            interactionSource = saveInteraction,
                            indication = ripple(),
                            enabled = changed && !saving && preview != null,
                            onClick = ::save
                        )
                        .padding(horizontal = 20.dp, vertical = 9.dp)
                ) {
                    Text(strings.save, color = palette.onAccent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Preview with crop frame
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                val image = preview
                if (image == null) {
                    if (loadFailed) {
                        Text(strings.unknown, color = Color.White.copy(alpha = 0.7f))
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
                        .pointerInput(imageRect) {
                            val touchSlop = 28.dp.toPx()
                            detectDragGestures(
                                onDragStart = { position ->
                                    activeHandle = hitTest(position, cropRectIn(imageRect, crop), touchSlop)
                                },
                                onDrag = { change, drag ->
                                    if (activeHandle != CropHandle.NONE) {
                                        change.consume()
                                        crop = applyDrag(crop, activeHandle, drag.x / imageRect.width, drag.y / imageRect.height)
                                    }
                                },
                                onDragEnd = { activeHandle = CropHandle.NONE },
                                onDragCancel = { activeHandle = CropHandle.NONE }
                            )
                        }
                ) {
                    drawImage(
                        image = image,
                        dstOffset = IntOffset(imageRect.left.roundToInt(), imageRect.top.roundToInt()),
                        dstSize = IntSize(imageRect.width.roundToInt(), imageRect.height.roundToInt())
                    )
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

            EditorToolbar(
                enabled = !saving && preview != null,
                onRotate = ::rotate,
                onMirrorHorizontally = ::mirrorHorizontally,
                onMirrorVertically = ::mirrorVertically,
                onReset = ::reset
            )
        }

        if (saving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
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
}

private fun cropRectIn(imageRect: Rect, crop: NormalizedRect) = Rect(
    imageRect.left + crop.left * imageRect.width,
    imageRect.top + crop.top * imageRect.height,
    imageRect.left + crop.right * imageRect.width,
    imageRect.top + crop.bottom * imageRect.height
)

private fun hitTest(position: Offset, rect: Rect, slop: Float): CropHandle {
    val nearLeft = abs(position.x - rect.left) < slop
    val nearRight = abs(position.x - rect.right) < slop
    val nearTop = abs(position.y - rect.top) < slop
    val nearBottom = abs(position.y - rect.bottom) < slop
    val withinX = position.x > rect.left - slop && position.x < rect.right + slop
    val withinY = position.y > rect.top - slop && position.y < rect.bottom + slop
    return when {
        nearLeft && nearTop -> CropHandle.TOP_LEFT
        nearRight && nearTop -> CropHandle.TOP_RIGHT
        nearLeft && nearBottom -> CropHandle.BOTTOM_LEFT
        nearRight && nearBottom -> CropHandle.BOTTOM_RIGHT
        nearLeft && withinY -> CropHandle.LEFT
        nearRight && withinY -> CropHandle.RIGHT
        nearTop && withinX -> CropHandle.TOP
        nearBottom && withinX -> CropHandle.BOTTOM
        rect.contains(position) -> CropHandle.MOVE
        else -> CropHandle.NONE
    }
}

private fun applyDrag(crop: NormalizedRect, handle: CropHandle, dx: Float, dy: Float): NormalizedRect {
    var left = crop.left
    var top = crop.top
    var right = crop.right
    var bottom = crop.bottom
    if (handle == CropHandle.MOVE) {
        val width = right - left
        val height = bottom - top
        left = (left + dx).coerceIn(0f, 1f - width)
        top = (top + dy).coerceIn(0f, 1f - height)
        return NormalizedRect(left, top, left + width, top + height)
    }
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

/** Rotate / mirror / reset tools in the One UI toolbar style. */
@Composable
private fun EditorToolbar(
    enabled: Boolean,
    onRotate: () -> Unit,
    onMirrorHorizontally: () -> Unit,
    onMirrorVertically: () -> Unit,
    onReset: () -> Unit
) {
    val tools: @Composable (Color) -> Unit = { tint ->
        EditorTool(Icons.Rounded.Rotate90DegreesCw, strings.rotate, tint, enabled, onRotate)
        EditorTool(Icons.Rounded.Flip, strings.flipHorizontal, tint, enabled, onMirrorHorizontally)
        EditorTool(Icons.Rounded.Flip, strings.flipVertical, tint, enabled, onMirrorVertically, iconRotation = 90f)
        EditorTool(Icons.Rounded.Restore, strings.reset, tint, enabled, onReset)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) { tools(Color.White) }
    }
}

@Composable
private fun EditorTool(
    icon: ImageVector,
    label: String,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    iconRotation: Float = 0f
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .pressScale(interaction, 0.88f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = ripple(), enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) tint else tint.copy(alpha = 0.4f),
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer { rotationZ = iconRotation }
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = if (enabled) tint else tint.copy(alpha = 0.4f), fontSize = 11.sp, maxLines = 1)
    }
}
