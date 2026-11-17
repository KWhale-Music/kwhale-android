package org.dueattendant149.kwhale.ui

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign
import org.dueattendant149.kwhale.KwhaleApp
import org.dueattendant149.kwhale.design.KwhaleCoverPalette
import org.dueattendant149.kwhale.design.KwhaleSpacing
import org.dueattendant149.kwhale.design.rememberReducedMotionEnabled
import org.dueattendant149.kwhale.network.model.Track as ApiTrack
import org.dueattendant149.kwhale.player.PlaybackState
import org.dueattendant149.kwhale.player.RepeatMode as PlayerRepeatMode
import org.dueattendant149.kwhale.ui.components.CoverArt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    app: KwhaleApp,
    state: PlaybackState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onClose: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onLyrics: () -> Unit,
    onQueue: () -> Unit,
    onMenu: (ApiTrack) -> Unit,
) {
    val track = state.current ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val reducedMotion = rememberReducedMotionEnabled()

    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val content = if (isLight) MaterialTheme.colorScheme.onSurface else Color.White
    val secondary = content.copy(alpha = 0.62f)
    val accent = MaterialTheme.colorScheme.primary

    val likedOverrides by app.reactions.likedOverrides.collectAsState()
    val dislikedOverrides by app.reactions.dislikedOverrides.collectAsState()
    val censorshipOverrides by app.reactions.censorshipOverrides.collectAsState()
    val censorshipInFlight by app.reactions.censorshipInFlight.collectAsState()
    val playbackError by app.player.playbackError.collectAsState()

    val trackKey = track.remoteId ?: track.id.toString()
    val liked = app.reactions.isLiked(trackKey, track.isLiked, likedOverrides)
    val disliked = app.reactions.isDisliked(trackKey, track.isDisliked, dislikedOverrides)
    val censorshipReported = app.reactions.isCensorshipReported(
        trackId = trackKey,
        serverReported = track.isCensorshipReported,
        overrides = censorshipOverrides,
    )
    val censorshipBusy = trackKey in censorshipInFlight

    var loadingMenu by remember(track.id) { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val smoothedProgress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "playerProgress",
    )

    val likedScale by animateFloatAsState(
        targetValue = if (liked) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "playerLikeScale",
    )

    val artwork = PlayerArtwork(
        id = track.remoteId ?: track.id.toString(),
        coverUrl = track.coverUrl,
        title = track.title,
    )
    val artworkSwipeOffset = remember(artwork.id) { Animatable(0f) }

    fun showPlayerMsg(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    @Composable
    fun PlayerHeader() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Свернуть",
                    tint = content,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Сейчас играет",
                    style = MaterialTheme.typography.labelMedium,
                    color = secondary,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.isPlaying) {
                        EqualizerIndicator(color = accent, modifier = Modifier.padding(end = 6.dp))
                    }
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.titleSmall,
                        color = content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = { shareTrack(app, context, track) }) {
                Icon(
                    Icons.Rounded.Share,
                    contentDescription = "Поделиться",
                    tint = content,
                )
            }
        }
    }

    @Composable
    fun PlayerArtworkCard(modifier: Modifier = Modifier) {
        val shape = RoundedCornerShape(20.dp)
        val swipeThresholdPx = with(density) { 72.dp.toPx() }
        val maxSwipePx = with(density) { 140.dp.toPx() }
        val flingVelocityPx = with(density) { 900.dp.toPx() } // fast flick skips even over a short drag

        Box(
            modifier = modifier.pointerInput(artwork.id) {
                var totalDrag = 0f
                val velocityTracker = VelocityTracker()
                // Track phase: 1:1 with the finger; past the travel limit the
                // cover resists (rubber-band) instead of hitting a hard wall.
                fun rubberBand(drag: Float): Float {
                    val excess = abs(drag) - maxSwipePx
                    return if (excess <= 0f) drag
                    else sign(drag) * (maxSwipePx + excess * 0.25f)
                }
                detectHorizontalDragGestures(
                    onDragStart = {
                        totalDrag = 0f
                        velocityTracker.resetTracking()
                    },
                    onDragCancel = {
                        totalDrag = 0f
                        scope.launch { artworkSwipeOffset.animateTo(0f, spring()) }
                    },
                    onDragEnd = {
                        val delta = totalDrag
                        val velocity = velocityTracker.calculateVelocity().x
                        totalDrag = 0f
                        // Dismiss on distance OR velocity: a fast flick counts
                        // even when the cover barely moved.
                        when {
                            delta > swipeThresholdPx || velocity > flingVelocityPx ->
                                app.player.previous()
                            delta < -swipeThresholdPx || velocity < -flingVelocityPx ->
                                app.player.next()
                        }
                        // Release phase: a spring seeded with the finger's
                        // velocity — the cover leaves the finger at its speed.
                        scope.launch {
                            artworkSwipeOffset.animateTo(
                                0f,
                                spring(
                                    dampingRatio = 0.75f,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                                initialVelocity = velocity,
                            )
                        }
                    },
                ) { change, dragAmount ->
                    change.consume()
                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                    totalDrag = rubberBand(totalDrag + dragAmount)
                    if (!reducedMotion) {
                        scope.launch { artworkSwipeOffset.snapTo(totalDrag) }
                    }
                }
            },
        ) {
            AnimatedContent(
                targetState = artwork,
                transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(160)) },
                label = "artwork",
            ) { art ->
                key(art.id) {
                    CoverArt(
                        imageUrl = art.coverUrl,
                        contentDescription = "Обложка: ${art.title}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .graphicsLayer {
                                translationX = if (reducedMotion) 0f else artworkSwipeOffset.value
                            }
                            .then(
                                if (isLight) Modifier.shadow(16.dp, shape)
                                else Modifier,
                            ),
                        shape = shape,
                        iconSize = 64,
                        seed = art.title,
                    )
                }
            }
        }
    }

    @Composable
    fun ColumnScope.PlayerControlsSection(isTwoPane: Boolean) {
        // ── title / actions ──────────────────────────────────────────────
        AnimatedContent(
            targetState = track.title,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
            label = "title",
        ) { title ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val canOpenCollection = !track.albumRemoteId.isNullOrBlank() || !track.artistRemoteId.isNullOrBlank()
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Medium,
                        color = content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(
                            enabled = canOpenCollection,
                            onClickLabel = "Открыть альбом",
                        ) {
                            val albumId = track.albumRemoteId
                            if (!albumId.isNullOrBlank()) onOpenAlbum(albumId)
                            else track.artistRemoteId?.let(onOpenArtist)
                        },
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(
                            enabled = !track.artistRemoteId.isNullOrBlank(),
                            onClickLabel = "Открыть исполнителя",
                        ) {
                            track.artistRemoteId?.let(onOpenArtist)
                        },
                    )
                }

                IconButton(
                    onClick = {
                        app.reactions.toggleDislike(
                            track = track,
                            surface = "player",
                            onChanged = { nowDisliked ->
                                if (nowDisliked) app.player.next()
                            },
                            onError = ::showPlayerMsg,
                        )
                    },
                    modifier = Modifier.semantics {
                        stateDescription = if (disliked) "Скрыто" else "Не скрыто"
                    },
                ) {
                    Icon(
                        Icons.Outlined.ThumbDown,
                        contentDescription = "Скрыть из рекомендаций",
                        tint = if (disliked) accent else secondary,
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .semantics(mergeDescendants = true) {
                            role = Role.Button
                            stateDescription = if (liked) "В моей музыке" else "Не в моей музыке"
                        }
                        .clickable(
                            onClickLabel = if (liked) "Убрать из моей музыки" else "В мою музыку",
                        ) {
                            app.reactions.toggleLike(
                                track = track,
                                surface = "player",
                                onError = ::showPlayerMsg,
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (liked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (liked) "Убрать из моей музыки" else "В мою музыку",
                        tint = if (liked) accent else secondary,
                        modifier = Modifier
                            .size(26.dp)
                            .graphicsLayer {
                                scaleX = likedScale
                                scaleY = likedScale
                            },
                    )
                }
            }
        }

        playbackError?.let { errMsg ->
            Spacer(Modifier.height(KwhaleSpacing.xs))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(start = KwhaleSpacing.sm, end = KwhaleSpacing.xxs, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = errMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TextButton(onClick = { app.player.retryCurrent() }) {
                        Text("Повторить", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                    IconButton(onClick = { app.player.clearError() }) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Скрыть ошибку",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(KwhaleSpacing.sm))

        // ── progress ─────────────────────────────────────────────────────
        val sliderValue = dragValue ?: smoothedProgress
        Slider(
            value = sliderValue,
            onValueChange = { dragValue = it },
            onValueChangeFinished = {
                dragValue?.let { app.player.seekTo((it * state.durationMs).toLong()) }
                dragValue = null
            },
            modifier = Modifier.fillMaxWidth(),
            thumb = {
                Box(
                    Modifier
                        .size(20.dp)
                        .background(accent, CircleShape),
                )
            },
            colors = SliderDefaults.colors(
                activeTrackColor = accent,
                inactiveTrackColor = content.copy(alpha = 0.22f),
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatVkTime(
                    if (dragValue != null) (dragValue!! * state.durationMs).toLong()
                    else state.positionMs,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = secondary,
            )
            Text(
                text = formatVkTime(state.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = secondary,
            )
        }

        Spacer(Modifier.height(KwhaleSpacing.md))

        // ── transport ────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val repeatDescription = when (state.repeatMode) {
                PlayerRepeatMode.OFF -> "Повтор: выключен"
                PlayerRepeatMode.ALL -> "Повтор: вся очередь"
                PlayerRepeatMode.ONE -> "Повтор: один трек"
            }
            IconButton(
                onClick = app.player::toggleRepeat,
                modifier = Modifier.semantics { stateDescription = repeatDescription },
            ) {
                Icon(
                    imageVector = if (state.repeatMode == PlayerRepeatMode.ONE) {
                        Icons.Rounded.RepeatOne
                    } else {
                        Icons.Rounded.Repeat
                    },
                    contentDescription = repeatDescription,
                    tint = if (state.repeatMode != PlayerRepeatMode.OFF) accent else content,
                )
            }
            IconButton(onClick = app.player::previous, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    contentDescription = "Предыдущий",
                    tint = content,
                    modifier = Modifier.size(36.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(if (isLight) content.copy(alpha = 0.12f) else Color.White)
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        stateDescription = when {
                            state.isBuffering -> "Буферизация"
                            state.isPlaying -> "Играет"
                            else -> "Пауза"
                        }
                    }
                    .clickable(
                        onClickLabel = if (state.isPlaying) "Пауза" else "Играть",
                        onClick = app.player::toggle,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (state.isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        strokeWidth = 2.5.dp,
                        color = accent,
                    )
                }
                AnimatedContent(
                    targetState = state.isPlaying,
                    transitionSpec = {
                        fadeIn(tween(120)) togetherWith fadeOut(tween(80))
                    },
                    label = "playPause",
                ) { playing ->
                    Icon(
                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playing) "Пауза" else "Играть",
                        tint = if (isLight) accent else Color.Black,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }
            IconButton(onClick = app.player::next, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = "Следующий",
                    tint = content,
                    modifier = Modifier.size(36.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (state.shuffle) accent.copy(alpha = 0.16f) else Color.Transparent)
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        stateDescription = if (state.shuffle) "Включено" else "Выключено"
                    }
                    .clickable(
                        onClickLabel = "Перемешать",
                        onClick = app.player::toggleShuffle,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = "Перемешать",
                    tint = if (state.shuffle) accent else content,
                )
            }
        }

        if (!isTwoPane) {
            Spacer(Modifier.weight(1f))
        } else {
            Spacer(Modifier.height(KwhaleSpacing.md))
        }

        // ── secondary row: Текст | Цензура (1 клик) | Очередь | Ещё ──────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = KwhaleSpacing.lg),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .semantics { role = Role.Button }
                    .clickable(onClick = onLyrics)
                    .padding(horizontal = KwhaleSpacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Текст",
                    style = MaterialTheme.typography.labelLarge,
                    color = content,
                )
            }
            IconButton(
                enabled = !censorshipBusy,
                onClick = {
                    app.reactions.reportCensorshipOneClick(
                        track = track,
                        positionMs = state.positionMs,
                        onAlreadyReported = {
                            scope.launch {
                                val res = snackbarHostState.showSnackbar(
                                    message = "Трек уже отправлен на пересмотр",
                                    actionLabel = "Отменить",
                                    duration = SnackbarDuration.Long,
                                )
                                if (res == SnackbarResult.ActionPerformed) {
                                    app.reactions.cancelCensorshipReport(
                                        track = track,
                                        onCancelled = { showPlayerMsg("Отправка на пересмотр отменена") },
                                        onError = ::showPlayerMsg,
                                    )
                                }
                            }
                        },
                        onSent = {
                            scope.launch {
                                val res = snackbarHostState.showSnackbar(
                                    message = "Отправлено на пересмотр в админку",
                                    actionLabel = "Отменить",
                                    duration = SnackbarDuration.Long,
                                )
                                if (res == SnackbarResult.ActionPerformed) {
                                    app.reactions.cancelCensorshipReport(
                                        track = track,
                                        onCancelled = { showPlayerMsg("Отправка отменена") },
                                        onError = ::showPlayerMsg,
                                    )
                                }
                            }
                        },
                        onError = ::showPlayerMsg,
                    )
                },
                modifier = Modifier.semantics {
                    stateDescription = if (censorshipReported) "Отправлено на пересмотр" else "Не отправлено"
                },
            ) {
                if (censorshipBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = content,
                    )
                } else {
                    Icon(
                        imageVector = if (censorshipReported) Icons.Rounded.Flag else Icons.Outlined.Flag,
                        contentDescription = if (censorshipReported) {
                            "Отправлено на пересмотр (цензура)"
                        } else {
                            "Отправить на цензуру"
                        },
                        tint = if (censorshipReported) MaterialTheme.colorScheme.error else content,
                    )
                }
            }
            IconButton(onClick = onQueue) {
                Icon(
                    Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = "Очередь",
                    tint = content,
                )
            }
            IconButton(
                enabled = !loadingMenu,
                onClick = {
                    val id = track.remoteId
                    if (id == null) {
                        showPlayerMsg("Меню недоступно для локального файла")
                        return@IconButton
                    }
                    loadingMenu = true
                    scope.launch {
                        runCatching { app.api.track(id) }
                            .onSuccess {
                                loadingMenu = false
                                onMenu(it)
                            }
                            .onFailure {
                                loadingMenu = false
                                showPlayerMsg("Не удалось загрузить меню трека")
                            }
                    }
                },
            ) {
                if (loadingMenu) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = content,
                    )
                } else {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Ещё", tint = content)
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = artwork,
            transitionSpec = { fadeIn(tween(320)) togetherWith fadeOut(tween(180)) },
            label = "playerBackground",
            modifier = Modifier.fillMaxSize(),
        ) { art ->
            PlayerBackground(
                coverUrl = art.coverUrl,
                seed = art.title,
                isLight = isLight,
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            val boxMaxWidth = maxWidth
            val boxMaxHeight = maxHeight
            val isTwoPane = boxMaxWidth >= 600.dp || boxMaxWidth > boxMaxHeight
            if (isTwoPane) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = KwhaleSpacing.gutter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(KwhaleSpacing.xl),
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        PlayerArtworkCard(
                            modifier = Modifier.widthIn(
                                max = (boxMaxHeight * 0.78f).coerceAtLeast(180.dp).coerceAtMost(380.dp),
                            ),
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PlayerHeader()
                        Spacer(Modifier.height(KwhaleSpacing.sm))
                        PlayerControlsSection(isTwoPane = true)
                    }
                }
            } else {
                val maxArtworkWidth = (boxMaxHeight * 0.46f).coerceAtLeast(180.dp)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = boxMaxHeight)
                            .padding(horizontal = KwhaleSpacing.gutter),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PlayerHeader()
                        Spacer(Modifier.height(KwhaleSpacing.sm))
                        PlayerArtworkCard(
                            modifier = Modifier.widthIn(max = maxArtworkWidth),
                        )
                        Spacer(Modifier.height(KwhaleSpacing.lg))
                        PlayerControlsSection(isTwoPane = false)
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = KwhaleSpacing.md, vertical = KwhaleSpacing.sm),
        )
    }
}

/**
 * Three-bar "playing" indicator.
 *
 * The player header used to be static text; a moving indicator makes the
 * paused/playing distinction readable at a glance.
 */
@Composable
private fun EqualizerIndicator(
    color: Color,
    modifier: Modifier = Modifier,
    bars: Int = 3,
) {
    val reducedMotion = rememberReducedMotionEnabled()
    if (reducedMotion) {
        val staticFractions = remember(bars) { List(bars) { i -> if (i % 2 == 0) 0.55f else 0.9f } }
        Row(
            modifier = modifier.height(12.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            staticFractions.forEach { f ->
                Box(
                    Modifier
                        .width(2.5.dp)
                        .height((12f * f).dp)
                        .background(color, RoundedCornerShape(2.dp)),
                )
            }
        }
        return
    }
    val transition = rememberInfiniteTransition(label = "eq")
    Row(
        modifier = modifier.height(12.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(bars) { index ->
            val fraction by transition.animateFloat(
                initialValue = 0.30f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 420 + index * 130,
                        easing = FastOutSlowInEasing,
                    ),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
                ),
                label = "eqBar$index",
            )
            Box(
                Modifier
                    .width(2.5.dp)
                    .height((12f * fraction).dp)
                    .background(color, RoundedCornerShape(2.dp)),
            )
        }
    }
}

private data class PlayerArtwork(
    val id: String,
    val coverUrl: String?,
    val title: String,
)

@Composable
private fun PlayerBackground(coverUrl: String?, seed: String, isLight: Boolean) {
    val gradient = remember(seed, isLight) { KwhaleCoverPalette.gradient(seed, isLight) }
    // The gradient plate is always painted: while the next cover is loading
    // the old one has already faded out, and a transparent layer would show
    // the window background for a second.
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(gradient)),
    ) {
        if (!coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(32.dp),
            )
        }
        // Scrim keeps text legible over any artwork brightness.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    if (isLight) MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                    else Color.Black.copy(alpha = 0.52f),
                ),
        )
    }
}

private fun formatVkTime(ms: Long): String {
    val total = (ms / 1000L).toInt().coerceAtLeast(0)
    return "%02d:%02d".format(total / 60, total % 60)
}

private fun shareTrack(app: KwhaleApp, context: android.content.Context, track: org.dueattendant149.kwhale.model.Track) {
    app.events.share(track, "player")
    val text = buildString {
        append(track.artist)
        append(" — ")
        append(track.title)
        track.album?.let { append(" (").append(it).append(")") }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Поделиться"))
}