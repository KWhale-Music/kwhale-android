package org.dueattendant149.kwhale.ui.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PlaylistRemove
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.dueattendant149.kwhale.KwhaleApp
import org.dueattendant149.kwhale.data.toUi
import org.dueattendant149.kwhale.download.Downloads
import org.dueattendant149.kwhale.download.TrackDownloadStatus
import org.dueattendant149.kwhale.network.model.Track as ApiTrack
import org.dueattendant149.kwhale.ui.components.CoverArt
import org.dueattendant149.kwhale.ui.components.artistLine
import org.dueattendant149.kwhale.ui.components.imageUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackMenuSheet(
    app: KwhaleApp,
    track: ApiTrack,
    onDismiss: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    onMessage: (String) -> Unit = {},
    onRemoveFromPlaylist: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val likedOverrides by app.reactions.likedOverrides.collectAsState()
    val dislikedOverrides by app.reactions.dislikedOverrides.collectAsState()
    val censorshipOverrides by app.reactions.censorshipOverrides.collectAsState()
    val downloadStatuses by app.downloadObserver.statusesByRemoteId.collectAsState()

    val liked = app.reactions.isLiked(track.id, track.isLiked, likedOverrides)
    val disliked = app.reactions.isDisliked(track.id, track.isDisliked, dislikedOverrides)
    val censorshipReported = app.reactions.isCensorshipReported(
        trackId = track.id,
        serverReported = track.isCensorshipReported,
        overrides = censorshipOverrides,
    )
    val downloadStatus = app.downloadObserver.statusOf(track.id, downloadStatuses)

    val validArtists = remember(track.artists) {
        track.artists.filter { !it.id.startsWith("unknown-") && it.name.isNotBlank() }
    }
    var showArtistPicker by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverArt(
                imageUrl = track.imageUrl(app.api),
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(10.dp),
                iconSize = 22,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.artistLine(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        MenuAction(
            icon = if (liked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
            title = if (liked) "Убрать из моей музыки" else "В мою музыку",
            onClick = {
                app.reactions.toggleLike(track, app.api, "track_menu", onError = onMessage)
            },
        )
        MenuAction(
            icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
            title = "Включить следующим",
            onClick = {
                app.player.playNext(track.toUi(app.api), surface = "track_menu")
                onMessage("Включим следующим")
                onDismiss()
            },
        )
        MenuAction(
            icon = Icons.AutoMirrored.Rounded.QueueMusic,
            title = "Добавить в очередь",
            onClick = {
                app.player.addQueue(listOf(track.toUi(app.api)), surface = "track_menu")
                onMessage("Добавлено в очередь")
                onDismiss()
            },
        )
        MenuAction(
            icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
            title = "В плейлист",
            onClick = onAddToPlaylist,
        )
        onRemoveFromPlaylist?.let { removeAction ->
            MenuAction(
                icon = Icons.Outlined.PlaylistRemove,
                title = "Убрать из плейлиста",
                onClick = {
                    removeAction()
                    onDismiss()
                },
            )
        }
        when (downloadStatus) {
            TrackDownloadStatus.NotDownloaded -> {
                MenuAction(
                    icon = Icons.Outlined.Download,
                    title = "Скачать на устройство",
                    onClick = onDownload,
                )
            }
            TrackDownloadStatus.Queued,
            is TrackDownloadStatus.Downloading, -> {
                val label = if (downloadStatus is TrackDownloadStatus.Downloading) {
                    "Отменить скачивание (${downloadStatus.progressPercent}%)"
                } else {
                    "Отменить скачивание"
                }
                MenuAction(
                    icon = Icons.Outlined.Close,
                    title = label,
                    onClick = {
                        Downloads.cancel(context, track.id)
                        onMessage("Скачивание отменено")
                        onDismiss()
                    },
                )
            }
            is TrackDownloadStatus.Downloaded -> {
                MenuAction(
                    icon = Icons.Outlined.Delete,
                    title = "Удалить с устройства",
                    onClick = {
                        Downloads.cancel(context, track.id)
                        app.downloads.remove(track.id)
                        onMessage("Удалено из загрузок")
                        onDismiss()
                    },
                )
            }
            TrackDownloadStatus.Failed -> {
                MenuAction(
                    icon = Icons.Outlined.Refresh,
                    title = "Повторить скачивание",
                    onClick = {
                        Downloads.enqueue(context, track.toUi(app.api), replaceExisting = true)
                        onMessage("Повторное скачивание")
                        onDismiss()
                    },
                )
            }
        }
        if (validArtists.isNotEmpty()) {
            MenuAction(
                icon = Icons.Rounded.Person,
                title = if (validArtists.size == 1) {
                    "К артисту: ${validArtists.first().name}"
                } else {
                    "Перейти к артисту"
                },
                onClick = {
                    if (validArtists.size == 1) {
                        onOpenArtist(validArtists.first().id)
                    } else {
                        showArtistPicker = true
                    }
                },
            )
        }
        track.album?.id?.let { albumId ->
            MenuAction(
                icon = Icons.Rounded.Album,
                title = "Перейти к альбому",
                onClick = { onOpenAlbum(albumId) },
            )
        }
        MenuAction(
            icon = Icons.Rounded.Share,
            title = "Поделиться",
            onClick = {
                onShare()
                onDismiss()
            },
        )
        MenuAction(
            icon = if (censorshipReported) Icons.Rounded.Flag else Icons.Outlined.Flag,
            title = if (censorshipReported) "Отменить отправку на цензуру" else "Отправить на цензуру",
            onClick = {
                val ui = track.toUi(app.api)
                if (censorshipReported) {
                    app.reactions.cancelCensorshipReport(
                        track = ui,
                        onCancelled = { onMessage("Отправка на пересмотр отменена") },
                        onError = onMessage,
                    )
                } else {
                    app.reactions.reportCensorshipOneClick(
                        track = ui,
                        positionMs = app.player.state.value.takeIf { it.current?.remoteId == track.id }?.positionMs,
                        onAlreadyReported = { onMessage("Трек уже на пересмотре") },
                        onSent = { onMessage("Отправлено на пересмотр в админку") },
                        onError = onMessage,
                    )
                }
                onDismiss()
            },
        )
        MenuAction(
            icon = Icons.Outlined.ThumbDown,
            title = if (disliked) "Убрать дизлайк" else "Скрыть из рекомендаций",
            tint = MaterialTheme.colorScheme.error,
            onClick = {
                app.reactions.toggleDislike(
                    track = track.toUi(app.api),
                    surface = "track_menu",
                    onChanged = { nowDisliked ->
                        if (nowDisliked && app.player.state.value.current?.remoteId == track.id) {
                            app.player.next()
                        }
                        onMessage(if (nowDisliked) "Трек скрыт из рекомендаций" else "Дизлайк убран")
                    },
                    onError = onMessage,
                )
                onDismiss()
            },
        )
        MenuAction(
            icon = Icons.Outlined.Info,
            title = "О треке",
            onClick = { showInfo = true },
        )
    }

    if (showArtistPicker) {
        AlertDialog(
            onDismissRequest = { showArtistPicker = false },
            title = { Text("Выберите артиста") },
            text = {
                Column {
                    validArtists.forEach { artist ->
                        ListItem(
                            headlineContent = { Text(artist.name) },
                            leadingContent = { Icon(Icons.Rounded.Person, contentDescription = null) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                showArtistPicker = false
                                onOpenArtist(artist.id)
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showArtistPicker = false }) {
                    Text("Закрыть")
                }
            },
        )
    }

    if (showInfo) {
        ModalBottomSheet(
            onDismissRequest = { showInfo = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            TrackInfoSheet(app = app, track = track, onDismiss = { showInfo = false })
        }
    }
}

@Composable
private fun MenuAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    ListItem(
        headlineContent = { Text(title, color = tint) },
        leadingContent = { Icon(icon, contentDescription = null, tint = tint) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
