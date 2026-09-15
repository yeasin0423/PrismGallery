package com.example.amoledgallery.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.example.amoledgallery.data.MediaThumbnail
import java.util.Locale

/**
 * Data model representing a media item (image or video) from MediaStore.
 * Annotated with @Immutable for compose recomposition optimization.
 *
 * @property id Unique MediaStore ID
 * @property uri Content Uri pointing to the media file
 * @property isVideo True if the media item is a video, false if image
 * @property dateAdded Timestamp (in seconds) when media was added
 * @property durationMs Duration in milliseconds (for videos)
 * @property displayName File name (e.g. IMG_20240912.jpg)
 * @property bucketId MediaStore bucket ID
 * @property bucketDisplayName Folder name
 * @property relativePath Relative folder path (e.g. Pictures/Vacation/)
 * @property dateModified Timestamp (in seconds) when media was modified
 * @property isTrashed True if media is in trash
 * @property mimeType MIME type (e.g. image/jpeg, image/gif, video/mp4)
 * @property dataPath Absolute filesystem path if available
 */
@Immutable
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val isVideo: Boolean,
    val dateAdded: Long,
    val durationMs: Long = 0L,
    val displayName: String = "",
    val bucketId: String = "",
    val bucketDisplayName: String = "",
    val relativePath: String = "",
    val dateModified: Long = 0L,
    val isTrashed: Boolean = false,
    val mimeType: String = "",
    val dataPath: String = "",
    /** When the photo/video was captured (seconds), from EXIF / container metadata; 0 if unknown. */
    val dateTaken: Long = 0L,
    /** Pixel size as stored by MediaStore (before [orientation] is applied); 0 if unknown. */
    val width: Int = 0,
    val height: Int = 0,
    /** Clockwise rotation in degrees that has to be applied to show the media upright. */
    val orientation: Int = 0
) {
    /** Width / height as displayed on screen, or 0 if the size is unknown. */
    val displayAspectRatio: Float
        get() {
            if (width <= 0 || height <= 0) return 0f
            val rotated = orientation % 180 != 0
            return if (rotated) height.toFloat() / width else width.toFloat() / height
        }

    /**
     * The date the gallery sorts and groups by (seconds): when it was taken, else when the file was
     * last modified. DATE_ADDED is only a fallback - moving a file to the other storage volume creates
     * a new MediaStore row "added" right now, which pushed years-old photos to the top.
     */
    val date: Long
        get() = when {
            dateTaken > 0L -> dateTaken
            dateModified > 0L -> dateModified
            else -> dateAdded
        }

    val isGif: Boolean
        get() = mimeType.equals("image/gif", ignoreCase = true) ||
                displayName.endsWith(".gif", ignoreCase = true)

    val isImage: Boolean
        get() = !isVideo && !isGif

    val thumbnail: MediaThumbnail
        get() = MediaThumbnail(
            uri = uri,
            isVideo = isVideo,
            id = id,
            dateModified = dateModified
        )

    /**
     * Formats duration into MM:SS or HH:MM:SS string.
     */
    fun formattedDuration(): String {
        if (!isVideo || durationMs <= 0) return ""
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }
}
