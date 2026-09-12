package com.example.amoledgallery.model

import androidx.compose.runtime.Immutable

/**
 * Grouped media section with header title (e.g. "Heute", "12. September 2024", or "September 2024")
 * and its list of media items. Annotated with @Immutable for Compose performance.
 */
@Immutable
data class MediaDateSection(
    val title: String,
    val items: List<MediaItem>
)
