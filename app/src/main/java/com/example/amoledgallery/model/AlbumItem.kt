package com.example.amoledgallery.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.example.amoledgallery.data.MediaThumbnail

/**
 * Data model representing an Album (folder) containing media items.
 * Supports hierarchical sub-album relationships and filesystem paths.
 * Annotated with @Immutable for compose recomposition skip optimization.
 *
 * @property bucketId MediaStore BUCKET_ID uniquely identifying the folder
 * @property name Folder display name (BUCKET_DISPLAY_NAME)
 * @property count Number of media items in this album
 * @property coverUri Thumbnail Uri for the album cover
 * @property isSdCard True if this album is located on an external SD card
 * @property relativePath Relative directory path (e.g. "Pictures/Vacation/")
 * @property directoryPath Absolute folder path (e.g. "/storage/emulated/0/Pictures/Vacation")
 * @property parentDirectoryPath Absolute parent folder path
 * @property subAlbumCount Number of direct sub-albums inside this album
 */
@Immutable
data class AlbumItem(
    val bucketId: String,
    val name: String,
    val count: Int,
    val coverUri: Uri? = null,
    val isSdCard: Boolean = false,
    val relativePath: String = "",
    val directoryPath: String = "",
    val parentDirectoryPath: String = "",
    val subAlbumCount: Int = 0
) {
    val coverThumbnail: MediaThumbnail?
        get() = coverUri?.let { MediaThumbnail(it) }
}
