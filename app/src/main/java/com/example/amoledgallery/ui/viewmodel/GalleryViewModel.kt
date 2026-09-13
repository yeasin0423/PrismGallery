package com.example.amoledgallery.ui.viewmodel

import android.app.Application
import android.app.PendingIntent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.amoledgallery.data.MediaRepository
import com.example.amoledgallery.data.MoveResult
import com.example.amoledgallery.data.RenameResult
import com.example.amoledgallery.data.ThumbnailManager
import com.example.amoledgallery.model.AlbumItem
import com.example.amoledgallery.model.MediaItem
import com.example.amoledgallery.service.MoveMediaService
import com.example.amoledgallery.service.MoveProgressHolder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import android.content.Context
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class GalleryTab {
    BILDER,
    ALBEN,
    MENU
}

enum class DetailSource {
    ALL,
    ALBUM,
    TRASH,
    FAVORITES
}

enum class MediaFilter(val label: String) {
    ALL("Alle"),
    IMAGES("Bilder"),
    VIDEOS("Videos"),
    GIFS("GIFs")
}

sealed interface GalleryUiState {
    data object Loading : GalleryUiState
    data object PermissionRequired : GalleryUiState
    data object Empty : GalleryUiState
    data class Success(val items: List<MediaItem>) : GalleryUiState
}

data class PendingRenameData(
    val uri: Uri,
    val oldName: String,
    val targetName: String
)

data class PendingMoveData(
    val itemsToMove: List<MediaItem>,
    val targetAlbum: AlbumItem
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)

    // Current Navigation Tab (Bilder vs Alben)
    private val _selectedTab = MutableStateFlow(GalleryTab.BILDER)
    val selectedTab: StateFlow<GalleryTab> = _selectedTab.asStateFlow()

    // Hierarchical Album Breadcrumb Stack (empty = root Alben)
    private val _breadcrumbStack = MutableStateFlow<List<AlbumItem>>(emptyList())
    val breadcrumbStack: StateFlow<List<AlbumItem>> = _breadcrumbStack.asStateFlow()

    // Active Album (null when viewing root tabs)
    private val _selectedAlbum = MutableStateFlow<AlbumItem?>(null)
    val selectedAlbum: StateFlow<AlbumItem?> = _selectedAlbum.asStateFlow()

    // Sub-albums of currently opened album
    private val _currentSubAlbums = MutableStateFlow<List<AlbumItem>>(emptyList())
    val currentSubAlbums: StateFlow<List<AlbumItem>> = _currentSubAlbums.asStateFlow()

    // Media Filter State (Alle, Bilder, Videos, GIFs)
    private val _selectedFilter = MutableStateFlow(MediaFilter.ALL)
    val selectedFilter: StateFlow<MediaFilter> = _selectedFilter.asStateFlow()

    // Trash View state
    private val _isTrashOpen = MutableStateFlow(false)
    val isTrashOpen: StateFlow<Boolean> = _isTrashOpen.asStateFlow()

    // Raw unfiltered media items
    private val _rawMedia = MutableStateFlow<List<MediaItem>>(emptyList())

    // Main media state
    private val _uiState = MutableStateFlow<GalleryUiState>(GalleryUiState.Loading)
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    // Filtered media for currently selected album
    private val _albumMediaState = MutableStateFlow<List<MediaItem>>(emptyList())
    val albumMediaState: StateFlow<List<MediaItem>> = _albumMediaState.asStateFlow()

    // All available albums/folders
    private val _albumsState = MutableStateFlow<List<AlbumItem>>(emptyList())
    val albumsState: StateFlow<List<AlbumItem>> = _albumsState.asStateFlow()

    // Root-level albums (not contained within another album folder)
    val rootAlbums: StateFlow<List<AlbumItem>> = _albumsState
        .map { repository.getRootAlbums(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Trashed media items
    private val _trashedMediaState = MutableStateFlow<List<MediaItem>>(emptyList())
    val trashedMediaState: StateFlow<List<MediaItem>> = _trashedMediaState.asStateFlow()

    // Persistent Favorites system
    private val favPrefs = application.getSharedPreferences("amoled_gallery_fav_prefs", Context.MODE_PRIVATE)
    private val KEY_FAVORITES = "favorite_media_ids"

    private val _favoriteIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteIds: StateFlow<Set<Long>> = _favoriteIds.asStateFlow()

    // Favorited media items stream derived from loaded media and favorite IDs
    val favoriteMediaState: StateFlow<List<MediaItem>> = combine(_rawMedia, _favoriteIds) { media, favIds ->
        media.filter { it.id in favIds }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Grid column count: 2 to 5 (OneUI style pinch-to-zoom, fixed 5 columns as default)
    private val _columnCount = MutableStateFlow(
        when (com.example.amoledgallery.AmoledGalleryApp.performanceProfile) {
            com.example.amoledgallery.PerformanceProfile.LOW,
            com.example.amoledgallery.PerformanceProfile.MEDIUM -> 4
            com.example.amoledgallery.PerformanceProfile.HIGH,
            com.example.amoledgallery.PerformanceProfile.ULTRA -> 5
        }
    )
    val columnCount: StateFlow<Int> = _columnCount.asStateFlow()

    // Fullscreen media detail state
    private val _selectedItemIndex = MutableStateFlow<Int?>(null)
    val selectedItemIndex: StateFlow<Int?> = _selectedItemIndex.asStateFlow()

    private val _detailSource = MutableStateFlow(DetailSource.ALL)
    val detailSource: StateFlow<DetailSource> = _detailSource.asStateFlow()

    // Multi-Selection State
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    // Move to album bottom sheet state
    private val _isMoveSheetOpen = MutableStateFlow(false)
    val isMoveSheetOpen: StateFlow<Boolean> = _isMoveSheetOpen.asStateFlow()

    // Hidden albums state
    private val _hiddenAlbums = MutableStateFlow<List<AlbumItem>>(emptyList())
    val hiddenAlbums: StateFlow<List<AlbumItem>> = _hiddenAlbums.asStateFlow()

    // Trigger IntentSender for user authorization dialogs (Trash, Delete, Write)
    private val _pendingIntentSender = MutableSharedFlow<PendingIntent>()
    val pendingIntentSender: SharedFlow<PendingIntent> = _pendingIntentSender.asSharedFlow()

    // Feedback messages (Snackbar/Toast)
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // Temporary storage for pending rename action awaiting write permission
    private var pendingRename: PendingRenameData? = null

    // Temporary storage for pending move action awaiting write permission
    private var pendingMove: PendingMoveData? = null

    // Move Progress state
    data class MoveProgress(val isMoving: Boolean = false, val current: Int = 0, val total: Int = 0)
    val moveProgress = MutableStateFlow(MoveProgress())
    private val _isMoveProgressVisible = MutableStateFlow(false)
    val isMoveProgressVisible: StateFlow<Boolean> = _isMoveProgressVisible.asStateFlow()

    // MicroSD Card detection
    val isSdCardAvailable: Boolean get() = repository.isSdCardAvailable()

    // Storage free/total space, queried fresh each time the settings dialog reads them
    fun getInternalStorageSpace() = repository.getInternalStorageSpace()
    fun getSdCardStorageSpace() = repository.getSdCardStorageSpace()

    fun dismissMoveProgress() {
        _isMoveProgressVisible.value = false
    }

    // Manual Graphics Quality tier (Low/Medium/High/Ultra), like a game's video settings
    private val _graphicsQuality = MutableStateFlow(com.example.amoledgallery.AmoledGalleryApp.performanceProfile)
    val graphicsQuality: StateFlow<com.example.amoledgallery.PerformanceProfile> = _graphicsQuality.asStateFlow()

    fun setGraphicsQuality(quality: com.example.amoledgallery.PerformanceProfile) {
        _graphicsQuality.value = quality
        ThumbnailManager.getInstance(getApplication()).quality = quality
        com.example.amoledgallery.AmoledGalleryApp.applyGraphicsQuality(getApplication(), quality)
    }

    init {
        loadFavorites()
        ThumbnailManager.getInstance(application).quality = _graphicsQuality.value

        // Listen for MediaStore Scoped Storage write permission requests and progress from MoveMediaService
        viewModelScope.launch {
            MoveProgressHolder.progress.collect { progress ->
                if (progress.isRunning) {
                    moveProgress.value = MoveProgress(isMoving = true, current = progress.current, total = progress.total)
                } else if (progress.isCompleted || progress.error != null) {
                    moveProgress.value = MoveProgress(isMoving = false)
                }

                val intent = progress.permissionRequiredIntent
                val targetAlbum = progress.pendingTargetAlbum
                if (intent != null && targetAlbum != null) {
                    pendingMove = PendingMoveData(progress.pendingItemsToMove, targetAlbum)
                    _pendingIntentSender.emit(intent)
                }
            }
        }
    }

    private fun loadFavorites() {
        val stringSet = favPrefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        _favoriteIds.value = stringSet.mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun toggleFavorite(id: Long) {
        val current = _favoriteIds.value
        val isNowFav = id !in current
        val newSet = if (isNowFav) current + id else current - id
        _favoriteIds.value = newSet
        favPrefs.edit().putStringSet(KEY_FAVORITES, newSet.map { it.toString() }.toSet()).apply()
        viewModelScope.launch {
            _userMessage.emit(if (isNowFav) "Zu Favoriten hinzugefügt" else "Aus Favoriten entfernt")
        }
    }

    fun isFavorite(id: Long): Boolean {
        return _favoriteIds.value.contains(id)
    }

    fun updatePermissionState(hasPermission: Boolean) {
        if (hasPermission) {
            loadMedia()
        } else {
            _uiState.value = GalleryUiState.PermissionRequired
        }
    }

    fun selectTab(tab: GalleryTab) {
        _selectedTab.value = tab
        closeAlbum()
        clearSelection()
        _isTrashOpen.value = false
    }

    /**
     * Updates the media filter and re-computes the visible list.
     */
    fun selectFilter(filter: MediaFilter) {
        _selectedFilter.value = filter
        applyFilter()
    }

    private fun applyFilter() {
        val raw = _rawMedia.value

        val filtered = filterMediaList(raw, _selectedFilter.value)
        _uiState.value = if (filtered.isEmpty()) {
            GalleryUiState.Empty
        } else {
            GalleryUiState.Success(filtered)
        }
    }

    private fun filterMediaList(items: List<MediaItem>, filter: MediaFilter): List<MediaItem> {
        return when (filter) {
            MediaFilter.ALL -> items
            MediaFilter.IMAGES -> items.filter { it.isImage }
            MediaFilter.VIDEOS -> items.filter { it.isVideo }
            MediaFilter.GIFS -> items.filter { it.isGif }
        }
    }

    /**
     * Calculates proportional scroll target index for the fast-scroll scrubber.
     */
    fun calculateScrolledIndex(dragFraction: Float, totalCount: Int): Int {
        if (totalCount <= 0) return 0
        val target = (dragFraction.coerceIn(0f, 1f) * (totalCount - 1)).roundToInt()
        return target.coerceIn(0, totalCount - 1)
    }

    /**
     * Formats German Month & Year ("August 2024") for scrubber indicator pill.
     */
    fun getMonthYearForIndex(items: List<MediaItem>, index: Int): String {
        if (items.isEmpty()) return ""
        val safeIndex = index.coerceIn(0, items.size - 1)
        val item = items[safeIndex]
        val millis = item.dateAdded * 1000L
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.GERMAN)
        val text = sdf.format(Date(millis))
        return text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.GERMAN) else it.toString() }
    }

    // --- Multi-Selection Mode ---

    fun startSelection(initialId: Long? = null) {
        _isSelectionMode.value = true
        _selectedIds.value = if (initialId != null) setOf(initialId) else emptySet()
    }

    fun toggleSelection(id: Long) {
        val current = _selectedIds.value
        _selectedIds.value = if (current.contains(id)) {
            current - id
        } else {
            current + id
        }
    }

    fun selectAll(items: List<MediaItem>) {
        _selectedIds.value = items.map { it.id }.toSet()
    }

    fun deselectAll() {
        _selectedIds.value = emptySet()
    }

    fun setItemSelection(id: Long, selected: Boolean) {
        val current = _selectedIds.value
        if (selected && !current.contains(id)) {
            _selectedIds.value = current + id
        } else if (!selected && current.contains(id)) {
            _selectedIds.value = current - id
        }
    }

    fun setItemSelections(ids: Collection<Long>, selected: Boolean) {
        if (ids.isEmpty()) return
        val current = _selectedIds.value
        _selectedIds.value = if (selected) {
            current + ids
        } else {
            current - ids.toSet()
        }
    }

    fun setSelectedIds(ids: Set<Long>) {
        _selectedIds.value = ids
    }

    fun isIdSelected(id: Long): Boolean {
        return _selectedIds.value.contains(id)
    }

    /**
     * Batch trashes all currently selected media items using MediaStore.createTrashRequest.
     */
    fun trashSelectedMedia() {
        viewModelScope.launch {
            val selected = _selectedIds.value
            if (selected.isEmpty()) return@launch

            val allItems = if (_selectedAlbum.value != null) {
                _albumMediaState.value
            } else {
                _rawMedia.value
            }

            val itemsToTrash = allItems.filter { it.id in selected }
            if (itemsToTrash.isEmpty()) return@launch

            val uris = itemsToTrash.map { it.uri }
            val pendingIntent = withContext(Dispatchers.IO) { repository.createTrashRequest(uris, trash = true) }
            if (pendingIntent != null) {
                _pendingIntentSender.emit(pendingIntent)
            } else {
                _userMessage.emit("${itemsToTrash.size} Element(e) in Papierkorb verschoben")
                clearSelection()
                loadMedia()
                loadTrash()
                _selectedAlbum.value?.let { loadAlbumContents(it) }
            }
        }
    }

    fun clearSelection() {
        _isSelectionMode.value = false
        _selectedIds.value = emptySet()
        _isMoveSheetOpen.value = false
    }

    // --- Move to Album ---

    fun openMoveSheet() {
        if (_selectedIds.value.isNotEmpty()) {
            _isMoveSheetOpen.value = true
        }
    }

    fun closeMoveSheet() {
        _isMoveSheetOpen.value = false
    }

    fun createAlbumAndMove(albumName: String, onSdCard: Boolean) {
        viewModelScope.launch {
            closeMoveSheet()
            val result = withContext(Dispatchers.IO) { repository.createAlbumFolder(albumName, onSdCard) }
            result.onSuccess { dir ->
                val targetLocation = if (onSdCard) "auf SD-Karte" else "im internen Speicher"
                _userMessage.emit("Album '${dir.name}' $targetLocation erstellt")
                loadAlbums()
                val newAlbum = AlbumItem(
                    bucketId = "",
                    name = dir.name,
                    count = 0,
                    isSdCard = onSdCard,
                    directoryPath = dir.absolutePath,
                    relativePath = "Pictures/${dir.name}/"
                )
                moveSelectedItems(newAlbum)
            }.onFailure { err ->
                _userMessage.emit("Fehler beim Erstellen: ${err.localizedMessage}")
            }
        }
    }

    fun moveSelectedItems(targetAlbum: AlbumItem) {
        viewModelScope.launch(Dispatchers.IO) {
            closeMoveSheet()
            val selected = _selectedIds.value
            if (selected.isEmpty()) {
                _userMessage.emit("Keine Elemente ausgewählt")
                return@launch
            }

            val allItems = (_rawMedia.value + _albumMediaState.value).distinctBy { it.id }
            val itemsToMove = allItems.filter { it.id in selected }
            if (itemsToMove.isEmpty()) {
                _userMessage.emit("Keine Elemente zum Verschieben gefunden")
                return@launch
            }

            clearSelection()
            
            _userMessage.emit("Verschieben nach '${targetAlbum.name}' gestartet...")
            
            moveProgress.value = MoveProgress(isMoving = true, current = 0, total = itemsToMove.size)

            var lastUpdate = System.currentTimeMillis()
            val result = withContext(Dispatchers.IO) {
                repository.moveMediaItems(itemsToMove, targetAlbum) { current, totalCount ->
                    val now = System.currentTimeMillis()
                    if (current == totalCount || now - lastUpdate >= 200 || current % maxOf(1, totalCount / 20) == 0) {
                        // Local moveProgress only - deliberately NOT mirrored into the global
                        // MoveProgressHolder (that singleton is for the actual MoveMediaService
                        // foreground-service path only). Writing to both created a race where the
                        // init{} collector below could clobber the final isMoving=false with a
                        // stale isRunning=true from a delayed emission, freezing the progress dialog.
                        moveProgress.value = moveProgress.value.copy(current = current, total = totalCount)
                        lastUpdate = now
                    }
                }
            }

            moveProgress.value = moveProgress.value.copy(isMoving = false)

            loadMedia()
            loadAlbums()
            _selectedAlbum.value?.let { loadAlbumContents(it) }
            _userMessage.emit("${itemsToMove.size} Element(e) erfolgreich in '${targetAlbum.name}' verschoben")
        }
    }

    // --- Hidden Albums ---

    fun loadHiddenAlbums() {
        viewModelScope.launch {
            _hiddenAlbums.value = withContext(Dispatchers.IO) { repository.getHiddenAlbums() }
        }
    }

    fun hideAlbum(album: AlbumItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.setAlbumHidden(album.bucketId, true) }
            _userMessage.emit("Album '${album.name}' ausgeblendet")
            loadMedia()
            loadAlbums()
            loadHiddenAlbums()
        }
    }

    fun unhideAlbum(bucketId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.setAlbumHidden(bucketId, false) }
            _userMessage.emit("Album wieder eingeblendet")
            loadMedia()
            loadAlbums()
            loadHiddenAlbums()
        }
    }

    // --- Album Navigation ---

    /**
     * Opens an album and adds it to the breadcrumb stack.
     */
    fun openAlbum(album: AlbumItem) {
        clearSelection()
        val currentStack = _breadcrumbStack.value
        _breadcrumbStack.value = currentStack + album
        _selectedAlbum.value = album
        loadAlbumContents(album)
    }

    /**
     * Navigates back to a specific breadcrumb level (0-based index).
     * Index -1 resets back to root ("Alben").
     */
    fun navigateToBreadcrumb(index: Int) {
        clearSelection()
        if (index < 0 || index >= _breadcrumbStack.value.size) {
            closeAlbum()
            return
        }
        val newStack = _breadcrumbStack.value.take(index + 1)
        _breadcrumbStack.value = newStack
        val targetAlbum = newStack.last()
        _selectedAlbum.value = targetAlbum
        loadAlbumContents(targetAlbum)
    }

    /**
     * Pops one level from the breadcrumb stack.
     * Returns true if popped, false if already at root.
     */
    fun popAlbumBreadcrumb(): Boolean {
        clearSelection()
        val stack = _breadcrumbStack.value
        return if (stack.isNotEmpty()) {
            if (stack.size == 1) {
                closeAlbum()
            } else {
                navigateToBreadcrumb(stack.size - 2)
            }
            true
        } else {
            false
        }
    }

    private fun loadAlbumContents(album: AlbumItem) {
        viewModelScope.launch {
            _albumMediaState.value = withContext(Dispatchers.IO) { repository.getAllMedia(album.bucketId) }
            _currentSubAlbums.value = withContext(Dispatchers.IO) { repository.getSubAlbums(album, _albumsState.value) }
        }
    }

    fun closeAlbum() {
        clearSelection()
        _breadcrumbStack.value = emptyList()
        _selectedAlbum.value = null
        _albumMediaState.value = emptyList()
        _currentSubAlbums.value = emptyList()
    }

    fun openTrash() {
        clearSelection()
        _isTrashOpen.value = true
        loadTrash()
    }

    fun closeTrash() {
        _isTrashOpen.value = false
    }

    fun setColumnCount(count: Int) {
        _columnCount.value = count.coerceIn(2, 5)
    }

    fun zoomIn() {
        // Pinching out -> fewer columns (larger tiles)
        if (_columnCount.value > 2) {
            _columnCount.value -= 1
        }
    }

    fun zoomOut() {
        // Pinching in -> more columns (smaller tiles)
        if (_columnCount.value < 5) {
            _columnCount.value += 1
        }
    }

    fun loadMedia() {
        viewModelScope.launch {
            _uiState.value = GalleryUiState.Loading
            withContext(Dispatchers.IO) {
                // Single MediaStore scan (images + videos) reused to derive the main feed,
                // the visible albums and the hidden albums - avoids 3-4x redundant re-queries.
                val allMediaIncludingHidden = repository.getAllMedia(includeHidden = true)
                val hiddenBuckets = repository.getHiddenBucketIds()
                val visibleMedia = if (hiddenBuckets.isEmpty()) {
                    allMediaIncludingHidden
                } else {
                    allMediaIncludingHidden.filter { it.bucketId !in hiddenBuckets }
                }

                val visibleAlbums = repository.groupIntoAlbums(visibleMedia, includeHidden = false)
                val hiddenAlbums = if (hiddenBuckets.isEmpty()) {
                    emptyList()
                } else {
                    repository.groupIntoAlbums(allMediaIncludingHidden, includeHidden = true)
                        .filter { it.bucketId in hiddenBuckets }
                }

                _rawMedia.value = visibleMedia
                _albumsState.value = visibleAlbums
                _hiddenAlbums.value = hiddenAlbums
                ThumbnailManager.getInstance(getApplication()).startPreindexing(visibleMedia)
            }
            applyFilter()
            _selectedAlbum.value?.let { active ->
                _currentSubAlbums.value = withContext(Dispatchers.IO) { repository.getSubAlbums(active, _albumsState.value) }
            }
            loadTrash()
            loadFavorites()
        }
    }

    fun loadAlbums() {
        viewModelScope.launch {
            val albums = withContext(Dispatchers.IO) { repository.getAlbums() }
            _albumsState.value = albums
            val active = _selectedAlbum.value
            if (active != null) {
                _currentSubAlbums.value = withContext(Dispatchers.IO) { repository.getSubAlbums(active, albums) }
            }
        }
    }

    fun loadTrash() {
        viewModelScope.launch {
            val trashed = withContext(Dispatchers.IO) { repository.getTrashedMedia() }
            _trashedMediaState.value = trashed
        }
    }

    fun openDetail(index: Int, source: DetailSource = DetailSource.ALL) {
        if (_isSelectionMode.value) return
        _detailSource.value = source
        _selectedItemIndex.value = index
    }

    fun closeDetail() {
        _selectedItemIndex.value = null
    }

    /**
     * Moves media to MediaStore Trash.
     */
    fun trashMedia(item: MediaItem) {
        viewModelScope.launch {
            val pendingIntent = withContext(Dispatchers.IO) { repository.createTrashRequest(listOf(item.uri), trash = true) }
            if (pendingIntent != null) {
                _pendingIntentSender.emit(pendingIntent)
            } else {
                _userMessage.emit("In Papierkorb verschoben")
                loadMedia()
            }
        }
    }

    /**
     * Restores media from MediaStore Trash.
     */
    fun restoreMedia(item: MediaItem) {
        viewModelScope.launch {
            val pendingIntent = withContext(Dispatchers.IO) { repository.createTrashRequest(listOf(item.uri), trash = false) }
            if (pendingIntent != null) {
                _pendingIntentSender.emit(pendingIntent)
            } else {
                _userMessage.emit("Wiederhergestellt")
                loadTrash()
                loadMedia()
            }
        }
    }

    /**
     * Restores all media items from the trash.
     */
    fun restoreAllTrash() {
        viewModelScope.launch {
            val uris = _trashedMediaState.value.map { it.uri }
            if (uris.isEmpty()) return@launch
            val pendingIntent = withContext(Dispatchers.IO) { repository.createTrashRequest(uris, trash = false) }
            if (pendingIntent != null) {
                _pendingIntentSender.emit(pendingIntent)
            } else {
                _userMessage.emit("Alle Elemente wiederhergestellt")
                loadTrash()
                loadMedia()
            }
        }
    }

    /**
     * Permanently deletes a media item from MediaStore.
     */
    fun deletePermanently(item: MediaItem) {
        viewModelScope.launch {
            val pendingIntent = withContext(Dispatchers.IO) { repository.createDeleteRequest(listOf(item.uri)) }
            if (pendingIntent != null) {
                _pendingIntentSender.emit(pendingIntent)
            } else {
                _userMessage.emit("Endgültig gelöscht")
                loadTrash()
                loadMedia()
            }
        }
    }

    /**
     * Permanently deletes all items in trash.
     */
    fun emptyTrash() {
        viewModelScope.launch {
            val uris = _trashedMediaState.value.map { it.uri }
            if (uris.isEmpty()) return@launch
            val pendingIntent = withContext(Dispatchers.IO) { repository.createDeleteRequest(uris) }
            if (pendingIntent != null) {
                _pendingIntentSender.emit(pendingIntent)
            } else {
                _userMessage.emit("Papierkorb geleert")
                loadTrash()
                loadMedia()
            }
        }
    }

    /**
     * Renames media item by updating MediaStore.MediaColumns.DISPLAY_NAME.
     */
    fun renameMedia(item: MediaItem, newName: String) {
        viewModelScope.launch {
            when (val result = withContext(Dispatchers.IO) { repository.renameMedia(item.uri, item.displayName, newName) }) {
                is RenameResult.Success -> {
                    _userMessage.emit("Datei umbenannt in ${result.newName}")
                    loadMedia()
                    _selectedAlbum.value?.let { openAlbum(it) }
                }
                is RenameResult.PermissionRequired -> {
                    pendingRename = PendingRenameData(item.uri, item.displayName, result.targetName)
                    _pendingIntentSender.emit(result.pendingIntent)
                }
                is RenameResult.Error -> {
                    _userMessage.emit("Fehler: ${result.message}")
                }
            }
        }
    }

    /**
     * Called when the system ActivityResultLauncher completes with RESULT_OK.
     */
    fun onIntentSenderSuccess() {
        viewModelScope.launch {
            val rename = pendingRename
            val move = pendingMove

            if (rename != null) {
                pendingRename = null
                when (val result = withContext(Dispatchers.IO) { repository.renameMedia(rename.uri, rename.oldName, rename.targetName) }) {
                    is RenameResult.Success -> {
                        _userMessage.emit("Datei umbenannt in ${result.newName}")
                    }
                    is RenameResult.Error -> {
                        _userMessage.emit("Fehler: ${result.message}")
                    }
                    is RenameResult.PermissionRequired -> Unit
                }
            } else if (move != null) {
                pendingMove = null
                _isMoveProgressVisible.value = true
                MoveMediaService.start(
                    context = getApplication(),
                    items = move.itemsToMove,
                    targetAlbum = move.targetAlbum,
                    onCompletion = {
                        loadMedia()
                        loadAlbums()
                        _selectedAlbum.value?.let { loadAlbumContents(it) }
                        viewModelScope.launch {
                            _userMessage.emit("${move.itemsToMove.size} Element(e) erfolgreich nach '${move.targetAlbum.name}' verschoben")
                            kotlinx.coroutines.delay(2500)
                            dismissMoveProgress()
                        }
                    }
                )
                clearSelection()
            } else {
                _userMessage.emit("Aktion erfolgreich ausgeführt")
                // Covers Trash/Delete/Album-migration write requests: MediaStore commits the
                // underlying row changes shortly after RESULT_OK, so a brief delay avoids
                // refreshing on stale data (e.g. a just-migrated album still showing its old location).
                kotlinx.coroutines.delay(300)
            }

            clearSelection()
            loadMedia()
            loadTrash()
            loadAlbums()
            _selectedAlbum.value?.let { openAlbum(it) }
        }
    }

    /**
     * Creates a new album folder.
     */
    fun createAlbum(albumName: String, onSdCard: Boolean) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repository.createAlbumFolder(albumName, onSdCard) }
            result.onSuccess { dir ->
                val targetLocation = if (onSdCard) "auf SD-Karte" else "im internen Speicher"
                _userMessage.emit("Album '${dir.name}' $targetLocation erstellt")
                loadAlbums()
            }.onFailure { err ->
                _userMessage.emit("Fehler beim Erstellen: ${err.localizedMessage}")
            }
        }
    }

    /**
     * Creates a new sub-album folder inside [parentAlbum].
     */
    fun createSubAlbum(parentAlbum: AlbumItem, subAlbumName: String) {
        viewModelScope.launch {
            val result = repository.createSubAlbumFolder(parentAlbum, subAlbumName)
            result.onSuccess { dir ->
                val location = if (parentAlbum.isSdCard) "auf SD-Karte" else "im internen Speicher"
                _userMessage.emit("Unteralbum '${dir.name}' in '${parentAlbum.name}' $location erstellt")
                loadAlbums()
                loadAlbumContents(parentAlbum)
            }.onFailure { err ->
                _userMessage.emit("Fehler beim Erstellen: ${err.localizedMessage}")
            }
        }
    }

    /**
     * Renames an album folder physically and updates MediaStore.
     */
    fun renameAlbum(album: AlbumItem, newName: String) {
        viewModelScope.launch {
            val result = repository.renameAlbum(album, newName)
            result.onSuccess {
                _userMessage.emit("Album in '$newName' umbenannt")
                loadAlbums()
                loadMedia()
                if (_selectedAlbum.value?.bucketId == album.bucketId) {
                    val updated = _albumsState.value.firstOrNull { it.name == newName } ?: album.copy(name = newName)
                    _selectedAlbum.value = updated
                    loadAlbumContents(updated)
                }
            }.onFailure { err ->
                _userMessage.emit("Fehler beim Umbenennen: ${err.localizedMessage}")
            }
        }
    }

    /**
     * Deletes an album by moving its media to Trash and removing the folder.
     */
    fun deleteAlbum(album: AlbumItem) {
        viewModelScope.launch {
            val result = repository.deleteAlbum(album)
            result.onSuccess { pendingIntent ->
                if (pendingIntent != null) {
                    _pendingIntentSender.emit(pendingIntent)
                } else {
                    _userMessage.emit("Album '${album.name}' gelöscht")
                    if (_selectedAlbum.value?.bucketId == album.bucketId) {
                        closeAlbum()
                    }
                    loadAlbums()
                    loadMedia()
                    loadTrash()
                }
            }.onFailure { err ->
                _userMessage.emit("Fehler beim Löschen: ${err.localizedMessage}")
            }
        }
    }

    /**
     * Migrates an album between Internal storage and MicroSD card.
     */
    fun migrateAlbum(album: AlbumItem, toSdCard: Boolean) {
        viewModelScope.launch {
            val destName = if (toSdCard) "SD-Karte" else "internen Speicher"
            _userMessage.emit("Verschiebe '${album.name}' auf $destName...")

            moveProgress.value = MoveProgress(isMoving = true, current = 0, total = album.count)

            var lastUpdate = System.currentTimeMillis()
            val result = withContext(Dispatchers.IO) {
                repository.migrateAlbumStorage(album, toSdCard) { current, totalCount ->
                    val now = System.currentTimeMillis()
                    if (current == totalCount || now - lastUpdate >= 200 || current % maxOf(1, totalCount / 20) == 0) {
                        moveProgress.value = moveProgress.value.copy(current = current, total = totalCount)
                        lastUpdate = now
                    }
                }
            }

            moveProgress.value = moveProgress.value.copy(isMoving = false)

            result.onSuccess { pendingIntent ->
                if (pendingIntent != null) {
                    _pendingIntentSender.emit(pendingIntent)
                } else {
                    _userMessage.emit("Album '${album.name}' erfolgreich auf $destName verschoben")
                    loadAlbums()
                    loadMedia()
                    if (_selectedAlbum.value?.bucketId == album.bucketId) {
                        closeAlbum()
                    }
                }
            }.onFailure { err ->
                _userMessage.emit("Fehler bei Migration: ${err.localizedMessage}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ThumbnailManager.getInstance(getApplication()).stopPreindexing()
    }
}
