package com.example.amoledgallery.service
import com.example.amoledgallery.ui.i18n.strings

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.amoledgallery.data.MediaRepository
import com.example.amoledgallery.data.MoveResult
import com.example.amoledgallery.model.AlbumItem
import com.example.amoledgallery.model.MediaItem
import android.app.PendingIntent
import android.content.pm.ServiceInfo
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
    val pendingItemsToMove: List<MediaItem> = emptyList()
) {
    val fraction: Float get() = if (total > 0) (current.toFloat() / total).coerceIn(0f, 1f) else 0f
    val percentage: Int get() = (fraction * 100).toInt()
}

object MoveProgressHolder {
    private val _progress = MutableStateFlow(MoveProgress())
    val progress: StateFlow<MoveProgress> = _progress.asStateFlow()

    // Temporary storage for items to move passed into the service
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

    fun permissionRequired(pendingIntent: PendingIntent, targetAlbum: AlbumItem, itemsToMove: List<MediaItem>) {
        _progress.value = MoveProgress(
            isRunning = false,
            permissionRequiredIntent = pendingIntent,
            pendingTargetAlbum = targetAlbum,
            pendingItemsToMove = itemsToMove
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
 * Foreground Service for robust batch media moving operations:
 * - Persistent notification with progress bar (setProgress(total, current, false))
 * - In-App observable StateFlow via MoveProgressHolder
 * - Survives app backgrounding when moving 1,000+ items
 */
class MoveMediaService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val items = MoveProgressHolder.pendingItems
        val targetAlbum = MoveProgressHolder.pendingTargetAlbum

        if (items.isEmpty() || targetAlbum == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val total = items.size
        MoveProgressHolder.updateProgress(0, total, targetAlbum.name)

        val initialNotification = buildNotification(0, total, targetAlbum.name)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        serviceScope.launch {
            val repository = MediaRepository(applicationContext)
            val result = kotlinx.coroutines.withContext(Dispatchers.IO) {
                repository.moveMediaItems(items, targetAlbum) { current: Int, totalCount: Int ->
                    MoveProgressHolder.updateProgress(current, totalCount, targetAlbum.name)
                    notificationManager.notify(NOTIFICATION_ID, buildNotification(current, totalCount, targetAlbum.name))
                }
            }

            when (result) {
                is MoveResult.Success -> {
                    MoveProgressHolder.complete(targetAlbum.name, result.count)
                    
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    
                    val completeNotification = NotificationCompat.Builder(this@MoveMediaService, CHANNEL_ID)
                        .setSmallIcon(com.example.amoledgallery.R.drawable.ic_launcher_foreground)
                        .setContentTitle(strings.moveDone)
                        .setContentText(strings.movedItems(result.count, targetAlbum.name))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .build()
                    notificationManager.notify(NOTIFICATION_ID, completeNotification)
                }
                is MoveResult.Error -> {
                    MoveProgressHolder.error(result.message)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
                is MoveResult.PermissionRequired -> {
                    MoveProgressHolder.permissionRequired(result.pendingIntent, result.targetAlbum, result.itemsToMove)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
            }

            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                strings.moveChannelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = strings.moveChannelDescription
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(current: Int, total: Int, targetAlbumName: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.example.amoledgallery.R.drawable.ic_launcher_foreground)
            .setContentTitle(if (targetAlbumName.isNotBlank()) strings.movingTo(targetAlbumName) else strings.movingMedia)
            .setContentText(strings.progressCompleted(current, total))
            .setProgress(total, current, total == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    companion object {
        const val CHANNEL_ID = "move_media_channel"
        const val NOTIFICATION_ID = 4001

        fun updateProgress(current: Int, total: Int) {
            MoveProgressHolder.updateProgress(current, total, "")
        }

        fun start(context: Context, items: List<MediaItem>, targetAlbum: AlbumItem, onCompletion: () -> Unit) {
            MoveProgressHolder.pendingItems = items
            MoveProgressHolder.pendingTargetAlbum = targetAlbum
            MoveProgressHolder.onCompletionCallback = onCompletion
            MoveProgressHolder.updateProgress(0, items.size, targetAlbum.name)

            val intent = Intent(context, MoveMediaService::class.java)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    context.startService(intent)
                } catch (e2: Exception) {
                    e2.printStackTrace()
                    runInProcessFallback(context, items, targetAlbum, onCompletion)
                }
            }
        }

        private fun runInProcessFallback(
            context: Context,
            items: List<MediaItem>,
            targetAlbum: AlbumItem,
            onCompletion: () -> Unit
        ) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val total = items.size
            fun postFallbackNotification(curr: Int) {
                val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(com.example.amoledgallery.R.drawable.ic_launcher_foreground)
                    .setContentTitle(strings.movingTo(targetAlbum.name))
                    .setContentText(strings.progressProcessed(curr, total))
                    .setProgress(total, curr, total == 0)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build()
                notificationManager?.notify(NOTIFICATION_ID, notif)
            }

            postFallbackNotification(0)

            CoroutineScope(Dispatchers.IO).launch {
                val repository = MediaRepository(context.applicationContext)
                val result = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    repository.moveMediaItems(items, targetAlbum) { current, totalCount ->
                        MoveProgressHolder.updateProgress(current, totalCount, targetAlbum.name)
                        postFallbackNotification(current)
                    }
                }
                notificationManager?.cancel(NOTIFICATION_ID)
                when (result) {
                    is MoveResult.Success -> {
                        MoveProgressHolder.complete(targetAlbum.name, result.count)
                        val doneNotif = NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(com.example.amoledgallery.R.drawable.ic_launcher_foreground)
                            .setContentTitle(strings.moveDone)
                            .setContentText(strings.movedItems(result.count, targetAlbum.name))
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setAutoCancel(true)
                            .build()
                        notificationManager?.notify(NOTIFICATION_ID, doneNotif)
                    }
                    is MoveResult.Error -> MoveProgressHolder.error(result.message)
                    is MoveResult.PermissionRequired -> MoveProgressHolder.permissionRequired(result.pendingIntent, result.targetAlbum, result.itemsToMove)
                }
                onCompletion()
            }
        }
    }
}
