package com.example.amoledgallery.data

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.example.amoledgallery.model.AlbumItem
import com.example.amoledgallery.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed interface RenameResult {
    data class Success(val newName: String) : RenameResult
    data class PermissionRequired(val pendingIntent: PendingIntent, val targetName: String) : RenameResult
    data class Error(val message: String) : RenameResult
}

sealed interface MoveResult {
    data class Success(val count: Int) : MoveResult
    data class PermissionRequired(
        val pendingIntent: PendingIntent,
        val targetAlbum: AlbumItem,
        val itemsToMove: List<MediaItem>
    ) : MoveResult
    data class Error(val message: String) : MoveResult
}

/**
 * Repository responsible for querying images, videos, albums and trashed media using ContentResolver.
 * Utilizes MediaStore.VOLUME_EXTERNAL to seamlessly query both internal
 * storage and inserted MicroSD card storage.
 *
 * Supports:
 * - Persistent hidden albums filtering via SharedPreferences
 * - Scoped Storage and filesystem media move operations across albums
 * - MediaStore Trash and Delete API
 */
class MediaRepository(private val context: Context) {

    private val contentResolver: ContentResolver get() = context.contentResolver

    private val hiddenPrefs = context.getSharedPreferences("amoled_gallery_hidden_prefs", Context.MODE_PRIVATE)
    private val KEY_HIDDEN_BUCKETS = "hidden_bucket_ids"

    /**
     * Gets set of persistently hidden album BUCKET_IDs.
     */
    fun getHiddenBucketIds(): Set<String> {
        return hiddenPrefs.getStringSet(KEY_HIDDEN_BUCKETS, emptySet()) ?: emptySet()
    }

    /**
     * Toggles an album's hidden state and persists to SharedPreferences.
     */
    fun setAlbumHidden(bucketId: String, hidden: Boolean) {
        val current = getHiddenBucketIds().toMutableSet()
        if (hidden) {
            current.add(bucketId)
        } else {
            current.remove(bucketId)
        }
        hiddenPrefs.edit().putStringSet(KEY_HIDDEN_BUCKETS, current).apply()
    }

    /**
     * Queries all images and videos across all external volumes (Internal + MicroSD).
     * Optionally filtered by [bucketId] for album view.
     * When [includeHidden] is false and [bucketId] is null (main feed), hidden albums are filtered out.
     */
    suspend fun getAllMedia(
        bucketId: String? = null,
        includeHidden: Boolean = false
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = ArrayList<MediaItem>()

        val imagesUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATA
        )

        val selection = if (bucketId != null) "${MediaStore.Images.Media.BUCKET_ID} = ?" else null
        val selectionArgs = if (bucketId != null) arrayOf(bucketId) else null
        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

        try {
            contentResolver.query(
                imagesUri,
                imageProjection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                parseCursor(cursor, imagesUri, isVideo = false, mediaList)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val videosUri = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.RELATIVE_PATH,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATA
        )

        val videoSelection = if (bucketId != null) "${MediaStore.Video.Media.BUCKET_ID} = ?" else null
        val videoSelectionArgs = if (bucketId != null) arrayOf(bucketId) else null

        try {
            contentResolver.query(
                videosUri,
                videoProjection,
                videoSelection,
                videoSelectionArgs,
                sortOrder
            )?.use { cursor ->
                parseCursor(cursor, videosUri, isVideo = true, mediaList)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Filter out hidden bucket IDs if querying main feed ("Bilder")
        if (!includeHidden && bucketId == null) {
            val hidden = getHiddenBucketIds()
            if (hidden.isNotEmpty()) {
                mediaList.removeAll { it.bucketId in hidden }
            }
        }

        mediaList.sortByDescending { it.dateAdded }
        mediaList
    }

    /**
     * Parses cursor entries into [MediaItem] instances.
     */
    private fun parseCursor(
        cursor: Cursor,
        baseUri: Uri,
        isVideo: Boolean,
        outList: MutableList<MediaItem>,
        isTrashed: Boolean = false
    ) {
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
        val dateAddedColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
        val nameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
        val bucketIdColumn = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_ID)
        val bucketNameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
        val relativePathColumn = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
        val dateModifiedColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
        val mimeTypeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
        val dataColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
        val durationColumn = if (isVideo) cursor.getColumnIndex(MediaStore.Video.Media.DURATION) else -1

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val dateAdded = if (dateAddedColumn != -1) cursor.getLong(dateAddedColumn) else 0L
            val displayName = if (nameColumn != -1) cursor.getString(nameColumn) ?: "" else ""
            val bucketId = if (bucketIdColumn != -1) cursor.getString(bucketIdColumn) ?: "" else ""
            val bucketDisplayName = if (bucketNameColumn != -1) cursor.getString(bucketNameColumn) ?: "" else ""
            val relativePath = if (relativePathColumn != -1) cursor.getString(relativePathColumn) ?: "" else ""
            val dateModified = if (dateModifiedColumn != -1) cursor.getLong(dateModifiedColumn) else 0L
            val mimeType = if (mimeTypeColumn != -1) cursor.getString(mimeTypeColumn) ?: "" else ""
            val dataPath = if (dataColumn != -1) cursor.getString(dataColumn) ?: "" else ""
            val durationMs = if (durationColumn != -1) cursor.getLong(durationColumn) else 0L
            val contentUri = ContentUris.withAppendedId(baseUri, id)

            outList.add(
                MediaItem(
                    id = id,
                    uri = contentUri,
                    isVideo = isVideo,
                    dateAdded = dateAdded,
                    durationMs = durationMs,
                    displayName = displayName,
                    bucketId = bucketId,
                    bucketDisplayName = bucketDisplayName,
                    relativePath = relativePath,
                    dateModified = dateModified,
                    isTrashed = isTrashed,
                    mimeType = mimeType,
                    dataPath = dataPath
                )
            )
        }
    }

    /**
     * Queries all folders/albums (BUCKET_DISPLAY_NAME) containing media.
     * When [includeHidden] is false, albums in [getHiddenBucketIds] are filtered out.
     */
    suspend fun getAlbums(includeHidden: Boolean = false): List<AlbumItem> = withContext(Dispatchers.IO) {
        val allMedia = getAllMedia(includeHidden = includeHidden)
        if (allMedia.isEmpty()) return@withContext emptyList()

        val hiddenBuckets = if (!includeHidden) getHiddenBucketIds() else emptySet()

        // Group media by BUCKET_ID
        val bucketGroups = allMedia.groupBy { it.bucketId }
        val rawAlbums = ArrayList<AlbumItem>()

        for ((bucketId, items) in bucketGroups) {
            if (bucketId.isBlank() || items.isEmpty()) continue
            if (!includeHidden && bucketId in hiddenBuckets) continue

            val firstItem = items.first()
            val folderName = firstItem.bucketDisplayName.ifBlank { "Unbekannt" }
            val dataPath = items.firstOrNull { it.dataPath.isNotBlank() }?.dataPath ?: ""
            val dirFile = if (dataPath.isNotBlank()) File(dataPath).parentFile else null
            val directoryPath = dirFile?.absolutePath ?: ""
            val parentDirectoryPath = dirFile?.parentFile?.absolutePath ?: ""
            val isSdCard = isPathOnSdCard(dataPath) || isPathOnSdCard(firstItem.relativePath)

            rawAlbums.add(
                AlbumItem(
                    bucketId = bucketId,
                    name = folderName,
                    count = items.size,
                    coverUri = firstItem.uri,
                    isSdCard = isSdCard,
                    relativePath = firstItem.relativePath,
                    directoryPath = directoryPath,
                    parentDirectoryPath = parentDirectoryPath
                )
            )
        }

        // Calculate subAlbumCount for each album
        val albumsWithSubCount = rawAlbums.map { album ->
            val subCount = if (album.directoryPath.isNotBlank()) {
                rawAlbums.count { other ->
                    other.directoryPath != album.directoryPath &&
                    normalizeDirPath(other.parentDirectoryPath) == normalizeDirPath(album.directoryPath)
                }
            } else 0
            album.copy(subAlbumCount = subCount)
        }

        // Sort: Camera / DCIM / Screenshots first, then alphabetically
        albumsWithSubCount.sortedWith(
            compareBy<AlbumItem> {
                val lower = it.name.lowercase()
                when {
                    lower == "camera" || lower == "kamera" -> 0
                    lower.contains("dcim") -> 1
                    lower.contains("screenshot") -> 2
                    else -> 3
                }
            }.thenBy { it.name.lowercase() }
        )
    }

    /**
     * Returns list of all currently hidden albums.
     */
    suspend fun getHiddenAlbums(): List<AlbumItem> = withContext(Dispatchers.IO) {
        val hiddenIds = getHiddenBucketIds()
        if (hiddenIds.isEmpty()) return@withContext emptyList()
        val allAlbums = getAlbums(includeHidden = true)
        allAlbums.filter { it.bucketId in hiddenIds }
    }

    /**
     * Filters top-level root albums (albums that do not have a parent album in the gallery).
     */
    fun getRootAlbums(allAlbums: List<AlbumItem>): List<AlbumItem> {
        val allDirPaths = allAlbums.mapNotNull { it.directoryPath.takeIf { p -> p.isNotBlank() } }
            .map { normalizeDirPath(it) }
            .toSet()

        return allAlbums.filter { album ->
            val parent = normalizeDirPath(album.parentDirectoryPath)
            parent.isBlank() || !allDirPaths.contains(parent)
        }
    }

    /**
     * Gets direct sub-albums for a given [parentAlbum], combining MediaStore albums
     * and any newly created empty directories on disk.
     */
    fun getSubAlbums(parentAlbum: AlbumItem, allAlbums: List<AlbumItem>): List<AlbumItem> {
        val parentNormalized = normalizeDirPath(parentAlbum.directoryPath)
        val subAlbums = allAlbums.filter { other ->
            other.directoryPath != parentAlbum.directoryPath &&
            normalizeDirPath(other.parentDirectoryPath) == parentNormalized
        }.toMutableList()

        // Also check filesystem for newly created empty subdirectories
        if (parentAlbum.directoryPath.isNotBlank()) {
            val parentDir = File(parentAlbum.directoryPath)
            if (parentDir.exists() && parentDir.isDirectory) {
                val diskDirs = parentDir.listFiles { file -> file.isDirectory && !file.isHidden }
                diskDirs?.forEach { subDir ->
                    val normalizedSub = normalizeDirPath(subDir.absolutePath)
                    if (subAlbums.none { normalizeDirPath(it.directoryPath) == normalizedSub }) {
                        subAlbums.add(
                            AlbumItem(
                                bucketId = "disk_${subDir.absolutePath.hashCode()}",
                                name = subDir.name,
                                count = 0,
                                coverUri = null,
                                isSdCard = parentAlbum.isSdCard,
                                relativePath = "${parentAlbum.relativePath.trimEnd('/')}/${subDir.name}/",
                                directoryPath = subDir.absolutePath,
                                parentDirectoryPath = parentAlbum.directoryPath,
                                subAlbumCount = 0
                            )
                        )
                    }
                }
            }
        }

        return subAlbums.sortedBy { it.name.lowercase() }
    }

    private fun normalizeDirPath(path: String): String {
        return path.trimEnd('/', '\\').lowercase()
    }

    /**
     * Moves media items physically into [targetAlbum] directory using Scoped Storage / MediaStore operations.
     * Updates MediaStore immediately and invalidates local thumbnail caches.
     */
    suspend fun moveMediaItems(
        items: List<MediaItem>,
        targetAlbum: AlbumItem,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): MoveResult = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext MoveResult.Success(0)

        val targetDir = if (targetAlbum.directoryPath.isNotBlank()) {
            File(targetAlbum.directoryPath)
        } else {
            if (targetAlbum.isSdCard) {
                val sdDir = getSdCardPicturesDirectory()
                    ?: return@withContext MoveResult.Error("SD-Karte nicht verfügbar")
                File(sdDir, targetAlbum.name)
            } else {
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), targetAlbum.name)
            }
        }

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val targetRelPath = if (targetAlbum.relativePath.isNotBlank()) {
            val trimmed = targetAlbum.relativePath.trim('/')
            "$trimmed/"
        } else {
            "Pictures/${targetAlbum.name}/"
        }

        var movedCount = 0
        val securityExceptionItems = mutableListOf<MediaItem>()
        val thumbnailManager = ThumbnailManager.getInstance(context)

        for ((index, item) in items.withIndex()) {
            val isItemOnSd = isPathOnSdCard(item.dataPath) || isPathOnSdCard(item.relativePath)
            val isSameVolume = (isItemOnSd == targetAlbum.isSdCard)
            var itemMoved = false

            // Fast path: On same storage volume with Android 10+ Scoped Storage,
            // updating RELATIVE_PATH physically moves the file on disk via MediaStore
            if (isSameVolume && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, targetRelPath)
                }
                try {
                    val updated = contentResolver.update(item.uri, values, null, null)
                    if (updated > 0) {
                        movedCount++
                        // Invalidate thumbnail cache since modification timestamp changed
                        thumbnailManager.getThumbnailFile(item).delete()
                        onProgress?.invoke(index + 1, items.size)
                        itemMoved = true
                    }
                } catch (_: Exception) {
                    // Fall back to physical file copy below
                }
            }

            if (!itemMoved) {
                // Cross-volume (Internal <-> MicroSD) or filesystem copy fallback
                try {
                    val fileName = item.displayName.ifBlank { "media_${item.id}.jpg" }
                    var copied = false
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, targetRelPath)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                        }
                    }

                    val collection = if (item.isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    val destUri = contentResolver.insert(collection, values)

                    if (destUri != null) {
                        try {
                            contentResolver.openInputStream(item.uri)?.use { input ->
                                contentResolver.openOutputStream(destUri)?.use { output ->
                                    input.copyTo(output)
                                    output.flush()
                                    copied = true
                                }
                            }
                        } finally {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                values.clear()
                                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                                contentResolver.update(destUri, values, null, null)
                            }
                        }
                    }

                    if (copied) {
                        // Delete original file from MediaStore
                        try {
                            val deleted = contentResolver.delete(item.uri, null, null)
                            if (deleted == 0) {
                                securityExceptionItems.add(item)
                            }
                        } catch (_: SecurityException) {
                            securityExceptionItems.add(item)
                        } catch (_: Exception) {}

                        thumbnailManager.getThumbnailFile(item).delete()
                        movedCount++
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                onProgress?.invoke(index + 1, items.size)
            }
        }

        if (securityExceptionItems.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pendingIntent = MediaStore.createWriteRequest(
                contentResolver,
                securityExceptionItems.map { it.uri }
            )
            return@withContext MoveResult.PermissionRequired(pendingIntent, targetAlbum, securityExceptionItems)
        }

        MoveResult.Success(movedCount)
    }

    /**
     * Queries trashed items using MediaStore.QUERY_ARG_MATCH_TRASHED (Android 11+).
     */
    suspend fun getTrashedMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return@withContext emptyList()
        }

        val trashedList = ArrayList<MediaItem>()
        val imagesUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val videosUri = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.DATE_MODIFIED
        )

        val bundle = Bundle().apply {
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
            putString(
                ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )
        }

        try {
            contentResolver.query(imagesUri, projection, bundle, null)?.use { cursor ->
                parseCursor(cursor, imagesUri, isVideo = false, trashedList, isTrashed = true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.RELATIVE_PATH,
            MediaStore.Video.Media.DATE_MODIFIED
        )

        try {
            contentResolver.query(videosUri, videoProjection, bundle, null)?.use { cursor ->
                parseCursor(cursor, videosUri, isVideo = true, trashedList, isTrashed = true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        trashedList.sortByDescending { it.dateModified }
        trashedList
    }

    /**
     * Trashes or restores media items using MediaStore.createTrashRequest.
     * Returns a [PendingIntent] for user confirmation dialog on Android 11+.
     */
    fun createTrashRequest(uris: List<Uri>, trash: Boolean): PendingIntent? {
        if (uris.isEmpty()) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createTrashRequest(contentResolver, uris, trash)
        } else {
            uris.forEach { contentResolver.delete(it, null, null) }
            null
        }
    }

    /**
     * Permanently deletes items from MediaStore.
     * Returns a [PendingIntent] on Android 11+.
     */
    fun createDeleteRequest(uris: List<Uri>): PendingIntent? {
        if (uris.isEmpty()) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createDeleteRequest(contentResolver, uris)
        } else {
            uris.forEach { contentResolver.delete(it, null, null) }
            null
        }
    }

    /**
     * Renames a media file by updating MediaStore.MediaColumns.DISPLAY_NAME.
     */
    suspend fun renameMedia(
        uri: Uri,
        oldDisplayName: String,
        newDisplayName: String
    ): RenameResult = withContext(Dispatchers.IO) {
        val trimmedNewName = newDisplayName.trim()
        if (trimmedNewName.isBlank()) {
            return@withContext RenameResult.Error("Name darf nicht leer sein.")
        }

        val finalName = if (!trimmedNewName.contains(".") && oldDisplayName.contains(".")) {
            val extension = oldDisplayName.substringAfterLast(".")
            "$trimmedNewName.$extension"
        } else {
            trimmedNewName
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, finalName)
        }

        try {
            val rows = contentResolver.update(uri, values, null, null)
            if (rows > 0) {
                RenameResult.Success(finalName)
            } else {
                RenameResult.Error("Umbenennen fehlgeschlagen.")
            }
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pendingIntent = MediaStore.createWriteRequest(contentResolver, listOf(uri))
                RenameResult.PermissionRequired(pendingIntent, finalName)
            } else {
                RenameResult.Error(e.localizedMessage ?: "Berechtigung verweigert.")
            }
        } catch (e: Exception) {
            RenameResult.Error(e.localizedMessage ?: "Fehler beim Umbenennen.")
        }
    }

    /**
     * Creates a new album folder either on internal storage or SD card.
     */
    suspend fun createAlbumFolder(albumName: String, onSdCard: Boolean): Result<File> = withContext(Dispatchers.IO) {
        try {
            val cleanName = albumName.trim()
            if (cleanName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Name darf nicht leer sein."))
            }

            val targetDir: File = if (onSdCard) {
                val sdDir = getSdCardPicturesDirectory()
                    ?: return@withContext Result.failure(IllegalStateException("SD-Karte nicht gefunden oder nicht beschreibbar."))
                File(sdDir, cleanName)
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                File(picturesDir, cleanName)
            }

            if (!targetDir.exists()) {
                val created = targetDir.mkdirs()
                if (!created && !targetDir.exists()) {
                    return@withContext Result.failure(IllegalStateException("Ordner konnte nicht erstellt werden."))
                }
            }

            Result.success(targetDir)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a new sub-album folder inside [parentAlbum].
     */
    suspend fun createSubAlbumFolder(parentAlbum: AlbumItem, subAlbumName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val cleanName = subAlbumName.trim()
            if (cleanName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Name darf nicht leer sein."))
            }

            val parentDir: File = if (parentAlbum.directoryPath.isNotBlank()) {
                File(parentAlbum.directoryPath)
            } else {
                if (parentAlbum.isSdCard) {
                    val sdDir = getSdCardPicturesDirectory()
                        ?: return@withContext Result.failure(IllegalStateException("SD-Karte nicht gefunden oder nicht beschreibbar."))
                    File(sdDir, parentAlbum.relativePath)
                } else {
                    val root = Environment.getExternalStorageDirectory()
                    File(root, parentAlbum.relativePath)
                }
            }

            if (!parentDir.exists()) {
                parentDir.mkdirs()
            }

            val targetDir = File(parentDir, cleanName)
            if (!targetDir.exists()) {
                val created = targetDir.mkdirs()
                if (!created && !targetDir.exists()) {
                    return@withContext Result.failure(IllegalStateException("Unterordner konnte nicht erstellt werden."))
                }
            }

            Result.success(targetDir)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks if SD card storage is available.
     */
    fun isSdCardAvailable(): Boolean {
        return getSdCardPicturesDirectory() != null
    }

    private fun getSdCardPicturesDirectory(): File? {
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
        if (storageManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val sdVolume = storageManager.storageVolumes.firstOrNull { it.isRemovable && it.state == Environment.MEDIA_MOUNTED }
            val dir = sdVolume?.directory
            if (dir != null) {
                val pictures = File(dir, "Pictures")
                if (!pictures.exists()) pictures.mkdirs()
                return pictures
            }
        }

        // Fallback check through external files dirs
        val externalDirs = ContextCompat.getExternalFilesDirs(context, null)
        for (dir in externalDirs) {
            if (dir != null && Environment.isExternalStorageRemovable(dir)) {
                return dir
            }
        }
        return null
    }

    private fun isPathOnSdCard(path: String): Boolean {
        return path.contains("/storage/") && !path.contains("/emulated/")
    }

    /**
     * Renames an album folder physically on disk and updates RELATIVE_PATH in MediaStore.
     */
    suspend fun renameAlbum(album: AlbumItem, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanName = newName.trim()
            if (cleanName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Name darf nicht leer sein."))
            }

            var targetDir: File? = null
            if (album.directoryPath.isNotBlank()) {
                val currentDir = File(album.directoryPath)
                if (currentDir.exists() && currentDir.isDirectory) {
                    val parent = currentDir.parentFile ?: currentDir
                    val dest = File(parent, cleanName)
                    if (currentDir.renameTo(dest)) {
                        targetDir = dest
                    }
                }
            }

            // Calculate new relative path
            val oldRel = album.relativePath.trim('/')
            val newRelPath = if (oldRel.contains('/')) {
                val prefix = oldRel.substringBeforeLast('/')
                "$prefix/$cleanName/"
            } else {
                "Pictures/$cleanName/"
            }

            // Update all media items belonging to this album in MediaStore
            val items = getAllMedia(album.bucketId, includeHidden = true)
            val thumbnailManager = ThumbnailManager.getInstance(context)
            for (item in items) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, newRelPath)
                    }
                    try {
                        contentResolver.update(item.uri, values, null, null)
                    } catch (_: Exception) {}
                }
                thumbnailManager.getThumbnailFile(item).delete()
            }

            // Trigger rescan of the renamed folder
            targetDir?.let { dir ->
                val files = dir.listFiles { f -> f.isFile }?.map { it.absolutePath }?.toTypedArray()
                if (!files.isNullOrEmpty()) {
                    MediaScannerConnection.scanFile(context, files, null) { _, _ -> }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes an album by moving all contained media items to the MediaStore Trash
     * and deleting the directory on disk.
     */
    suspend fun deleteAlbum(album: AlbumItem): Result<PendingIntent?> = withContext(Dispatchers.IO) {
        try {
            val items = getAllMedia(album.bucketId, includeHidden = true)
            val thumbnailManager = ThumbnailManager.getInstance(context)

            if (items.isNotEmpty()) {
                val uris = items.map { it.uri }
                val pendingIntent = createTrashRequest(uris, trash = true)
                items.forEach { thumbnailManager.getThumbnailFile(it).delete() }

                if (pendingIntent != null) {
                    return@withContext Result.success(pendingIntent)
                }
            }

            // Remove empty folder on disk
            if (album.directoryPath.isNotBlank()) {
                val dir = File(album.directoryPath)
                if (dir.exists()) {
                    dir.deleteRecursively()
                }
            }

            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Physically migrates all files of an album between Internal Storage and MicroSD card.
     * Updates the MediaStore index and invalidates thumbnail caches.
     */
    suspend fun migrateAlbumStorage(album: AlbumItem, toSdCard: Boolean): Result<PendingIntent?> = withContext(Dispatchers.IO) {
        try {
            val targetDir: File = if (toSdCard) {
                val sdDir = getSdCardPicturesDirectory()
                    ?: return@withContext Result.failure(IllegalStateException("SD-Karte nicht verfügbar"))
                File(sdDir, album.name)
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                File(picturesDir, album.name)
            }

            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val items = getAllMedia(album.bucketId, includeHidden = true)
            val thumbnailManager = ThumbnailManager.getInstance(context)
            val securityExceptionUris = mutableListOf<Uri>()

            for (item in items) {
                val fileName = item.displayName.ifBlank { "media_${item.id}.jpg" }
                var destFile = File(targetDir, fileName)
                if (destFile.exists()) {
                    val base = fileName.substringBeforeLast(".")
                    val ext = fileName.substringAfterLast(".", "")
                    val extSuffix = if (ext.isNotBlank()) ".$ext" else ""
                    destFile = File(targetDir, "${base}_${System.currentTimeMillis()}$extSuffix")
                }

                var copied = false
                contentResolver.openInputStream(item.uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                        output.flush()
                        copied = true
                    }
                }

                if (copied && destFile.exists() && destFile.length() > 0L) {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(destFile.absolutePath),
                        if (item.mimeType.isNotBlank()) arrayOf(item.mimeType) else null
                    ) { _, _ -> }

                    try {
                        contentResolver.delete(item.uri, null, null)
                    } catch (e: SecurityException) {
                        securityExceptionUris.add(item.uri)
                    } catch (_: Exception) {}

                    thumbnailManager.getThumbnailFile(item).delete()
                }
            }

            // Remove old folder if empty
            if (album.directoryPath.isNotBlank()) {
                val oldDir = File(album.directoryPath)
                if (oldDir.exists() && oldDir.listFiles().isNullOrEmpty()) {
                    oldDir.delete()
                }
            }

            if (securityExceptionUris.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pendingIntent = createDeleteRequest(securityExceptionUris)
                return@withContext Result.success(pendingIntent)
            }

            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
