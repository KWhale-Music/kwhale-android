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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.dueattendant149.kwhale.KwhaleApp
import org.dueattendant149.kwhale.data.ContentCache
import org.dueattendant149.kwhale.data.HomeSnapshot
import org.dueattendant149.kwhale.data.toUi
import org.dueattendant149.kwhale.design.KwhalePlural
import org.dueattendant149.kwhale.design.KwhaleSpacing
import org.dueattendant149.kwhale.design.LocalMiniPlayerBottomPadding
import org.dueattendant149.kwhale.network.model.Banner
import org.dueattendant149.kwhale.network.model.HomeSection
import org.dueattendant149.kwhale.network.model.Mix
import org.dueattendant149.kwhale.network.model.Track as ApiTrack
import org.dueattendant149.kwhale.ui.components.BannerCard
import org.dueattendant149.kwhale.ui.components.CardCarouselSkeleton
import org.dueattendant149.kwhale.ui.components.ErrorState
import org.dueattendant149.kwhale.ui.components.LocalSnackbarHost
import org.dueattendant149.kwhale.ui.components.SectionHeader
import org.dueattendant149.kwhale.ui.components.ShimmerBox
import org.dueattendant149.kwhale.ui.components.TrackRow
import org.dueattendant149.kwhale.ui.components.TrackRowSkeleton
import org.dueattendant149.kwhale.ui.components.TrackTile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    app: KwhaleApp,
    onOpenSearch: () -> Unit,
    onSeeAll: (String) -> Unit = {},
    onTrackMenu: (ApiTrack) -> Unit,
    onOpenProfile: () -> Unit = {},
    onOpenCollection: (String, String) -> Unit = { _, _ -> },
    onDownloads: () -> Unit = {},
) {
    val cached = remember { app.cache.read(ContentCache.HOME, HomeSnapshot.serializer()) }
    var home by remember { mutableStateOf(cached?.home) }
    var mixes by remember { mutableStateOf(cached?.mixes.orEmpty()) }
    var loading by remember { mutableStateOf(cached == null) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var prompt by remember { mutableStateOf("") }
    var promptLoading by remember { mutableStateOf(false) }
    var promptError by remember { mutableStateOf<String?>(null) }
    var loadingMixId by remember { mutableStateOf<String?>(null) }

    val likedOverrides by app.reactions.likedOverrides.collectAsState()
    val playerState by app.player.state.collectAsState()
    val currentRemoteId = playerState.current?.remoteId
    val snackbar = LocalSnackbarHost.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(reload) {
        if (reload == 0 && home != null && app.cache.isFresh(ContentCache.HOME, 30_000)) {
            loading = false
            refreshing = false
            return@LaunchedEffect
        }
        loading = home == null
        error = null
        runCatching {
            coroutineScope {
                val homeDeferred = async { app.api.home() }
                val mixesDeferred = async {
                    runCatching { app.api.mixes(kind = "personal", limit = 20).items }
                        .getOrDefault(emptyList())
                }
                val nextHome = homeDeferred.await()
                val nextMixes = mixesDeferred.await()
                home = nextHome
                mixes = nextMixes
                app.cache.write(
                    ContentCache.HOME,
                    HomeSnapshot.serializer(),
                    HomeSnapshot(nextHome, nextMixes),
                )
            }
        }.onFailure { err ->
            val msg = err.message ?: "Ошибка сети"
            error = msg
            if (home != null) {
                snackbar?.showSnackbar("Не удалось обновить: $msg")
            }
        }
        loading = false
        refreshing = false
    }

    if (loading && home == null) {
        HomeSkeleton()
        return
    }
    if (home == null) {
        ErrorState(
            message = error ?: "Ошибка сети",
            onRetry = { reload++ },
            secondaryAction = "Скачанные треки",
            onSecondaryAction = onDownloads,
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    val sections = home?.sections.orEmpty()

    val heroMixes = mixes.filter { it.kind == "personal" || it.kind == "discovery" }
    val moodMixes = mixes.filter { it.kind == "mood" }
    val moreMixes = mixes.filter { it.kind == "genre" || it.kind == "artist" }

    val seenMixImpressions = remember { mutableSetOf<String>() }
    LaunchedEffect(mixes) {
        mixes.forEachIndexed { idx, mix ->
            if (seenMixImpressions.add(mix.id)) {
                app.events.impression(
                    track = null,
                    surface = "home",
                    mixId = mix.id,
                    position = idx,
                )
            }
        }
    }

    fun playMix(mix: Mix) {
        if (loadingMixId != null) return
        loadingMixId = mix.id
        app.events.mixClick(mix.id)
        scope.launch {
            runCatching { app.api.mixTracks(mix.id, limit = 50) }
                .onSuccess { page ->
                    val tracks = page.items.map { it.track.toUi(app.api) }
                    if (tracks.isNotEmpty()) {
                        app.player.playQueue(tracks, 0, surface = "mix", mixId = mix.id)
                    } else {
                        snackbar?.showSnackbar("В миксе пока нет доступных треков")
                    }
                }
                .onFailure {
                    snackbar?.showSnackbar("Не удалось загрузить микс")
                }
            loadingMixId = null
        }
    }

    fun handleBanner(banner: Banner) {
        val action = banner.action?.trim().orEmpty()
        when {
            action.startsWith("album:") -> onOpenCollection("album", action.removePrefix("album:"))
            action.startsWith("playlist:") -> onOpenCollection("playlist", action.removePrefix("playlist:"))
            action.startsWith("artist:") -> onOpenCollection("artist", action.removePrefix("artist:"))
            action.startsWith("mix:") -> {
                val mixId = action.removePrefix("mix:")
                mixes.firstOrNull { it.id == mixId }?.let(::playMix)
                    ?: run {
                        app.events.mixClick(mixId)
                        scope.launch {
                            runCatching { app.api.mixTracks(mixId, limit = 50) }
                                .onSuccess { page ->
                                    val tracks = page.items.map { it.track.toUi(app.api) }
                                    if (tracks.isNotEmpty()) {
                                        app.player.playQueue(tracks, 0, surface = "mix", mixId = mixId)
                                    }
                                }
                        }
                    }
            }
            action == "search" -> onOpenSearch()
            action.isNotBlank() -> onOpenSearch()
        }
    }

    fun submitPrompt() {
        val text = prompt.trim()
        if (text.isEmpty() || promptLoading) return
        focusManager.clearFocus()
        promptLoading = true
        promptError = null
        scope.launch {
            runCatching {
                app.api.promptMix(
                    text,
                    30,
                    llmApiKey = app.settings.llmApiKey.ifBlank { null },
                    llmEndpoint = app.settings.llmEndpoint.ifBlank { null },
                    llmModel = app.settings.llmModel.ifBlank { null },
                )
            }
                .onSuccess { page ->
                    val tracks = page.items.map { it.track.toUi(app.api) }
                    if (tracks.isEmpty()) {
                        promptError = "Ничего не нашлось — попробуйте другой запрос"
                    } else {
                        app.player.playQueue(
                            tracks,
                            0,
                            surface = "mix",
                            mixId = "prompt:" + text.trim().take(64),
                        )
                    }
                }
                .onFailure { promptError = it.message ?: "Ошибка сети" }
            promptLoading = false
        }
    }

    val miniPlayerBottom = LocalMiniPlayerBottomPadding.current
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
                        .padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Rounded.Search, contentDescription = "Поиск")
                    }
                    IconButton(onClick = onOpenProfile) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Person,
                                    contentDescription = "Профиль",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    text = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
                        in 5..11 -> "Доброе утро"
                        in 12..16 -> "Добрый день"
                        in 17..22 -> "Добрый вечер"
                        else -> "Доброй ночи"
                    },
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            // Kept expanded by user decision #2; polished visuals + Go keyboard action.
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Собери плейлист под настроение",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = prompt,
                            onValueChange = {
                                prompt = it
                                if (promptError != null) promptError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("спокойный джаз на вечер, без рэпа") },
                            trailingIcon = {
                                if (prompt.isNotEmpty() && !promptLoading) {
                                    IconButton(onClick = { prompt = ""; promptError = null }) {
                                        Icon(Icons.Rounded.Clear, contentDescription = "Очистить")
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = { submitPrompt() }),
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
                            val isLoading = loadingMixId == mix.id
                            Card(
                                modifier = Modifier
                                    .width(260.dp)
                                    .height(140.dp),
                                onClick = { playMix(mix) },
                                enabled = loadingMixId == null,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                                shape = RoundedCornerShape(20.dp),
                            ) {
                                Box(Modifier.fillMaxSize().padding(16.dp)) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = mix.title,
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                        Text(
                                            text = mix.subtitle ?: KwhalePlural.tracks(mix.trackCount),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        )
                                    }
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }
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
                            val isLoading = loadingMixId == mix.id
                            Card(
                                modifier = Modifier.width(150.dp),
                                onClick = { playMix(mix) },
                                enabled = loadingMixId == null,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                ),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = mix.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                        if (isLoading) {
                                            Spacer(Modifier.width(6.dp))
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp,
                                            )
                                        }
                                    }
                                    Text(
                                        text = KwhalePlural.tracks(mix.trackCount),
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
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(moreMixes, key = { it.id }) { mix ->
                            FilterChip(
                                selected = loadingMixId == mix.id,
                                enabled = loadingMixId == null,
                                onClick = { playMix(mix) },
                                label = {
                                    Text(
                                        text = mix.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                            )
                        }
                    }
                }
            }

            // Library-owned / duplicated blocks are not rendered on Home:
            // collections and liked tracks live in Library; personal/discovery
            // shelves repeat the mix cards above.
            val homeDuplicates = setOf(
                "playlists",
                "albums",
                "liked",
                "for_you",
                "discovery",
                "moods",
            )
            sections.filterNot { it.id in homeDuplicates }.forEach { section ->
                when (section.id) {
                    "recent" -> {
                        item { SectionHeader("Недавно играли", action = "Все", onAction = { onSeeAll("recent") }) }
                        item {
                            TrackCarousel(
                                section = section,
                                app = app,
                                currentRemoteId = currentRemoteId,
                                onPlay = { tracks, index ->
                                    app.player.playQueue(tracks.map { it.toUi(app.api) }, index, surface = "home")
                                },
                                onMenu = onTrackMenu,
                            )
                        }
                    }
                    else -> {
                        when (section.kind) {
                            "banner" -> {
                                item {
                                    section.items.firstOrNull()?.banner?.let { banner ->
                                        BannerCard(
                                            banner = banner,
                                            api = app.api,
                                            onOpen = { handleBanner(banner) },
                                        )
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }
                            }
                            "shelf" -> {
                                item {
                                    SectionHeader(section.title ?: "Треки")
                                    TrackCarousel(
                                        section = section,
                                        app = app,
                                        currentRemoteId = currentRemoteId,
                                        onPlay = { tracks, index ->
                                            app.player.playQueue(tracks.map { it.toUi(app.api) }, index, surface = "home")
                                        },
                                        onMenu = onTrackMenu,
                                    )
                                }
                            }
                            "list" -> {
                                item { SectionHeader(section.title ?: "Треки") }
                                TrackItems(
                                    section = section,
                                    app = app,
                                    likedOverrides = likedOverrides,
                                    currentRemoteId = currentRemoteId,
                                    onPlay = { tracks, index ->
                                        app.player.playQueue(tracks.map { it.toUi(app.api) }, index, surface = "home")
                                    },
                                    onToggleLike = { track ->
                                        app.reactions.toggleLike(track, app.api, "home") { msg ->
                                            scope.launch { snackbar?.showSnackbar(msg) }
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
    }
}

@Composable
private fun TrackCarousel(
    section: HomeSection,
    app: KwhaleApp,
    currentRemoteId: String?,
    onPlay: (List<ApiTrack>, Int) -> Unit,
    onMenu: (ApiTrack) -> Unit,
) {
    val tracks = section.items.mapNotNull { it.track }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
            TrackTile(
                track = track,
                api = app.api,
                isCurrent = track.id == currentRemoteId,
                onClick = { onPlay(tracks, index) },
                onMenu = { onMenu(track) },
            )
        }
    }
}

private fun LazyListScope.TrackItems(
    section: HomeSection,
    app: KwhaleApp,
    likedOverrides: Map<String, Boolean>,
    currentRemoteId: String?,
    onPlay: (List<ApiTrack>, Int) -> Unit,
    onToggleLike: (ApiTrack) -> Unit,
    onMenu: (ApiTrack) -> Unit,
) {
    val tracks = section.items.mapNotNull { it.track }
    // lazily emitted into the parent LazyColumn (a nested Column rendered the whole
    // list eagerly, which is O(N) for large libraries)
    itemsIndexed(tracks, key = { index, track -> "${track.id}-$index" }) { index, track ->
        TrackRow(
            track = track,
            api = app.api,
            liked = app.reactions.isLiked(track.id, track.isLiked, likedOverrides),
            isCurrent = track.id == currentRemoteId,
            onClick = { onPlay(tracks, index) },
            onToggleLike = { onToggleLike(track) },
            onOverflow = { onMenu(track) },
            modifier = Modifier.animateItem(),
        )
    }
}

/**
 * Load skeleton matching the real layout (header, greeting card, two carousels).
 *
 * A centred spinner threw away the whole screen on every cold start; this keeps
 * the shape so content settles in place instead of jumping.
 */
@Composable
private fun HomeSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = KwhaleSpacing.gutter, vertical = KwhaleSpacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShimmerBox(Modifier.width(150.dp).height(30.dp), RoundedCornerShape(8.dp))
            Spacer(Modifier.weight(1f))
            ShimmerBox(Modifier.size(40.dp), CircleShape)
        }
        Spacer(Modifier.height(KwhaleSpacing.lg))
        ShimmerBox(Modifier.width(220.dp).height(26.dp), RoundedCornerShape(8.dp))
        Spacer(Modifier.height(KwhaleSpacing.lg))
        ShimmerBox(Modifier.fillMaxWidth().height(150.dp), RoundedCornerShape(20.dp))
        Spacer(Modifier.height(KwhaleSpacing.xxl))
        ShimmerBox(Modifier.width(180.dp).height(22.dp), RoundedCornerShape(8.dp))
        Spacer(Modifier.height(KwhaleSpacing.sm))
        CardCarouselSkeleton(cardWidth = 260, count = 2)
        Spacer(Modifier.height(KwhaleSpacing.xl))
        ShimmerBox(Modifier.width(150.dp).height(22.dp), RoundedCornerShape(8.dp))
        Spacer(Modifier.height(KwhaleSpacing.sm))
        TrackRowSkeleton()
        TrackRowSkeleton()
        TrackRowSkeleton()
    }
}
