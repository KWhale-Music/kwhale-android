package org.dueattendant149.kwhale.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.runtime.CompositionLocalProvider
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
import org.dueattendant149.kwhale.KwhaleApp
import org.dueattendant149.kwhale.data.toUi
import org.dueattendant149.kwhale.design.KwhaleAccents
import org.dueattendant149.kwhale.design.KwhalePlural
import org.dueattendant149.kwhale.design.KwhaleSpacing
import org.dueattendant149.kwhale.design.KwhaleMotion
import org.dueattendant149.kwhale.design.LocalMiniPlayerBottomPadding
import org.dueattendant149.kwhale.download.Downloads
import org.dueattendant149.kwhale.model.Track as UiTrack
import org.dueattendant149.kwhale.network.model.Album
import org.dueattendant149.kwhale.network.model.Artist
import org.dueattendant149.kwhale.network.model.Playlist
import org.dueattendant149.kwhale.network.model.Track as ApiTrack
import org.dueattendant149.kwhale.ui.components.CoverArt
import org.dueattendant149.kwhale.ui.components.artistLine
import org.dueattendant149.kwhale.ui.screens.AppearanceScreen
import org.dueattendant149.kwhale.ui.screens.CollectionScreen
import org.dueattendant149.kwhale.ui.screens.DownloadsScreen
import org.dueattendant149.kwhale.ui.screens.HistoryScreen
import org.dueattendant149.kwhale.ui.screens.HomeScreen
import org.dueattendant149.kwhale.ui.screens.LibraryAllScreen
import org.dueattendant149.kwhale.ui.screens.LibraryScreen
import org.dueattendant149.kwhale.ui.screens.ProfileScreen
import org.dueattendant149.kwhale.ui.screens.SearchScreen
import org.dueattendant149.kwhale.ui.screens.SettingsScreen
import org.dueattendant149.kwhale.ui.sheets.LyricsSheet
import org.dueattendant149.kwhale.ui.sheets.QueueSheet
import org.dueattendant149.kwhale.ui.sheets.TrackMenuSheet

private val tabRoutes = setOf("home", "search", "library", "profile")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KwhaleRoot(
    app: KwhaleApp,
    themeMode: org.dueattendant149.kwhale.design.ThemeMode,
    accentId: Int,
    monet: Boolean,
    onThemeChanged: (org.dueattendant149.kwhale.design.ThemeMode, Int, Boolean) -> Unit,
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

    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(
            LocalMiniPlayerBottomPadding provides if (showMini) KwhaleSpacing.miniPlayer else 0.dp,
        ) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
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
                },
            ) { innerPadding ->
        // Cross-fade + subtle scale on route changes. Tab switches stay instant
        // (no slide) so the bottom bar does not feel like it flickers.
        val navTransition = fadeIn(
            animationSpec = tween(KwhaleMotion.durationStd, easing = KwhaleMotion.emphasizedDecelerate),
        ) + scaleIn(
            initialScale = 0.98f,
            animationSpec = tween(KwhaleMotion.durationStd, easing = KwhaleMotion.emphasizedDecelerate),
        )
        val navExit = fadeOut(
            animationSpec = tween(KwhaleMotion.durationFast, easing = KwhaleMotion.emphasizedAccelerate),
        )

        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
            enterTransition = { navTransition },
            exitTransition = { navExit },
            popEnterTransition = { navTransition },
            popExitTransition = { navExit },
        ) {
            composable("home") {
                HomeScreen(
                    app = app,
                    onOpenSearch = { navController.navigateTop("search") },
                    onSeeAll = { kind -> navController.navigate("all/$kind") },
                    onTrackMenu = { menuTrack = it },
                    onOpenProfile = { navController.navigateTop("profile") },
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
        }
        if (showMini) {
            val navBarInset = WindowInsets.navigationBars
                .asPaddingValues()
                .calculateBottomPadding()
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        bottom = navBarInset +
                            (if (showNav) KwhaleSpacing.navBar else 0.dp) +
                            KwhaleSpacing.xs,
                    ),
            ) {
                MiniPlayer(
                    track = playerState.current,
                    isPlaying = playerState.isPlaying,
                    progress = playerState.progress,
                    onOpen = { showPlayer = true },
                    onToggle = app.player::toggle,
                    onNext = app.player::next,
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

    // Slides in when a queue starts instead of popping into place.
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        ) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KwhaleSpacing.xs, vertical = KwhaleSpacing.xxs)
                .clickable(onClick = onOpen),
            shape = RoundedCornerShape(28.dp),
            // Floating over content, not in a bottom bar. Keep the capsule in
            // the theme's surface family: a border + shadow separates it from
            // the page without the stark inverseSurface (white) contrast.
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            ),
            shadowElevation = 8.dp,
        ) {
            Column {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // content below is unchanged except spacing tokens
                    MiniPlayerContent(
                        track = track,
                        isPlaying = isPlaying,
                        onToggle = onToggle,
                        onNext = onNext,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerContent(
    track: UiTrack,
    isPlaying: Boolean,
    onToggle: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = KwhaleSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverArt(
            imageUrl = track.coverUrl,
            modifier = Modifier.size(46.dp),
            shape = RoundedCornerShape(10.dp),
            iconSize = 18,
            seed = track.title,
        )
        Spacer(Modifier.width(KwhaleSpacing.sm))
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

@Composable
private fun AddToPlaylistSheet(
    app: KwhaleApp,
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
                        supportingContent = { Text(KwhalePlural.tracks(playlist.trackCount)) },
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
                                    .onSuccess {
                                        Toast.makeText(context, "Добавлено в плейлист", Toast.LENGTH_SHORT).show()
                                        onDone()
                                    }
                                    .onFailure {
                                        Toast.makeText(context, "Не удалось добавить в плейлист", Toast.LENGTH_SHORT).show()
                                    }
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
                                        org.dueattendant149.kwhale.network.model.PlaylistCreate(name),
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
