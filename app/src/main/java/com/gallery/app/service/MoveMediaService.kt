package com.gallery.app.service

import android.app.PendingIntent
import android.content.Context
import com.gallery.app.data.MediaRepository
import com.gallery.app.data.MoveResult
import com.gallery.app.model.AlbumItem
import com.gallery.app.model.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MoveProgress(
    val isRunning: Boolean = false,
    val current: Int = 0,
    val total: Int = 0,
    val targetAlbumName: String = "",
    val isCompleted: Boolean = false,
    val error: String? = null,
    val permissionRequiredIntent: PendingIntent? = null,
    val pendingTargetAlbum: AlbumItem? = null,
    val pendingItemsToMove: List<MediaItem> = emptyList(),
    val pendingOriginalsToDelete: List<MediaItem> = emptyList()
) {
    val fraction: Float get() = if (total > 0) (current.toFloat() / total).coerceIn(0f, 1f) else 0f
    val percentage: Int get() = (fraction * 100).toInt()
}

object MoveProgressHolder {
    private val _progress = MutableStateFlow(MoveProgress())
    val progress: StateFlow<MoveProgress> = _progress.asStateFlow()

    // Temporary storage for the items of the running move
    var pendingItems: List<MediaItem> = emptyList()
    var pendingTargetAlbum: AlbumItem? = null
    var onCompletionCallback: (() -> Unit)? = null

    fun updateProgress(current: Int, total: Int, targetAlbumName: String) {
        _progress.value = MoveProgress(
            isRunning = true,
            current = current,
            total = total,
            targetAlbumName = targetAlbumName
        )
    }

    fun complete(targetAlbumName: String, count: Int) {
        _progress.value = MoveProgress(
            isRunning = false,
            current = count,
            total = count,
            targetAlbumName = targetAlbumName,
            isCompleted = true
        )
        onCompletionCallback?.invoke()
    }

    fun permissionRequired(result: MoveResult.PermissionRequired) {
        _progress.value = MoveProgress(
            isRunning = false,
            permissionRequiredIntent = result.pendingIntent,
            pendingTargetAlbum = result.targetAlbum,
            pendingItemsToMove = result.itemsToMove,
            pendingOriginalsToDelete = result.originalsToDelete
        )
    }

    fun error(message: String) {
        _progress.value = MoveProgress(
            isRunning = false,
            error = message
        )
    }

    fun reset() {
        _progress.value = MoveProgress()
        pendingItems = emptyList()
        pendingTargetAlbum = null
    }
}

/**
 * Runs batch moves in the background inside the app process. Progress is reported through
 * [MoveProgressHolder] and shown by the app's own progress dialog, so no notification is needed.
 */
object MoveMediaService {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun updateProgress(current: Int, total: Int) {
        MoveProgressHolder.updateProgress(current, total, "")
    }

    fun start(context: Context, items: List<MediaItem>, targetAlbum: AlbumItem, onCompletion: () -> Unit) {
        MoveProgressHolder.pendingItems = items
        MoveProgressHolder.pendingTargetAlbum = targetAlbum
        MoveProgressHolder.onCompletionCallback = onCompletion
        MoveProgressHolder.updateProgress(0, items.size, targetAlbum.name)

        val appContext = context.applicationContext
        scope.launch {
            val repository = MediaRepository(appContext)
            // The move runs after the user already granted access, so never ask again from here.
            val result = repository.moveMediaItems(items, targetAlbum, requestPermission = false) { current: Int, totalCount: Int ->
                MoveProgressHolder.updateProgress(current, totalCount, targetAlbum.name)
            }
            when (result) {
                is MoveResult.Success -> MoveProgressHolder.complete(targetAlbum.name, result.count)
                is MoveResult.Error -> MoveProgressHolder.error(result.message)
                is MoveResult.PermissionRequired -> MoveProgressHolder.permissionRequired(result)
            }
        }
    }
}
