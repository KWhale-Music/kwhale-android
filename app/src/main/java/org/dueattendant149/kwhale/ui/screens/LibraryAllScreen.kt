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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import org.dueattendant149.kwhale.data.toUi
import org.dueattendant149.kwhale.design.LocalMiniPlayerBottomPadding
import org.dueattendant149.kwhale.network.model.Album
import org.dueattendant149.kwhale.network.model.Playlist
import org.dueattendant149.kwhale.network.model.PlaylistCreate
import org.dueattendant149.kwhale.network.model.Track as ApiTrack
import org.dueattendant149.kwhale.ui.components.AlbumCard
import org.dueattendant149.kwhale.ui.components.EmptyState
import org.dueattendant149.kwhale.ui.components.ErrorState
import org.dueattendant149.kwhale.ui.components.LocalSnackbarHost
import org.dueattendant149.kwhale.ui.components.PlaylistCard
import org.dueattendant149.kwhale.ui.components.ShimmerBox
import org.dueattendant149.kwhale.ui.components.TrackRow

private val trackSortModes = listOf(
    "default" to "По умолчанию",
    "added" to "Недавно добавленные",
    "title" to "По названию",
    "artist" to "По артисту",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryAllScreen(
    app: KwhaleApp,
    kind: String,
    onBack: () -> Unit,
    onOpenCollection: (String, String) -> Unit,
    onTrackMenu: (ApiTrack) -> Unit = {},
) {
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var tracks by remember { mutableStateOf<List<ApiTrack>>(emptyList()) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var loadingMore by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = albums, 1 = playlists
    var selectedSort by remember { mutableStateOf("default") }
    var trackQuery by remember { mutableStateOf("") }

    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newDescription by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }

    val likedOverrides by app.reactions.likedOverrides.collectAsState()
    val playerState by app.player.state.collectAsState()
    val currentRemoteId = playerState.current?.remoteId
    val snackbar = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val miniPlayerBottom = LocalMiniPlayerBottomPadding.current

    val title = when (kind) {
        "collections" -> "Коллекции"
        "playlists" -> "Плейлисты"
        "tracks", "liked" -> "Любимые треки"
        else -> "Альбомы"
    }
    val canCreatePlaylist = kind == "albums" || kind == "collections" || kind == "playlists"
    val isTracksMode = kind == "tracks" || kind == "liked"

    LaunchedEffect(kind, reload, selectedSort, trackQuery) {
        if (isTracksMode && trackQuery.isNotBlank()) {
            delay(250)
        }
        loading = true
        error = null
        nextCursor = null
        runCatching {
            when (kind) {
                "playlists" -> playlists = app.api.libraryPlaylists(limit = 100).items
                "albums" -> albums = app.api.libraryAlbums(limit = 100).items
                "collections" -> {
                    playlists = app.api.libraryPlaylists(limit = 100).items
                    albums = app.api.libraryAlbums(limit = 100).items
                }
                else -> {
                    val page = app.api.libraryTracks(
                        limit = 100,
                        sort = selectedSort,
                        query = trackQuery.trim().ifBlank { null },
                    )
                    tracks = page.items
                    nextCursor = page.nextCursor
                }
            }
        }.onFailure { err ->
            error = err.message ?: "Ошибка загрузки"
        }
        loading = false
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

    fun createPlaylist() {
        val name = newName.trim()
        if (name.isEmpty() || creating) return
        creating = true
        createError = null
        scope.launch {
            runCatching {
                app.api.createPlaylist(
                    PlaylistCreate(
                        title = name,
                        description = newDescription.trim().ifBlank { null },
                    ),
                )
            }.onSuccess { playlist ->
                app.cache.invalidateLibrary()
                showCreate = false
                newName = ""
                newDescription = ""
                onOpenCollection("playlist", playlist.id)
            }.onFailure {
                createError = it.message ?: "Не удалось создать плейлист"
            }
            creating = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                }
            },
            actions = {
                if (canCreatePlaylist) {
                    IconButton(onClick = { showCreate = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Создать плейлист")
                    }
                }
            },
        )

        if (kind == "collections") {
            SecondaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Альбомы") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Плейлисты") },
                )
            }
        }

        if (isTracksMode) {
            OutlinedTextField(
                value = trackQuery,
                onValueChange = { trackQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                placeholder = { Text("Поиск по библиотеке") },
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
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 6.dp),
            ) {
                items(trackSortModes, key = { it.first }) { (sortKey, label) ->
                    FilterChip(
                        selected = selectedSort == sortKey,
                        onClick = { selectedSort = sortKey },
                        label = { Text(label) },
                    )
                }
            }
        }

        if (loading) {
            LibraryAllSkeleton()
            return@Column
        }

        if (error != null) {
            ErrorState(
                message = error ?: "Ошибка загрузки",
                onRetry = { reload++ },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            return@Column
        }

        when (kind) {
            "collections" -> {
                if (selectedTab == 0) {
                    AlbumGrid(
                        albums = albums,
                        app = app,
                        miniPlayerBottom = miniPlayerBottom,
                        onOpenCollection = onOpenCollection,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    PlaylistGrid(
                        playlists = playlists,
                        app = app,
                        miniPlayerBottom = miniPlayerBottom,
                        onOpenCollection = onOpenCollection,
                        onCreate = { showCreate = true },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            "playlists" -> PlaylistGrid(
                playlists = playlists,
                app = app,
                miniPlayerBottom = miniPlayerBottom,
                onOpenCollection = onOpenCollection,
                onCreate = { showCreate = true },
                modifier = Modifier.weight(1f),
            )
            "albums" -> AlbumGrid(
                albums = albums,
                app = app,
                miniPlayerBottom = miniPlayerBottom,
                onOpenCollection = onOpenCollection,
                modifier = Modifier.weight(1f),
            )
            else -> {
                if (tracks.isEmpty()) {
                    EmptyState(
                        title = if (trackQuery.isNotBlank()) "Ничего не найдено" else "Пока пусто",
                        message = if (trackQuery.isNotBlank()) {
                            "Попробуйте изменить фильтр"
                        } else {
                            "Лайкните трек, чтобы он появился здесь"
                        },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            bottom = 24.dp + miniPlayerBottom,
                        ),
                    ) {
                        itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                            TrackRow(
                                track = track,
                                api = app.api,
                                liked = app.reactions.isLiked(track.id, track.isLiked, likedOverrides),
                                isCurrent = track.id == currentRemoteId,
                                onClick = {
                                    app.player.playQueue(tracks.map { it.toUi(app.api) }, index)
                                },
                                onToggleLike = {
                                    app.cache.invalidateLibrary()
                                    app.reactions.toggleLike(track, app.api, "library") { msg ->
                                        scope.launch { snackbar?.showSnackbar(msg) }
                                    }
                                },
                                onOverflow = { onTrackMenu(track) },
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
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        } else {
                                            Text("Загрузить ещё")
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

    if (showCreate) {
        ModalBottomSheet(
            onDismissRequest = { if (!creating) showCreate = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Новый плейлист", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Название") },
                )
                OutlinedTextField(
                    value = newDescription,
                    onValueChange = { newDescription = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    label = { Text("Описание (необязательно)") },
                )
                createError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { createPlaylist() },
                        enabled = newName.isNotBlank() && !creating,
                    ) {
                        if (creating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Создать")
                        }
                    }
                    TextButton(onClick = { if (!creating) showCreate = false }) {
                        Text("Отмена")
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumGrid(
    albums: List<Album>,
    app: KwhaleApp,
    miniPlayerBottom: androidx.compose.ui.unit.Dp,
    onOpenCollection: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (albums.isEmpty()) {
        EmptyState(
            "Пока нет альбомов",
            "Сохраните альбом кнопкой «Добавить» на его экране",
            modifier = modifier.fillMaxWidth(),
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 156.dp),
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 12.dp,
            bottom = 24.dp + miniPlayerBottom,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(albums, key = { it.id }) { album ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                AlbumCard(
                    album = album,
                    api = app.api,
                    onClick = { onOpenCollection("album", album.id) },
                    width = 156,
                )
            }
        }
    }
}

@Composable
private fun PlaylistGrid(
    playlists: List<Playlist>,
    app: KwhaleApp,
    miniPlayerBottom: androidx.compose.ui.unit.Dp,
    onOpenCollection: (String, String) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (playlists.isEmpty()) {
        EmptyState(
            "Пока нет плейлистов",
            "Создайте первый плейлист и добавьте в него треки",
            action = "Создать плейлист",
            onAction = onCreate,
            modifier = modifier.fillMaxWidth(),
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 156.dp),
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 12.dp,
            bottom = 24.dp + miniPlayerBottom,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(playlists, key = { it.id }) { playlist ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                PlaylistCard(
                    playlist = playlist,
                    api = app.api,
                    onClick = { onOpenCollection("playlist", playlist.id) },
                    width = 156,
                )
            }
        }
    }
}

@Composable
private fun LibraryAllSkeleton() {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ShimmerBox(Modifier.width(100.dp).height(32.dp), RoundedCornerShape(8.dp))
            ShimmerBox(Modifier.width(110.dp).height(32.dp), RoundedCornerShape(8.dp))
        }
        Spacer(Modifier.height(16.dp))
        repeat(3) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                repeat(2) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        ShimmerBox(Modifier.fillMaxWidth().height(150.dp), RoundedCornerShape(10.dp))
                        Spacer(Modifier.height(8.dp))
                        ShimmerBox(Modifier.fillMaxWidth(0.7f).height(12.dp))
                    }
                }
            }
        }
    }
}
