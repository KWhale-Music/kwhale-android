package org.dueattendant149.kwhale.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import org.dueattendant149.kwhale.data.toUi
import org.dueattendant149.kwhale.design.LocalMiniPlayerBottomPadding
import org.dueattendant149.kwhale.network.model.Album
import org.dueattendant149.kwhale.network.model.Track as ApiTrack
import org.dueattendant149.kwhale.ui.components.AlbumCard
import org.dueattendant149.kwhale.ui.components.EmptyState
import org.dueattendant149.kwhale.ui.components.ErrorState
import org.dueattendant149.kwhale.ui.components.LocalSnackbarHost
import org.dueattendant149.kwhale.ui.components.TrackRow
import org.dueattendant149.kwhale.ui.components.TrackRowSkeleton
import org.dueattendant149.kwhale.ui.components.isSingleLike
import org.dueattendant149.kwhale.ui.components.primaryArtistId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistAllScreen(
    app: KwhaleApp,
    artistId: String,
    initialTab: Int,
    onBack: () -> Unit,
    onOpenCollection: (String, String) -> Unit,
    onTrackMenu: (ApiTrack) -> Unit,
) {
    var tab by remember { mutableStateOf(initialTab) }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (tab == 0) "Все треки" else "Все альбомы") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                }
            },
        )
        SecondaryTabRow(selectedTabIndex = tab) {
            Tab(
                selected = tab == 0,
                onClick = { tab = 0 },
                text = { Text("Треки") },
            )
            Tab(
                selected = tab == 1,
                onClick = { tab = 1 },
                text = { Text("Альбомы") },
            )
        }
        if (tab == 0) {
            ArtistTracks(app, artistId, onTrackMenu, onOpenCollection)
        } else {
            ArtistAlbums(app, artistId, onOpenCollection)
        }
    }
}

@Composable
private fun ArtistTracks(
    app: KwhaleApp,
    artistId: String,
    onTrackMenu: (ApiTrack) -> Unit,
    onOpenCollection: (String, String) -> Unit,
) {
    var items by remember { mutableStateOf<List<ApiTrack>>(emptyList()) }
    var next by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableStateOf(0) }
    val likedOverrides by app.reactions.likedOverrides.collectAsState()
    val playerState by app.player.state.collectAsState()
    val snackbarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val miniPlayerBottom = LocalMiniPlayerBottomPadding.current

    LaunchedEffect(artistId) {
        runCatching { app.api.syncArtist(artistId) }
        repeat(8) {
            delay(1500)
            val state = runCatching { app.api.artistSyncStatus(artistId) }.getOrNull()
                ?: return@LaunchedEffect
            if (state.status == "done") {
                refreshKey++
                return@LaunchedEffect
            }
            if (state.status == "failed" || state.status == "unavailable") return@LaunchedEffect
        }
    }

    LaunchedEffect(artistId, refreshKey) {
        loading = items.isEmpty()
        error = null
        runCatching { app.api.artistTracks(artistId, limit = 50) }
            .onSuccess {
                items = it.items
                next = it.nextCursor
                error = null
            }
            .onFailure {
                if (items.isEmpty()) {
                    error = it.message ?: "Ошибка сети"
                } else {
                    snackbarHost?.showSnackbar("Не удалось обновить список")
                }
            }
        loading = false
    }

    fun loadMore() {
        val cursor = next ?: return
        if (loadingMore) return
        loadingMore = true
        scope.launch {
            runCatching { app.api.artistTracks(artistId, cursor, 50) }
                .onSuccess {
                    items = items + it.items
                    next = it.nextCursor
                }
                .onFailure {
                    snackbarHost?.showSnackbar("Не удалось загрузить ещё")
                }
            loadingMore = false
        }
    }

    if (loading) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            repeat(8) { TrackRowSkeleton() }
        }
        return
    }
    if (error != null && items.isEmpty()) {
        ErrorState(
            message = error ?: "Ошибка сети",
            onRetry = {
                loading = true
                error = null
                refreshKey++
            },
            modifier = Modifier.fillMaxSize(),
        )
        return
    }
    if (items.isEmpty()) {
        EmptyState(
            "Пока нет треков",
            "У этого артиста ещё нет известных серверу треков",
            modifier = Modifier.fillMaxSize(),
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            bottom = 24.dp + miniPlayerBottom,
        ),
    ) {
        itemsIndexed(items, key = { index, track -> "${track.id}:$index" }) { index, track ->
            val isLiked = app.reactions.isLiked(track.id, track.isLiked, likedOverrides)
            TrackRow(
                track = track,
                api = app.api,
                liked = isLiked,
                isCurrent = playerState.current?.remoteId == track.id,
                onClick = { app.player.playQueue(items.map { it.toUi(app.api) }, index) },
                onToggleLike = {
                    app.reactions.toggleLike(
                        track = track,
                        api = app.api,
                        surface = "artist_all",
                        onError = { msg -> scope.launch { snackbarHost?.showSnackbar(msg) } },
                    )
                },
                onOverflow = { onTrackMenu(track) },
                onArtistClick = {
                    track.primaryArtistId()
                        ?.takeIf { it != artistId }
                        ?.let { id -> onOpenCollection("artist", id) }
                },
            )
        }
        if (next != null) {
            item {
                LaunchedEffect(next, items.size) { loadMore() }
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun ArtistAlbums(
    app: KwhaleApp,
    artistId: String,
    onOpenCollection: (String, String) -> Unit,
) {
    var items by remember { mutableStateOf<List<Album>>(emptyList()) }
    var next by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableStateOf(0) }
    var typeFilter by remember { mutableStateOf("all") }
    val snackbarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val miniPlayerBottom = LocalMiniPlayerBottomPadding.current

    LaunchedEffect(artistId) {
        runCatching { app.api.syncArtist(artistId) }
        repeat(8) {
            delay(1500)
            val state = runCatching { app.api.artistSyncStatus(artistId) }.getOrNull()
                ?: return@LaunchedEffect
            if (state.status == "done") {
                refreshKey++
                return@LaunchedEffect
            }
            if (state.status == "failed" || state.status == "unavailable") return@LaunchedEffect
        }
    }

    LaunchedEffect(artistId, refreshKey) {
        loading = items.isEmpty()
        error = null
        runCatching { app.api.artistAlbums(artistId, limit = 50) }
            .onSuccess {
                items = it.items
                next = it.nextCursor
                error = null
            }
            .onFailure {
                if (items.isEmpty()) {
                    error = it.message ?: "Ошибка сети"
                } else {
                    snackbarHost?.showSnackbar("Не удалось обновить список")
                }
            }
        loading = false
    }

    fun loadMore() {
        val cursor = next ?: return
        if (loadingMore) return
        loadingMore = true
        scope.launch {
            runCatching { app.api.artistAlbums(artistId, cursor, 50) }
                .onSuccess {
                    items = items + it.items
                    next = it.nextCursor
                }
                .onFailure {
                    snackbarHost?.showSnackbar("Не удалось загрузить ещё")
                }
            loadingMore = false
        }
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (error != null && items.isEmpty()) {
        ErrorState(
            message = error ?: "Ошибка сети",
            onRetry = {
                loading = true
                error = null
                refreshKey++
            },
            modifier = Modifier.fillMaxSize(),
        )
        return
    }
    if (items.isEmpty()) {
        EmptyState(
            "Пока нет альбомов",
            "Дискография ещё не загружена",
            modifier = Modifier.fillMaxSize(),
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 156.dp),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 12.dp,
            bottom = 24.dp + miniPlayerBottom,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all" to "Все", "album" to "Альбомы", "single" to "Синглы и EP").forEach { (key, label) ->
                    FilterChip(
                        selected = typeFilter == key,
                        onClick = { typeFilter = key },
                        label = { Text(label) },
                    )
                }
            }
        }
        items(
            items.filter { album ->
                when (typeFilter) {
                    "album" -> !isSingleLike(album)
                    "single" -> isSingleLike(album)
                    else -> true
                }
            },
            key = { it.id },
        ) { album ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                AlbumCard(
                    album = album,
                    api = app.api,
                    onClick = { onOpenCollection("album", album.id) },
                    width = 152,
                    onArtistClick = {
                        album.primaryArtistId()
                            ?.takeIf { it != artistId }
                            ?.let { id -> onOpenCollection("artist", id) }
                    },
                )
            }
        }
        if (next != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LaunchedEffect(next, items.size) { loadMore() }
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
