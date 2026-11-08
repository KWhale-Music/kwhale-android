package org.dueattendant149.kwhale.data

import kotlinx.serialization.Serializable
import org.dueattendant149.kwhale.network.model.Album
import org.dueattendant149.kwhale.network.model.Artist
import org.dueattendant149.kwhale.network.model.EnrichmentProposal
import org.dueattendant149.kwhale.network.model.Genre
import org.dueattendant149.kwhale.network.model.HomeResponse
import org.dueattendant149.kwhale.network.model.Mix
import org.dueattendant149.kwhale.network.model.Playlist
import org.dueattendant149.kwhale.network.model.Track

@Serializable
data class HomeSnapshot(
    val home: HomeResponse? = null,
    val mixes: List<Mix> = emptyList(),
)

@Serializable
data class LibrarySnapshot(
    val tracks: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val albums: List<Album> = emptyList(),
)

@Serializable
data class SearchBootstrapSnapshot(
    val history: List<String> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val artists: List<Artist> = emptyList(),
)

@Serializable
data class ProfileSnapshot(
    val name: String = "Профиль",
    val email: String = "",
    val avatar: String? = null,
    val trackCount: Int = 0,
    val playlistCount: Int = 0,
    val downloadCount: Int = 0,
    val enrichments: List<EnrichmentProposal> = emptyList(),
)

@Serializable
data class CollectionSnapshot(
    val title: String = "",
    val subtitle: String = "",
    val meta: String = "",
    val cover: String? = null,
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val latestRelease: Album? = null,
    val canEdit: Boolean = false,
    val isFavorite: Boolean = false,
    val isFollowed: Boolean = false,
    val isOwned: Boolean = false,
)
