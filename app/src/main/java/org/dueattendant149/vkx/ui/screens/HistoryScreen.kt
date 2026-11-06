package org.dueattendant149.vkx.ui.screens

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dueattendant149.vkx.VkxApp
import org.dueattendant149.vkx.data.toUi
import org.dueattendant149.vkx.network.model.Track as ApiTrack
import org.dueattendant149.vkx.ui.components.EmptyState
import org.dueattendant149.vkx.ui.components.SectionHeader
import org.dueattendant149.vkx.ui.components.TrackRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    app: VkxApp,
    onBack: () -> Unit,
    onPlay: (List<org.dueattendant149.vkx.model.Track>, Int) -> Unit,
    onTrackMenu: (ApiTrack) -> Unit,
) {
    var tracks by remember { mutableStateOf<List<ApiTrack>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val liked = remember { mutableStateMapOf<String, Boolean>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        tracks = runCatching { app.api.history(limit = 50).items.map { it.track } }
            .getOrDefault(emptyList())
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
        )
        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }
        if (tracks.isEmpty()) {
            EmptyState(
                title = "История пуста",
                message = "Здесь появятся прослушанные треки",
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            return@Column
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            item { SectionHeader("Недавнее") }
            itemsIndexed(tracks) { index, track ->
                TrackRow(
                    track = track,
                    api = app.api,
                    liked = liked[track.id] ?: track.isLiked,
                    onClick = {
                        onPlay(tracks.map { it.toUi(app.api) }, index)
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
    }
}
