package org.dueattendant149.vkx.ui

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import kotlinx.coroutines.launch
import org.dueattendant149.vkx.VkxApp
import org.dueattendant149.vkx.design.VkxCoverPalette
import org.dueattendant149.vkx.design.VkxSpacing
import org.dueattendant149.vkx.network.model.Track as ApiTrack
import org.dueattendant149.vkx.player.PlaybackState
import org.dueattendant149.vkx.ui.components.CoverArt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    app: VkxApp,
    state: PlaybackState,
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
    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val content = if (isLight) MaterialTheme.colorScheme.onSurface else Color.White
    val secondary = content.copy(alpha = 0.62f)
    val accent = MaterialTheme.colorScheme.primary

    var liked by remember(track.id, track.isLiked) { mutableStateOf(track.isLiked) }
    var disliked by remember(track.id, track.isDisliked) { mutableStateOf(track.isDisliked) }
    var dragValue by remember { mutableStateOf<Float?>(null) }

    val likedScale by animateFloatAsState(
        targetValue = if (liked) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "playerLikeScale",
    )

    Box(Modifier.fillMaxSize()) {
        PlayerBackground(
            coverUrl = track.coverUrl,
            seed = track.title,
            isLight = isLight,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = VkxSpacing.gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── header ────────────────────────────────────────────────────────
            // Shows the artist, not the raw `album` field: connector albums are
            // often distributor strings ("Uzden.net"), which read as garbage
            // where a listener expects the performer.
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
                IconButton(onClick = { shareTrack(context, track) }) {
                    Icon(
                        Icons.Rounded.Share,
                        contentDescription = "Поделиться",
                        tint = content,
                    )
                }
            }

            Spacer(Modifier.height(VkxSpacing.sm))

            // ── artwork ──────────────────────────────────────────────────────
            // Crossfades between tracks and never shows an empty plate: a
            // cover-less track gets its own gradient + initial.
            val shape = RoundedCornerShape(20.dp)
            AnimatedContent(
                targetState = track.remoteId ?: track.id.toString(),
                transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(160)) },
                label = "artwork",
            ) { targetId ->
                key(targetId) {
                    CoverArt(
                        imageUrl = track.coverUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .then(
                                if (isLight) Modifier.shadow(16.dp, shape)
                                else Modifier,
                            ),
                        shape = shape,
                        iconSize = 64,
                        seed = track.title,
                    )
                }
            }

            Spacer(Modifier.height(VkxSpacing.lg))

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
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Medium,
                            color = content,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable {
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
                            modifier = Modifier.clickable {
                                track.artistRemoteId?.let(onOpenArtist)
                            },
                        )
                    }

                    // Unlike = a real "hide" affordance, kept quiet until needed.
                    IconButton(onClick = {
                        disliked = !disliked
                        if (disliked) liked = false
                        val remoteId = track.remoteId ?: return@IconButton
                        scope.launch {
                            runCatching {
                                if (disliked) app.api.dislike(remoteId) else app.api.undislike(remoteId)
                            }
                            if (disliked) app.player.next()
                        }
                    }) {
                        Icon(
                            Icons.Outlined.ThumbDown,
                            contentDescription = "Скрыть из рекомендаций",
                            tint = if (disliked) accent else secondary,
                        )
                    }

                    // Like is a heart everywhere else in the app — it was a
                    // checkmark here, which nobody reads as "save to library".
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable {
                                liked = !liked
                                if (liked) disliked = false
                                app.player.updateTrackLiked(track.id, liked)
                                val remoteId = track.remoteId ?: return@clickable
                                scope.launch {
                                    runCatching {
                                        if (liked) app.api.like(remoteId) else app.api.unlike(remoteId)
                                    }
                                }
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

            Spacer(Modifier.height(VkxSpacing.sm))

            // ── progress ─────────────────────────────────────────────────────
            val sliderValue = dragValue ?: state.progress
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

            Spacer(Modifier.height(VkxSpacing.md))

            // ── transport ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = app.player::toggleRepeat) {
                    Icon(
                        Icons.Rounded.Repeat,
                        contentDescription = "Повтор",
                        tint = if (state.repeat) accent else content,
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
                        .clickable(onClick = app.player::toggle),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) "Пауза" else "Играть",
                        tint = if (isLight) accent else Color.Black,
                        modifier = Modifier.size(34.dp),
                    )
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
                        .clickable(onClick = app.player::toggleShuffle),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = "Перемешать",
                        tint = if (state.shuffle) accent else content,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // ── secondary row ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = VkxSpacing.lg),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Текст",
                    style = MaterialTheme.typography.labelLarge,
                    color = content,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onLyrics)
                        .padding(horizontal = VkxSpacing.sm, vertical = VkxSpacing.xs),
                )
                IconButton(onClick = {
                    val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                    runCatching { context.startActivity(intent) }
                        .onFailure {
                            Toast.makeText(context, "Эквалайзер недоступен", Toast.LENGTH_SHORT).show()
                        }
                }) {
                    Icon(Icons.Rounded.Tune, contentDescription = "Эквалайзер", tint = content)
                }
                IconButton(onClick = onQueue) {
                    Icon(
                        Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = "Очередь",
                        tint = content,
                    )
                }
                IconButton(onClick = {
                    track.remoteId?.let { id ->
                        scope.launch {
                            runCatching { app.api.track(id) }.onSuccess(onMenu)
                        }
                    }
                }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Ещё", tint = content)
                }
            }
        }
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
                        easing = androidx.compose.animation.core.FastOutSlowInEasing,
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

@Composable
private fun PlayerBackground(coverUrl: String?, seed: String, isLight: Boolean) {
    val gradient = remember(seed, isLight) { VkxCoverPalette.gradient(seed, isLight) }
    Box(Modifier.fillMaxSize()) {
        if (!coverUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(56.dp),
                loading = { Box(Modifier.fillMaxSize().background(Brush.linearGradient(gradient))) },
                error = { Box(Modifier.fillMaxSize().background(Brush.linearGradient(gradient))) },
                success = { SubcomposeAsyncImageContent() },
            )
        } else {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(gradient)))
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

private fun shareTrack(context: android.content.Context, track: org.dueattendant149.vkx.model.Track) {
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