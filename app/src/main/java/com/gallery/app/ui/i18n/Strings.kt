package com.gallery.app.ui.i18n

import com.gallery.app.PerformanceProfile
import com.gallery.app.ui.theme.AppColorPalette
import com.gallery.app.ui.theme.CUSTOM_PALETTE_ID
import com.gallery.app.ui.viewmodel.MediaFilter
import java.util.Locale

/** The app ships in English only. Kept as a tiny state holder for date formatting and remember keys. */
enum class AppLanguage(val displayName: String) {
    ENGLISH("English")
}

object LanguageState {
    val language = AppLanguage.ENGLISH
    val locale: Locale = Locale.ENGLISH
}

/** Texts of the app. Readable from composables and plain code alike. */
val strings: AppStrings
    get() = EnglishStrings

val MediaFilter.displayLabel: String
    get() = when (this) {
        MediaFilter.ALL -> strings.filterAll
        MediaFilter.IMAGES -> strings.filterImages
        MediaFilter.VIDEOS -> strings.filterVideos
    }

interface AppStrings {
    // General actions
    val back: String
    val close: String
    val cancel: String
    val create: String
    val rename: String
    val delete: String
    val hide: String
    val move: String
    val share: String
    val send: String
    val edit: String
    val save: String
    val saving: String
    val reset: String
    val rotate: String
    val flipHorizontal: String
    val flipVertical: String
    val editSaved: String
    fun editFailed(reason: String?): String
    val details: String
    val favorite: String
    val done: String
    val all: String
    val none: String
    val options: String
    val unknown: String
    val restore: String
    val emptyAction: String
    val apply: String
    val play: String
    val pause: String
    val retry: String
    val videoPlaybackFailed: String
    val playbackSpeed: String
    fun speedLabel(speed: Float): String
    val rotateAuto: String
    val rotateLandscape: String
    val rotatePortrait: String
    fun seekBackward(seconds: Int): String
    fun seekForward(seconds: Int): String
    val name: String

    // Tabs, filters, grid
    val tabPictures: String
    val tabAlbums: String
    val select: String
    val infoDatePattern: String
    val selectAllItems: String
    val deselectAllItems: String
    val filterAll: String
    val filterImages: String
    val filterVideos: String
    fun items(count: Int): String
    fun selectionTitle(count: Int): String
    val largerThumbnails: String
    val smallerThumbnails: String
    val largerSubAlbums: String
    val smallerSubAlbums: String
    val emptyGallery: String
    val today: String
    val yesterday: String
    val dayPattern: String
    val dayWithYearPattern: String
    val longDatePattern: String

    // Albums
    fun albums(count: Int): String
    val allAlbums: String
    val noAlbums: String
    val subAlbums: String
    val media: String
    val albumEmpty: String
    val createSubAlbum: String
    val newAlbum: String
    val newSubAlbum: String
    val hiddenAlbums: String
    val sortAlbums: String
    val sortStandard: String
    val sortNameAsc: String
    val sortNameDesc: String
    val sortMostItems: String
    val sortCustom: String
    val moveAlbumToTop: String
    val noHiddenAlbums: String
    val unhide: String
    val moveToInternal: String
    val moveToSd: String
    val moveToUsb: String
    val usbStorage: String
    val showAlbums: String
    val showAll: String
    fun freeSpace(size: String): String
    fun selectionSize(count: Int, size: String): String
    val sdCard: String
    val internal: String
    val renameAlbum: String
    val hideAlbum: String
    val deleteAlbum: String
    val hideAlbumTitle: String
    fun hideAlbumMessage(name: String): String
    val searchAlbums: String
    val searchAlbumsPlaceholder: String
    fun noAlbumsFound(query: String): String
    fun deleteAlbumTitle(name: String): String
    fun deleteAlbumMessage(count: Int): String
    fun deleteEmptyAlbumMessage(name: String): String
    val changeLocationTitle: String
    fun migrateMessage(name: String, count: Int, toSdCard: Boolean): String
    fun moveSizeInfo(size: String, target: String, free: String): String
    fun subAlbumSuffix(count: Int): String
    fun inAlbum(name: String, location: String): String
    val subAlbumName: String
    val albumName: String
    val newAlbumName: String
    val internalStorage: String
    val sdCardUnavailable: String
    val externalStorage: String

    // Move sheet
    val moveToAlbum: String
    val createNewAlbum: String
    val createNewAlbumSubtitle: String
    fun albumTargetSubtitle(isSdCard: Boolean, count: Int): String

    // Favorites & trash
    val favorites: String
    val noFavorites: String
    val noFavoritesMessage: String
    val trash: String
    val recentlyDeleted: String
    val trashEmpty: String
    val trashEmptyMessage: String
    val trashInfo: String
    val deletePermanently: String
    val emptyTrashTitle: String
    fun emptyTrashMessage(count: Int): String
    val deleteSelectedTitle: String
    fun deleteSelectedMessage(count: Int): String

    // Viewer & details
    val shareItem: String
    val shareMedia: String
    val renameFile: String
    val loadingMetadata: String
    val fileName: String
    val resolution: String
    val videoDuration: String
    val bitrate: String
    val cameraModel: String
    val captureLocation: String
    val map: String
    val gallery: String
    val metadataDatePattern: String
    fun durationText(hours: Long, minutes: Long, seconds: Long): String

    // Menu & settings
    fun freeOf(free: String, total: String): String
    fun free(amount: String): String
    fun version(versionName: String): String
    val design: String
    val appearance: String
    val settings: String
    val graphicsQuality: String
    val graphicsAndVideo: String
    val videoPlayback: String
    val videoPlaybackDescription: String
    val seekDuration: String
    val seekDurationDescription: String
    fun secondsLabel(seconds: Int): String
    val storage: String
    val info: String
    val library: String
    val general: String
    val colors: String
    val light: String
    val dark: String
    val appVersion: String
    val debugBuild: String
    val prereleaseBuild: String
    val customColor: String
    val customAccentTitle: String
    fun customAccentMessage(lightMode: Boolean, designName: String): String
    val customPalette: String
    val language: String
    fun qualityDescription(tier: PerformanceProfile): String
    fun paletteLabel(palette: AppColorPalette): String

    // Settings pages
    val legendPhotos: String
    val legendVideos: String
    val legendOther: String
    fun freeUpTrash(size: String): String
    val reviewMedia: String
    fun reviewMediaMessage(size: String): String
    val storageCalculating: String
    val appearanceModeHeader: String
    val accentColorHeader: String
    val fromWallpaper: String
    val accentFooterText: String
    val appNameLabel: String
    val colorPaletteLabel: String

    // Storage and info
    val storageAvailable: String
    val emptyTrashAction: String

    // One UI
    val copyToClipboard: String
    val setAsWallpaper: String
    val wallpaperHome: String
    val wallpaperLock: String
    val wallpaperBoth: String
    val wallpaperSet: String
    val liveWallpaper: String
    val liveWallpaperTooLong: String
    val wallpaperFailed: String
    val imageInfo: String
    val manageStorage: String
    val usedWord: String
    val suggestions: String

    // Permissions
    val permissionTitle: String
    val permissionDescription: String
    val permissionGrant: String
    val permissionWarning: String
    val permissionMissingMedia: String
    val permissionOpenSettings: String

    // Messages (toasts)
    val favoriteAdded: String
    val favoriteRemoved: String
    fun movedToTrash(count: Int): String
    fun albumCreated(name: String, onSdCard: Boolean): String
    fun createError(message: String?): String
    val noItemsSelected: String
    val noItemsToMove: String
    fun moveStarted(albumName: String): String
    fun movedItems(count: Int, albumName: String): String
    fun albumHidden(name: String): String
    fun albumsHidden(count: Int): String
    fun migratingAlbums(count: Int, toSdCard: Boolean): String
    fun migratedAlbums(count: Int, toSdCard: Boolean): String
    fun migratedAlbumsPartly(moved: Int, failed: Int): String
    val selectAlbums: String
    val hideSelected: String
    val moveSelected: String
    val moveAlbumsTitle: String
    val albumUnhidden: String
    val itemTrashed: String
    val restored: String
    val allRestored: String
    val deletedPermanently: String
    val trashEmptied: String
    fun fileRenamed(name: String): String
    fun error(message: String?): String
    val actionSucceeded: String
    fun subAlbumCreated(name: String, parentName: String, onSdCard: Boolean): String
    fun albumRenamed(name: String): String
    fun renameError(message: String?): String
    fun albumDeleted(name: String): String
    fun deleteError(message: String?): String
    fun migrating(name: String, toSdCard: Boolean): String
    fun migrated(name: String, toSdCard: Boolean): String
    fun migrationError(message: String?): String
    fun migratedPartly(name: String, toSdCard: Boolean, moved: Int, failed: Int): String
    fun notEnoughSpace(neededBytes: Long, freeBytes: Long): String
    val movingMediaTitle: String
    fun progressOf(current: Int, total: Int): String

    // Repository errors
    val sdCardNotAvailable: String
    val nameEmpty: String
    val renameFailed: String
    val permissionDenied: String
    val renameFailedGeneric: String
    val sdCardNotWritable: String
    val folderCreateFailed: String
    val subFolderCreateFailed: String
}


object EnglishStrings : AppStrings {
    override val back = "Back"
    override val close = "Close"
    override val cancel = "Cancel"
    override val create = "Create"
    override val rename = "Rename"
    override val delete = "Delete"
    override val hide = "Hide"
    override val move = "Move"
    override val share = "Share"
    override val send = "Send"
    override val edit = "Edit"
    override val save = "Save"
    override val saving = "Saving …"
    override val reset = "Reset"
    override val rotate = "Rotate"
    override val flipHorizontal = "Mirror"
    override val flipVertical = "Flip"
    override val editSaved = "Saved as a copy"
    override fun editFailed(reason: String?) = if (reason.isNullOrBlank()) "Saving failed" else "Saving failed: $reason"
    override val details = "Details"
    override val favorite = "Favorite"
    override val done = "Done"
    override val all = "All"
    override val none = "None"
    override val options = "Options"
    override val unknown = "Unknown"
    override val restore = "Restore"
    override val emptyAction = "Empty"
    override val apply = "Apply"
    override val play = "Play"
    override val pause = "Pause"
    override val retry = "Retry"
    override val videoPlaybackFailed = "This video can't be played."
    override val playbackSpeed = "Playback speed"
    override fun speedLabel(speed: Float) = if (speed == speed.toInt().toFloat()) "${speed.toInt()}×" else "${speed}×"
    override val rotateAuto = "Auto rotate"
    override val rotateLandscape = "Lock landscape"
    override val rotatePortrait = "Lock portrait"
    override fun seekBackward(seconds: Int) = "−${seconds}s"
    override fun seekForward(seconds: Int) = "+${seconds}s"
    override val name = "Name"

    override val tabPictures = "Photos"
    override val tabAlbums = "Albums"
    override val select = "Select"
    override val infoDatePattern = "EEEE, MMMM d, yyyy 'at' HH:mm"
    override val selectAllItems = "Select All"
    override val deselectAllItems = "Deselect All"
    override val filterAll = "All"
    override val filterImages = "Images"
    override val filterVideos = "Videos"
    override fun items(count: Int) = if (count == 1) "1 item" else "$count items"
    override fun selectionTitle(count: Int) = if (count == 0) "Select items" else "$count selected"
    override val largerThumbnails = "Larger thumbnails"
    override val smallerThumbnails = "Smaller thumbnails"
    override val largerSubAlbums = "Larger sub-albums"
    override val smallerSubAlbums = "Smaller sub-albums"
    override val emptyGallery = "No media found"
    override val today = "Today"
    override val yesterday = "Yesterday"
    override val dayPattern = "MMMM d"
    override val dayWithYearPattern = "MMMM d, yyyy"
    override val longDatePattern = "MMMM d, yyyy"

    override fun albums(count: Int) = if (count == 1) "1 album" else "$count albums"
    override val allAlbums = "All albums"
    override val noAlbums = "No albums yet"
    override val subAlbums = "Sub-albums"
    override val media = "Media"
    override val albumEmpty = "This album is empty"
    override val createSubAlbum = "Create sub-album"
    override val newAlbum = "New album"
    override val newSubAlbum = "New sub-album"
    override val hiddenAlbums = "Hidden albums"
    override val sortAlbums = "Sort albums"
    override val sortStandard = "Standard"
    override val sortNameAsc = "Name (A–Z)"
    override val sortNameDesc = "Name (Z–A)"
    override val sortMostItems = "Most items"
    override val sortCustom = "Custom order"
    override val moveAlbumToTop = "Move to top"
    override val noHiddenAlbums = "No hidden albums"
    override val unhide = "Show again"
    override val moveToInternal = "Move to internal storage"
    override val moveToSd = "Move to SD card"
    override val moveToUsb = "Move to USB storage"
    override val usbStorage = "USB storage"
    override val showAlbums = "Show albums"
    override val showAll = "All"
    override fun freeSpace(size: String) = "$size free"
    override fun selectionSize(count: Int, size: String) = "$count item(s) · $size"
    override val sdCard = "SD card"
    override val internal = "Internal"
    override val renameAlbum = "Rename album"
    override val hideAlbum = "Hide album"
    override val deleteAlbum = "Delete album"
    override val hideAlbumTitle = "Hide album?"
    override fun hideAlbumMessage(name: String) =
        "“$name” and its contents will be hidden from the gallery and your albums. You can show it again at any time under “Hidden albums”."
    override val searchAlbums = "Search albums"
    override val searchAlbumsPlaceholder = "Album name"
    override fun noAlbumsFound(query: String) = "No albums found for “$query”"
    override fun deleteAlbumTitle(name: String) = "Delete “$name”?"
    override fun deleteAlbumMessage(count: Int) =
        "All $count items in this album will be moved to the trash and the folder will be removed."
    override fun deleteEmptyAlbumMessage(name: String) = "The empty folder “$name” will be deleted permanently."
    override val changeLocationTitle = "Change storage location?"
    override fun moveSizeInfo(size: String, target: String, free: String) = "Size: $size · Free on $target: $free"
    override fun migrateMessage(name: String, count: Int, toSdCard: Boolean) =
        "“$name” with $count item(s) will be moved to ${if (toSdCard) "the SD card" else "internal storage"}. The files are physically moved and the media index is updated right away."
    override fun subAlbumSuffix(count: Int) = " · $count sub-albums"
    override fun inAlbum(name: String, location: String) = "In “$name” · $location"
    override val subAlbumName = "Sub-album name"
    override val albumName = "Album name"
    override val newAlbumName = "New album name"
    override val internalStorage = "Internal storage"
    override val sdCardUnavailable = "SD card (none inserted)"
    override val externalStorage = "External storage"

    override val moveToAlbum = "Move to album"
    override val createNewAlbum = "Create new album"
    override val createNewAlbumSubtitle = "Create a folder and move there"
    override fun albumTargetSubtitle(isSdCard: Boolean, count: Int) =
        "${if (isSdCard) sdCard else internal} · ${items(count)}"

    override val favorites = "Favorites"
    override val noFavorites = "No favorites"
    override val noFavoritesMessage = "Tap the heart in the full-screen view to collect photos and videos here."
    override val trash = "Trash"
    override val recentlyDeleted = "Recently deleted"
    override val trashEmpty = "Trash is empty"
    override val trashEmptyMessage = "Deleted media is kept here for 30 days."
    override val trashInfo = "Items in the trash are deleted permanently after 30 days."
    override val deletePermanently = "Delete permanently"
    override val emptyTrashTitle = "Empty trash?"
    override fun emptyTrashMessage(count: Int) = "All $count items in the trash will be deleted permanently."
    override val deleteSelectedTitle = "Delete selected items?"
    override fun deleteSelectedMessage(count: Int) =
        "The $count selected item${if (count == 1) "" else "s"} will be deleted permanently."

    override val shareItem = "Share item"
    override val shareMedia = "Share media"
    override val renameFile = "Rename file"
    override val loadingMetadata = "Loading metadata…"
    override val fileName = "File name"
    override val resolution = "Resolution"
    override val videoDuration = "Video length"
    override val bitrate = "Bitrate"
    override val cameraModel = "Camera model"
    override val captureLocation = "Location"
    override val map = "Map"
    override val gallery = "Gallery"
    override val metadataDatePattern = "MMMM d, yyyy, HH:mm"
    override fun durationText(hours: Long, minutes: Long, seconds: Long) =
        if (hours > 0) "$hours h $minutes min $seconds s" else "$minutes min $seconds s"

    override fun freeOf(free: String, total: String) = "$free free of $total"
    override fun free(amount: String) = "$amount free"
    override fun version(versionName: String) = "Version ${versionName.ifBlank { "unknown" }}"
    override val design = "Design"
    override val appearance = "Appearance"
    override val settings = "Settings"
    override val legendPhotos = "Photos"
    override val legendVideos = "Videos"
    override val legendOther = "Other"
    override fun freeUpTrash(size: String) = "Free up $size. All photos and videos in “Recently Deleted” will be permanently deleted."
    override val reviewMedia = "Review Photos and Videos"
    override fun reviewMediaMessage(size: String) = "Photos and videos use $size. Review them in the library and delete the ones you no longer need."
    override val storageCalculating = "Calculating…"
    override val appearanceModeHeader = "Appearance"
    override val accentColorHeader = "Accent Color"
    override val fromWallpaper = "From wallpaper"
    override val accentFooterText = "The accent color is used for icons, selections and highlighted text."
    override val appNameLabel = "Name"
    override val colorPaletteLabel = "Color Palette"
    override val storageAvailable = "Available"
    override val emptyTrashAction = "Empty trash"
    override val copyToClipboard = "Copy to clipboard"
    override val setAsWallpaper = "Set as wallpaper"
    override val wallpaperHome = "Home screen"
    override val wallpaperLock = "Lock screen"
    override val wallpaperBoth = "Home and lock screen"
    override val wallpaperSet = "Wallpaper set"
    override val liveWallpaper = "Set as live wallpaper"
    override val liveWallpaperTooLong = "Only videos up to 15 seconds can be a live wallpaper."
    override val wallpaperFailed = "Could not set the wallpaper"
    override val imageInfo = "Image info"
    override val manageStorage = "Manage storage"
    override val usedWord = "used"
    override val suggestions = "Suggestions"
    override val graphicsQuality = "Graphics quality"
    override val graphicsAndVideo = "Graphics and video"
    override val videoPlayback = "Video playback"
    override val videoPlaybackDescription =
        "Videos play on their own page. There you can change the playback speed, lock the screen " +
            "orientation, and double-tap the left or right side to skip backward or forward."
    override val seekDuration = "Seek duration"
    override val seekDurationDescription = "How far a double-tap skips."
    override fun secondsLabel(seconds: Int) = if (seconds == 1) "1 second" else "$seconds seconds"
    override val storage = "Storage"
    override val info = "Info"
    override val library = "Library"
    override val general = "General"
    override val colors = "Colors"
    override val light = "Light"
    override val dark = "Dark"
    override val appVersion = "App version"
    override val debugBuild = "Debug"
    override val prereleaseBuild = "Pre-release"
    override val customColor = "Custom color"
    override val customAccentTitle = "Custom accent color"
    override fun customAccentMessage(lightMode: Boolean, designName: String) =
        "The ${if (lightMode) "light" else "dark"} base colors of $designName stay the same - only the accent changes."
    override val customPalette = "Custom palette"
    override val language = "Language"
    override fun qualityDescription(tier: PerformanceProfile) = when (tier) {
        PerformanceProfile.LOW -> "Just the essentials: no animations, no glass, smaller thumbnails, lowest memory and battery use."
        PerformanceProfile.MEDIUM -> "Balanced: reduced animations, solid instead of glass bars, good image quality."
        PerformanceProfile.HIGH -> "Real glass with background blur, smooth animations, high image quality."
        PerformanceProfile.ULTRA -> "The full effect palette: glass, moving waves, the longest animations, the highest image quality and the largest caches."
    }
    override fun paletteLabel(palette: AppColorPalette) = when (palette.id) {
        CUSTOM_PALETTE_ID -> customPalette
        "oneui_blue" -> "OneUI Blue"
        "oneui_lavender" -> "OneUI Lavender"
        "oneui_green" -> "OneUI Green"
        "oneui_light_blue" -> "OneUI Light Blue"
        "oneui_light_lavender" -> "OneUI Light Lavender"
        "oneui_light_green" -> "OneUI Light Green"
        else -> palette.label
    }

    override val permissionTitle = "Access to photos and videos"
    override val permissionDescription = "Allow access to your media library to show your photos and videos."
    override val permissionGrant = "Grant permissions"
    override val permissionWarning = "The app won't work properly while these permissions are missing:"
    override val permissionMissingMedia = "Access to photos and videos — without it the gallery can't show anything"
    override val permissionOpenSettings = "Open in settings"

    override val favoriteAdded = "Added to favorites"
    override val favoriteRemoved = "Removed from favorites"
    override fun movedToTrash(count: Int) = "Moved $count item(s) to the trash"
    override fun albumCreated(name: String, onSdCard: Boolean) =
        "Album '$name' created ${if (onSdCard) "on the SD card" else "in internal storage"}"
    override fun createError(message: String?) = "Could not create: $message"
    override val noItemsSelected = "No items selected"
    override val noItemsToMove = "No items found to move"
    override fun moveStarted(albumName: String) = "Moving to '$albumName'..."
    override fun movedItems(count: Int, albumName: String) = "Moved $count item(s) to '$albumName'"
    override fun albumHidden(name: String) = "Album '$name' hidden"
    override fun albumsHidden(count: Int) = "$count albums hidden"
    override fun migratingAlbums(count: Int, toSdCard: Boolean) =
        "Moving $count albums to ${if (toSdCard) "the SD card" else "internal storage"} …"
    override fun migratedAlbums(count: Int, toSdCard: Boolean) =
        "Moved $count albums to ${if (toSdCard) "the SD card" else "internal storage"}"
    override fun migratedAlbumsPartly(moved: Int, failed: Int) =
        "Moved $moved files, $failed could not be moved"
    override val selectAlbums = "Select"
    override val hideSelected = "Hide"
    override val moveSelected = "Move"
    override val moveAlbumsTitle = "Move where?"
    override val albumUnhidden = "Album shown again"
    override val itemTrashed = "Moved to trash"
    override val restored = "Restored"
    override val allRestored = "All items restored"
    override val deletedPermanently = "Deleted permanently"
    override val trashEmptied = "Trash emptied"
    override fun fileRenamed(name: String) = "File renamed to $name"
    override fun error(message: String?) = "Error: $message"
    override val actionSucceeded = "Done"
    override fun subAlbumCreated(name: String, parentName: String, onSdCard: Boolean) =
        "Sub-album '$name' created in '$parentName' ${if (onSdCard) "on the SD card" else "in internal storage"}"
    override fun albumRenamed(name: String) = "Album renamed to '$name'"
    override fun renameError(message: String?) = "Could not rename: $message"
    override fun albumDeleted(name: String) = "Album '$name' deleted"
    override fun deleteError(message: String?) = "Could not delete: $message"
    override fun migrating(name: String, toSdCard: Boolean) =
        "Moving '$name' to ${if (toSdCard) "the SD card" else "internal storage"}..."
    override fun migrated(name: String, toSdCard: Boolean) =
        "Album '$name' moved to ${if (toSdCard) "the SD card" else "internal storage"}"
    override fun migrationError(message: String?) = "Migration failed: $message"
    override fun migratedPartly(name: String, toSdCard: Boolean, moved: Int, failed: Int) =
        "Moved $moved item(s) of '$name' to ${if (toSdCard) "the SD card" else "internal storage"}; $failed could not be moved and are still where they were"
    override fun notEnoughSpace(neededBytes: Long, freeBytes: Long) =
        "Not enough space: ${gigabytes(neededBytes)} needed, ${gigabytes(freeBytes)} free"
    override val movingMediaTitle = "Moving media…"
    override fun progressOf(current: Int, total: Int) = "$current of $total"

    override val sdCardNotAvailable = "SD card not available"
    override val nameEmpty = "Name must not be empty."
    override val renameFailed = "Rename failed."
    override val permissionDenied = "Permission denied."
    override val renameFailedGeneric = "Error while renaming."
    override val sdCardNotWritable = "SD card not found or not writable."
    override val folderCreateFailed = "The folder could not be created."
    override val subFolderCreateFailed = "The sub-folder could not be created."
}

/** A size as "1.4 GB" (one decimal), for storage messages. */
internal fun gigabytes(bytes: Long): String =
    String.format(java.util.Locale.getDefault(), "%.1f GB", bytes / 1_000_000_000.0)

/** "Internal storage", "SD card" or "USB storage". */
internal fun storageLabel(kind: com.gallery.app.util.StorageKind): String = when (kind) {
    com.gallery.app.util.StorageKind.INTERNAL -> strings.internalStorage
    com.gallery.app.util.StorageKind.SD_CARD -> strings.sdCard
    com.gallery.app.util.StorageKind.USB -> strings.usbStorage
}

/** A size in bytes as people read it: "850 MB", "1,4 GB". */
internal fun readableSize(bytes: Long): String {
    val locale = LanguageState.locale
    return when {
        bytes >= 1_000_000_000L -> String.format(locale, "%.1f GB", bytes / 1_000_000_000.0)
        bytes >= 1_000_000L -> String.format(locale, "%.0f MB", bytes / 1_000_000.0)
        else -> String.format(locale, "%.0f KB", bytes / 1_000.0)
    }
}
