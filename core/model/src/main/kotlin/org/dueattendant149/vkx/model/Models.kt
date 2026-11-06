package org.dueattendant149.vkx.model

import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val artists: List<String> = listOf(artist),
    val artistId: Long? = null,
    val artistRemoteId: String? = null,
    val album: String? = null,
    val albumId: Long? = null,
    val albumRemoteId: String? = null,
    val durationSec: Int,
    val genre: String? = null,
    val year: Int? = null,
    val isExplicit: Boolean = false,
    val streamUrl: String? = null,
    val coverUrl: String? = null,
    val remoteId: String? = null,
    val hasLyrics: Boolean = false,
    val isLiked: Boolean = false,
    val isDisliked: Boolean = false,
    val isOfflineCached: Boolean = false,
)

@Serializable
data class Album(
    val id: Long,
    val title: String,
    val artistId: Long,
    val artistName: String,
    val year: Int? = null,
    val coverUrl: String? = null,
    val trackIds: List<Long> = emptyList(),
    val remoteId: String? = null,
) {
    val trackCount: Int get() = trackIds.size
}

@Serializable
data class Artist(
    val id: Long,
    val name: String,
    val genre: String? = null,
    val coverUrl: String? = null,
    val albumIds: List<Long> = emptyList(),
    val topTrackIds: List<Long> = emptyList(),
    val remoteId: String? = null,
)

@Serializable
enum class Visibility { PRIVATE, UNLISTED, PUBLIC }

@Serializable
data class Playlist(
    val id: Long,
    val name: String,
    val description: String? = null,
    val ownerName: String = "Вы",
    val isSystem: Boolean = false,
    val coverUrl: String? = null,
    val trackIds: List<Long> = emptyList(),
    val visibility: Visibility = Visibility.PRIVATE,
    val remoteId: String? = null,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
) {
    val trackCount: Int get() = trackIds.size
}

@Serializable
data class User(
    val id: Long,
    val name: String,
    val avatarUrl: String? = null,
)

data class SearchResult(
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
) {
    val isEmpty: Boolean get() = tracks.isEmpty() && albums.isEmpty() && artists.isEmpty() && playlists.isEmpty()
}

data class Library(
    val favorites: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
)
