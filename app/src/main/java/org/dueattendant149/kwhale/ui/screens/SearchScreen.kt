package org.dueattendant149.kwhale.ui.screens

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dueattendant149.kwhale.KwhaleApp
import org.dueattendant149.kwhale.data.ContentCache
import org.dueattendant149.kwhale.data.SearchBootstrapSnapshot
import org.dueattendant149.kwhale.data.toUi
import org.dueattendant149.kwhale.design.KwhalePlural
import org.dueattendant149.kwhale.design.LocalMiniPlayerBottomPadding
import org.dueattendant149.kwhale.network.model.SearchResponse
import org.dueattendant149.kwhale.network.model.SearchTop
import org.dueattendant149.kwhale.network.model.Suggestion
import org.dueattendant149.kwhale.network.model.Track as ApiTrack
import org.dueattendant149.kwhale.ui.components.AlbumCard
import org.dueattendant149.kwhale.ui.components.ArtistCard
import org.dueattendant149.kwhale.ui.components.CoverArt
import org.dueattendant149.kwhale.ui.components.EmptyState
import org.dueattendant149.kwhale.ui.components.ErrorState
import org.dueattendant149.kwhale.ui.components.LocalSnackbarHost
import org.dueattendant149.kwhale.ui.components.PlaylistCard
import org.dueattendant149.kwhale.ui.components.SectionHeader
import org.dueattendant149.kwhale.ui.components.TrackRow
import org.dueattendant149.kwhale.ui.components.TrackRowSkeleton
import org.dueattendant149.kwhale.ui.components.artistLine
import org.dueattendant149.kwhale.ui.components.imageUrl
import org.dueattendant149.kwhale.ui.components.isSingleLike
import org.dueattendant149.kwhale.ui.components.primaryArtistId

private val searchTypes = listOf(
    "all" to "Всё",
    "tracks" to "Треки",
    "albums" to "Альбомы",
    "artists" to "Артисты",
    "playlists" to "Плейлисты",
)

@Composable
fun SearchScreen(
    app: KwhaleApp,
    onOpenCollection: (String, String) -> Unit,
    onTrackMenu: (ApiTrack) -> Unit,
) {
    val cached = remember { app.cache.read(ContentCache.SEARCH_BOOTSTRAP, SearchBootstrapSnapshot.serializer()) }
    // Query/results live in the app-scoped session so returning from an artist
    // page restores the previous search instead of an empty screen.
    val session = app.searchSession
    var query by remember { session.query }
    var selectedType by remember { session.selectedType }
    var results by remember { session.results }
    var albumFilter by remember { mutableStateOf("all") }
    var suggestions by remember { mutableStateOf<List<Suggestion>>(emptyList()) }
    var history by remember { mutableStateOf(cached?.history.orEmpty()) }
    var genres by remember { mutableStateOf(cached?.genres.orEmpty()) }
    var topArtists by remember { mutableStateOf(cached?.artists.orEmpty()) }
    var loading by remember { mutableStateOf(false) }
    var refiningSlowResults by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchTick by remember { mutableIntStateOf(0) }

    val likedOverrides by app.reactions.likedOverrides.collectAsState()
    val playerState by app.player.state.collectAsState()
    val currentRemoteId = playerState.current?.remoteId
    val snackbar = LocalSnackbarHost.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val miniPlayerBottom = LocalMiniPlayerBottomPadding.current

    LaunchedEffect(Unit) {
        if (cached != null && app.cache.isFresh(ContentCache.SEARCH_BOOTSTRAP, 60_000)) return@LaunchedEffect
        val nextHistory = runCatching { app.api.searchHistory().items }.getOrDefault(emptyList())
        val nextGenres = runCatching { app.api.genres().items }.getOrDefault(emptyList())
        val nextArtists = runCatching { app.api.topArtists(limit = 12).items }.getOrDefault(emptyList())
        history = nextHistory
        genres = nextGenres
        topArtists = nextArtists
        app.cache.write(
            ContentCache.SEARCH_BOOTSTRAP,
            SearchBootstrapSnapshot.serializer(),
            SearchBootstrapSnapshot(nextHistory, nextGenres, nextArtists),
        )
    }

    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(180)
        suggestions = runCatching { app.api.suggestions(trimmed).items }
            .getOrDefault(emptyList())
            .filter { !it.text.equals(trimmed, ignoreCase = true) }
    }

    LaunchedEffect(query, selectedType, searchTick) {
        if (query.isBlank()) {
            results = null
            error = null
            loading = false
            refiningSlowResults = false
            return@LaunchedEffect
        }
        val trimmed = query.trim()
        // Back from an artist page: restored results already match this query
        // and category — keep them, don't blank the screen into skeletons.
        if (searchTick == 0 && results?.query == trimmed && session.resultsType.value == selectedType) {
            return@LaunchedEffect
        }
        delay(300)
        loading = true
        refiningSlowResults = false
        error = null
        try {
            // Two-wave search: show fast local hits right away, then wait for external sources.
            val quick = app.api.search(trimmed, type = selectedType, limit = 30, wait = false)
            results = quick
            session.resultsType.value = selectedType
            app.cache.invalidateSearchBootstrap()
            loading = false
            if (quick.partial) {
                refiningSlowResults = true
                try {
                    results = app.api.search(trimmed, type = selectedType, limit = 30, wait = true)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Keep quick partial results rather than throwing away what arrived.
                } finally {
                    refiningSlowResults = false
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            results = null
            refiningSlowResults = false
            error = "Не удалось выполнить поиск: ${e.message ?: "сеть"}"
            loading = false
        }
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
                    IconButton(onClick = { query = ""; suggestions = emptyList() }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Очистить")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        )

        if (query.isNotBlank()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                items(searchTypes, key = { it.first }) { (typeKey, label) ->
                    FilterChip(
                        selected = selectedType == typeKey,
                        onClick = { selectedType = typeKey },
                        label = { Text(label) },
                    )
                }
            }
            if (suggestions.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    items(suggestions.take(8), key = { "${it.type}-${it.text}" }) { suggestion ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                query = suggestion.text
                                focusManager.clearFocus()
                            },
                            label = { Text(suggestion.text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }
            }
            if (refiningSlowResults) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(2.dp),
                )
            }
        }

        if (query.isBlank()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp + miniPlayerBottom),
            ) {
                if (history.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Недавние запросы",
                            action = "Очистить",
                            onAction = {
                                scope.launch {
                                    runCatching { app.api.clearSearchHistory() }
                                        .onSuccess {
                                            history = emptyList()
                                            app.cache.invalidateSearchBootstrap()
                                        }
                                        .onFailure {
                                            snackbar?.showSnackbar("Не удалось очистить историю поиска")
                                        }
                                }
                            },
                        )
                    }
                    items(history.take(6)) { q ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    query = q
                                    focusManager.clearFocus()
                                }
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
                                    onClick = {
                                        query = genre.name
                                        focusManager.clearFocus()
                                    },
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
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    repeat(5) { TrackRowSkeleton() }
                }
            } else {
                val data = results
                val err = error
                val noHits = data == null || (data.tracks.isEmpty() && data.albums.isEmpty() && data.artists.isEmpty() && data.playlists.isEmpty())
                when {
                    err != null -> {
                        ErrorState(
                            message = err,
                            onRetry = { searchTick++ },
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                    }
                    noHits -> {
                        EmptyState(
                            title = "Ничего не найдено",
                            message = "Попробуйте изменить запрос или выбрать категорию «Всё»",
                            icon = Icons.Rounded.Search,
                            action = if (selectedType != "all") "Показать все категории" else null,
                            onAction = if (selectedType != "all") ({ selectedType = "all" }) else null,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp + miniPlayerBottom),
                        ) {
                            item { TopResult(data!!.top, app, onOpenCollection) }
                            val top = data!!.top
                            val tracks = data.tracks.filterNot {
                                top?.type == "track" && it.id == top.track?.id
                            }
                            val albums = data.albums.filterNot {
                                top?.type == "album" && it.id == top.album?.id
                            }
                            val artists = data.artists.filterNot {
                                top?.type == "artist" && it.id == top.artist?.id
                            }
                            val playlists = data.playlists.filterNot {
                                top?.type == "playlist" && it.id == top.playlist?.id
                            }
                            if (tracks.isNotEmpty()) {
                                item { SectionHeader("Треки") }
                                itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                                    TrackRow(
                                        track = track,
                                        api = app.api,
                                        liked = app.reactions.isLiked(track.id, track.isLiked, likedOverrides),
                                        isCurrent = track.id == currentRemoteId,
                                        onClick = {
                                            focusManager.clearFocus()
                                            app.player.playQueue(tracks.map { it.toUi(app.api) }, index)
                                        },
                                        onToggleLike = {
                                            app.reactions.toggleLike(track, app.api, "search") { msg ->
                                                scope.launch { snackbar?.showSnackbar(msg) }
                                            }
                                        },
                                        onOverflow = { onTrackMenu(track) },
                                        onArtistClick = {
                                            track.primaryArtistId()?.let { onOpenCollection("artist", it) }
                                        },
                                    )
                                }
                            }
                            if (albums.isNotEmpty()) {
                                item {
                                    SectionHeader("Альбомы")
                                }
                                item {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("all" to "Все", "album" to "Альбомы", "single" to "Синглы и EP").forEach { (key, label) ->
                                            FilterChip(
                                                selected = albumFilter == key,
                                                onClick = { albumFilter = key },
                                                label = { Text(label) },
                                            )
                                        }
                                    }
                                }
                                val filteredAlbums = when (albumFilter) {
                                    "album" -> albums.filterNot { isSingleLike(it) }
                                    "single" -> albums.filter { isSingleLike(it) }
                                    else -> albums
                                }
                                if (filteredAlbums.isEmpty()) {
                                    item {
                                        Text(
                                            text = "В этой категории ничего нет",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 8.dp),
                                        )
                                    }
                                } else {
                                    item {
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items(filteredAlbums) { album ->
                                                AlbumCard(
                                                    album = album,
                                                    api = app.api,
                                                    onClick = { onOpenCollection("album", album.id) },
                                                    onArtistClick = {
                                                        album.primaryArtistId()?.let { onOpenCollection("artist", it) }
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (playlists.isNotEmpty()) {
                                item { SectionHeader("Плейлисты") }
                                item {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(playlists) { playlist ->
                                            PlaylistCard(
                                                playlist = playlist,
                                                api = app.api,
                                                onClick = { onOpenCollection("playlist", playlist.id) },
                                            )
                                        }
                                    }
                                }
                            }
                            if (artists.isNotEmpty()) {
                                item { SectionHeader("Артисты") }
                                item {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(artists) { artist ->
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
}

@Composable
private fun TopResult(
    top: SearchTop?,
    app: KwhaleApp,
    onOpenCollection: (String, String) -> Unit,
) {
    val result = when (top?.type) {
        "artist" -> top.artist?.let {
            Quad(
                it.name,
                "Артист",
                it.imageUrl(app.api),
                { onOpenCollection("artist", it.id) },
                null,
            )
        }
        "album" -> top.album?.let { album ->
            Quad(
                album.title,
                album.artists.joinToString(", ") { artist -> artist.name },
                album.imageUrl(app.api),
                { onOpenCollection("album", album.id) },
                { album.primaryArtistId()?.let { id -> onOpenCollection("artist", id) } },
            )
        }
        "track" -> top.track?.let { track ->
            Quad(
                track.title,
                track.artistLine(),
                track.imageUrl(app.api),
                { app.player.playQueue(listOf(track.toUi(app.api)), 0) },
                { track.primaryArtistId()?.let { id -> onOpenCollection("artist", id) } },
            )
        }
        "playlist" -> top.playlist?.let {
            Quad(
                it.title,
                KwhalePlural.tracks(it.trackCount),
                it.imageUrl(app.api),
                { onOpenCollection("playlist", it.id) },
                null,
            )
        }
        else -> null
    } ?: return
    val (title, subtitle, cover, onClick, onArtistClick) = result

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
                    color = if (onArtistClick != null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (onArtistClick != null) {
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = LocalIndication.current,
                                onClickLabel = "Открыть исполнителя",
                                onClick = onArtistClick,
                            )
                    } else Modifier,
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
    val onArtistClick: (() -> Unit)? = null,
)
