package org.dueattendant149.kwhale.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.dueattendant149.kwhale.KwhaleApp
import org.dueattendant149.kwhale.design.LocalMiniPlayerBottomPadding
import org.dueattendant149.kwhale.download.DownloadStore
import org.dueattendant149.kwhale.download.DownloadedTrack
import org.dueattendant149.kwhale.model.Track
import org.dueattendant149.kwhale.ui.components.CoverArt
import org.dueattendant149.kwhale.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    app: KwhaleApp,
    onBack: () -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
) {
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<DownloadedTrack>>(emptyList()) }
    val miniPlayerBottom = LocalMiniPlayerBottomPadding.current

    LaunchedEffect(Unit) {
        items = DownloadStore(context).all().filter { it.file().exists() }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Скачано") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                }
            },
        )
        if (items.isEmpty()) {
            EmptyState(
                title = "Пока пусто",
                message = "Скачанные треки появятся здесь",
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            return@Column
        }
        val tracks = items.map { item ->
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
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp + miniPlayerBottom),
        ) {
            items(items, key = { it.remoteId }) { item ->
                ListItem(
                    headlineContent = { Text(item.title) },
                    supportingContent = { Text(item.artist) },
                    leadingContent = {
                        CoverArt(
                            imageUrl = item.coverUrl,
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(8.dp),
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = {
                            DownloadStore(context).remove(item.remoteId)
                            items = items.filterNot { it.remoteId == item.remoteId }
                        }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Удалить")
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        val index = items.indexOf(item).coerceAtLeast(0)
                        onPlay(tracks, index)
                    },
                )
            }
        }
    }
}
