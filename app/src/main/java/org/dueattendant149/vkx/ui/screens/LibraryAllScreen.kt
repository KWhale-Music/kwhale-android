package org.dueattendant149.vkx.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import org.dueattendant149.vkx.ui.components.EmptyState
import org.dueattendant149.vkx.ui.components.PlaylistCard
import org.dueattendant149.vkx.ui.components.TrackRow
import org.dueattendant149.vkx.ui.components.artistLine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryAllScreen(
    app: VkxApp,
    kind: String,
    onBack: () -> Unit,
    onOpenCollection: (String, String) -> Unit,
) {
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var albums by remember { mutableStateOf<List<LibraryAlbum>>(emptyList()) }
    var collections by remember { mutableStateOf<List<LibraryCollection>>(emptyList()) }
    var tracks by remember { mutableStateOf<List<ApiTrack>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val liked = remember { mutableStateMapOf<String, Boolean>() }
    val scope = rememberCoroutineScope()
    val title = when (kind) {
        "playlists" -> "Плейлисты"
        "albums" -> "Альбомы"
        "tracks", "liked" -> "Треки"
        else -> "Коллекции"
    }

    LaunchedEffect(kind) {
        loading = true
        when (kind) {
            "playlists" -> playlists = runCatching {
                app.api.libraryPlaylists(limit = 100).items
            }.getOrDefault(emptyList())
            "albums", "collections" -> {
                tracks = runCatching { app.api.libraryTracks(limit = 200).items }
                    .getOrDefault(emptyList())
                albums = tracks.mapNotNull { it.album }.distinctBy { it.id }.map { ref ->
                    val artist = tracks.firstOrNull { it.album?.id == ref.id }?.artistLine().orEmpty()
                    LibraryAlbum(
                        id = ref.id,
                        title = ref.title,
                        artist = artist.ifBlank { "Альбом" },
                        coverUrl = ref.cover?.let { app.api.imageUrl(it.id) },
                        count = tracks.count { it.album?.id == ref.id },
                    )
                }
                if (kind == "collections") {
                    playlists = runCatching { app.api.libraryPlaylists(limit = 100).items }
                        .getOrDefault(emptyList())
                    collections = playlists.map { playlist ->
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
            }
            else -> tracks = runCatching { app.api.libraryTracks(limit = 200).items }
                .getOrDefault(emptyList())
        }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                }
            },
        )
        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }
        when (kind) {
            "collections" -> {
                if (collections.isEmpty()) {
                    EmptyState("Пока пусто", "Сохраните альбом или создайте плейлист", modifier = Modifier.weight(1f).fillMaxWidth())
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    ) {
                        items(collections, key = { it.key }) { collection ->
                            Box(Modifier.padding(6.dp)) {
                                AlbumCard(
                                    title = collection.title,
                                    artist = collection.subtitle,
                                    coverUrl = collection.coverUrl,
                                    onClick = { onOpenCollection(collection.kind, collection.id) },
                                    width = 150,
                                )
                            }
                        }
                    }
                }
            }
            "playlists" -> {
                if (playlists.isEmpty()) {
                    EmptyState("Пока пусто", "Создайте первый плейлист", modifier = Modifier.weight(1f).fillMaxWidth())
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    ) {
                        items(playlists, key = { it.id }) { playlist ->
                            Box(Modifier.padding(6.dp)) {
                                PlaylistCard(
                                    playlist = playlist,
                                    api = app.api,
                                    onClick = { onOpenCollection("playlist", playlist.id) },
                                    width = 150,
                                )
                            }
                        }
                    }
                }
            }
            "albums" -> {
                if (albums.isEmpty()) {
                    EmptyState("Пока пусто", "Сохраните альбом, чтобы он появился здесь", modifier = Modifier.weight(1f).fillMaxWidth())
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    ) {
                        items(albums, key = { it.id }) { album ->
                            Box(Modifier.padding(6.dp)) {
                                AlbumCard(
                                    title = album.title,
                                    artist = album.artist,
                                    coverUrl = album.coverUrl,
                                    onClick = { onOpenCollection("album", album.id) },
                                    width = 150,
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                if (tracks.isEmpty()) {
                    EmptyState("Пока пусто", "Лайкните трек, чтобы он появился здесь", modifier = Modifier.weight(1f).fillMaxWidth())
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    ) {
                        itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                            TrackRow(
                                track = track,
                                api = app.api,
                                liked = liked[track.id] ?: track.isLiked,
                                onClick = {
                                    app.player.playQueue(tracks.map { it.toUi(app.api) }, index)
                                },
                                onToggleLike = {
                                    val next = liked[track.id] ?: !track.isLiked
                                    liked[track.id] = next
                                    scope.launch {
                                        runCatching {
                                            if (next) app.api.like(track.id) else app.api.unlike(track.id)
                                        }
                                    }
                                },
                                onOverflow = {},
                            )
                        }
                    }
                }
            }
        }
    }
}
