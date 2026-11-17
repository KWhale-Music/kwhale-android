package org.dueattendant149.kwhale.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import kotlinx.coroutines.launch
import org.dueattendant149.kwhale.KwhaleApp
import org.dueattendant149.kwhale.data.toUi
import org.dueattendant149.kwhale.design.LocalMiniPlayerBottomPadding
import org.dueattendant149.kwhale.network.model.Track as ApiTrack
import org.dueattendant149.kwhale.ui.components.EmptyState
import org.dueattendant149.kwhale.ui.components.ErrorState
import org.dueattendant149.kwhale.ui.components.LocalSnackbarHost
import org.dueattendant149.kwhale.ui.components.SectionHeader
import org.dueattendant149.kwhale.ui.components.TrackRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    app: KwhaleApp,
    onBack: () -> Unit,
    onPlay: (List<org.dueattendant149.kwhale.model.Track>, Int) -> Unit,
    onTrackMenu: (ApiTrack) -> Unit,
) {
    var tracks by remember { mutableStateOf<List<ApiTrack>>(emptyList()) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var clearing by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    val likedOverrides by app.reactions.likedOverrides.collectAsState()
    val playerState by app.player.state.collectAsState()
    val snackbarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val miniPlayerBottom = LocalMiniPlayerBottomPadding.current

    LaunchedEffect(reloadKey) {
        loading = true
        error = null
        runCatching { app.api.history(limit = 50) }
            .onSuccess { page ->
                tracks = page.items.map { it.track }
                nextCursor = page.nextCursor
            }
            .onFailure { err ->
                error = err.message ?: "Не удалось загрузить историю"
            }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("История") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                }
            },
            actions = {
                if (tracks.isNotEmpty()) {
                    IconButton(
                        enabled = !clearing,
                        onClick = { confirmClear = true },
                    ) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "Очистить историю")
                    }
                }
            },
        )
        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }
        if (error != null && tracks.isEmpty()) {
            ErrorState(
                message = error ?: "Ошибка сети",
                onRetry = { reloadKey++ },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            return@Column
        }
        if (tracks.isEmpty()) {
            EmptyState(
                title = "История пуста",
                message = "Здесь появятся прослушанные треки",
                icon = Icons.Rounded.History,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            return@Column
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp + miniPlayerBottom),
        ) {
            item { SectionHeader("Недавнее") }
            itemsIndexed(tracks, key = { index, track -> "${track.id}:$index" }) { index, track ->
                val isLiked = app.reactions.isLiked(track.id, track.isLiked, likedOverrides)
                TrackRow(
                    track = track,
                    api = app.api,
                    liked = isLiked,
                    isCurrent = playerState.current?.remoteId == track.id,
                    onClick = {
                        onPlay(tracks.map { it.toUi(app.api) }, index)
                    },
                    onToggleLike = {
                        app.reactions.toggleLike(
                            track = track,
                            api = app.api,
                            surface = "history",
                            onError = { msg -> scope.launch { snackbarHost?.showSnackbar(msg) } },
                        )
                    },
                    onOverflow = { onTrackMenu(track) },
                )
            }
            if (nextCursor != null) {
                item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        if (loadingMore) {
                            CircularProgressIndicator()
                        } else {
                            Button(onClick = {
                                val cursor = nextCursor
                                if (cursor != null && !loadingMore) {
                                    loadingMore = true
                                    scope.launch {
                                        runCatching { app.api.history(cursor = cursor, limit = 50) }
                                            .onSuccess { page ->
                                                tracks = tracks + page.items.map { it.track }
                                                nextCursor = page.nextCursor
                                            }
                                            .onFailure {
                                                snackbarHost?.showSnackbar("Не удалось загрузить ещё")
                                            }
                                        loadingMore = false
                                    }
                                }
                            }) {
                                Text("Показать ещё")
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { if (!clearing) confirmClear = false },
            title = { Text("Очистить историю?") },
            text = { Text("Список недавно прослушанных треков будет очищен.") },
            confirmButton = {
                TextButton(
                    enabled = !clearing,
                    onClick = {
                        clearing = true
                        scope.launch {
                            runCatching { app.api.clearHistory() }
                                .onSuccess {
                                    tracks = emptyList()
                                    nextCursor = null
                                    confirmClear = false
                                    snackbarHost?.showSnackbar("История очищена")
                                }
                                .onFailure { err ->
                                    snackbarHost?.showSnackbar(err.message ?: "Не удалось очистить историю")
                                }
                            clearing = false
                        }
                    },
                ) { Text("Очистить") }
            },
            dismissButton = {
                TextButton(
                    enabled = !clearing,
                    onClick = { confirmClear = false },
                ) { Text("Отмена") }
            },
        )
    }
}
