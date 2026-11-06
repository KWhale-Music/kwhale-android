package org.dueattendant149.vkx.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dueattendant149.vkx.VkxApp
import org.dueattendant149.vkx.data.toUi
import org.dueattendant149.vkx.network.model.Playlist
import org.dueattendant149.vkx.network.model.Track as ApiTrack
import org.dueattendant149.vkx.ui.components.AlbumCard
import org.dueattendant149.vkx.ui.components.artistLine
import org.dueattendant149.vkx.ui.components.EmptyState
import org.dueattendant149.vkx.ui.components.ErrorState
import org.dueattendant149.vkx.ui.components.PlaylistCard
import org.dueattendant149.vkx.ui.components.SectionHeader
import org.dueattendant149.vkx.ui.components.TrackRow

@Composable
fun LibraryScreen(
    app: VkxApp,
    onOpenCollection: (String, String) -> Unit,
    onSeeAll: (String) -> Unit,
    onTrackMenu: (ApiTrack) -> Unit,
    onDownloads: () -> Unit,
    onSearch: () -> Unit = {},
) {
    var tracks by remember { mutableStateOf<List<ApiTrack>>(emptyList()) }
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    val liked = remember { mutableStateMapOf<String, Boolean>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reload) {
        loading = true
        error = null
        runCatching {
            val tracksPage = app.api.libraryTracks(limit = 100)
            val playlistsPage = app.api.libraryPlaylists(limit = 100)
            tracks = tracksPage.items
            playlists = playlistsPage.items
        }.onFailure { error = it.message ?: "Ошибка сети" }
        loading = false
    }

    if (loading && tracks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (error != null && tracks.isEmpty()) {
        ErrorState(
            message = error ?: "Ошибка сети",
            onRetry = { reload++ },
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    val albums = remember(tracks) {
        tracks.mapNotNull { track ->
            track.album?.let { it to track }
        }
            .groupBy({ it.first.id }, { it.first })
            .map { (id, refs) ->
                val first = refs.first()
                val artist = tracks.firstOrNull { it.album?.id == id }?.artistLine().orEmpty()
                LibraryAlbum(
                    id = id,
                    title = first.title,
                    artist = artist.ifBlank { "Альбом" },
                    coverUrl = first.cover?.let { app.api.imageUrl(it.id) },
                    count = tracks.count { it.album?.id == id },
                )
            }
    }

    val collections = remember(playlists, albums, app) {
        playlists.map { playlist ->
            LibraryCollection(
                key = "p-${playlist.id}",
                kind = "playlist",
                id = playlist.id,
                title = playlist.title,
                subtitle = "${playlist.trackCount} треков",
                coverUrl = playlist.cover?.let { app.api.imageUrl(it.id) },
            )
        } + albums.map { album ->
            LibraryCollection(
                key = "a-${album.id}",
                kind = "album",
                id = album.id,
                title = album.title,
                subtitle = album.artist,
                coverUrl = album.coverUrl,
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Библиотека",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onSearch) {
                    Icon(Icons.Rounded.Search, contentDescription = "Поиск")
                }
                IconButton(onClick = onDownloads) {
                    Icon(Icons.Rounded.Download, contentDescription = "Скачано")
                }
            }
        }

        if (collections.isNotEmpty()) {
            item { SectionHeader("Коллекции", action = "Все", onAction = { onSeeAll("collections") }) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(collections, key = { it.key }) { collection ->
                        AlbumCard(
                            title = collection.title,
                            artist = collection.subtitle,
                            coverUrl = collection.coverUrl,
                            onClick = { onOpenCollection(collection.kind, collection.id) },
                        )
                    }
                }
            }
        }

        if (tracks.isNotEmpty()) {
            item {
                Spacer(Modifier.size(16.dp))
                Button(
                    onClick = { app.player.playQueue(tracks.shuffled().map { it.toUi(app.api) }, 0) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                ) {
                    Icon(Icons.Rounded.Shuffle, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Перемешать всё")
                }
                SectionHeader("Любимые треки")
            }
            itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                TrackRow(
                    track = track,
                    api = app.api,
                    liked = liked[track.id] ?: track.isLiked,
                    onClick = { app.player.playQueue(tracks.map { it.toUi(app.api) }, index) },
                    onToggleLike = {
                        val next = liked[track.id] ?: !track.isLiked
                        liked[track.id] = next
                        scope.launch {
                            runCatching {
                                if (next) app.api.like(track.id) else app.api.unlike(track.id)
                            }
                        }
                    },
                    onOverflow = { onTrackMenu(track) },
                )
            }
        } else {
            item {
                EmptyState(
                    title = "Пока пусто",
                    message = "Лайкните трек, чтобы он появился здесь",
                    action = "Найти музыку",
                    onAction = { onSeeAll("search") },
                )
            }
        }
    }
}
