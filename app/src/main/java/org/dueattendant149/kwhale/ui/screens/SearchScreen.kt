package org.dueattendant149.kwhale.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dueattendant149.kwhale.KwhaleApp
import org.dueattendant149.kwhale.design.KwhalePlural
import org.dueattendant149.kwhale.data.toUi
import org.dueattendant149.kwhale.network.model.SearchResponse
import org.dueattendant149.kwhale.network.model.Track as ApiTrack
import org.dueattendant149.kwhale.ui.components.AlbumCard
import org.dueattendant149.kwhale.ui.components.ArtistCard
import org.dueattendant149.kwhale.ui.components.CoverArt
import org.dueattendant149.kwhale.ui.components.PlaylistCard
import org.dueattendant149.kwhale.ui.components.SectionHeader
import org.dueattendant149.kwhale.ui.components.TrackRow
import org.dueattendant149.kwhale.ui.components.artistLine
import org.dueattendant149.kwhale.ui.components.imageUrl

@Composable
fun SearchScreen(
    app: KwhaleApp,
    onOpenCollection: (String, String) -> Unit,
    onTrackMenu: (ApiTrack) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<SearchResponse?>(null) }
    var history by remember { mutableStateOf<List<String>>(emptyList()) }
    var genres by remember { mutableStateOf<List<org.dueattendant149.kwhale.network.model.Genre>>(emptyList()) }
    var topArtists by remember { mutableStateOf<List<org.dueattendant149.kwhale.network.model.Artist>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchTick by remember { mutableStateOf(0) }
    val liked = remember { mutableStateMapOf<String, Boolean>() }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        history = runCatching { app.api.searchHistory().items }.getOrDefault(emptyList())
        genres = runCatching { app.api.genres().items }.getOrDefault(emptyList())
        topArtists = runCatching { app.api.topArtists(limit = 12).items }.getOrDefault(emptyList())
    }

    LaunchedEffect(query, searchTick) {
        if (query.isBlank()) {
            results = null
            error = null
            loading = false
            return@LaunchedEffect
        }
        delay(350)
        loading = true
        error = null
        runCatching { app.api.search(query.trim(), limit = 30) }
            .onSuccess { results = it }
            .onFailure {
                results = null
                error = "Не удалось выполнить поиск"
            }
        loading = false
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            placeholder = { Text("Треки, альбомы, артисты") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Очистить")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        )

        if (query.isBlank()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            ) {
                if (history.isNotEmpty()) {
                    item { SectionHeader("Недавние запросы") }
                    items(history.take(6)) { q ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { query = q }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(q, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                if (genres.isNotEmpty()) {
                    item { SectionHeader("Жанры") }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(genres.take(12)) { genre ->
                                FilterChip(
                                    selected = false,
                                    onClick = { query = genre.name },
                                    label = { Text(genre.name) },
                                )
                            }
                        }
                    }
                }
                if (topArtists.isNotEmpty()) {
                    item { SectionHeader("Популярные артисты") }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(topArtists) { artist ->
                                ArtistCard(
                                    artist = artist,
                                    api = app.api,
                                    onClick = { onOpenCollection("artist", artist.id) },
                                )
                            }
                        }
                    }
                }
            }
        } else {
            if (loading) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val data = results
                val err = error
                if (err != null) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { searchTick++ }) {
                                Text("Повторить")
                            }
                        }
                    }
                } else if (data == null || (data.tracks.isEmpty() && data.albums.isEmpty() && data.artists.isEmpty() && data.playlists.isEmpty())) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Ничего не найдено",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
                    ) {
                        item { TopResult(data, app, onOpenCollection) }
                        val tracks = data.tracks
                        if (tracks.isNotEmpty()) {
                            item { SectionHeader("Треки") }
                            itemsIndexed(tracks) { index, track ->
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
                                    onOverflow = { onTrackMenu(track) },
                                )
                            }
                        }
                        if (data.albums.isNotEmpty()) {
                            item { SectionHeader("Альбомы") }
                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(data.albums) { album ->
                                        AlbumCard(
                                            album = album,
                                            api = app.api,
                                            onClick = { onOpenCollection("album", album.id) },
                                        )
                                    }
                                }
                            }
                        }
                        if (data.playlists.isNotEmpty()) {
                            item { SectionHeader("Плейлисты") }
                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(data.playlists) { playlist ->
                                        PlaylistCard(
                                            playlist = playlist,
                                            api = app.api,
                                            onClick = { onOpenCollection("playlist", playlist.id) },
                                        )
                                    }
                                }
                            }
                        }
                        if (data.artists.isNotEmpty()) {
                            item { SectionHeader("Артисты") }
                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(data.artists) { artist ->
                                        ArtistCard(
                                            artist = artist,
                                            api = app.api,
                                            onClick = { onOpenCollection("artist", artist.id) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopResult(
    data: SearchResponse,
    app: KwhaleApp,
    onOpenCollection: (String, String) -> Unit,
) {
    val artist = data.artists.firstOrNull()
    val album = data.albums.firstOrNull()
    val track = data.tracks.firstOrNull()
    val playlist = data.playlists.firstOrNull()

    val (title, subtitle, cover, onClick) = when {
        artist != null -> Quad(
            artist.name,
            "Артист",
            artist.imageUrl(app.api),
            { onOpenCollection("artist", artist.id) },
        )
        album != null -> Quad(
            album.title,
            album.artists.joinToString(", ") { it.name },
            album.imageUrl(app.api),
            { onOpenCollection("album", album.id) },
        )
        track != null -> Quad(
            track.title,
            track.artistLine(),
            track.imageUrl(app.api),
            { app.player.playQueue(listOf(track.toUi(app.api)), 0) },
        )
        playlist != null -> Quad(
            playlist.title,
            KwhalePlural.tracks(playlist.trackCount),
            playlist.imageUrl(app.api),
            { onOpenCollection("playlist", playlist.id) },
        )
        else -> return
    }

    SectionHeader("Лучший результат")
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverArt(
                imageUrl = cover,
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(10.dp),
                iconSize = 24,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private data class Quad(
    val title: String,
    val subtitle: String,
    val cover: String?,
    val onClick: () -> Unit,
)
