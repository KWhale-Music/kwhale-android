package org.dueattendant149.kwhale.data

import org.dueattendant149.kwhale.model.Album
import org.dueattendant149.kwhale.model.Artist
import org.dueattendant149.kwhale.model.Playlist
import org.dueattendant149.kwhale.model.Track
import org.dueattendant149.kwhale.model.Visibility
import org.dueattendant149.kwhale.network.KwhaleApi
import org.dueattendant149.kwhale.network.model.Album as ApiAlbum
import org.dueattendant149.kwhale.network.model.Artist as ApiArtist
import org.dueattendant149.kwhale.network.model.Playlist as ApiPlaylist
import org.dueattendant149.kwhale.network.model.Track as ApiTrack

internal fun String.stableId(): Long = toLongOrNull() ?: hashCode().toLong()

/** Mirrors the API's placeholder sentinel: such ids must never be requested. */
private fun imageIdOrNull(id: String?): String? =
    id?.takeIf { it.isNotBlank() && it != "placeholder" }

internal fun ApiTrack.toUi(api: KwhaleApi, isOfflineCached: Boolean = false): Track = Track(
    id = id.stableId(),
    title = title,
    artist = artists.joinToString(", ") { it.name }.ifBlank { "Неизвестный артист" },
    artists = artists.map { it.name },
    artistId = artists.firstOrNull()?.id?.stableId(),
    artistRemoteId = artists.firstOrNull()?.id?.takeIf { !it.startsWith("unknown-") },
    album = album?.title,
    albumId = album?.id?.stableId(),
    albumRemoteId = album?.id,
    durationSec = (durationMs / 1000L).toInt(),
    genre = genre?.name,
    year = year,
    isExplicit = explicit,
    streamUrl = api.streamUrl(id),
    // Track artwork wins; album artwork is the fallback (see imageUrl helper).
    coverUrl = imageIdOrNull(cover?.id)?.let { api.imageUrl(it, "lg") }
        ?: imageIdOrNull(album?.cover?.id)?.let { api.imageUrl(it, "lg") },
    remoteId = id,
    hasLyrics = hasLyrics,
    isLiked = isLiked,
    isDisliked = isDisliked,
    isOfflineCached = isOfflineCached,
)

internal fun ApiAlbum.toUi(api: KwhaleApi): Album = Album(
    id = id.stableId(),
    title = title,
    artistId = artists.firstOrNull()?.id?.stableId() ?: 0L,
    artistName = artists.joinToString(", ") { it.name }.ifBlank { "Неизвестный артист" },
    year = year,
    coverUrl = cover?.let { api.imageUrl(it.id) },
    trackIds = tracks.map { it.id.stableId() },
    remoteId = id,
)

internal fun ApiArtist.toUi(api: KwhaleApi): Artist = Artist(
    id = id.stableId(),
    name = name,
    genre = genres.firstOrNull()?.name,
    coverUrl = avatar?.let { api.imageUrl(it.id) },
    albumIds = albums.map { it.id.stableId() },
    topTrackIds = topTracks.map { it.id.stableId() },
    remoteId = id,
)

internal fun ApiPlaylist.toUi(api: KwhaleApi): Playlist = Playlist(
    id = id.stableId(),
    name = title,
    description = description,
    ownerName = owner?.displayName ?: "Вы",
    isSystem = isSystem,
    coverUrl = cover?.let { api.imageUrl(it.id) },
    trackIds = tracks.map { it.id.stableId() },
    visibility = when (visibility) {
        "public" -> Visibility.PUBLIC
        "unlisted" -> Visibility.UNLISTED
        else -> Visibility.PRIVATE
    },
    remoteId = id,
    canEdit = permissions.canEdit,
    canDelete = permissions.canDelete,
)
