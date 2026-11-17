package org.dueattendant149.kwhale.ui.sheets

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dueattendant149.kwhale.KwhaleApp
import org.dueattendant149.kwhale.model.Track
import org.dueattendant149.kwhale.network.model.LyricsLine
import org.dueattendant149.kwhale.ui.components.EmptyState
import org.dueattendant149.kwhale.ui.components.ErrorState

@Composable
fun LyricsSheet(
    app: KwhaleApp,
    track: Track,
    positionMs: Long,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var lines by remember(track.id) { mutableStateOf<List<LyricsLine>>(emptyList()) }
    var plain by remember(track.id) { mutableStateOf<String?>(null) }
    var loading by remember(track.id) { mutableStateOf(true) }
    var error by remember(track.id) { mutableStateOf(false) }
    var followPlayback by remember(track.id) { mutableStateOf(true) }
    val listState = rememberLazyListState()

    fun loadLyrics() {
        val remoteId = track.remoteId
        if (remoteId == null) {
            loading = false
            error = false
            return
        }
        loading = true
        error = false
        scope.launch {
            runCatching { app.api.lyrics(remoteId) }
                .onSuccess { lyrics ->
                    lines = lyrics.synced.filter { it.text.isNotBlank() }
                    plain = lyrics.plain
                    error = false
                    loading = false
                }
                .onFailure {
                    error = true
                    loading = false
                }
        }
    }

    LaunchedEffect(track.id) {
        loadLyrics()
    }

    val activeIndex = remember(lines, positionMs) {
        if (lines.isEmpty()) -1 else lines.indexOfLast { it.startMs <= positionMs }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            followPlayback = false
        }
    }

    LaunchedEffect(activeIndex, followPlayback) {
        if (followPlayback && activeIndex in lines.indices) {
            val viewport = listState.layoutInfo.viewportSize.height
            val centerOffset = if (viewport > 0) -(viewport / 3) else 0
            runCatching { listState.animateScrollToItem(activeIndex, scrollOffset = centerOffset) }
        }
    }

    val fullText = remember(lines, plain) {
        when {
            lines.isNotEmpty() -> lines.joinToString("\n") { it.text }
            !plain.isNullOrBlank() -> plain.orEmpty()
            else -> ""
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Текст песни",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "${track.title} · ${track.artist}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (fullText.isNotEmpty()) {
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString(fullText))
                        scope.launch { snackbarHostState.showSnackbar("Текст скопирован") }
                    }) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Копировать текст")
                    }
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${track.artist} — ${track.title}\n\n$fullText")
                        }
                        context.startActivity(Intent.createChooser(intent, "Поделиться текстом"))
                    }) {
                        Icon(Icons.Rounded.Share, contentDescription = "Поделиться текстом")
                    }
                }
            }

            when {
                loading -> {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                error -> {
                    ErrorState(
                        message = "Не удалось загрузить текст",
                        onRetry = { loadLyrics() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                lines.isNotEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 560.dp),
                    ) {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(vertical = 80.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
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
                                        .clickable {
                                            followPlayback = true
                                            app.player.seekTo(line.startMs)
                                        }
                                        .padding(vertical = 8.dp),
                                )
                            }
                        }

                        if (!followPlayback && activeIndex in lines.indices) {
                            FilledTonalButton(
                                onClick = {
                                    followPlayback = true
                                    scope.launch {
                                        val viewport = listState.layoutInfo.viewportSize.height
                                        val centerOffset = if (viewport > 0) -(viewport / 3) else 0
                                        runCatching {
                                            listState.animateScrollToItem(activeIndex, scrollOffset = centerOffset)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 12.dp),
                            ) {
                                Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("К текущей строке")
                            }
                        }
                    }
                }
                !plain.isNullOrBlank() -> {
                    Text(
                        text = plain.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .heightIn(max = 560.dp)
                            .verticalScroll(rememberScrollState()),
                    )
                }
                else -> {
                    EmptyState(
                        title = "Текст пока недоступен",
                        message = "Для этого трека синхронизированный или обычный текст ещё не добавлен",
                        icon = Icons.Rounded.Lyrics,
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
