package com.gallery.app.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import kotlin.math.abs

/**
 * The eight adjustment sliders. Values are -1..1 and 0 keeps the photo untouched.
 *
 * The matrices are deliberately simple global transforms: they are cheap to combine, they behave the
 * same in the live preview and in the exported bitmap, and every one of them is the identity at 0.
 */
enum class EditorAdjustment { Brightness, Contrast, Saturation, Warmth, Tint, BlackPoint, WhitePoint, Highlights }

data class EditorAdjustments(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f,
    val tint: Float = 0f,
    val blackPoint: Float = 0f,
    val whitePoint: Float = 0f,
    val highlights: Float = 0f
) {
    val isNeutral: Boolean
        get() = abs(brightness) < 0.001f && abs(contrast) < 0.001f && abs(saturation) < 0.001f &&
                abs(warmth) < 0.001f && abs(tint) < 0.001f && abs(blackPoint) < 0.001f &&
                abs(whitePoint) < 0.001f && abs(highlights) < 0.001f

    fun valueOf(adjustment: EditorAdjustment): Float = when (adjustment) {
        EditorAdjustment.Brightness -> brightness
        EditorAdjustment.Contrast -> contrast
        EditorAdjustment.Saturation -> saturation
        EditorAdjustment.Warmth -> warmth
        EditorAdjustment.Tint -> tint
        EditorAdjustment.BlackPoint -> blackPoint
        EditorAdjustment.WhitePoint -> whitePoint
        EditorAdjustment.Highlights -> highlights
    }

    fun with(adjustment: EditorAdjustment, value: Float): EditorAdjustments = when (adjustment) {
        EditorAdjustment.Brightness -> copy(brightness = value)
        EditorAdjustment.Contrast -> copy(contrast = value)
        EditorAdjustment.Saturation -> copy(saturation = value)
        EditorAdjustment.Warmth -> copy(warmth = value)
        EditorAdjustment.Tint -> copy(tint = value)
        EditorAdjustment.BlackPoint -> copy(blackPoint = value)
        EditorAdjustment.WhitePoint -> copy(whitePoint = value)
        EditorAdjustment.Highlights -> copy(highlights = value)
    }
}

/** A named preset: the 21 Tulsi filter matrices, ported one to one. */
data class EditorFilter(val id: String, val name: String, val matrix: FloatArray)

private val IdentityMatrix = floatArrayOf(
    1f, 0f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f, 0f,
    0f, 0f, 1f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f
)

val EditorFilterNone = EditorFilter("none", "None", IdentityMatrix)

private fun filterMatrix(block: ColorMatrix.() -> Unit): FloatArray =
    ColorMatrix().apply(block).values

private fun saturationFilter(value: Float): FloatArray =
    filterMatrix { setToSaturation(value) }

/** Tulsi's Solar preset: a warm red scale multiplied by the saturation matrix's diagonal. */
private fun solarFilter(): FloatArray {
    val matrix = ColorMatrix()
    val saturation = ColorMatrix().apply { setToSaturation(1.5f) }
    matrix[0, 0] = 1.2f
    matrix[0, 0] *= saturation[0, 0]
    matrix[1, 1] *= saturation[1, 1]
    matrix[2, 2] *= saturation[2, 2]
    return matrix.values
}

val EditorFilters: List<EditorFilter> = listOf(
    EditorFilterNone,
    EditorFilter(
        "inverted", "Inverted",
        floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    EditorFilter(
        "vibrant", "Vibrant",
        filterMatrix {
            setToSaturation(1.4f)
            this[0, 4] = 1f
            this[1, 4] = 1f
            this[2, 4] = 1f
        }
    ),
    EditorFilter(
        "bw", "B&W",
        floatArrayOf(
            1.25f, 1.25f, 1.25f, 0f, -160f,
            1.25f, 1.25f, 1.25f, 0f, -160f,
            1.25f, 1.25f, 1.25f, 0f, -160f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    EditorFilter(
        "film", "Film",
        floatArrayOf(
            1.438f, -0.062f, -0.062f, 0f, -0.03f * 255f,
            -0.122f, 1.378f, -0.122f, 0f, 0.05f * 255f,
            -0.016f, -0.016f, 1.483f, 0f, -0.02f * 255f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    EditorFilter(
        "grayscale", "Grayscale",
        filterMatrix {
            setToSaturation(0f)
            this[0, 4] = 1f
            this[1, 4] = 1f
            this[2, 4] = 1f
        }
    ),
    EditorFilter(
        "sepia", "Sepia",
        floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    EditorFilter(
        "bgr", "BGR",
        floatArrayOf(
            0f, 0f, 1f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            1f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    EditorFilter(
        "warm", "Warm",
        floatArrayOf(
            1.3f, 0f, 0f, 0f, 0f,
            0.2f, 0.9f, 0f, 0f, 0f,
            0.2f, 0f, 0.9f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    EditorFilter(
        "cool", "Cool",
        floatArrayOf(
            0.9f, 0f, 0.2f, 0f, 0f,
            0f, 0.9f, 0.2f, 0f, 0f,
            0f, 0f, 1.3f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    EditorFilter(
        "vintage", "Vintage",
        floatArrayOf(
            0.9f, 0.5f, 0.2f, 0f, 0f,
            0.4f, 0.8f, 0.2f, 0f, 0f,
            0.2f, 0.3f, 0.7f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    EditorFilter(
        "posterize", "Posterize",
        concat(
            floatArrayOf(
                0.5f, 0f, 0f, 0f, 56f,
                0f, 0.5f, 0f, 0f, 56f,
                0f, 0f, 0.5f, 0f, 56f,
                0f, 0f, 0f, 1f, 0f
            ),
            saturationFilter(0.8f)
        )
    ),
    EditorFilter(
        "glow", "Glow",
        floatArrayOf(
            1.6f, 0f, 0f, 0f, 0f,
            0f, 1.6f, 0f, 0f, 0f,
            0f, 0f, 1.6f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    EditorFilter(
        "mushroom", "Mushroom",
        concat(
            concat(
                filterMatrix { setToRotateRed(-22f) },
                filterMatrix { setToRotateGreen(47f) }
            ),
            filterMatrix { setToRotateBlue(62f) }
        )
    ),
    EditorFilter("solar", "Solar", solarFilter()),
    EditorFilter(
        "peachy", "Peachy",
        floatArrayOf(
            1.1f, 0.1f, 0f, 0f, 20f,
            0.05f, 1.05f, 0f, 0f, 10f,
            0f, 0.05f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    EditorFilter(
        "pastel", "Pastel",
        saturationFilter(0.8f).copyOf().also { matrix ->
            matrix[0] *= 1.1f
            matrix[6] *= 1.1f
            matrix[12] *= 1.1f
            matrix[2] *= 0.1f
            matrix[4] = 10f
            matrix[9] = 10f
            matrix[14] = 10f
        }
    ),
    EditorFilter(
        "rustic", "Rustic",
        floatArrayOf(
            1.65f, 0f, 0f, 0f, -45f,
            0f, 1.65f, 0f, 0f, -45f,
            0f, 0f, 1.65f, 0f, -45f,
            0f, 0f, 1f, 0f, 1f
        )
    ),
    EditorFilter(
        "dried", "Dried",
        filterMatrix {
            setToSaturation(0.6f)
            this[0, 0] *= 1.1f
            this[1, 1] *= 1.1f
            this[2, 2] *= 1.1f
            this[0, 4] = 25f
            this[1, 4] = 25f
            this[2, 4] = 25f
        }
    ),
    EditorFilter(
        "toasty", "Toasty",
        filterMatrix {
            setToSaturation(0.6f)
            for (row in 0..2) {
                for (column in 0..2) this[row, column] *= 1.25f
            }
            this[0, 4] = -45f
            this[1, 4] = -45f
            this[2, 4] = -45f
        }
    ),
    EditorFilter(
        "shimmer", "Shimmer",
        floatArrayOf(
            34f, 0f, 0f, 0f, -831f,
            0f, 9f, 0f, 0f, -831f,
            0f, 0f, 48f, 0f, -831f,
            0f, 0f, 0f, 1f, 0f
        )
    )
)

/** The combined matrix: all adjustments first, then the selected filter on top. */
fun editorColorMatrix(adjustments: EditorAdjustments, filter: EditorFilter): FloatArray {
    var matrix = IdentityMatrix.copyOf()
    matrix = concat(matrix, brightnessMatrix(adjustments.brightness))
    matrix = concat(matrix, contrastMatrix(adjustments.contrast))
    matrix = concat(matrix, saturationMatrix(adjustments.saturation))
    matrix = concat(matrix, warmthMatrix(adjustments.warmth))
    matrix = concat(matrix, tintMatrix(adjustments.tint))
    matrix = concat(matrix, blackPointMatrix(adjustments.blackPoint))
    matrix = concat(matrix, whitePointMatrix(adjustments.whitePoint))
    matrix = concat(matrix, highlightsMatrix(adjustments.highlights))
    return concat(matrix, filter.matrix)
}

/**
 * Returns a matrix that applies [first] and then [second]. Color matrices are 4x5, treated here as
 * 5x5 affine transforms with the implicit last row (0, 0, 0, 0, 1).
 */
internal fun concat(first: FloatArray, second: FloatArray): FloatArray {
    val result = FloatArray(20)
    for (row in 0..3) {
        for (col in 0..4) {
            var sum = 0f
            for (k in 0..3) {
                sum += second[row * 5 + k] * first[k * 5 + col]
            }
            if (col == 4) sum += second[row * 5 + 4]
            result[row * 5 + col] = sum
        }
    }
    return result
}

private fun brightnessMatrix(value: Float): FloatArray {
    if (value == 0f) return IdentityMatrix
    val offset = value * 127f
    return floatArrayOf(
        1f, 0f, 0f, 0f, offset,
        0f, 1f, 0f, 0f, offset,
        0f, 0f, 1f, 0f, offset,
        0f, 0f, 0f, 1f, 0f
    )
}

private fun contrastMatrix(value: Float): FloatArray {
    if (value == 0f) return IdentityMatrix
    val contrast = 1f + value
    val offset = 0.5f * (1f - contrast) * 255f
    return floatArrayOf(
        contrast, 0f, 0f, 0f, offset,
        0f, contrast, 0f, 0f, offset,
        0f, 0f, contrast, 0f, offset,
        0f, 0f, 0f, 1f, 0f
    )
}

private fun saturationMatrix(value: Float): FloatArray =
    if (value == 0f) IdentityMatrix else saturationFilter(1f + value)

private fun warmthMatrix(value: Float): FloatArray {
    if (value == 0f) return IdentityMatrix
    val warm = value.coerceIn(-1f, 1f)
    return floatArrayOf(
        1f + 0.25f * warm, 0f, 0f, 0f, 0f,
        0f, 1f + 0.05f * warm, 0f, 0f, 0f,
        0f, 0f, 1f - 0.25f * warm, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )
}

private fun tintMatrix(value: Float): FloatArray {
    if (value == 0f) return IdentityMatrix
    val strength = abs(value.coerceIn(-1f, 1f)) * 0.18f
    // Positive tint warms towards orange, negative tint cools towards blue.
    val color = if (value > 0f) Color(0.95f, 0.55f, 0.2f) else Color(0.25f, 0.55f, 1f)
    return floatArrayOf(
        1f, 0f, 0f, 0f, color.red * 255f * strength,
        0f, 1f, 0f, 0f, color.green * 255f * strength,
        0f, 0f, 1f, 0f, color.blue * 255f * strength,
        0f, 0f, 0f, 1f, 0f
    )
}

private fun blackPointMatrix(value: Float): FloatArray {
    if (value == 0f) return IdentityMatrix
    val offset = -100f * value
    return floatArrayOf(
        1f, 0f, 0f, 0f, offset,
        0f, 1f, 0f, 0f, offset,
        0f, 0f, 1f, 0f, offset,
        0f, 0f, 0f, 1f, 0f
    )
}

private fun whitePointMatrix(value: Float): FloatArray {
    if (value == 0f) return IdentityMatrix
    val scale = 1f + 0.5f * value.coerceIn(-1f, 1f)
    return floatArrayOf(
        scale, 0f, 0f, 0f, 0f,
        0f, scale, 0f, 0f, 0f,
        0f, 0f, scale, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )
}

private fun highlightsMatrix(value: Float): FloatArray {
    if (value == 0f) return IdentityMatrix
    val v = value.coerceIn(-1f, 1f)
    return if (v > 0f) {
        // Lift towards white: out = in * (1 - 0.5v) + 0.5v * 255.
        val scale = 1f - 0.5f * v
        val offset = 0.5f * v * 255f
        floatArrayOf(
            scale, 0f, 0f, 0f, offset,
            0f, scale, 0f, 0f, offset,
            0f, 0f, scale, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        )
    } else {
        val scale = 1f + 0.5f * v
        floatArrayOf(
            scale, 0f, 0f, 0f, 0f,
            0f, scale, 0f, 0f, 0f,
            0f, 0f, scale, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }
}
