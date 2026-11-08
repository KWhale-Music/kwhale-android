package org.dueattendant149.kwhale.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import org.dueattendant149.kwhale.design.KwhaleCoverPalette
import org.dueattendant149.kwhale.design.KwhalePlural
import org.dueattendant149.kwhale.design.KwhaleSpacing
import org.dueattendant149.kwhale.network.KwhaleApi
import org.dueattendant149.kwhale.network.model.Album
import org.dueattendant149.kwhale.network.model.Artist
import org.dueattendant149.kwhale.network.model.Banner
import org.dueattendant149.kwhale.network.model.Playlist
import org.dueattendant149.kwhale.network.model.Track as ApiTrack

fun formatDuration(ms: Long): String {
    val total = (ms / 1000L).toInt().coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

fun ApiTrack.artistLine(): String =
    artists.joinToString(", ") { it.name }.ifBlank { "Неизвестный артист" }

/**
 * Cover URL helpers.
 *
 * The API answers `placeholder` for rows without artwork; requesting it made the
 * client download a generated grey square and paint it over the real fallback.
 * Returning null instead lets [CoverArt] draw the gradient + initial plate.
 */
private const val PLACEHOLDER_IMAGE_ID = "placeholder"

private fun imageIdOrNull(id: String?): String? =
    id?.takeIf { it.isNotBlank() && it != PLACEHOLDER_IMAGE_ID }

fun ApiTrack.imageUrl(api: KwhaleApi, size: String = "md"): String? =
    // Fall back to the album artwork: the API frequently sends `placeholder`
    // for the track while the album row carries a real image (e.g. SoundCloud
    // uploads where only the album art was scraped).
    (imageIdOrNull(cover?.id) ?: imageIdOrNull(album?.cover?.id))
        ?.let { api.imageUrl(it, size) }

fun Album.imageUrl(api: KwhaleApi, size: String = "md"): String? =
    imageIdOrNull(cover?.id)?.let { api.imageUrl(it, size) }

fun Playlist.imageUrl(api: KwhaleApi, size: String = "md"): String? =
    imageIdOrNull(cover?.id)?.let { api.imageUrl(it, size) }

fun Artist.imageUrl(api: KwhaleApi, size: String = "md"): String? =
    imageIdOrNull(avatar?.id)?.let { api.imageUrl(it, size) }

/** Stable gradient seed for a cover-less entity. */
fun ApiTrack.coverSeed(): String = title.ifBlank { artistLine() }

fun Album.coverSeed(): String = title

fun Playlist.coverSeed(): String = title

fun Artist.coverSeed(): String = name

@Composable
private fun isDarkTheme(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

/**
 * Subtle press feedback shared by rows and cards.
 *
 * The ripple alone is easy to miss on a dense list; a 3% scale makes the
 * touch target feel physical without the "bouncy toy" effect.
 */
@Composable
fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Cover thumbnail with a designed fallback.
 *
 * Three states, all deliberate:
 *  1. artwork available -> fades in over the plate (no white flash);
 *  2. no artwork        -> brand gradient + first letter of [seed];
 *  3. loading           -> plate only, so the grid never jumps.
 */
@Composable
fun CoverArt(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    iconSize: Int = 20,
    seed: String = "",
    showInitial: Boolean = true,
) {
    val dark = isDarkTheme()
    val gradient = remember(seed, dark) { KwhaleCoverPalette.gradient(seed, dark) }
    val onGradient = remember(seed, dark) { KwhaleCoverPalette.onGradient(seed, dark) }
    val initial = remember(seed) { KwhaleCoverPalette.initialOf(seed) }
    val description = remember(seed) { seed.ifBlank { "Обложка" } }

    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(gradient))
            .semantics { contentDescription = "Обложка: $description" },
        contentAlignment = Alignment.Center,
    ) {
        if (showInitial && initial.isNotEmpty()) {
            Text(
                text = initial,
                color = onGradient,
                fontSize = (iconSize * 1.15f).sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = onGradient.copy(alpha = 0.85f),
                modifier = Modifier.size(iconSize.dp),
            )
        }

        if (!imageUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
                loading = { Box(Modifier.matchParentSize()) },
                error = { Box(Modifier.matchParentSize()) },
                success = { SubcomposeAsyncImageContent() },
            )
        }
    }
}

/**
 * Shimmer plate used while a list loads.
 *
 * Replaces bare `CircularProgressIndicator` screens: the layout keeps its final
 * shape so content does not jump into place when data arrives.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest

    Box(
        modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(base, highlight, base),
                    start = Offset(progress * 720f - 360f, 0f),
                    end = Offset(progress * 720f, 260f),
                ),
            ),
    )
}

/** Skeleton for a [TrackRow] list. */
@Composable
fun TrackRowSkeleton(modifier: Modifier = Modifier, withNumber: Boolean = false) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = KwhaleSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (withNumber) {
            ShimmerBox(Modifier.width(26.dp).height(12.dp), RoundedCornerShape(4.dp))
            Spacer(Modifier.width(KwhaleSpacing.xs))
        }
        ShimmerBox(Modifier.size(KwhaleSpacing.cover), RoundedCornerShape(10.dp))
        Spacer(Modifier.width(KwhaleSpacing.sm))
        Column(Modifier.weight(1f)) {
            ShimmerBox(Modifier.fillMaxWidth(0.62f).height(14.dp), RoundedCornerShape(4.dp))
            Spacer(Modifier.height(6.dp))
            ShimmerBox(Modifier.fillMaxWidth(0.38f).height(11.dp), RoundedCornerShape(4.dp))
        }
    }
}

/** Skeleton for horizontal card carousels. */
@Composable
fun CardCarouselSkeleton(modifier: Modifier = Modifier, cardWidth: Int = 140, count: Int = 3) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KwhaleSpacing.sm),
    ) {
        repeat(count) {
            Column(Modifier.width(cardWidth.dp)) {
                ShimmerBox(Modifier.fillMaxWidth().aspectRatio(1f), RoundedCornerShape(10.dp))
                Spacer(Modifier.height(KwhaleSpacing.xs))
                ShimmerBox(Modifier.fillMaxWidth(0.8f).height(12.dp), RoundedCornerShape(4.dp))
                Spacer(Modifier.height(5.dp))
                ShimmerBox(Modifier.fillMaxWidth(0.55f).height(10.dp), RoundedCornerShape(4.dp))
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = KwhaleSpacing.xl, bottom = KwhaleSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(action, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * One track line.
 *
 * Duration moved from its own column into the subtitle so the row keeps two
 * 48 dp touch targets (like / overflow) without squeezing the title on a 360 dp
 * screen. The like button animates on press instead of snapping.
 */
@Composable
fun TrackRow(
    track: ApiTrack,
    api: KwhaleApi,
    liked: Boolean,
    onClick: () -> Unit,
    onToggleLike: () -> Unit,
    onOverflow: () -> Unit,
    modifier: Modifier = Modifier,
    number: Int? = null,
    showCover: Boolean = true,
    isCurrent: Boolean = false,
) {
    val titleColor = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .pressScale(interaction)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(vertical = KwhaleSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (number != null) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(26.dp),
            )
        }
        if (showCover) {
            CoverArt(
                imageUrl = track.imageUrl(api, size = "sm"),
                modifier = Modifier.size(KwhaleSpacing.cover),
                shape = RoundedCornerShape(10.dp),
                iconSize = 20,
                seed = track.coverSeed(),
            )
            Spacer(Modifier.width(KwhaleSpacing.sm))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (track.explicit) {
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            text = "E",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
            }
            Text(
                text = if (track.durationMs > 0) {
                    "${track.artistLine()} · ${formatDuration(track.durationMs)}"
                } else {
                    track.artistLine()
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LikeIconButton(liked = liked, onClick = onToggleLike)
        IconButton(onClick = onOverflow) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "Ещё",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Heart with a spring press animation, shared by rows, menu and player. */
@Composable
fun LikeIconButton(
    liked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 22,
) {
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (liked) 1.12f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
        ),
        label = "likeScale",
    )
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = if (liked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (liked) "Убрать из любимых" else "В любимые",
            tint = if (liked) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(size.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale },
        )
    }
}

@Composable
fun TrackTile(
    track: ApiTrack,
    api: KwhaleApi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .pressScale(interaction)
            .width(132.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
    ) {
        CoverArt(
            imageUrl = track.imageUrl(api),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(10.dp),
            iconSize = 28,
            seed = track.coverSeed(),
        )
        Spacer(Modifier.height(KwhaleSpacing.xs))
        Text(
            text = track.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
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
}

@Composable
fun AlbumCard(
    album: Album,
    api: KwhaleApi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Int = 140,
) {
    AlbumCard(
        title = album.title,
        artist = album.artists.joinToString(", ") { it.name }.ifBlank { "Альбом" },
        coverUrl = album.imageUrl(api),
        seed = album.coverSeed(),
        onClick = onClick,
        modifier = modifier,
        width = width,
    )
}

@Composable
fun AlbumCard(
    title: String,
    artist: String,
    coverUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Int = 140,
    seed: String = "",
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .pressScale(interaction)
            .width(width.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
    ) {
        CoverArt(
            imageUrl = coverUrl,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(10.dp),
            iconSize = 28,
            seed = seed.ifBlank { title },
        )
        Spacer(Modifier.height(KwhaleSpacing.xs))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun PlaylistCard(
    playlist: Playlist,
    api: KwhaleApi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Int = 140,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .pressScale(interaction)
            .width(width.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
    ) {
        CoverArt(
            imageUrl = playlist.imageUrl(api),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(10.dp),
            iconSize = 28,
            seed = playlist.coverSeed(),
        )
        Spacer(Modifier.height(KwhaleSpacing.xs))
        Text(
            text = playlist.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = KwhalePlural.tracks(playlist.trackCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
fun ArtistCard(
    artist: Artist,
    api: KwhaleApi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 96,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .pressScale(interaction)
            .width(size.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CoverArt(
            imageUrl = artist.imageUrl(api),
            modifier = Modifier.size(size.dp),
            shape = CircleShape,
            iconSize = 28,
            seed = artist.coverSeed(),
        )
        Spacer(Modifier.height(KwhaleSpacing.xs))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun BannerCard(
    banner: Banner,
    api: KwhaleApi,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit = {},
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(20.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
        ) {
            CoverArt(
                imageUrl = imageIdOrNull(banner.image?.id)?.let { api.imageUrl(it, "lg") },
                modifier = Modifier.matchParentSize(),
                shape = RoundedCornerShape(0.dp),
                iconSize = 40,
                seed = banner.title,
                showInitial = false,
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f)),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(KwhaleSpacing.md),
            ) {
                Text(
                    text = banner.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                banner.subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun BlurredCoverBackground(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    seed: String = "",
) {
    val dark = isDarkTheme()
    val gradient = remember(seed, dark) { KwhaleCoverPalette.gradient(seed, dark) }
    Box(modifier.fillMaxSize()) {
        if (!imageUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(48.dp),
                loading = { Box(Modifier.fillMaxSize().background(Brush.linearGradient(gradient))) },
                error = { Box(Modifier.fillMaxSize().background(Brush.linearGradient(gradient))) },
                success = { SubcomposeAsyncImageContent() },
            )
        } else {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(gradient)))
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    if (dark) MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
                ),
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.MusicNote,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(KwhaleSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    modifier = Modifier.size(42.dp),
                )
            }
        }
        Spacer(Modifier.height(KwhaleSpacing.lg))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null && onAction != null) {
            Spacer(Modifier.height(KwhaleSpacing.lg))
            androidx.compose.material3.Button(onClick = onAction) {
                Text(action)
            }
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyState(
        title = "Не удалось загрузить",
        message = message,
        action = "Повторить",
        onAction = onRetry,
        modifier = modifier,
        icon = Icons.Rounded.MusicNote,
    )
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.material3.CircularProgressIndicator()
    }
}