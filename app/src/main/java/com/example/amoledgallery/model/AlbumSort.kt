package com.example.amoledgallery.model

/** The order of the album list. */
enum class AlbumSort {
    /** Camera, DCIM and Screenshots first, then A-Z. */
    STANDARD,
    NAME_ASC,
    NAME_DESC,
    MOST_ITEMS,
    /** Arranged by hand (moved up and down from each album's menu). */
    CUSTOM
}

/** [albums] (already in the standard order) in [sort]; [customOrder] holds bucket ids for [AlbumSort.CUSTOM]. */
fun sortAlbums(albums: List<AlbumItem>, sort: AlbumSort, customOrder: List<String>): List<AlbumItem> = when (sort) {
    AlbumSort.STANDARD -> albums
    AlbumSort.NAME_ASC -> albums.sortedBy { it.name.lowercase() }
    AlbumSort.NAME_DESC -> albums.sortedByDescending { it.name.lowercase() }
    AlbumSort.MOST_ITEMS -> albums.sortedByDescending { it.count }
    AlbumSort.CUSTOM -> {
        val position = customOrder.withIndex().associate { it.value to it.index }
        // Albums placed by hand in that order; new ones after them, in the standard order (stable sort).
        albums.sortedBy { position[it.bucketId] ?: Int.MAX_VALUE }
    }
}
