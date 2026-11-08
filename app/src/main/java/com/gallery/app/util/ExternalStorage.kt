package com.gallery.app.util

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.MediaStore
import java.io.File

/** Where an album or a file lives. */
enum class StorageKind { INTERNAL, SD_CARD, USB }

/**
 * A removable storage volume that is mounted right now: an SD card, or a USB drive / SSD plugged in
 * over USB (Android calls both "removable" and mounts them alike under /storage/XXXX-XXXX).
 */
data class ExternalVolume(
    val kind: StorageKind,
    val root: File,
    /** MediaStore's name for the volume ("1234-abcd"), for inserting files onto it; null before API 30. */
    val mediaStoreName: String?,
    /** The system's own name for it, e.g. "SanDisk SD card" or "USB drive". */
    val description: String,
    /** Mounted read-only (e.g. an exFAT card after filesystem errors): nothing can be written to it. */
    val readOnly: Boolean = false
) {
    fun freeBytes(): Long = runCatching { StatFs(root.path).availableBytes }.getOrDefault(0L)
    fun totalBytes(): Long = runCatching { StatFs(root.path).totalBytes }.getOrDefault(0L)
}

/**
 * Tells SD cards and USB storage apart. Android reports both as removable volumes, which is why an
 * external SSD used to show up as "SD card".
 *
 * The volume's id names the disk it sits on - "public:179,1" is a partition of an MMC/SD disk (block
 * major 179), "public:8,17" one of a SCSI disk, which is what USB drives appear as (major 8 and the
 * other SCSI majors). That id is not public API, so it is read by reflection; when that fails, the
 * system's description is compared with its own labels for SD cards and USB drives.
 */
object ExternalStorage {

    fun volumes(context: Context): List<ExternalVolume> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return legacyVolumes(context)
        val manager = context.getSystemService(StorageManager::class.java) ?: return emptyList()
        return runCatching {
            manager.storageVolumes
                .filter { it.isRemovable && !it.isPrimary }
                .filter { it.state == Environment.MEDIA_MOUNTED || it.state == Environment.MEDIA_MOUNTED_READ_ONLY }
                .mapNotNull { volume ->
                    val root = volume.directory ?: return@mapNotNull null
                    ExternalVolume(
                        kind = kindOf(context, volume),
                        root = root,
                        mediaStoreName = volume.mediaStoreVolumeName,
                        description = runCatching { volume.getDescription(context) }.getOrNull().orEmpty(),
                        readOnly = volume.state == Environment.MEDIA_MOUNTED_READ_ONLY
                    )
                }
        }.getOrDefault(emptyList())
    }

    /** The volume [path] is on; INTERNAL for anything not on a mounted removable volume. */
    fun kindOfPath(volumes: List<ExternalVolume>, path: String): StorageKind {
        if (path.isBlank()) return StorageKind.INTERNAL
        return volumes.firstOrNull { path.startsWith(it.root.path + "/", ignoreCase = true) || path.equals(it.root.path, ignoreCase = true) }
            ?.kind ?: StorageKind.INTERNAL
    }

    /** The volume [path] is on, or null for internal storage. */
    fun volumeOfPath(volumes: List<ExternalVolume>, path: String): ExternalVolume? =
        volumes.firstOrNull { path.startsWith(it.root.path + "/", ignoreCase = true) || path.equals(it.root.path, ignoreCase = true) }

    private fun kindOf(context: Context, volume: StorageVolume): StorageKind {
        // 1. The disk behind the volume, from its id ("public:<major>,<minor>").
        val id = runCatching { StorageVolume::class.java.getMethod("getId").invoke(volume) as? String }.getOrNull()
        val major = id?.let { Regex("""public:(\d+),""").find(it)?.groupValues?.get(1)?.toIntOrNull() }
        if (major != null) {
            if (major == 179) return StorageKind.SD_CARD
            if (major == 8 || major in 65..71 || major in 128..135) return StorageKind.USB
        }
        // 2. The system's own description, in the phone's language.
        val description = runCatching { volume.getDescription(context) }.getOrNull().orEmpty()
        val usbLabel = systemString("storage_usb_drive")
        if (usbLabel != null && description.contains(usbLabel, ignoreCase = true)) return StorageKind.USB
        if (description.contains("USB", ignoreCase = true)) return StorageKind.USB
        return StorageKind.SD_CARD
    }

    private fun systemString(name: String): String? = runCatching {
        val res = Resources.getSystem()
        val id = res.getIdentifier(name, "string", "android")
        if (id != 0) res.getString(id) else null
    }.getOrNull()

    /** API 29: no volume directories; the removable app-storage dirs point at the volumes instead. */
    private fun legacyVolumes(context: Context): List<ExternalVolume> =
        context.getExternalFilesDirs(null).filterNotNull()
            .filter { runCatching { Environment.isExternalStorageRemovable(it) }.getOrDefault(false) }
            .mapNotNull { dir ->
                // .../Android/data/<package>/files -> the volume root four levels up.
                val root = generateSequence(dir) { it.parentFile }.drop(4).firstOrNull() ?: return@mapNotNull null
                ExternalVolume(StorageKind.SD_CARD, root, root.name.lowercase().takeIf { it.isNotBlank() }?.let { name ->
                    runCatching { MediaStore.getExternalVolumeNames(context).firstOrNull { it.equals(name, ignoreCase = true) } }.getOrNull()
                }, "")
            }
}
