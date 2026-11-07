package org.dueattendant149.kwhale.ui.screens

internal data class LibraryAlbum(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String?,
    val count: Int,
)

internal data class LibraryCollection(
    val key: String,
    val kind: String,
    val id: String,
    val title: String,
    val subtitle: String,
    val coverUrl: String?,
)
