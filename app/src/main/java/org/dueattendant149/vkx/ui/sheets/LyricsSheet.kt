package org.dueattendant149.vkx.ui.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.dueattendant149.vkx.VkxApp
import org.dueattendant149.vkx.model.Track
import org.dueattendant149.vkx.network.model.LyricsLine

@Composable
fun LyricsSheet(
    app: VkxApp,
    track: Track,
    positionMs: Long,
    onDismiss: () -> Unit,
) {
    var lines by remember { mutableStateOf<List<LyricsLine>>(emptyList()) }
    var plain by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    LaunchedEffect(track.id) {
        val remoteId = track.remoteId
        if (remoteId == null) {
            loading = false
            return@LaunchedEffect
        }
        runCatching { app.api.lyrics(remoteId) }
            .onSuccess { lyrics ->
                lines = lyrics.synced.filter { it.text.isNotBlank() }
                plain = lyrics.plain
            }
            .onFailure { plain = null }
        loading = false
    }

    val activeIndex = remember(lines, positionMs) {
        if (lines.isEmpty()) -1 else lines.indexOfLast { it.startMs <= positionMs }
    }

    LaunchedEffect(activeIndex) {
        if (activeIndex in lines.indices) {
            runCatching { listState.animateScrollToItem(activeIndex) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
    ) {
        Text(
            text = "Текст песни",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        Text(
            text = "${track.title} · ${track.artist}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
        )
        if (loading) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (lines.isNotEmpty()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                itemsIndexed(lines) { index, line ->
                    val active = index == activeIndex
                    Text(
                        text = line.text,
                        style = if (active) MaterialTheme.typography.headlineSmall
                        else MaterialTheme.typography.titleLarge,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        color = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { app.player.seekTo(line.startMs) }
                            .padding(vertical = 8.dp),
                    )
                }
            }
        } else {
            Text(
                text = plain?.takeIf { it.isNotBlank() } ?: "Текст пока недоступен",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}
