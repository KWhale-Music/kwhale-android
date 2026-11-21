package org.dueattendant149.kwhale.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.dueattendant149.kwhale.KwhaleApp
import org.dueattendant149.kwhale.data.toUi
import org.dueattendant149.kwhale.design.KwhaleMotion
import org.dueattendant149.kwhale.design.KwhalePlural
import org.dueattendant149.kwhale.design.KwhaleSpacing
import org.dueattendant149.kwhale.design.LocalMiniPlayerBottomPadding
import org.dueattendant149.kwhale.design.rememberReducedMotionEnabled
import org.dueattendant149.kwhale.download.Downloads
import org.dueattendant149.kwhale.model.Track as UiTrack
import org.dueattendant149.kwhale.network.model.Playlist
import org.dueattendant149.kwhale.network.model.ShareKind
import org.dueattendant149.kwhale.network.model.Track as ApiTrack
import org.dueattendant149.kwhale.BuildConfig
import org.dueattendant149.kwhale.ui.components.AppUpdateDialog
import org.dueattendant149.kwhale.ui.components.CoverArt
import org.dueattendant149.kwhale.ui.components.ErrorState
import org.dueattendant149.kwhale.ui.components.LocalSnackbarHost
import org.dueattendant149.kwhale.ui.components.artistLine
import org.dueattendant149.kwhale.ui.components.imageUrl
import org.dueattendant149.kwhale.ui.screens.AppearanceScreen
import org.dueattendant149.kwhale.ui.screens.ArtistAllScreen
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
import org.dueattendant149.kwhale.ui.sheets.ShareSheet
import org.dueattendant149.kwhale.ui.sheets.ShareTarget
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
    openQueue: Boolean = false,
    onQueueOpened: () -> Unit = {},
    openPlayer: Boolean = false,
    onPlayerOpened: () -> Unit = {},
    deepLinkUri: String? = null,
    onDeepLinkHandled: () -> Unit = {},
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
    var shareTarget by remember { mutableStateOf<ShareTarget?>(null) }
    var deepLinkJob by remember { mutableStateOf<Job?>(null) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val showNav = route in tabRoutes
    val showMini = playerState.current != null

    val snackbarHostState = remember { SnackbarHostState() }
    val playerSnackbarHostState = remember { SnackbarHostState() }

    fun showMessage(msg: String) {
        val host = if (showPlayer && playerState.current != null) playerSnackbarHostState else snackbarHostState
        scope.launch {
            host.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    var miniTrack by remember { mutableStateOf(playerState.current) }
    val miniVisible = remember { MutableTransitionState(showMini) }
    LaunchedEffect(playerState.current) {
        if (playerState.current != null) {
            miniTrack = playerState.current
        }
        miniVisible.targetState = showMini
    }
    LaunchedEffect(miniVisible.currentState, miniVisible.targetState, showMini) {
        if (!showMini && !miniVisible.currentState && !miniVisible.targetState) {
            miniTrack = null
        }
    }

    LaunchedEffect(playerState.current) {
        if (playerState.current == null) showPlayer = false
    }
    LaunchedEffect(openQueue) {
        if (openQueue) {
            showQueue = true
            onQueueOpened()
        }
    }
    LaunchedEffect(openPlayer) {
        if (openPlayer) {
            showPlayer = true
            onPlayerOpened()
        }
    }
    LaunchedEffect(deepLinkUri) {
        val raw = deepLinkUri ?: return@LaunchedEffect
        onDeepLinkHandled()
        val parsed = runCatching { Uri.parse(raw) }.getOrNull() ?: return@LaunchedEffect
        val host = parsed.host?.lowercase().orEmpty()
        val firstSeg = parsed.pathSegments.firstOrNull()?.trim().orEmpty()
        if (parsed.scheme == "kwhale" && firstSeg.isNotBlank()) {
            // Run on composition-lifetime scope so clearing deepLinkUri via onDeepLinkHandled()
            // does not cancel in-flight API calls mid-request.
            deepLinkJob?.cancel()
            deepLinkJob = scope.launch {
                suspend fun <T> safeCall(block: suspend () -> T): T? = try {
                    block()
                } catch (ce: CancellationException) {
                    throw ce
                } catch (_: Throwable) {
                    null
                }
                val (kind, targetId) = if (host == "share") {
                    val resolved = safeCall { app.api.shareDetails(firstSeg) }
                    if (resolved == null) {
                        showMessage("Ссылка не найдена или устарела")
                        return@launch
                    }
                    resolved.kind to resolved.entityId
                } else {
                    val parsedKind = when (host) {
                        "track" -> ShareKind.TRACK
                        "album" -> ShareKind.ALBUM
                        "artist" -> ShareKind.ARTIST
                        else -> null
                    } ?: return@launch
                    parsedKind to firstSeg
                }
                when (kind) {
                    ShareKind.TRACK -> {
                        val apiTrack = safeCall { app.api.track(targetId) }
                        if (apiTrack == null) {
                            showMessage("Трек не найден")
                        } else {
                            app.player.playQueue(listOf(apiTrack.toUi(app.api)), 0, surface = "external")
                            showPlayer = true
                        }
                    }
                    ShareKind.ALBUM -> {
                        showPlayer = false
                        navController.navigate("collection/album/$targetId") { launchSingleTop = true }
                    }
                    ShareKind.ARTIST -> {
                        showPlayer = false
                        navController.navigate("collection/artist/$targetId") { launchSingleTop = true }
                    }
                }
            }
        }
    }
    // In-app updater: silent check at startup; the dialog appears only when a
    // newer versionCode is published on the server.
    LaunchedEffect(Unit) {
        app.updater.checkOnStartup()
    }
    AppUpdateDialog(updater = app.updater, currentVersionName = BuildConfig.VERSION_NAME)

    val configuration = LocalConfiguration.current
    val useRail = configuration.screenWidthDp >= 600
    val hasBottomBar = showNav && !useRail
    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val miniBottomPadding = (if (hasBottomBar) 0.dp else navBarInset) +
        (if (showMini) KwhaleSpacing.miniPlayer else 0.dp)
    val reducedMotion = rememberReducedMotionEnabled()

    Row(Modifier.fillMaxSize()) {
        if (useRail && showNav) {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Spacer(Modifier.statusBarsPadding().height(KwhaleSpacing.md))
                NavigationRailItem(
                    selected = route == "home",
                    onClick = { navController.navigateTop("home") },
                    icon = { Icon(Icons.Rounded.Home, null) },
                    label = { Text("Главная") },
                )
                NavigationRailItem(
                    selected = route == "search",
                    onClick = { navController.navigateTop("search") },
                    icon = { Icon(Icons.Rounded.Search, null) },
                    label = { Text("Поиск") },
                )
                NavigationRailItem(
                    selected = route == "library",
                    onClick = { navController.navigateTop("library") },
                    icon = { Icon(Icons.Rounded.LibraryMusic, null) },
                    label = { Text("Библиотека") },
                )
                NavigationRailItem(
                    selected = route == "profile",
                    onClick = { navController.navigateTop("profile") },
                    icon = { Icon(Icons.Rounded.Person, null) },
                    label = { Text("Профиль") },
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
        ) {
            CompositionLocalProvider(
                LocalMiniPlayerBottomPadding provides miniBottomPadding,
                LocalSnackbarHost provides snackbarHostState,
            ) {
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = {
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.padding(bottom = miniBottomPadding),
                        )
                    },
                    bottomBar = {
                        if (hasBottomBar) {
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
                    // Two different motions:
                    //  * tab → tab: fade-through (quick exit, calm enter), no slide/scale;
                    //  * tab → detail: a short horizontal push so hierarchy is obvious.
                    fun isTab(entry: androidx.navigation.NavBackStackEntry?): Boolean =
                        entry?.destination?.route?.let { it in tabRoutes } == true

                    val calmEnter = fadeIn(
                        tween(KwhaleMotion.durationStd, easing = KwhaleMotion.emphasizedDecelerate),
                    )
                    val quickExit = fadeOut(
                        tween(KwhaleMotion.durationFast, easing = KwhaleMotion.emphasizedAccelerate),
                    )
                    // Tab → tab is a true fade-through: the outgoing screen fades
                    // out completely (180 ms) BEFORE the incoming one starts
                    // (delayed 180 ms), so the two are never semi-visible at the
                    // same time — the old parallel fades were the «растворение».
                    val fadeThroughEnter = fadeIn(
                        tween(
                            KwhaleMotion.durationStd,
                            delayMillis = KwhaleMotion.durationFast,
                            easing = KwhaleMotion.emphasizedDecelerate,
                        ),
                    ) + scaleIn(
                        initialScale = 1.02f,
                        animationSpec = tween(
                            KwhaleMotion.durationStd,
                            delayMillis = KwhaleMotion.durationFast,
                            easing = KwhaleMotion.emphasizedDecelerate,
                        ),
                    )
                    val pushEnter = if (reducedMotion) {
                        calmEnter
                    } else {
                        slideInHorizontally(
                            animationSpec = tween(
                                KwhaleMotion.durationEmphasized,
                                easing = KwhaleMotion.emphasizedDecelerate,
                            ),
                            initialOffsetX = { it / 10 },
                        ) + calmEnter
                    }
                    val pushExit = if (reducedMotion) {
                        quickExit
                    } else {
                        slideOutHorizontally(
                            animationSpec = tween(
                                KwhaleMotion.durationFast,
                                easing = KwhaleMotion.emphasizedAccelerate,
                            ),
                            targetOffsetX = { it / 14 },
                        ) + quickExit
                    }
                    val popEnter = if (reducedMotion) {
                        calmEnter
                    } else {
                        slideInHorizontally(
                            animationSpec = tween(
                                KwhaleMotion.durationStd,
                                easing = KwhaleMotion.emphasizedDecelerate,
                            ),
                            initialOffsetX = { -it / 10 },
                        ) + calmEnter
                    }
                    val popExit = if (reducedMotion) {
                        quickExit
                    } else {
                        slideOutHorizontally(
                            animationSpec = tween(
                                KwhaleMotion.durationFast,
                                easing = KwhaleMotion.emphasizedAccelerate,
                            ),
                            targetOffsetX = { it / 10 },
                        ) + quickExit
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding),
                        enterTransition = {
                            if (reducedMotion) EnterTransition.None
                            else if (isTab(initialState) && isTab(targetState)) fadeThroughEnter else pushEnter
                        },
                        exitTransition = {
                            if (reducedMotion) ExitTransition.None
                            else if (isTab(initialState) && isTab(targetState)) quickExit else pushExit
                        },
                        popEnterTransition = {
                            if (reducedMotion) EnterTransition.None
                            else if (isTab(initialState) && isTab(targetState)) calmEnter else popEnter
                        },
                        popExitTransition = {
                            if (reducedMotion) ExitTransition.None
                            else if (isTab(initialState) && isTab(targetState)) quickExit else popExit
                        },
                    ) {
            composable("home") {
                HomeScreen(
                    app = app,
                    onOpenSearch = { navController.navigateTop("search") },
                    onSeeAll = { kind ->
                        if (kind == "recent") navController.navigate("history")
                        else navController.navigate("all/$kind")
                    },
                    onTrackMenu = { menuTrack = it },
                    onOpenProfile = { navController.navigateTop("profile") },
                    onOpenCollection = { kind, id -> navController.navigate("collection/$kind/$id") },
                    onDownloads = { navController.navigate("downloads") },
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
                    app = app,
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
                    onPlay = { queue, index -> app.player.playQueue(queue, index, surface = "library") },
                )
            }
            composable("history") {
                HistoryScreen(
                    app = app,
                    onBack = { navController.popBackStack() },
                    onPlay = { queue, index -> app.player.playQueue(queue, index, surface = "home") },
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
                val collectionKind = entry.arguments?.getString("kind").orEmpty()
                val collectionId = entry.arguments?.getString("id").orEmpty()
                CollectionScreen(
                    app = app,
                    kind = collectionKind,
                    collectionId = collectionId,
                    onBack = { navController.popBackStack() },
                    onOpenCollection = { kind, id -> navController.navigate("collection/$kind/$id") },
                    onTrackMenu = { menuTrack = it },
                    onOpenArtistAll = { tab ->
                        navController.navigate("artist/$collectionId/$tab")
                    },
                    onShare = { shareTarget = it },
                )
            }
            composable("artist/{id}/tracks") { entry ->
                ArtistAllScreen(
                    app = app,
                    artistId = entry.arguments?.getString("id").orEmpty(),
                    initialTab = 0,
                    onBack = { navController.popBackStack() },
                    onOpenCollection = { kind, id -> navController.navigate("collection/$kind/$id") },
                    onTrackMenu = { menuTrack = it },
                )
            }
            composable("artist/{id}/albums") { entry ->
                ArtistAllScreen(
                    app = app,
                    artistId = entry.arguments?.getString("id").orEmpty(),
                    initialTab = 1,
                    onBack = { navController.popBackStack() },
                    onOpenCollection = { kind, id -> navController.navigate("collection/$kind/$id") },
                    onTrackMenu = { menuTrack = it },
                )
            }
        }
        }
        }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        bottom = navBarInset +
                            (if (hasBottomBar) KwhaleSpacing.navBar else 0.dp) +
                            KwhaleSpacing.xs,
                    ),
            ) {
                MiniPlayer(
                    visible = miniVisible,
                    track = miniTrack,
                    isPlaying = playerState.isPlaying,
                    isBuffering = playerState.isBuffering,
                    progress = playerState.progress,
                    reducedMotion = reducedMotion,
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
            sheetMaxWidth = Dp.Unspecified,
            // Opaque container: with Color.Transparent the artwork cross-fade
            // composited at ~75% alpha and the screen underneath showed through
            // («растворение», 2026-11-19 video).
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = null,
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        ) {
            PlayerScreen(
                app = app,
                state = playerState,
                snackbarHostState = playerSnackbarHostState,
                onClose = { showPlayer = false },
                onOpenArtist = { id -> showPlayer = false; navController.navigate("collection/artist/$id") },
                onOpenAlbum = { id -> showPlayer = false; navController.navigate("collection/album/$id") },
                onLyrics = { showLyrics = true },
                onQueue = { showQueue = true },
                onMenu = { menuTrack = it },
                onShare = { t ->
                    shareTarget = ShareTarget(
                        kind = ShareKind.TRACK,
                        id = t.remoteId.orEmpty(),
                        title = t.title,
                        subtitle = t.artist,
                        meta = t.album,
                        coverUrl = t.coverUrl,
                        fallbackText = buildString {
                            append(t.artist).append(" — ").append(t.title)
                            t.album?.let { append(" (").append(it).append(")") }
                        },
                        surface = "player",
                        analyticsTrack = t,
                    )
                },
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
                onOpenArtist = { id ->
                    menuTrack = null
                    showPlayer = false
                    navController.navigate("collection/artist/$id")
                },
                onOpenAlbum = { id ->
                    menuTrack = null
                    showPlayer = false
                    navController.navigate("collection/album/$id")
                },
                onShare = {
                    val artistLine = track.artistLine()
                    menuTrack = null
                    shareTarget = ShareTarget(
                        kind = ShareKind.TRACK,
                        id = track.id,
                        title = track.title,
                        subtitle = artistLine,
                        meta = track.album?.title,
                        coverUrl = track.imageUrl(app.api),
                        fallbackText = buildString {
                            append(artistLine).append(" — ").append(track.title)
                            track.album?.title?.let { append(" (").append(it).append(")") }
                        },
                        surface = "track_menu",
                        analyticsTrack = track.toUi(app.api),
                    )
                },
                onDownload = {
                    val ui = track.toUi(app.api)
                    Downloads.enqueue(context, ui)
                    showMessage("Скачивание начато")
                    menuTrack = null
                },
                onMessage = ::showMessage,
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
                onDone = {
                    showMessage("Добавлено в плейлист")
                    addToPlaylistTrack = null
                },
            )
        }
    }

    shareTarget?.let { target ->
        ModalBottomSheet(
            onDismissRequest = { shareTarget = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            ShareSheet(
                app = app,
                target = target,
                onDismiss = { shareTarget = null },
                onMessage = ::showMessage,
            )
        }
    }
}

@Composable
private fun MiniPlayer(
    visible: MutableTransitionState<Boolean>,
    track: UiTrack?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    progress: Float,
    reducedMotion: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
) {
    val displayTrack = track ?: return

    // The player emits progress once per second; animate between ticks so the
    // hairline does not visibly jump.
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "miniProgress",
    )

    AnimatedVisibility(
        visibleState = visible,
        enter = if (reducedMotion) {
            fadeIn()
        } else {
            slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ) + fadeIn()
        },
        exit = if (reducedMotion) {
            fadeOut()
        } else {
            slideOutVertically(targetOffsetY = { it }) + fadeOut()
        },
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .padding(horizontal = KwhaleSpacing.xs, vertical = KwhaleSpacing.xxs)
                .semantics {
                    contentDescription = "Открыть плеер: ${displayTrack.title}, ${displayTrack.artist}"
                }
                .clickable(onClickLabel = "Открыть плеер", onClick = onOpen),
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
                if (isBuffering) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent,
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // content below is unchanged except spacing tokens
                    MiniPlayerContent(
                        track = displayTrack,
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
    val scope = rememberCoroutineScope()
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    fun loadPlaylists() {
        loading = true
        loadError = null
        scope.launch {
            runCatching { app.api.playlists(limit = 100).items }
                .onSuccess {
                    playlists = it
                    loading = false
                }
                .onFailure {
                    loadError = "Не удалось загрузить плейлисты"
                    loading = false
                }
        }
    }

    LaunchedEffect(Unit) {
        loadPlaylists()
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
        actionError?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
        when {
            loading -> {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            loadError != null -> {
                ErrorState(
                    message = loadError ?: "Ошибка загрузки",
                    onRetry = { loadPlaylists() },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }
            else -> {
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
                                    imageUrl = playlist.imageUrl(app.api),
                                    modifier = Modifier.size(44.dp),
                                    shape = RoundedCornerShape(8.dp),
                                )
                            },
                            trailingContent = {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable(enabled = !saving) {
                                saving = true
                                actionError = null
                                scope.launch {
                                    runCatching { app.api.addPlaylistTracks(playlist.id, listOf(track.id)) }
                                        .onSuccess {
                                            app.events.playlistAdd(track.toUi(app.api), playlist.id, "track_menu")
                                            saving = false
                                            onDone()
                                        }
                                        .onFailure {
                                            saving = false
                                            actionError = "Не удалось добавить в плейлист"
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
                            modifier = Modifier.clickable(enabled = !saving) { showCreate = true },
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { if (!saving) showCreate = false },
            title = { Text("Новый плейлист") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        singleLine = true,
                        enabled = !saving,
                        label = { Text("Название") },
                    )
                    actionError?.let { msg ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !saving && newName.trim().isNotEmpty(),
                    onClick = {
                        val name = newName.trim()
                        if (name.isNotEmpty()) {
                            saving = true
                            actionError = null
                            scope.launch {
                                runCatching {
                                    val created = app.api.createPlaylist(
                                        org.dueattendant149.kwhale.network.model.PlaylistCreate(name),
                                    )
                                    app.api.addPlaylistTracks(created.id, listOf(track.id))
                                    created.id
                                }.onSuccess { createdId ->
                                    app.events.playlistAdd(track.toUi(app.api), createdId, "track_menu")
                                    saving = false
                                    showCreate = false
                                    onDone()
                                }.onFailure {
                                    saving = false
                                    actionError = "Не удалось создать плейлист"
                                }
                            }
                        }
                    },
                ) {
                    if (saving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Создать")
                    }
                }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { showCreate = false }) { Text("Отмена") }
            },
        )
    }
}

private fun NavHostController.navigateTop(route: String) {
    navigate(route) {
        popUpTo("home") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
