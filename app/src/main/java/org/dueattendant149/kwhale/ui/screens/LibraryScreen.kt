package org.dueattendant149.kwhale.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
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
import org.dueattendant149.kwhale.KwhaleApp
import org.dueattendant149.kwhale.data.ContentCache
import org.dueattendant149.kwhale.data.LibrarySnapshot
import org.dueattendant149.kwhale.design.KwhalePlural
import org.dueattendant149.kwhale.design.LocalMiniPlayerBottomPadding
import org.dueattendant149.kwhale.data.toUi
import org.dueattendant149.kwhale.network.model.Album
import org.dueattendant149.kwhale.network.model.Playlist
import org.dueattendant149.kwhale.network.model.Track as ApiTrack
import org.dueattendant149.kwhale.ui.components.AlbumCard
import org.dueattendant149.kwhale.ui.components.CardCarouselSkeleton
import org.dueattendant149.kwhale.ui.components.EmptyState
import org.dueattendant149.kwhale.ui.components.ErrorState
import org.dueattendant149.kwhale.ui.components.SectionHeader
import org.dueattendant149.kwhale.ui.components.ShimmerBox
import org.dueattendant149.kwhale.ui.components.TrackRow
import org.dueattendant149.kwhale.ui.components.imageUrl
import org.dueattendant149.kwhale.ui.components.TrackRowSkeleton

@Composable
fun LibraryScreen(
    app: KwhaleApp,
    onOpenCollection: (String, String) -> Unit,
    onSeeAll: (String) -> Unit,
    onTrackMenu: (ApiTrack) -> Unit,
    onDownloads: () -> Unit,
    onSearch: () -> Unit = {},
) {
    val cached = remember { app.cache.read(ContentCache.LIBRARY, LibrarySnapshot.serializer()) }
    var tracks by remember { mutableStateOf(cached?.tracks.orEmpty()) }
    var playlists by remember { mutableStateOf(cached?.playlists.orEmpty()) }
    var albums by remember { mutableStateOf(cached?.albums.orEmpty()) }
    var loading by remember { mutableStateOf(cached == null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    val liked = remember { mutableStateMapOf<String, Boolean>() }
    val scope = rememberCoroutineScope()
    val miniPlayerBottom = LocalMiniPlayerBottomPadding.current

    LaunchedEffect(reload) {
        if (reload == 0 && cached != null && app.cache.isFresh(ContentCache.LIBRARY, 30_000)) {
            loading = false
            return@LaunchedEffect
        }
        loading = cached == null
        error = null
        runCatching {
            val tracksPage = app.api.libraryTracks(limit = 100)
            val playlistsPage = app.api.libraryPlaylists(limit = 100)
            val albumsPage = app.api.libraryAlbums(limit = 100)
            tracks = tracksPage.items
            playlists = playlistsPage.items
            albums = albumsPage.items
            app.cache.write(
                ContentCache.LIBRARY,
                LibrarySnapshot.serializer(),
                LibrarySnapshot(tracksPage.items, playlistsPage.items, albumsPage.items),
            )
        }.onFailure { error = it.message ?: "Ошибка сети" }
        loading = false
    }

    if (loading && tracks.isEmpty()) {
        LibrarySkeleton()
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

    // Library albums are explicitly saved cards only. They are NOT derived from
    // liked tracks: liking one track must not add its whole album to the library.
    val collections = remember(playlists, albums, app) {
        (playlists.map { playlist ->
            LibraryCollection(
                key = "p-${playlist.id}",
                kind = "playlist",
                id = playlist.id,
                title = playlist.title,
                subtitle = KwhalePlural.tracks(playlist.trackCount),
                coverUrl = playlist.imageUrl(app.api),
                addedAt = playlist.createdAt,
            )
        } + albums.map { album ->
            LibraryCollection(
                key = "a-${album.id}",
                kind = "album",
                id = album.id,
                title = album.title,
                subtitle = album.artists.joinToString(", ") { it.name }.ifBlank { "Альбом" },
                coverUrl = album.imageUrl(app.api),
                addedAt = album.addedAt,
            )
        }).sortedWith(
            compareByDescending<LibraryCollection> { it.addedAt ?: "" }
                .thenBy { it.title.lowercase() },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 12.dp,
            bottom = 28.dp + miniPlayerBottom,
        ),
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
            item { SectionHeader("Альбомы", action = "Все", onAction = { onSeeAll("collections") }) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(collections, key = { it.key }) { collection ->
                        AlbumCard(
                            title = collection.title,
                            artist = collection.subtitle,
                            coverUrl = collection.coverUrl,
                            coverTitle = if (collection.kind == "playlist") collection.title else null,
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
                        app.cache.invalidateLibrary()
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
                    onAction = onSearch,
                )
            }
        }
    }
}

@Composable
private fun LibrarySkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShimmerBox(Modifier.width(150.dp).height(30.dp))
            Spacer(Modifier.weight(1f))
            ShimmerBox(Modifier.size(40.dp), CircleShape)
        }
        Spacer(Modifier.size(24.dp))
        ShimmerBox(Modifier.width(140.dp).height(22.dp))
        Spacer(Modifier.size(10.dp))
        CardCarouselSkeleton(cardWidth = 140, count = 3)
        Spacer(Modifier.size(24.dp))
        ShimmerBox(Modifier.width(170.dp).height(22.dp))
        Spacer(Modifier.size(10.dp))
        repeat(6) { TrackRowSkeleton() }
    }
}
