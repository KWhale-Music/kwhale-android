package org.dueattendant149.kwhale.network.model

import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: String,
    val title: String,
    val artists: List<ArtistRef> = emptyList(),
    val album: AlbumRef? = null,
    val durationMs: Long = 0L,
    val explicit: Boolean = false,
    val cover: ImageRef? = null,
    val genre: Genre? = null,
    val year: Int? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val isLiked: Boolean = false,
    val isDisliked: Boolean = false,
    val hasLyrics: Boolean = false,
    val isDownloaded: Boolean = false,
    val downloadStatus: String = "pending",
    val previewAvailable: Boolean = false,
    val playCount: Long? = null,
    val createdAt: String? = null,
)

@Serializable
data class Album(
    val id: String,
    val title: String,
    val artists: List<ArtistRef> = emptyList(),
    val cover: ImageRef? = null,
    val year: Int? = null,
    val trackCount: Int = 0,
    val type: String? = null,
    val explicit: Boolean = false,
    val isFavorite: Boolean = false,
    val tracks: List<Track> = emptyList(),
    val createdAt: String? = null,
)

@Serializable
data class Artist(
    val id: String,
    val name: String,
    val avatar: ImageRef? = null,
    val bio: String? = null,
    val genres: List<Genre> = emptyList(),
    val followers: Long = 0L,
    val albumCount: Int = 0,
    val isFavorite: Boolean = false,
    val topTracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
)

@Serializable
data class PlaylistPermissions(
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val canAddTracks: Boolean = false,
)

@Serializable
data class Playlist(
    val id: String,
    val title: String,
    val description: String? = null,
    val owner: UserRef? = null,
    val cover: ImageRef? = null,
    val trackCount: Int = 0,
    val visibility: String = "private",
    val isSystem: Boolean = false,
    val isFollowed: Boolean = false,
    val permissions: PlaylistPermissions = PlaylistPermissions(),
    val tracks: List<Track> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class TracksPage(
    val items: List<Track> = emptyList(),
    val nextCursor: String? = null,
    val total: Int? = null,
)

@Serializable
data class AlbumsPage(
    val items: List<Album> = emptyList(),
    val nextCursor: String? = null,
)

@Serializable
data class ArtistsPage(
    val items: List<Artist> = emptyList(),
    val nextCursor: String? = null,
)

@Serializable
data class PlaylistsPage(
    val items: List<Playlist> = emptyList(),
    val nextCursor: String? = null,
    val total: Int? = null,
)

@Serializable
data class Suggestion(
    val text: String,
    val type: String = "query",
)

@Serializable
data class Suggestions(
    val items: List<Suggestion> = emptyList(),
)

@Serializable
data class SearchResponse(
    val query: String = "",
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val mixes: List<Mix> = emptyList(),
    val total: Int = 0,
    val partial: Boolean = false,
)

@Serializable
data class SearchHistory(
    val items: List<String> = emptyList(),
)

@Serializable
data class LyricsLine(
    val startMs: Long,
    val text: String,
    val endMs: Long? = null,
)

@Serializable
data class Lyrics(
    val trackId: String,
    val provider: String = "internal",
    val language: String = "ru",
    val plain: String? = null,
    val synced: List<LyricsLine> = emptyList(),
    val hasTimestamps: Boolean = false,
)

@Serializable
data class Banner(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val image: ImageRef? = null,
    val action: String? = null,
)

@Serializable
data class HomeAction(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val icon: String? = null,
    val action: String,
)

@Serializable
data class HomeItem(
    val type: String,
    val track: Track? = null,
    val album: Album? = null,
    val artist: Artist? = null,
    val playlist: Playlist? = null,
    val mix: Mix? = null,
    val banner: Banner? = null,
    val action: HomeAction? = null,
)

@Serializable
data class HomeSection(
    val id: String,
    val kind: String,
    val layout: String = "list",
    val title: String? = null,
    val subtitle: String? = null,
    val items: List<HomeItem> = emptyList(),
    val seeAll: Boolean = false,
    val nextCursor: String? = null,
)

@Serializable
data class HomeResponse(
    val sections: List<HomeSection> = emptyList(),
)
