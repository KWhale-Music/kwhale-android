package org.dueattendant149.kwhale.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dueattendant149.kwhale.KwhaleApp
import org.dueattendant149.kwhale.design.KwhalePlural
import org.dueattendant149.kwhale.design.LocalMiniPlayerBottomPadding
import org.dueattendant149.kwhale.data.toUi
import org.dueattendant149.kwhale.network.model.Album
import org.dueattendant149.kwhale.network.model.PlaylistUpdate
import org.dueattendant149.kwhale.network.model.Track as ApiTrack
import org.dueattendant149.kwhale.ui.components.AlbumCard
import org.dueattendant149.kwhale.ui.components.BlurredCoverBackground
import org.dueattendant149.kwhale.ui.components.CoverArt
import org.dueattendant149.kwhale.ui.components.ErrorState
import org.dueattendant149.kwhale.ui.components.SectionHeader
import org.dueattendant149.kwhale.ui.components.ShimmerBox
import org.dueattendant149.kwhale.ui.components.TrackRow
import org.dueattendant149.kwhale.ui.components.TrackRowSkeleton
import org.dueattendant149.kwhale.ui.components.imageUrl

@Composable
fun CollectionScreen(
    app: KwhaleApp,
    kind: String,
    collectionId: String,
    onBack: () -> Unit,
    onOpenCollection: (String, String) -> Unit,
    onTrackMenu: (ApiTrack) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var meta by remember { mutableStateOf("") }
    var cover by remember { mutableStateOf<String?>(null) }
    var tracks by remember { mutableStateOf<List<ApiTrack>>(emptyList()) }
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var canEdit by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    val miniPlayerBottom = LocalMiniPlayerBottomPadding.current
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    var isFavorite by remember { mutableStateOf(false) }
    var isFollowed by remember { mutableStateOf(false) }
    var isOwned by remember { mutableStateOf(false) }
    var actionBusy by remember { mutableStateOf(false) }
    val liked = remember { mutableStateMapOf<String, Boolean>() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(kind, collectionId, reload) {
        loading = true
        error = null
        runCatching {
            when (kind) {
                "album" -> {
                    val album = app.api.album(collectionId)
                    title = album.title
                    subtitle = album.artists.joinToString(", ") { it.name }
                    meta = listOfNotNull(album.year?.toString(), KwhalePlural.tracks(album.trackCount))
                        .joinToString(" · ")
                    cover = album.imageUrl(app.api)
                    isFavorite = album.isFavorite
                    tracks = album.tracks.ifEmpty { app.api.albumTracks(collectionId).items }
                }
                "artist" -> {
                    val artist = app.api.artist(collectionId)
                    title = artist.name
                    subtitle = artist.genres.joinToString(", ") { it.name }
                    meta = "${artist.followers} слушателей"
                    cover = artist.imageUrl(app.api)
                    isFavorite = artist.isFavorite
                    tracks = artist.topTracks.ifEmpty { app.api.artistTopTracks(collectionId).items }
                    albums = artist.albums.ifEmpty { app.api.artistAlbums(collectionId).items }
                }
                "playlist" -> {
                    val playlist = app.api.playlist(collectionId)
                    title = playlist.title
                    subtitle = playlist.owner?.displayName ?: "Вы"
                    meta = KwhalePlural.tracks(playlist.trackCount)
                    cover = playlist.imageUrl(app.api)
                    canEdit = playlist.permissions.canEdit
                    isOwned = playlist.permissions.canEdit
                    isFollowed = playlist.isFollowed
                    tracks = playlist.tracks.ifEmpty { app.api.playlistTracks(collectionId).items }
                }
                else -> error("Неизвестная коллекция")
            }
        }.onFailure { error = it.message ?: "Ошибка сети" }
        loading = false
    }

    fun toggleSaved() {
        if (actionBusy || (kind == "playlist" && isOwned)) return
        val saved = if (kind == "playlist") isFollowed else isFavorite
        actionBusy = true
        scope.launch {
            runCatching {
                when (kind) {
                    "album" -> if (saved) app.api.unlikeAlbum(collectionId) else app.api.likeAlbum(collectionId)
                    "artist" -> if (saved) app.api.unlikeArtist(collectionId) else app.api.likeArtist(collectionId)
                    "playlist" -> if (saved) app.api.unfollowPlaylist(collectionId) else app.api.followPlaylist(collectionId)
                }
            }.onSuccess {
                if (kind == "playlist") isFollowed = !saved else isFavorite = !saved
                Toast.makeText(
                    context,
                    if (saved) "Убрано из библиотеки" else "Сохранено в библиотеку",
                    Toast.LENGTH_SHORT,
                ).show()
            }.onFailure {
                Toast.makeText(context, "Не удалось сохранить", Toast.LENGTH_SHORT).show()
            }
            actionBusy = false
        }
    }

    fun downloadAll() {
        if (actionBusy || kind == "artist") return
        actionBusy = true
        scope.launch {
            runCatching {
                when (kind) {
                    "album" -> app.api.likeAlbumTracks(collectionId)
                    "playlist" -> app.api.likePlaylistTracks(collectionId)
                    else -> error("unsupported kind")
                }
            }.onSuccess { result ->
                val total = result.liked + result.alreadyLiked
                val suffix = if (result.failed > 0) ", ошибок: ${result.failed}" else ""
                Toast.makeText(
                    context,
                    "Скачивается треков: $total$suffix",
                    Toast.LENGTH_SHORT,
                ).show()
            }.onFailure {
                Toast.makeText(context, "Не удалось поставить в загрузку", Toast.LENGTH_SHORT).show()
            }
            actionBusy = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        BlurredCoverBackground(cover)
        if (loading) {
            CollectionSkeleton()
            return@Box
        }
        if (error != null) {
            ErrorState(
                message = error ?: "Ошибка сети",
                onRetry = { reload++ },
                modifier = Modifier.fillMaxSize(),
            )
            return@Box
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 28.dp + miniPlayerBottom,
            ),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                    Spacer(Modifier.weight(1f))
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "Ещё")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            if (kind == "playlist" && canEdit) {
                                DropdownMenuItem(
                                    text = { Text("Переименовать") },
                                    onClick = {
                                        showMenu = false
                                        renameText = title
                                        showRename = true
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Удалить плейлист") },
                                    onClick = {
                                        showMenu = false
                                        showDelete = true
                                    },
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Действия недоступны") },
                                    onClick = { showMenu = false },
                                )
                            }
                        }
                    }
                }
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CoverArt(
                        imageUrl = cover,
                        modifier = Modifier.size(if (kind == "artist") 220.dp else 180.dp),
                        shape = RoundedCornerShape(12.dp),
                        iconSize = 44,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (meta.isNotBlank()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            item {
                val saved = if (kind == "playlist") isFollowed else isFavorite
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    if (!(kind == "playlist" && isOwned)) {
                        ActionItem(
                            icon = if (saved) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            label = if (saved) "Добавлено" else "Добавить",
                            enabled = !actionBusy,
                            onClick = { toggleSaved() },
                        )
                    }
                    if (kind != "artist") {
                        ActionItem(
                            icon = Icons.Rounded.Download,
                            label = "Скачать всё",
                            enabled = !actionBusy,
                            onClick = { downloadAll() },
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = {
                            if (tracks.isNotEmpty()) {
                                app.player.playQueue(tracks.map { it.toUi(app.api) }, 0)
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Слушать")
                    }
                    FilledTonalButton(
                        onClick = {
                            if (tracks.isNotEmpty()) {
                                app.player.playQueue(
                                    tracks.shuffled().map { it.toUi(app.api) },
                                    0,
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Shuffle, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Перемешать")
                    }
                }
            }
            item { SectionHeader(if (kind == "artist") "Популярные треки" else "Треки") }
            itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                TrackRow(
                    track = track,
                    api = app.api,
                    modifier = Modifier.animateItem(),
                    liked = liked[track.id] ?: track.isLiked,
                    onClick = { app.player.playQueue(tracks.map { it.toUi(app.api) }, index) },
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
                    number = index + 1,
                )
            }
            if (albums.isNotEmpty()) {
                item { SectionHeader("Альбомы") }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(albums, key = { it.id }) { album ->
                            AlbumCard(
                                album = album,
                                api = app.api,
                                onClick = { onOpenCollection("album", album.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRename) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Переименовать плейлист") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("Название") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRename = false
                    scope.launch {
                        runCatching {
                            app.api.updatePlaylist(collectionId, PlaylistUpdate(title = renameText))
                        }.onSuccess {
                            title = renameText
                            reload++
                        }
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) { Text("Отмена") }
            },
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Удалить плейлист?") },
            text = { Text("Плейлист будет удалён безвозвратно.") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    scope.launch {
                        runCatching { app.api.deletePlaylist(collectionId) }.onSuccess { onBack() }
                    }
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun ActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f)
    Column(
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = if (enabled) 1f else 0.38f,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun CollectionSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShimmerBox(Modifier.size(24.dp), CircleShape)
            Spacer(Modifier.weight(1f))
            ShimmerBox(Modifier.size(24.dp), CircleShape)
        }
        Spacer(Modifier.size(8.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ShimmerBox(Modifier.size(180.dp), RoundedCornerShape(12.dp))
            Spacer(Modifier.size(14.dp))
            ShimmerBox(Modifier.width(220.dp).height(26.dp))
            Spacer(Modifier.size(8.dp))
            ShimmerBox(Modifier.width(130.dp).height(16.dp))
        }
        Spacer(Modifier.size(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            repeat(2) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ShimmerBox(Modifier.size(24.dp), CircleShape)
                    Spacer(Modifier.size(6.dp))
                    ShimmerBox(Modifier.width(64.dp).height(12.dp))
                }
            }
        }
        Spacer(Modifier.size(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ShimmerBox(Modifier.weight(1f).height(40.dp), RoundedCornerShape(20.dp))
            ShimmerBox(Modifier.weight(1f).height(40.dp), RoundedCornerShape(20.dp))
        }
        Spacer(Modifier.size(16.dp))
        repeat(6) { TrackRowSkeleton() }
    }
}
