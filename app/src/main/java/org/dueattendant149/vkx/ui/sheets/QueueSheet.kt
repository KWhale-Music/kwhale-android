package org.dueattendant149.vkx.ui.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.dueattendant149.vkx.VkxApp
import org.dueattendant149.vkx.ui.components.CoverArt
import org.dueattendant149.vkx.ui.components.formatDuration

@Composable
fun QueueSheet(
    app: VkxApp,
    onDismiss: () -> Unit,
) {
    val state by app.player.state.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Очередь",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = {
                app.player.stop()
                onDismiss()
            }) {
                Text("Очистить")
            }
        }
        if (state.queue.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Очередь пуста",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }
        Text(
            text = "Сейчас играет",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        state.current?.let { track ->
            ListItem(
                headlineContent = {
                    Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                supportingContent = {
                    Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                leadingContent = {
                    CoverArt(
                        imageUrl = track.coverUrl,
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(8.dp),
                    )
                },
                trailingContent = {
                    Text(
                        text = formatDuration(state.durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        Text(
            text = "Далее",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp),
        ) {
            itemsIndexed(state.queue, key = { index, item -> "${item.id}-$index" }) { index, track ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = track.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (index == state.index) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    supportingContent = {
                        Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    leadingContent = {
                        CoverArt(
                            imageUrl = track.coverUrl,
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(8.dp),
                        )
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { app.player.moveInQueue(index, index - 1) },
                                enabled = index > 0,
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(Icons.Rounded.ArrowUpward, contentDescription = "Выше")
                            }
                            IconButton(
                                onClick = { app.player.moveInQueue(index, index + 1) },
                                enabled = index < state.queue.lastIndex,
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(Icons.Rounded.ArrowDownward, contentDescription = "Ниже")
                            }
                            IconButton(
                                onClick = { app.player.removeFromQueue(index) },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Удалить")
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { app.player.jumpTo(index) },
                )
            }
        }
    }
}
