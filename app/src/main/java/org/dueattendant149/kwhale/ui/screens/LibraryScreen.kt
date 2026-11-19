package org.dueattendant149.kwhale.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dueattendant149.kwhale.KwhaleApp
import org.dueattendant149.kwhale.data.ContentCache
import org.dueattendant149.kwhale.data.LibrarySnapshot
import org.dueattendant149.kwhale.data.toUi
import org.dueattendant149.kwhale.design.KwhalePlural
import org.dueattendant149.kwhale.design.LocalMiniPlayerBottomPadding
import org.dueattendant149.kwhale.network.model.PlaylistCreate
import org.dueattendant149.kwhale.network.model.Track as ApiTrack
import org.dueattendant149.kwhale.ui.components.AlbumCard
import org.dueattendant149.kwhale.ui.components.CardCarouselSkeleton
import org.dueattendant149.kwhale.ui.components.EmptyState
import org.dueattendant149.kwhale.ui.components.ErrorState
import org.dueattendant149.kwhale.ui.components.LocalSnackbarHost
import org.dueattendant149.kwhale.ui.components.SectionHeader
import org.dueattendant149.kwhale.ui.components.ShimmerBox
import org.dueattendant149.kwhale.ui.components.TrackRow
import org.dueattendant149.kwhale.ui.components.TrackRowSkeleton
import org.dueattendant149.kwhale.ui.components.imageUrl

private val trackSortModes = listOf(
    "default" to "По умолчанию",
    "added" to "Недавно добавленные",
    "title" to "По названию",
    "artist" to "По артисту",
)

@OptIn(ExperimentalMaterial3Api::class)
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
    var loadingTracks by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var selectedSort by remember { mutableStateOf("default") }
    var trackQuery by remember { mutableStateOf("") }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    var showCreatePlaylist by remember { mutableStateOf(false) }
    var newPlaylistTitle by remember { mutableStateOf("") }
    var creatingPlaylist by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }

    val likedOverrides by app.reactions.likedOverrides.collectAsState()
    val playerState by app.player.state.collectAsState()
    val currentRemoteId = playerState.current?.remoteId
    val snackbar = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val miniPlayerBottom = LocalMiniPlayerBottomPadding.current

    LaunchedEffect(reload) {
        if (reload == 0 && cached != null && app.cache.isFresh(ContentCache.LIBRARY, 30_000)) {
            loading = false
            refreshing = false
            return@LaunchedEffect
        }
        loading = cached == null && tracks.isEmpty() && playlists.isEmpty() && albums.isEmpty()
        error = null
        runCatching {
            val playlistsPage = app.api.libraryPlaylists(limit = 100)
            val albumsPage = app.api.libraryAlbums(limit = 100)
            playlists = playlistsPage.items
            albums = albumsPage.items
            app.cache.write(
                ContentCache.LIBRARY,
                LibrarySnapshot.serializer(),
                LibrarySnapshot(tracks, playlistsPage.items, albumsPage.items),
            )
        }.onFailure { err ->
            val msg = err.message ?: "Ошибка сети"
            error = msg
            if (tracks.isNotEmpty() || playlists.isNotEmpty() || albums.isNotEmpty()) {
                snackbar?.showSnackbar("Не удалось обновить: $msg")
            }
        }
        loading = false
        refreshing = false
    }

    // Liked tracks: the library shows the COMPLETE list inline (no separate
    // «Все треки» screen) — first page here, cursor pages on scroll.
    LaunchedEffect(reload, selectedSort, trackQuery) {
        if (trackQuery.isNotBlank()) delay(250) // debounce typing
        loadingTracks = true
        runCatching {
            val page = app.api.libraryTracks(
                limit = 100,
                sort = selectedSort,
                query = trackQuery.trim().ifBlank { null },
            )
            tracks = page.items
            nextCursor = page.nextCursor
            app.cache.write(
                ContentCache.LIBRARY,
                LibrarySnapshot.serializer(),
                LibrarySnapshot(page.items, playlists, albums),
            )
        }.onFailure { err ->
            if (tracks.isEmpty()) error = err.message ?: "Ошибка сети"
            else snackbar?.showSnackbar("Не удалось обновить список")
        }
        loadingTracks = false
        refreshing = false
    }

    fun loadMoreTracks() {
        val cursor = nextCursor ?: return
        if (loadingMore) return
        loadingMore = true
        scope.launch {
            runCatching {
                app.api.libraryTracks(
                    cursor = cursor,
                    limit = 100,
                    sort = selectedSort,
                    query = trackQuery.trim().ifBlank { null },
                )
            }.onSuccess { page ->
                tracks = (tracks + page.items).distinctBy { it.id }
                nextCursor = page.nextCursor
            }.onFailure {
                snackbar?.showSnackbar("Не удалось догрузить треки")
            }
            loadingMore = false
        }
    }

    if (loading && tracks.isEmpty() && playlists.isEmpty() && albums.isEmpty()) {
        LibrarySkeleton()
        return
    }
    if (error != null && tracks.isEmpty() && playlists.isEmpty() && albums.isEmpty()) {
        ErrorState(
            message = error ?: "Ошибка сети",
            onRetry = { reload++ },
            secondaryAction = "Скачанные треки",
            onSecondaryAction = onDownloads,
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

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            if (!refreshing) {
                refreshing = true
                reload++
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
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
                    IconButton(onClick = {
                        createError = null
                        newPlaylistTitle = ""
                        showCreatePlaylist = true
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Создать плейлист")
                    }
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
                                coverTitle = if (collection.kind == "playlist") collection.title else null,
                                onClick = { onOpenCollection(collection.kind, collection.id) },
                            )
                        }
                    }
                }
            }

            if (tracks.isNotEmpty() || trackQuery.isNotBlank() || selectedSort != "default") {
                item {
                    Spacer(Modifier.size(16.dp))
                    Button(
                        onClick = {
                            app.player.playQueue(
                                tracks.shuffled().map { it.toUi(app.api) },
                                0,
                                surface = "library",
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        enabled = tracks.isNotEmpty(),
                    ) {
                        Icon(Icons.Rounded.Shuffle, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Перемешать всё")
                    }
                    SectionHeader(title = "Любимые треки")
                    OutlinedTextField(
                        value = trackQuery,
                        onValueChange = { trackQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        placeholder = { Text("Поиск по любимым") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (trackQuery.isNotBlank()) {
                                IconButton(onClick = { trackQuery = "" }) {
                                    Icon(Icons.Rounded.Clear, contentDescription = "Очистить")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(trackSortModes, key = { it.first }) { (sortKey, label) ->
                            FilterChip(
                                selected = selectedSort == sortKey,
                                onClick = { selectedSort = sortKey },
                                label = { Text(label) },
                            )
                        }
                    }
                    Spacer(Modifier.size(4.dp))
                }
                if (loadingTracks) {
                    item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                }
                itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                    TrackRow(
                        track = track,
                        api = app.api,
                        liked = app.reactions.isLiked(track.id, track.isLiked, likedOverrides),
                        isCurrent = track.id == currentRemoteId,
                        onClick = {
                            app.player.playQueue(tracks.map { it.toUi(app.api) }, index, surface = "library")
                        },
                        onToggleLike = {
                            app.cache.invalidateLibrary()
                            app.reactions.toggleLike(track, app.api, "library") { msg ->
                                scope.launch { snackbar?.showSnackbar(msg) }
                            }
                        },
                        onOverflow = { onTrackMenu(track) },
                        modifier = Modifier.animateItem(),
                    )
                }
                if (nextCursor != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            OutlinedButton(
                                onClick = { loadMoreTracks() },
                                enabled = !loadingMore,
                            ) {
                                if (loadingMore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Text("Загрузить ещё")
                                }
                            }
                        }
                    }
                }
                if (tracks.isEmpty() && !loadingTracks) {
                    item {
                        EmptyState(
                            title = "Ничего не найдено",
                            message = "Попробуйте изменить запрос или сортировку",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else if (loadingTracks) {
                items(5) { TrackRowSkeleton() }
            } else {
                item {
                    EmptyState(
                        title = "Пока пусто",
                        message = "Лайкните трек, чтобы он появился здесь",
                        action = "Найти музыку",
                        onAction = onSearch,
                        secondaryAction = "Скачано",
                        onSecondaryAction = onDownloads,
                    )
                }
            }
        }
    }

    if (showCreatePlaylist) {
        AlertDialog(
            onDismissRequest = { if (!creatingPlaylist) showCreatePlaylist = false },
            title = { Text("Новый плейлист") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPlaylistTitle,
                        onValueChange = { newPlaylistTitle = it },
                        label = { Text("Название") },
                        singleLine = true,
                        enabled = !creatingPlaylist,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    createError?.let { msg ->
                        Spacer(Modifier.height(8.dp))
                        Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newPlaylistTitle.trim().isNotEmpty() && !creatingPlaylist,
                    onClick = {
                        val name = newPlaylistTitle.trim()
                        if (name.isEmpty()) return@TextButton
                        creatingPlaylist = true
                        createError = null
                        scope.launch {
                            runCatching { app.api.createPlaylist(PlaylistCreate(title = name)) }
                                .onSuccess { created ->
                                    app.cache.invalidateLibrary()
                                    creatingPlaylist = false
                                    showCreatePlaylist = false
                                    onOpenCollection("playlist", created.id)
                                }
                                .onFailure { err ->
                                    creatingPlaylist = false
                                    createError = err.message ?: "Не удалось создать плейлист"
                                }
                        }
                    },
                ) {
                    if (creatingPlaylist) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Создать")
                    }
                }
            },
            dismissButton = {
                TextButton(enabled = !creatingPlaylist, onClick = { showCreatePlaylist = false }) {
                    Text("Отмена")
                }
            },
        )
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
