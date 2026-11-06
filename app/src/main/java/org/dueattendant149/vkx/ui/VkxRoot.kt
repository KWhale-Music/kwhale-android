package org.dueattendant149.vkx.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.dueattendant149.vkx.VkxApp
import org.dueattendant149.vkx.data.toUi
import org.dueattendant149.vkx.design.VkxAccents
import org.dueattendant149.vkx.download.Downloads
import org.dueattendant149.vkx.model.Track as UiTrack
import org.dueattendant149.vkx.network.model.Album
import org.dueattendant149.vkx.network.model.Artist
import org.dueattendant149.vkx.network.model.Playlist
import org.dueattendant149.vkx.network.model.Track as ApiTrack
import org.dueattendant149.vkx.ui.components.CoverArt
import org.dueattendant149.vkx.ui.components.artistLine
import org.dueattendant149.vkx.ui.screens.AppearanceScreen
import org.dueattendant149.vkx.ui.screens.CollectionScreen
import org.dueattendant149.vkx.ui.screens.DownloadsScreen
import org.dueattendant149.vkx.ui.screens.HistoryScreen
import org.dueattendant149.vkx.ui.screens.HomeScreen
import org.dueattendant149.vkx.ui.screens.LibraryAllScreen
import org.dueattendant149.vkx.ui.screens.LibraryScreen
import org.dueattendant149.vkx.ui.screens.ProfileScreen
import org.dueattendant149.vkx.ui.screens.SearchScreen
import org.dueattendant149.vkx.ui.screens.SettingsScreen
import org.dueattendant149.vkx.ui.sheets.LyricsSheet
import org.dueattendant149.vkx.ui.sheets.QueueSheet
import org.dueattendant149.vkx.ui.sheets.TrackMenuSheet

private val tabRoutes = setOf("home", "search", "library", "profile")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VkxRoot(
    app: VkxApp,
    themeMode: org.dueattendant149.vkx.design.ThemeMode,
    accentId: Int,
    monet: Boolean,
    onThemeChanged: (org.dueattendant149.vkx.design.ThemeMode, Int, Boolean) -> Unit,
    onLogout: () -> Unit,
) {
    val navController = rememberNavController()
    val playerState by app.player.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showPlayer by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var menuTrack by remember { mutableStateOf<ApiTrack?>(null) }
    var addToPlaylistTrack by remember { mutableStateOf<ApiTrack?>(null) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val showNav = route in tabRoutes
    val showMini = playerState.current != null

    LaunchedEffect(playerState.current) {
        if (playerState.current == null) showPlayer = false
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showMini || showNav) {
                Column(
                    modifier = if (showNav) Modifier else Modifier.navigationBarsPadding(),
                ) {
                    if (showMini) {
                        MiniPlayer(
                            track = playerState.current,
                            isPlaying = playerState.isPlaying,
                            progress = playerState.progress,
                            onOpen = { showPlayer = true },
                            onToggle = app.player::toggle,
                            onNext = app.player::next,
                        )
                    }
                    if (showNav) {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                            NavigationBarItem(
                                selected = route == "home",
                                onClick = { navController.navigateTop("home") },
                                icon = { Icon(Icons.Rounded.Home, null) },
                                label = { Text("Главная") },
                            )
                            NavigationBarItem(
                                selected = route == "search",
                                onClick = { navController.navigateTop("search") },
                                icon = { Icon(Icons.Rounded.Search, null) },
                                label = { Text("Поиск") },
                            )
                            NavigationBarItem(
                                selected = route == "library",
                                onClick = { navController.navigateTop("library") },
                                icon = { Icon(Icons.Rounded.LibraryMusic, null) },
                                label = { Text("Библиотека") },
                            )
                            NavigationBarItem(
                                selected = route == "profile",
                                onClick = { navController.navigateTop("profile") },
                                icon = { Icon(Icons.Rounded.Person, null) },
                                label = { Text("Профиль") },
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("home") {
                HomeScreen(
                    app = app,
                    playerState = playerState,
                    onOpenPlayer = { showPlayer = true },
                    onOpenCollection = { kind, id -> navController.navigate("collection/$kind/$id") },
                    onOpenSearch = { navController.navigateTop("search") },
                    onSeeAll = { kind -> navController.navigate("all/$kind") },
                    onTrackMenu = { menuTrack = it },
                )
            }
            composable("search") {
                SearchScreen(
                    app = app,
                    onOpenCollection = { kind, id -> navController.navigate("collection/$kind/$id") },
                    onTrackMenu = { menuTrack = it },
                )
            }
            composable("library") {
                LibraryScreen(
                    app = app,
                    onOpenCollection = { kind, id -> navController.navigate("collection/$kind/$id") },
                    onSeeAll = { kind -> navController.navigate("all/$kind") },
                    onTrackMenu = { menuTrack = it },
                    onDownloads = { navController.navigate("downloads") },
                    onSearch = { navController.navigateTop("search") },
                )
            }
            composable("profile") {
                ProfileScreen(
                    app = app,
                    onSettings = { navController.navigate("settings") },
                    onAppearance = { navController.navigate("appearance") },
                    onDownloads = { navController.navigate("downloads") },
                    onHistory = { navController.navigate("history") },
                    onLogout = onLogout,
                )
            }
            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onAppearance = { navController.navigate("appearance") },
                    onDownloads = { navController.navigate("downloads") },
                    onHistory = { navController.navigate("history") },
                )
            }
            composable("appearance") {
                AppearanceScreen(
                    currentMode = themeMode,
                    currentAccentId = accentId,
                    monet = monet,
                    onBack = { navController.popBackStack() },
                    onChanged = onThemeChanged,
                )
            }
            composable("downloads") {
                DownloadsScreen(
                    app = app,
                    onBack = { navController.popBackStack() },
                    onPlay = { queue, index -> app.player.playQueue(queue, index) },
                )
            }
            composable("history") {
                HistoryScreen(
                    app = app,
                    onBack = { navController.popBackStack() },
                    onPlay = { queue, index -> app.player.playQueue(queue, index) },
                    onTrackMenu = { menuTrack = it },
                )
            }
            composable("all/{kind}") { entry ->
                LibraryAllScreen(
                    app = app,
                    kind = entry.arguments?.getString("kind").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onOpenCollection = { kind, id -> navController.navigate("collection/$kind/$id") },
                )
            }
            composable("collection/{kind}/{id}") { entry ->
                CollectionScreen(
                    app = app,
                    kind = entry.arguments?.getString("kind").orEmpty(),
                    collectionId = entry.arguments?.getString("id").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onOpenCollection = { kind, id -> navController.navigate("collection/$kind/$id") },
                    onTrackMenu = { menuTrack = it },
                )
            }
        }
    }

    if (showPlayer && playerState.current != null) {
        ModalBottomSheet(
            onDismissRequest = { showPlayer = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.Transparent,
            dragHandle = null,
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        ) {
            PlayerScreen(
                app = app,
                state = playerState,
                onClose = { showPlayer = false },
                onOpenArtist = { id -> showPlayer = false; navController.navigate("collection/artist/$id") },
                onOpenAlbum = { id -> showPlayer = false; navController.navigate("collection/album/$id") },
                onLyrics = { showLyrics = true },
                onQueue = { showQueue = true },
                onMenu = { showPlayer = false; menuTrack = it },
            )
        }
    }

    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            QueueSheet(
                app = app,
                onDismiss = { showQueue = false },
            )
        }
    }

    if (showLyrics && playerState.current != null) {
        ModalBottomSheet(
            onDismissRequest = { showLyrics = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            LyricsSheet(
                app = app,
                track = playerState.current!!,
                positionMs = playerState.positionMs,
                onDismiss = { showLyrics = false },
            )
        }
    }

    menuTrack?.let { track ->
        ModalBottomSheet(
            onDismissRequest = { menuTrack = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            TrackMenuSheet(
                app = app,
                track = track,
                onDismiss = { menuTrack = null },
                onAddToPlaylist = {
                    addToPlaylistTrack = track
                    menuTrack = null
                },
                onOpenArtist = { id -> menuTrack = null; navController.navigate("collection/artist/$id") },
                onOpenAlbum = { id -> menuTrack = null; navController.navigate("collection/album/$id") },
                onShare = { shareTrack(context, track) },
                onDownload = {
                    val ui = track.toUi(app.api)
                    Downloads.enqueue(context, ui)
                    Toast.makeText(context, "Скачивание начато", Toast.LENGTH_SHORT).show()
                    menuTrack = null
                },
            )
        }
    }

    addToPlaylistTrack?.let { track ->
        ModalBottomSheet(
            onDismissRequest = { addToPlaylistTrack = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            AddToPlaylistSheet(
                app = app,
                track = track,
                onDismiss = { addToPlaylistTrack = null },
                onDone = { addToPlaylistTrack = null },
            )
        }
    }
}

@Composable
private fun MiniPlayer(
    track: UiTrack?,
    isPlaying: Boolean,
    progress: Float,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
) {
    if (track == null) return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoverArt(
                    imageUrl = track.coverUrl,
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    iconSize = 18,
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Пауза" else "Играть",
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Следующий")
                }
            }
        }
    }
}

@Composable
private fun AddToPlaylistSheet(
    app: VkxApp,
    track: ApiTrack,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        playlists = runCatching { app.api.playlists(limit = 100).items }.getOrDefault(emptyList())
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
    ) {
        Text(
            text = "В плейлист",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        if (loading) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
            ) {
                itemsIndexed(playlists, key = { _, p -> p.id }) { _, playlist ->
                    ListItem(
                        headlineContent = { Text(playlist.title) },
                        supportingContent = { Text("${playlist.trackCount} треков") },
                        leadingContent = {
                            CoverArt(
                                imageUrl = playlist.cover?.let { app.api.imageUrl(it.id) },
                                modifier = Modifier.size(44.dp),
                                shape = RoundedCornerShape(8.dp),
                            )
                        },
                        trailingContent = {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable {
                            scope.launch {
                                runCatching { app.api.addPlaylistTracks(playlist.id, listOf(track.id)) }
                                Toast.makeText(context, "Добавлено в плейлист", Toast.LENGTH_SHORT).show()
                                onDone()
                            }
                        },
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("Новый плейлист") },
                        leadingContent = { Icon(Icons.Rounded.Add, null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { showCreate = true },
                    )
                }
            }
        }
    }

    if (showCreate) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("Новый плейлист") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text("Название") },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newName.trim()
                        if (name.isNotEmpty()) {
                            scope.launch {
                                runCatching {
                                    val created = app.api.createPlaylist(
                                        org.dueattendant149.vkx.network.model.PlaylistCreate(name),
                                    )
                                    app.api.addPlaylistTracks(created.id, listOf(track.id))
                                }
                                showCreate = false
                                Toast.makeText(context, "Добавлено в плейлист", Toast.LENGTH_SHORT).show()
                                onDone()
                            }
                        }
                    },
                ) { Text("Создать") }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("Отмена") }
            },
        )
    }
}

private fun shareTrack(context: android.content.Context, track: ApiTrack) {
    val text = buildString {
        append(track.artistLine())
        append(" — ")
        append(track.title)
        track.album?.title?.let { append(" (").append(it).append(")") }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Поделиться"))
}

private fun NavHostController.navigateTop(route: String) {
    navigate(route) {
        popUpTo("home") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
