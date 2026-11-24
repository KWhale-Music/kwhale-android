package org.dueattendant149.kwhale.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dueattendant149.kwhale.KwhaleApp
import org.dueattendant149.kwhale.data.CollectionSnapshot
import org.dueattendant149.kwhale.data.ContentCache
import org.dueattendant149.kwhale.data.LibrarySnapshot
import org.dueattendant149.kwhale.data.toUi
import org.dueattendant149.kwhale.design.KwhalePlural
import org.dueattendant149.kwhale.design.LocalMiniPlayerBottomPadding
import org.dueattendant149.kwhale.download.Downloads
import org.dueattendant149.kwhale.download.TrackDownloadStatus
import org.dueattendant149.kwhale.network.model.Album
import org.dueattendant149.kwhale.network.model.PlaylistUpdate
import org.dueattendant149.kwhale.network.model.ShareKind
import org.dueattendant149.kwhale.network.model.Track as ApiTrack
import org.dueattendant149.kwhale.ui.sheets.ShareTarget
import org.dueattendant149.kwhale.ui.components.AlbumCard
import org.dueattendant149.kwhale.ui.components.BlurredCoverBackground
import org.dueattendant149.kwhale.ui.components.CoverArt
import org.dueattendant149.kwhale.ui.components.ErrorState
import org.dueattendant149.kwhale.ui.components.LocalSnackbarHost
import org.dueattendant149.kwhale.ui.components.SectionHeader
import org.dueattendant149.kwhale.ui.components.ShimmerBox
import org.dueattendant149.kwhale.ui.components.TrackRow
import org.dueattendant149.kwhale.ui.components.TrackRowSkeleton
import org.dueattendant149.kwhale.ui.components.isSingleLike
import org.dueattendant149.kwhale.ui.components.primaryArtistId
import org.dueattendant149.kwhale.ui.components.artistLine
import org.dueattendant149.kwhale.ui.components.imageUrl
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    app: KwhaleApp,
    kind: String,
    collectionId: String,
    onBack: () -> Unit,
    onOpenCollection: (String, String) -> Unit,
    onTrackMenu: (ApiTrack) -> Unit,
    onOpenArtistAll: (String) -> Unit = {},
    onShare: (ShareTarget) -> Unit = {},
) {
    val cacheKey = ContentCache.collection(kind, collectionId)
    val cached = remember(kind, collectionId) {
        app.cache.read(cacheKey, CollectionSnapshot.serializer())
    }
    var title by remember { mutableStateOf(cached?.title.orEmpty()) }
    var subtitle by remember { mutableStateOf(cached?.subtitle.orEmpty()) }
    var meta by remember { mutableStateOf(cached?.meta.orEmpty()) }
    var cover by remember { mutableStateOf(cached?.cover) }
    var tracks by remember { mutableStateOf(cached?.tracks.orEmpty()) }
    var albums by remember { mutableStateOf(cached?.albums.orEmpty()) }
    var latestRelease by remember { mutableStateOf(cached?.latestRelease) }
    var syncStatus by remember { mutableStateOf<org.dueattendant149.kwhale.network.model.ArtistSyncStatus?>(null) }
    var canEdit by remember { mutableStateOf(cached?.canEdit ?: false) }
    var showMenu by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showAddTracks by remember { mutableStateOf(false) }
    var showDownloadSheet by remember { mutableStateOf(false) }
    var editingTracks by remember { mutableStateOf(false) }
    var draftTracks by remember { mutableStateOf<List<ApiTrack>>(emptyList()) }
    var savingOrder by remember { mutableStateOf(false) }
    var removingTrackId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var renameBusy by remember { mutableStateOf(false) }
    var renameError by remember { mutableStateOf<String?>(null) }
    var deleteBusy by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(cached == null) }
    val miniPlayerBottom = LocalMiniPlayerBottomPadding.current
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    var isFavorite by remember { mutableStateOf(cached?.isFavorite ?: false) }
    var isFollowed by remember { mutableStateOf(cached?.isFollowed ?: false) }
    var isOwned by remember { mutableStateOf(cached?.isOwned ?: false) }
    var actionBusy by remember { mutableStateOf(false) }
    val likedOverrides by app.reactions.likedOverrides.collectAsState()
    val playerState by app.player.state.collectAsState()
    val downloadStatuses by app.downloadObserver.statusesByRemoteId.collectAsState()
    val snackbarHost = LocalSnackbarHost.current
    val listState = rememberLazyListState()
    val showPinnedTitle by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 360
        }
    }
    val displayTracks = if (kind == "artist") tracks.take(5) else tracks
    // Singles/EPs get their own shelf — the «не отличить сингл от альбома» feedback.
    val fullAlbums = albums.filterNot { isSingleLike(it) }
    val singlesAndEps = albums.filter { isSingleLike(it) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val albumGridCols = when {
        screenWidthDp >= 840 -> 4
        screenWidthDp >= 600 -> 3
        else -> 2
    }

    val offlineDownloadedCount = remember(tracks, downloadStatuses) {
        tracks.count { downloadStatuses[it.id] is TrackDownloadStatus.Downloaded }
    }
    val offlineActiveCount = remember(tracks, downloadStatuses) {
        tracks.count {
            val status = downloadStatuses[it.id]
            status is TrackDownloadStatus.Queued || status is TrackDownloadStatus.Downloading
        }
    }

    BackHandler(enabled = editingTracks) {
        if (!savingOrder) {
            editingTracks = false
        }
    }

    val coverPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val ext = when {
                        mime.contains("png") -> "png"
                        mime.contains("webp") -> "webp"
                        else -> "jpg"
                    }
                    val file = File.createTempFile("playlist_cover", ".$ext", context.cacheDir)
                    try {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            file.outputStream().use { output -> input.copyTo(output) }
                        } ?: error("Не удалось прочитать изображение")
                        app.api.uploadPlaylistCover(collectionId, file, mime)
                    } finally {
                        file.delete()
                    }
                }.onSuccess { updated ->
                    cover = updated.imageUrl(app.api)
                    app.cache.invalidateCollection(kind, collectionId)
                    app.cache.invalidateLibrary()
                    snackbarHost?.showSnackbar("Обложка обновлена")
                    reload++
                }.onFailure {
                    snackbarHost?.showSnackbar("Не удалось загрузить обложку")
                }
            }
        }
    }

    LaunchedEffect(kind, collectionId, reload) {
        if (reload == 0 && cached != null && app.cache.isFresh(cacheKey, 30_000)) {
            loading = false
            return@LaunchedEffect
        }
        loading = cached == null && tracks.isEmpty()
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
                    meta = KwhalePlural.listeners(artist.followers.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                    cover = artist.imageUrl(app.api)
                    isFavorite = artist.isFavorite
                    latestRelease = artist.latestRelease
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
            app.cache.write(
                cacheKey,
                CollectionSnapshot.serializer(),
                CollectionSnapshot(
                    title = title,
                    subtitle = subtitle,
                    meta = meta,
                    cover = cover,
                    tracks = tracks,
                    albums = albums,
                    latestRelease = latestRelease,
                    canEdit = canEdit,
                    isFavorite = isFavorite,
                    isFollowed = isFollowed,
                    isOwned = isOwned,
                ),
            )
        }.onFailure {
            if (cached == null && tracks.isEmpty()) {
                error = it.message ?: "Ошибка сети"
            } else if (reload > 0) {
                snackbarHost?.showSnackbar("Не удалось обновить коллекцию")
            }
        }
        loading = false
    }

    LaunchedEffect(kind, collectionId) {
        if (kind != "artist") return@LaunchedEffect
        syncStatus = runCatching { app.api.syncArtist(collectionId) }.getOrNull()
        repeat(8) {
            delay(2000)
            val state = runCatching { app.api.artistSyncStatus(collectionId) }.getOrNull()
                ?: return@LaunchedEffect
            syncStatus = state
            if (state.status in setOf("done", "failed", "unavailable")) {
                if (state.status == "done") {
                    app.cache.invalidateCollection(kind, collectionId)
                    reload++
                }
                return@LaunchedEffect
            }
        }
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
                app.cache.invalidateCollection(kind, collectionId)
                app.cache.invalidateLibrary()
                snackbarHost?.showSnackbar(
                    if (saved) "Убрано из библиотеки" else "Сохранено в библиотеку",
                )
            }.onFailure {
                snackbarHost?.showSnackbar("Не удалось сохранить")
            }
            actionBusy = false
        }
    }

    fun downloadAllToServer() {
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
                app.cache.invalidateCollection(kind, collectionId)
                app.cache.invalidateLibrary()
                val total = result.liked + result.alreadyLiked
                val suffix = if (result.failed > 0) ", ошибок: ${result.failed}" else ""
                snackbarHost?.showSnackbar("На сервере в библиотеке: $total$suffix")
            }.onFailure {
                snackbarHost?.showSnackbar("Не удалось добавить треки на сервер")
            }
            actionBusy = false
        }
    }

    fun downloadAllToDevice() {
        if (tracks.isEmpty()) return
        val pending = tracks.filter { track ->
            val st = downloadStatuses[track.id]
            st !is TrackDownloadStatus.Downloaded &&
                st !is TrackDownloadStatus.Downloading &&
                st !is TrackDownloadStatus.Queued
        }
        if (pending.isEmpty()) {
            scope.launch {
                snackbarHost?.showSnackbar("Все треки уже скачаны или загружаются")
            }
            return
        }
        val queued = Downloads.enqueueAll(context, pending.map { it.toUi(app.api) })
        scope.launch {
            snackbarHost?.showSnackbar(
                if (queued > 0) "Добавлено в офлайн-загрузку: $queued"
                else "Нет доступных треков для загрузки",
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        BlurredCoverBackground(cover)
        fun triggerCollectionShare() {
            if ((kind != "album" && kind != "artist") || title.isBlank()) return
            val shareKind = if (kind == "artist") ShareKind.ARTIST else ShareKind.ALBUM
            val fb = if (kind == "album" && subtitle.isNotBlank()) "$subtitle — $title" else title
            onShare(
                ShareTarget(
                    kind = shareKind,
                    id = collectionId,
                    title = title,
                    subtitle = subtitle.ifBlank { null },
                    meta = meta.ifBlank { null },
                    coverUrl = cover,
                    fallbackText = fb,
                    surface = kind,
                ),
            )
        }

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Surface(
                color = if (showPinnedTitle || editingTracks) {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                } else {
                    Color.Transparent
                },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (editingTracks) {
                        TextButton(
                            enabled = !savingOrder,
                            onClick = { editingTracks = false },
                        ) { Text("Отмена") }
                        Text(
                            text = "Порядок и состав",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        )
                        if (savingOrder) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 12.dp).size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            TextButton(
                                onClick = {
                                    if (draftTracks.map { it.id } == tracks.map { it.id }) {
                                        editingTracks = false
                                        return@TextButton
                                    }
                                    savingOrder = true
                                    scope.launch {
                                        runCatching {
                                            app.api.reorderPlaylistTracks(
                                                collectionId,
                                                draftTracks.map { it.id },
                                            )
                                        }.onSuccess {
                                            tracks = draftTracks
                                            app.cache.invalidateCollection(kind, collectionId)
                                            app.cache.invalidateLibrary()
                                            editingTracks = false
                                            snackbarHost?.showSnackbar("Порядок сохранён")
                                        }.onFailure {
                                            snackbarHost?.showSnackbar("Не удалось сохранить порядок")
                                        }
                                        savingOrder = false
                                    }
                                },
                            ) { Text("Готово") }
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                        }
                        AnimatedVisibility(
                            visible = showPinnedTitle,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                        if (!showPinnedTitle) {
                            Spacer(Modifier.weight(1f))
                        }
                        if ((kind == "album" || kind == "artist") && title.isNotBlank() && !loading && error == null) {
                            IconButton(onClick = { triggerCollectionShare() }) {
                                Icon(Icons.Rounded.Share, contentDescription = "Поделиться")
                            }
                        }
                        if (kind == "playlist" && canEdit && !loading && error == null) {
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Rounded.MoreVert, contentDescription = "Действия с плейлистом")
                                }
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Изменить обложку") },
                                        onClick = {
                                            showMenu = false
                                            coverPicker.launch(
                                                PickVisualMediaRequest(
                                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                                ),
                                            )
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Удалить обложку") },
                                        onClick = {
                                            showMenu = false
                                            scope.launch {
                                                runCatching { app.api.deletePlaylistCover(collectionId) }
                                                    .onSuccess {
                                                        cover = null
                                                        app.cache.invalidateCollection(kind, collectionId)
                                                        app.cache.invalidateLibrary()
                                                        snackbarHost?.showSnackbar("Обложка удалена")
                                                        reload++
                                                    }
                                                    .onFailure {
                                                        snackbarHost?.showSnackbar("Не удалось удалить обложку")
                                                    }
                                            }
                                        },
                                    )
                                    if (tracks.isNotEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("Порядок и состав") },
                                            onClick = {
                                                showMenu = false
                                                draftTracks = tracks
                                                editingTracks = true
                                            },
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Переименовать") },
                                        onClick = {
                                            showMenu = false
                                            renameText = title
                                            renameError = null
                                            showRename = true
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Удалить плейлист",
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            deleteError = null
                                            showDelete = true
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            when {
                loading -> CollectionSkeleton()
                error != null -> ErrorState(
                    message = error ?: "Ошибка сети",
                    onRetry = { reload++ },
                    modifier = Modifier.fillMaxSize(),
                )
                editingTracks -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 8.dp,
                        bottom = 28.dp + miniPlayerBottom,
                    ),
                ) {
                    itemsIndexed(draftTracks, key = { _, t -> t.id }) { index, track ->
                        PlaylistEditTrackRow(
                            track = track,
                            api = app.api,
                            canMoveUp = index > 0 && !savingOrder,
                            canMoveDown = index < draftTracks.lastIndex && !savingOrder,
                            removing = removingTrackId == track.id,
                            onMoveUp = {
                                val next = draftTracks.toMutableList()
                                next[index] = next[index - 1].also { next[index - 1] = next[index] }
                                draftTracks = next
                            },
                            onMoveDown = {
                                val next = draftTracks.toMutableList()
                                next[index] = next[index + 1].also { next[index + 1] = next[index] }
                                draftTracks = next
                            },
                            onRemove = {
                                if (removingTrackId != null || savingOrder) return@PlaylistEditTrackRow
                                removingTrackId = track.id
                                scope.launch {
                                    runCatching { app.api.removePlaylistTrack(collectionId, track.id) }
                                        .onSuccess {
                                            draftTracks = draftTracks.filterNot { it.id == track.id }
                                            tracks = tracks.filterNot { it.id == track.id }
                                            meta = KwhalePlural.tracks(tracks.size)
                                            app.cache.invalidateCollection(kind, collectionId)
                                            app.cache.invalidateLibrary()
                                            snackbarHost?.showSnackbar("Трек удалён из плейлиста")
                                        }
                                        .onFailure {
                                            snackbarHost?.showSnackbar("Не удалось удалить трек")
                                        }
                                    removingTrackId = null
                                }
                            },
                        )
                    }
                }
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 8.dp,
                        bottom = 28.dp + miniPlayerBottom,
                    ),
                ) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CoverArt(
                                imageUrl = cover,
                                modifier = Modifier.size(if (kind == "artist") 220.dp else 180.dp),
                                shape = RoundedCornerShape(if (kind == "artist") 28.dp else 16.dp),
                                iconSize = 44,
                                seed = title,
                                titleText = if (kind == "playlist") title else null,
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
                    if (kind == "artist" && syncStatus?.status == "running") {
                        item {
                            Text(
                                text = "Обновляем дискографию…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 6.dp),
                            )
                        }
                    }
                    latestRelease?.let { release ->
                        item { SectionHeader("Последний релиз") }
                        item {
                            LatestReleaseCard(
                                album = release,
                                app = app,
                                onClick = { onOpenCollection("album", release.id) },
                            )
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
                            if (kind == "playlist" && isOwned) {
                                ActionItem(
                                    icon = Icons.Rounded.Add,
                                    label = "Добавить треки",
                                    enabled = !actionBusy,
                                    onClick = { showAddTracks = true },
                                )
                            } else {
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
                                    label = if (offlineActiveCount > 0) "Загрузка ($offlineActiveCount)" else "Скачать",
                                    enabled = !actionBusy && tracks.isNotEmpty(),
                                    onClick = { showDownloadSheet = true },
                                )
                            }
                            if (kind == "album" || kind == "artist") {
                                ActionItem(
                                    icon = Icons.Rounded.Share,
                                    label = "Поделиться",
                                    enabled = title.isNotBlank() && !actionBusy,
                                    onClick = { triggerCollectionShare() },
                                )
                            }
                        }
                    }
                    if (kind != "artist" && tracks.isNotEmpty() && (offlineDownloadedCount > 0 || offlineActiveCount > 0)) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = if (offlineActiveCount > 0) {
                                        "Офлайн-загрузка: $offlineDownloadedCount из ${tracks.size}"
                                    } else {
                                        "Доступно офлайн: $offlineDownloadedCount из ${tracks.size}"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                if (offlineActiveCount > 0) {
                                    val progress = offlineDownloadedCount.toFloat() / tracks.size.coerceAtLeast(1)
                                    LinearProgressIndicator(
                                        progress = { progress.coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }
                    item {
                        val collectionSurface = when (kind) {
                            "album" -> "album"
                            "artist" -> "artist"
                            "playlist" -> "playlist"
                            else -> "library"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Button(
                                onClick = {
                                    if (tracks.isNotEmpty()) {
                                        app.player.playQueue(
                                            tracks.map { it.toUi(app.api) },
                                            0,
                                            surface = collectionSurface,
                                        )
                                    }
                                },
                                enabled = tracks.isNotEmpty(),
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
                                            surface = collectionSurface,
                                        )
                                    }
                                },
                                enabled = tracks.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Rounded.Shuffle, null, Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Перемешать")
                            }
                        }
                    }
                    item {
                        SectionHeader(
                            title = if (kind == "artist") "Популярные треки" else "Треки",
                            action = if (kind == "artist") "Все" else null,
                            onAction = if (kind == "artist") {
                                { onOpenArtistAll("tracks") }
                            } else {
                                null
                            },
                        )
                    }
                    itemsIndexed(displayTracks, key = { index, track -> "${track.id}:$index" }) { index, track ->
                        val isLiked = app.reactions.isLiked(track.id, track.isLiked, likedOverrides)
                        val collectionSurface = when (kind) {
                            "album" -> "album"
                            "artist" -> "artist"
                            "playlist" -> "playlist"
                            else -> "library"
                        }
                        TrackRow(
                            track = track,
                            api = app.api,
                            liked = isLiked,
                            isCurrent = playerState.current?.remoteId == track.id,
                            onClick = {
                                app.player.playQueue(
                                    tracks.map { it.toUi(app.api) },
                                    index,
                                    surface = collectionSurface,
                                )
                            },
                            onToggleLike = {
                                app.reactions.toggleLike(
                                    track = track,
                                    api = app.api,
                                    surface = collectionSurface,
                                    onError = { msg -> scope.launch { snackbarHost?.showSnackbar(msg) } },
                                )
                                app.cache.invalidateCollection(kind, collectionId)
                            },
                            onOverflow = { onTrackMenu(track) },
                            number = index + 1,
                            modifier = Modifier.animateItem(),
                            onArtistClick = {
                                track.primaryArtistId()
                                    ?.takeIf { kind != "artist" || it != collectionId }
                                    ?.let { onOpenCollection("artist", it) }
                            },
                        )
                    }
                    if (albums.isNotEmpty() || kind == "artist") {
                        val gridCols = albumGridCols
                        val previewLimit = gridCols * 3

                        if (fullAlbums.isNotEmpty() || singlesAndEps.isEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Альбомы",
                                    action = if (kind == "artist") "Все" else null,
                                    onAction = if (kind == "artist") {
                                        { onOpenArtistAll("albums") }
                                    } else {
                                        null
                                    },
                                )
                            }
                            if (fullAlbums.isNotEmpty()) {
                                val albumRows = fullAlbums.take(previewLimit).chunked(gridCols)
                                items(albumRows, key = { row -> "alb:${row.first().id}" }) { rowAlbums ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 14.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        rowAlbums.forEach { album ->
                                            AlbumCard(
                                                album = album,
                                                api = app.api,
                                                onClick = { onOpenCollection("album", album.id) },
                                                fillWidth = true,
                                                onArtistClick = {
                                                    album.primaryArtistId()
                                                        ?.takeIf { kind != "artist" || it != collectionId }
                                                        ?.let { id -> onOpenCollection("artist", id) }
                                                },
                                                modifier = Modifier.weight(1f),
                                            )
                                        }
                                        repeat(gridCols - rowAlbums.size) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                        if (singlesAndEps.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Синглы и EP",
                                    action = if (kind == "artist") "Все" else null,
                                    onAction = if (kind == "artist") {
                                        { onOpenArtistAll("albums") }
                                    } else {
                                        null
                                    },
                                )
                            }
                            val singleRows = singlesAndEps.take(previewLimit).chunked(gridCols)
                            items(singleRows, key = { row -> "sng:${row.first().id}" }) { rowSingles ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    rowSingles.forEach { album ->
                                        AlbumCard(
                                            album = album,
                                            api = app.api,
                                            onClick = { onOpenCollection("album", album.id) },
                                            fillWidth = true,
                                            onArtistClick = {
                                                album.primaryArtistId()
                                                    ?.takeIf { kind != "artist" || it != collectionId }
                                                    ?.let { id -> onOpenCollection("artist", id) }
                                            },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    repeat(gridCols - rowSingles.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDownloadSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDownloadSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
            ) {
                Text(
                    text = "Куда скачать?",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                ListItem(
                    headlineContent = { Text("На устройство (офлайн)") },
                    supportingContent = {
                        Text("Сохранить в память устройства (${KwhalePlural.tracks(tracks.size)})")
                    },
                    leadingContent = {
                        Icon(Icons.Rounded.DownloadForOffline, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        showDownloadSheet = false
                        downloadAllToDevice()
                    },
                )
                ListItem(
                    headlineContent = { Text("На сервер (в библиотеку)") },
                    supportingContent = {
                        Text("Добавить все треки в вашу медиатеку на сервере")
                    },
                    leadingContent = {
                        Icon(Icons.Rounded.CloudDownload, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        showDownloadSheet = false
                        downloadAllToServer()
                    },
                )
            }
        }
    }

    if (showAddTracks) {
        AddTracksSheet(
            app = app,
            playlistId = collectionId,
            existingIds = tracks.map { it.id }.toSet(),
            onDismiss = { showAddTracks = false },
            onAdded = { added ->
                tracks = (tracks + added).distinctBy { it.id }
                meta = KwhalePlural.tracks(tracks.size)
                app.cache.invalidateCollection(kind, collectionId)
                app.cache.invalidateLibrary()
                showAddTracks = false
                scope.launch {
                    snackbarHost?.showSnackbar("Добавлено треков: ${added.size}")
                }
            },
        )
    }

    if (showRename) {
        AlertDialog(
            onDismissRequest = { if (!renameBusy) showRename = false },
            title = { Text("Переименовать плейлист") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = {
                            renameText = it
                            renameError = null
                        },
                        singleLine = true,
                        enabled = !renameBusy,
                        isError = renameError != null,
                        label = { Text("Название") },
                    )
                    renameError?.let { msg ->
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !renameBusy && renameText.trim().isNotEmpty(),
                    onClick = {
                        val trimmed = renameText.trim()
                        if (trimmed.isEmpty()) return@TextButton
                        renameBusy = true
                        renameError = null
                        scope.launch {
                            runCatching {
                                app.api.updatePlaylist(collectionId, PlaylistUpdate(title = trimmed))
                            }.onSuccess {
                                title = trimmed
                                showRename = false
                                app.cache.invalidateCollection(kind, collectionId)
                                app.cache.invalidateLibrary()
                                reload++
                            }.onFailure { err ->
                                renameError = err.message ?: "Не удалось переименовать плейлист"
                            }
                            renameBusy = false
                        }
                    },
                ) {
                    if (renameBusy) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Сохранить")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !renameBusy,
                    onClick = { showRename = false },
                ) { Text("Отмена") }
            },
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { if (!deleteBusy) showDelete = false },
            title = { Text("Удалить плейлист?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Плейлист будет удалён безвозвратно.")
                    deleteError?.let { msg ->
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !deleteBusy,
                    onClick = {
                        deleteBusy = true
                        deleteError = null
                        scope.launch {
                            runCatching { app.api.deletePlaylist(collectionId) }
                                .onSuccess {
                                    showDelete = false
                                    app.cache.invalidateCollection(kind, collectionId)
                                    app.cache.invalidateLibrary()
                                    onBack()
                                }
                                .onFailure { err ->
                                    deleteError = err.message ?: "Не удалось удалить плейлист"
                                }
                            deleteBusy = false
                        }
                    },
                ) {
                    if (deleteBusy) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Удалить", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !deleteBusy,
                    onClick = { showDelete = false },
                ) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun PlaylistEditTrackRow(
    track: ApiTrack,
    api: org.dueattendant149.kwhale.network.KwhaleApi,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    removing: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverArt(
            imageUrl = track.imageUrl(api, size = "sm"),
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(8.dp),
            seed = track.title,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artistLine(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onMoveUp, enabled = canMoveUp && !removing) {
            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Переместить выше")
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown && !removing) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Переместить ниже")
        }
        if (removing) {
            CircularProgressIndicator(
                modifier = Modifier.padding(12.dp).size(20.dp),
                strokeWidth = 2.dp,
            )
        } else {
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.Close, contentDescription = "Убрать из плейлиста")
            }
        }
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
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 12.dp, vertical = 10.dp),
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
            .padding(horizontal = 20.dp),
    ) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTracksSheet(
    app: KwhaleApp,
    playlistId: String,
    existingIds: Set<String>,
    onDismiss: () -> Unit,
    onAdded: (List<ApiTrack>) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetSnackbar = remember { SnackbarHostState() }
    val cached = remember {
        app.cache.read(ContentCache.LIBRARY, LibrarySnapshot.serializer())?.tracks.orEmpty()
    }
    var all by remember { mutableStateOf(cached) }
    var ready by remember { mutableStateOf(all.isNotEmpty()) }
    var loading by remember { mutableStateOf(all.isEmpty()) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(reloadKey) {
        if (all.isNotEmpty() && app.cache.isFresh(ContentCache.LIBRARY, 30_000)) {
            ready = true
            loading = false
            return@LaunchedEffect
        }
        loading = all.isEmpty()
        runCatching { app.api.libraryTracks(limit = 200).items }
            .onSuccess { fresh ->
                if (fresh.isNotEmpty()) all = fresh
                loadError = null
                ready = true
            }
            .onFailure {
                loadError = it.message ?: "Ошибка сети"
                if (all.isEmpty()) ready = true
            }
        loading = false
    }

    var filtered by remember { mutableStateOf<List<ApiTrack>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    LaunchedEffect(query, all, existingIds) {
        if (query.isBlank()) {
            searching = false
            filtered = all.filter { track -> track.id !in existingIds }
        } else {
            searching = true
            delay(250)
            filtered = runCatching {
                app.api.libraryTracks(limit = 100, query = query.trim()).items
            }.getOrDefault(emptyList()).filter { track -> track.id !in existingIds }
            searching = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!saving) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Добавить треки", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Поиск в библиотеке") },
                )
                when {
                    !ready || loading || searching -> Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                    loadError != null && all.isEmpty() -> Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Не удалось загрузить библиотеку",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { reloadKey++ }) { Text("Повторить") }
                    }
                    filtered.isEmpty() -> Text(
                        if (all.isEmpty()) "В библиотеке пока нет треков" else "Ничего не найдено",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 20.dp),
                    )
                    else -> LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                        items(filtered, key = { it.id }) { track ->
                            val checked = track.id in selected
                            ListItem(
                                headlineContent = {
                                    Text(
                                        track.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        track.artistLine(),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                leadingContent = {
                                    CoverArt(
                                        imageUrl = track.imageUrl(app.api, size = "sm"),
                                        modifier = Modifier.size(44.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        seed = track.title,
                                    )
                                },
                                trailingContent = {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = {
                                            selected = if (checked) selected - track.id else selected + track.id
                                        },
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable {
                                    selected = if (checked) selected - track.id else selected + track.id
                                },
                            )
                        }
                    }
                }
                Button(
                    onClick = {
                        if (selected.isEmpty() || saving) return@Button
                        saving = true
                        scope.launch {
                            runCatching { app.api.addPlaylistTracks(playlistId, selected.toList()) }
                                .onSuccess { playlist ->
                                    val added = playlist.tracks
                                        .filter { it.id in selected }
                                        .ifEmpty { all.filter { it.id in selected } }
                                    onAdded(added)
                                }
                                .onFailure {
                                    sheetSnackbar.showSnackbar("Не удалось добавить треки")
                                }
                            saving = false
                        }
                    },
                    enabled = selected.isNotEmpty() && !saving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (saving) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (selected.isEmpty()) "Выберите треки" else "Добавить (${selected.size})")
                    }
                }
            }
            SnackbarHost(
                hostState = sheetSnackbar,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun LatestReleaseCard(
    album: Album,
    app: KwhaleApp,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverArt(
                imageUrl = album.imageUrl(app.api),
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(12.dp),
                iconSize = 24,
                seed = album.title,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = album.releaseDate?.take(10) ?: album.year?.toString().orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
