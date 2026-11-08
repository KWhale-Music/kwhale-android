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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.dueattendant149.kwhale.KwhaleApp
import org.dueattendant149.kwhale.data.toUi
import org.dueattendant149.kwhale.network.model.HomeResponse
import org.dueattendant149.kwhale.network.model.HomeSection
import org.dueattendant149.kwhale.network.model.Mix
import org.dueattendant149.kwhale.network.model.Track as ApiTrack
import org.dueattendant149.kwhale.ui.components.BannerCard
import org.dueattendant149.kwhale.ui.components.ErrorState
import org.dueattendant149.kwhale.ui.components.CardCarouselSkeleton
import org.dueattendant149.kwhale.ui.components.SectionHeader
import org.dueattendant149.kwhale.ui.components.ShimmerBox
import org.dueattendant149.kwhale.ui.components.TrackRowSkeleton
import org.dueattendant149.kwhale.ui.components.TrackRow
import org.dueattendant149.kwhale.ui.components.TrackTile
import org.dueattendant149.kwhale.design.KwhalePlural
import org.dueattendant149.kwhale.design.KwhaleSpacing
import org.dueattendant149.kwhale.design.LocalMiniPlayerBottomPadding

@Composable
fun HomeScreen(
    app: KwhaleApp,
    onOpenSearch: () -> Unit,
    onSeeAll: (String) -> Unit = {},
    onTrackMenu: (ApiTrack) -> Unit,
    onOpenProfile: () -> Unit = {},
) {
    var home by remember { mutableStateOf<HomeResponse?>(null) }
    var mixes by remember { mutableStateOf<List<Mix>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
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
                home = homeDeferred.await()
                mixes = mixesDeferred.await()
            }
        }.onFailure { error = it.message ?: "Ошибка сети" }
        loading = false
    }

    if (loading && home == null) {
        HomeSkeleton()
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
                        promptError = "Ничего не нашлось"
                    } else {
                        app.player.playQueue(tracks, 0)
                    }
                }
                .onFailure { promptError = it.message ?: "Ошибка сети" }
            promptLoading = false
        }
    }
    val miniPlayerBottom = LocalMiniPlayerBottomPadding.current
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
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onOpenSearch) {
                    Icon(Icons.Rounded.Search, contentDescription = "Поиск")
                }
                Surface(
                    onClick = onOpenProfile,
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
                                    text = mix.subtitle ?: KwhalePlural.tracks(mix.trackCount),
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
                            selected = false,
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
                            onPlay = { tracks, index ->
                                app.player.playQueue(tracks.map { it.toUi(app.api) }, index)
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
                                    BannerCard(banner = banner, api = app.api)
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
                                    onPlay = { tracks, index ->
                                        app.player.playQueue(tracks.map { it.toUi(app.api) }, index)
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
                                onPlay = { tracks, index ->
                                    app.player.playQueue(tracks.map { it.toUi(app.api) }, index)
                                },
                                onToggleLike = { track ->
                                    scope.launch {
                                        runCatching {
                                            if (track.isLiked) app.api.unlike(track.id)
                                            else app.api.like(track.id)
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
}

@Composable
private fun TrackCarousel(
    section: HomeSection,
    app: KwhaleApp,
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

private fun LazyListScope.TrackItems(
    section: HomeSection,
    app: KwhaleApp,
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
            modifier = Modifier.animateItem(),
            liked = track.isLiked,
            onClick = { onPlay(tracks, index) },
            onToggleLike = { onToggleLike(track) },
            onOverflow = { onMenu(track) },
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
