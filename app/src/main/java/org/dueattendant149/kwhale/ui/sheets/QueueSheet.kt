package org.dueattendant149.kwhale.ui.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import org.dueattendant149.kwhale.KwhaleApp
import org.dueattendant149.kwhale.ui.components.CoverArt
import org.dueattendant149.kwhale.ui.components.formatDuration

@Composable
fun QueueSheet(
    app: KwhaleApp,
    onDismiss: () -> Unit,
) {
    val state by app.player.state.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val density = LocalDensity.current
    val rowHeightPx = with(density) { 68.dp.toPx() }

    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxWidth()) {
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
                if (state.queue.isNotEmpty()) {
                    TextButton(onClick = {
                        app.player.stop()
                        onDismiss()
                    }) {
                        Text("Очистить")
                    }
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

            val upcoming = remember(state.queue, state.index) {
                val start = (state.index + 1).coerceAtLeast(0)
                if (start <= state.queue.lastIndex) {
                    val seenIds = mutableMapOf<Long, Int>()
                    state.queue.subList(start, state.queue.size).mapIndexed { offset, t ->
                        val occ = seenIds[t.id] ?: 0
                        seenIds[t.id] = occ + 1
                        Triple(start + offset, t, "q-${t.id}-$occ")
                    }
                } else {
                    emptyList()
                }
            }

            Text(
                text = if (upcoming.isEmpty()) "Далее" else "Далее (${upcoming.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp),
            )

            if (upcoming.isEmpty()) {
                Text(
                    text = "Больше нет треков в очереди",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp),
                ) {
                    itemsIndexed(
                        items = upcoming,
                        key = { _, item -> item.third },
                    ) { _, (queueIndex, track, itemKey) ->
                        val isDragged = draggingKey == itemKey
                        val minUpcomingIndex = (state.index + 1).coerceAtLeast(0)
                        val maxUpcomingIndex = state.queue.lastIndex
                        var latestIndex by remember { mutableStateOf(queueIndex) }
                        SideEffect { latestIndex = queueIndex }

                        Surface(
                            tonalElevation = if (isDragged) 4.dp else 0.dp,
                            color = Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isDragged) 1f else 0f)
                                .graphicsLayer {
                                    translationY = if (isDragged) dragOffsetY else 0f
                                }
                                .semantics {
                                    customActions = buildList {
                                        if (queueIndex > minUpcomingIndex) {
                                            add(
                                                CustomAccessibilityAction("Переместить выше") {
                                                    app.player.moveInQueue(queueIndex, queueIndex - 1)
                                                    true
                                                },
                                            )
                                        }
                                        if (queueIndex < maxUpcomingIndex) {
                                            add(
                                                CustomAccessibilityAction("Переместить ниже") {
                                                    app.player.moveInQueue(queueIndex, queueIndex + 1)
                                                    true
                                                },
                                            )
                                        }
                                    }
                                },
                        ) {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = track.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .pointerInput(itemKey) {
                                                    detectDragGestures(
                                                        onDragStart = {
                                                            draggingKey = itemKey
                                                            dragOffsetY = 0f
                                                        },
                                                        onDragCancel = {
                                                            draggingKey = null
                                                            dragOffsetY = 0f
                                                        },
                                                        onDragEnd = {
                                                            draggingKey = null
                                                            dragOffsetY = 0f
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            dragOffsetY += dragAmount.y
                                                            val cur = latestIndex
                                                            val minIdx = (app.player.state.value.index + 1).coerceAtLeast(0)
                                                            val maxIdx = app.player.state.value.queue.lastIndex
                                                            if (cur < minIdx || cur > maxIdx) return@detectDragGestures
                                                            if (dragOffsetY > rowHeightPx * 0.65f && cur < maxIdx) {
                                                                app.player.moveInQueue(cur, cur + 1)
                                                                latestIndex = cur + 1
                                                                dragOffsetY -= rowHeightPx
                                                            } else if (dragOffsetY < -rowHeightPx * 0.65f && cur > minIdx) {
                                                                app.player.moveInQueue(cur, cur - 1)
                                                                latestIndex = cur - 1
                                                                dragOffsetY += rowHeightPx
                                                            }
                                                        },
                                                    )
                                                },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                Icons.Rounded.DragHandle,
                                                contentDescription = "Перетащить",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                val removed = track
                                                val removedIdx = latestIndex
                                                app.player.removeFromQueue(removedIdx)
                                                scope.launch {
                                                    val res = snackbarHostState.showSnackbar(
                                                        message = "Удалено: ${removed.title}",
                                                        actionLabel = "Отменить",
                                                        duration = SnackbarDuration.Short,
                                                    )
                                                    if (res == SnackbarResult.ActionPerformed) {
                                                        app.player.addQueue(listOf(removed))
                                                        val last = app.player.state.value.queue.lastIndex
                                                        if (removedIdx in 0..last) {
                                                            app.player.moveInQueue(last, removedIdx)
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(48.dp),
                                        ) {
                                            Icon(Icons.Rounded.Close, contentDescription = "Удалить из очереди")
                                        }
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable { app.player.jumpTo(latestIndex) },
                            )
                        }
                    }
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
