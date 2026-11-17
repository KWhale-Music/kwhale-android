package org.dueattendant149.kwhale.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dueattendant149.kwhale.KwhaleApp
import org.dueattendant149.kwhale.design.LocalMiniPlayerBottomPadding
import org.dueattendant149.kwhale.download.Downloads
import org.dueattendant149.kwhale.download.TrackDownloadStatus
import org.dueattendant149.kwhale.model.Track
import org.dueattendant149.kwhale.ui.components.CoverArt
import org.dueattendant149.kwhale.ui.components.EmptyState
import org.dueattendant149.kwhale.ui.components.LocalSnackbarHost
import org.dueattendant149.kwhale.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    app: KwhaleApp,
    onBack: () -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbarHost.current
    val allDownloaded by app.downloadObserver.downloadedTracks.collectAsState()
    val activeDownloads by app.downloadObserver.activeDownloads.collectAsState()
    val playerState by app.player.state.collectAsState()
    val items = remember(allDownloaded) { allDownloaded.filter { it.file().exists() } }
    val miniPlayerBottom = LocalMiniPlayerBottomPadding.current
    var showClearDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Скачано") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                }
            },
            actions = {
                if (items.isNotEmpty()) {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "Очистить все загрузки")
                    }
                }
            },
        )
        if (items.isEmpty() && activeDownloads.isEmpty()) {
            EmptyState(
                title = "Пока пусто",
                message = "Скачанные треки появятся здесь и будут играть без интернета",
                icon = Icons.Rounded.Download,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            return@Column
        }
        val tracks = remember(items) {
            items.map { item ->
                Track(
                    id = item.remoteId.hashCode().toLong(),
                    title = item.title,
                    artist = item.artist,
                    durationSec = item.durationSec,
                    coverUrl = item.coverUrl,
                    streamUrl = item.streamUrl,
                    remoteId = item.remoteId,
                    isOfflineCached = true,
                )
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp + miniPlayerBottom),
        ) {
            if (activeDownloads.isNotEmpty()) {
                item {
                    SectionHeader("Загружаются (${activeDownloads.size})")
                }
                items(activeDownloads, key = { "active-${it.remoteId}" }) { active ->
                    val meta = active.track
                    ListItem(
                        headlineContent = {
                            Text(
                                text = meta?.title ?: "Трек ${active.remoteId}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        supportingContent = {
                            Column {
                                Text(
                                    text = when (val st = active.status) {
                                        TrackDownloadStatus.Queued -> "В очереди..."
                                        is TrackDownloadStatus.Downloading -> "Загрузка ${st.progressPercent}%"
                                        TrackDownloadStatus.Failed -> "Ошибка загрузки"
                                        else -> meta?.artist.orEmpty()
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (active.status is TrackDownloadStatus.Failed) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                                when (val st = active.status) {
                                    TrackDownloadStatus.Queued -> {
                                        Spacer(Modifier.height(6.dp))
                                        LinearProgressIndicator(Modifier.fillMaxWidth())
                                    }
                                    is TrackDownloadStatus.Downloading -> {
                                        Spacer(Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { st.progressPercent / 100f },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                    else -> Unit
                                }
                            }
                        },
                        leadingContent = {
                            CoverArt(
                                imageUrl = meta?.coverUrl,
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                seed = meta?.title ?: active.remoteId,
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (active.status is TrackDownloadStatus.Failed && meta != null) {
                                    IconButton(onClick = {
                                        Downloads.enqueue(context, meta, replaceExisting = true)
                                    }) {
                                        Icon(Icons.Rounded.Refresh, contentDescription = "Повторить загрузку")
                                    }
                                }
                                IconButton(onClick = { Downloads.cancel(context, active.remoteId) }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Отменить загрузку")
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }

            if (items.isNotEmpty()) {
                item {
                    SectionHeader("Сохранено (${items.size})")
                }
                items(items, key = { it.remoteId }) { item ->
                    val isCurrent = item.remoteId == playerState.current?.remoteId
                    ListItem(
                        headlineContent = {
                            Text(
                                text = item.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        supportingContent = {
                            Text(
                                text = item.artist,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        leadingContent = {
                            CoverArt(
                                imageUrl = item.coverUrl,
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                seed = item.title,
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = {
                                Downloads.cancel(context, item.remoteId)
                                app.downloads.remove(item.remoteId)
                                scope.launch { snackbar?.showSnackbar("Удалено: ${item.title}") }
                            }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Удалить")
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = if (isCurrent) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            } else {
                                Color.Transparent
                            },
                        ),
                        modifier = Modifier.clickable {
                            val index = items.indexOf(item).coerceAtLeast(0)
                            onPlay(tracks, index)
                        },
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Удалить все загрузки?") },
            text = { Text("Все скачанные треки будут удалены с устройства. Вы сможете скачать их снова позже.") },
            confirmButton = {
                TextButton(onClick = {
                    items.forEach {
                        Downloads.cancel(context, it.remoteId)
                        app.downloads.remove(it.remoteId)
                    }
                    showClearDialog = false
                    scope.launch { snackbar?.showSnackbar("Все загрузки удалены") }
                }) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Отмена") }
            },
        )
    }
}
