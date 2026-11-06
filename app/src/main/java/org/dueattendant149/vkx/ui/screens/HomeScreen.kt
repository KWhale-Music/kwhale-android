package org.dueattendant149.vkx.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.dueattendant149.vkx.VkxApp
import org.dueattendant149.vkx.data.toUi
import org.dueattendant149.vkx.network.model.HomeResponse
import org.dueattendant149.vkx.network.model.HomeSection
import org.dueattendant149.vkx.network.model.Mix
import org.dueattendant149.vkx.network.model.Track as ApiTrack
import org.dueattendant149.vkx.player.PlaybackState
import org.dueattendant149.vkx.ui.components.AlbumCard
import org.dueattendant149.vkx.ui.components.ArtistCard
import org.dueattendant149.vkx.ui.components.BannerCard
import org.dueattendant149.vkx.ui.components.ContinueCard
import org.dueattendant149.vkx.ui.components.ErrorState
import org.dueattendant149.vkx.ui.components.PlaylistCard
import org.dueattendant149.vkx.ui.components.SectionHeader
import org.dueattendant149.vkx.ui.components.TrackRow
import org.dueattendant149.vkx.ui.components.TrackTile
import org.dueattendant149.vkx.ui.components.imageUrl

@Composable
fun HomeScreen(
    app: VkxApp,
    playerState: PlaybackState,
    onOpenPlayer: () -> Unit,
    onOpenCollection: (String, String) -> Unit,
    onOpenSearch: () -> Unit,
    onSeeAll: (String) -> Unit = {},
    onTrackMenu: (ApiTrack) -> Unit,
) {
    var home by remember { mutableStateOf<HomeResponse?>(null) }
    var mixes by remember { mutableStateOf<List<Mix>>(emptyList()) }
    var artists by remember { mutableStateOf<List<org.dueattendant149.vkx.network.model.Artist>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    val liked = remember { mutableStateMapOf<String, Boolean>() }
    var prompt by remember { mutableStateOf("") }
    var promptLoading by remember { mutableStateOf(false) }
    var promptError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reload) {
        loading = true
        error = null
        runCatching {
            coroutineScope {
                val homeDeferred = async { app.api.home() }
                val mixesDeferred = async {
                    runCatching { app.api.mixes(kind = "personal", limit = 20).items }
                        .getOrDefault(emptyList())
                }
                val artistsDeferred = async {
                    runCatching { app.api.topArtists(limit = 10).items }
                        .getOrDefault(emptyList())
                }
                home = homeDeferred.await()
                mixes = mixesDeferred.await()
                artists = artistsDeferred.await()
            }
        }.onFailure { error = it.message ?: "Ошибка сети" }
        loading = false
    }

    if (loading && home == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (home == null) {
        ErrorState(
            message = error ?: "Ошибка сети",
            onRetry = { reload++ },
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    val sections = home?.sections.orEmpty()
    val libraryPlaylists = sections.firstOrNull { it.id == "playlists" }
        ?.items?.mapNotNull { it.playlist }.orEmpty()
    val libraryAlbums = sections.firstOrNull { it.id == "albums" }
        ?.items?.mapNotNull { it.album }.orEmpty()

    val heroMixes = mixes.filter { it.kind == "personal" || it.kind == "discovery" }
    val moodMixes = mixes.filter { it.kind == "mood" }
    val moreMixes = mixes.filter { it.kind == "genre" || it.kind == "artist" }

    fun playMix(mix: Mix) {
        app.events.mixClick(mix.id)
        scope.launch {
            runCatching { app.api.mixTracks(mix.id, limit = 50) }
                .onSuccess { page ->
                    val tracks = page.items.map { it.track.toUi(app.api) }
                    if (tracks.isNotEmpty()) app.player.playQueue(tracks, 0)
                }
        }
    }

    fun submitPrompt() {
        val text = prompt.trim()
        if (text.isEmpty() || promptLoading) return
        promptLoading = true
        promptError = null
        scope.launch {
            runCatching { app.api.promptMix(text, 30) }
                .onSuccess { page ->
                    val tracks = page.items.map { it.track.toUi(app.api) }
                    if (tracks.isEmpty()) {
                        promptError = "Ничего не нашлось"
                    } else {
                        app.player.playQueue(tracks, 0)
                    }
                }
                .onFailure { promptError = it.message ?: "Ошибка сети" }
            promptLoading = false
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
                    text = "Главная",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                androidx.compose.material3.TextButton(onClick = onOpenSearch) {
                    Text("Обзор")
                }
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Person,
                            contentDescription = "Профиль",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item {
            Text(
                text = "Добрый вечер",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Собери плейлист под настроение",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("спокойный джаз на вечер, без рэпа") },
                    )
                    promptError?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { submitPrompt() },
                        enabled = prompt.isNotBlank() && !promptLoading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (promptLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Собрать")
                        }
                    }
                }
            }
        }

        if (heroMixes.isNotEmpty()) {
            item { SectionHeader("Миксы для тебя") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(heroMixes, key = { it.id }) { mix ->
                        Card(
                            modifier = Modifier.width(260.dp).height(140.dp),
                            onClick = { playMix(mix) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                            shape = RoundedCornerShape(20.dp),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = mix.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = mix.subtitle ?: "${mix.trackCount} треков",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (moodMixes.isNotEmpty()) {
            item { SectionHeader("Настроение") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(moodMixes, key = { it.id }) { mix ->
                        Card(
                            modifier = Modifier.width(150.dp),
                            onClick = { playMix(mix) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(
                                    text = mix.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${mix.trackCount} треков",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (moreMixes.isNotEmpty()) {
            item { SectionHeader("Жанры и артисты") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(moreMixes, key = { it.id }) { mix ->
                        Card(
                            modifier = Modifier.width(170.dp),
                            onClick = { playMix(mix) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(
                                    text = mix.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = mix.subtitle ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        playerState.current?.let { current ->
            item {
                ContinueCard(
                    track = current,
                    progress = playerState.progress,
                    isPlaying = playerState.isPlaying,
                    onOpen = onOpenPlayer,
                    onToggle = app.player::toggle,
                )
                Spacer(Modifier.height(4.dp))
            }
        }

        sections.filterNot { it.id == "playlists" || it.id == "albums" }.forEach { section ->
            when (section.id) {
                "recent" -> {
                    item { SectionHeader("Недавно играли", action = "Все", onAction = { onSeeAll("tracks") }) }
                    item {
                        TrackCarousel(
                            section = section,
                            app = app,
                            onPlay = { tracks, index ->
                                app.player.playQueue(tracks.map { it.toUi(app.api) }, index)
                            },
                            onMenu = onTrackMenu,
                        )
                    }
                }
                "liked" -> {
                    item { SectionHeader("Любимое", action = "Все", onAction = { onSeeAll("liked") }) }
                    item {
                        TrackList(
                            section = section,
                            app = app,
                            liked = liked,
                            onPlay = { tracks, index ->
                                app.player.playQueue(tracks.map { it.toUi(app.api) }, index)
                            },
                            onToggleLike = { track ->
                                val next = liked[track.id] ?: !track.isLiked
                                liked[track.id] = next
                                scope.launch {
                                    runCatching {
                                        if (next) app.api.like(track.id) else app.api.unlike(track.id)
                                    }
                                }
                            },
                            onMenu = onTrackMenu,
                        )
                    }
                }
                "playlists" -> {
                    item { SectionHeader("Плейлисты", action = "Все", onAction = { onSeeAll("playlists") }) }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(section.items.mapNotNull { it.playlist }, key = { it.id }) { playlist ->
                                PlaylistCard(
                                    playlist = playlist,
                                    api = app.api,
                                    onClick = { onOpenCollection("playlist", playlist.id) },
                                )
                            }
                        }
                    }
                }
                "albums" -> {
                    item { SectionHeader("Альбомы", action = "Все", onAction = { onSeeAll("albums") }) }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(section.items.mapNotNull { it.album }, key = { it.id }) { album ->
                                AlbumCard(
                                    album = album,
                                    api = app.api,
                                    onClick = { onOpenCollection("album", album.id) },
                                )
                            }
                        }
                    }
                }
                else -> {
                    when (section.kind) {
                        "banner" -> {
                            item {
                                section.items.firstOrNull()?.banner?.let { banner ->
                                    BannerCard(banner = banner, api = app.api)
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                        "list" -> {
                            item {
                                SectionHeader(section.title ?: "Треки")
                                TrackList(
                                    section = section,
                                    app = app,
                                    liked = liked,
                                    onPlay = { tracks, index ->
                                        app.player.playQueue(tracks.map { it.toUi(app.api) }, index)
                                    },
                                    onToggleLike = { track ->
                                        val next = liked[track.id] ?: !track.isLiked
                                        liked[track.id] = next
                                        scope.launch {
                                            runCatching {
                                                if (next) app.api.like(track.id) else app.api.unlike(track.id)
                                            }
                                        }
                                    },
                                    onMenu = onTrackMenu,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (libraryPlaylists.isNotEmpty() || libraryAlbums.isNotEmpty()) {
            item { SectionHeader("Коллекции", action = "Все", onAction = { onSeeAll("collections") }) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(libraryPlaylists, key = { "p-${it.id}" }) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            api = app.api,
                            onClick = { onOpenCollection("playlist", playlist.id) },
                        )
                    }
                    items(libraryAlbums, key = { "a-${it.id}" }) { album ->
                        AlbumCard(
                            album = album,
                            api = app.api,
                            onClick = { onOpenCollection("album", album.id) },
                        )
                    }
                }
            }
        }

        if (artists.isNotEmpty()) {
            item { SectionHeader("Артисты") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(artists, key = { it.id }) { artist ->
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

@Composable
private fun TrackCarousel(
    section: HomeSection,
    app: VkxApp,
    onPlay: (List<ApiTrack>, Int) -> Unit,
    onMenu: (ApiTrack) -> Unit,
) {
    val tracks = section.items.mapNotNull { it.track }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
            TrackTile(
                track = track,
                api = app.api,
                onClick = { onPlay(tracks, index) },
            )
        }
    }
}

@Composable
private fun TrackList(
    section: HomeSection,
    app: VkxApp,
    liked: Map<String, Boolean>,
    onPlay: (List<ApiTrack>, Int) -> Unit,
    onToggleLike: (ApiTrack) -> Unit,
    onMenu: (ApiTrack) -> Unit,
) {
    val tracks = section.items.mapNotNull { it.track }
    Column {
        tracks.forEachIndexed { index, track ->
            TrackRow(
                track = track,
                api = app.api,
                liked = liked[track.id] ?: track.isLiked,
                onClick = { onPlay(tracks, index) },
                onToggleLike = { onToggleLike(track) },
                onOverflow = { onMenu(track) },
            )
        }
    }
}
