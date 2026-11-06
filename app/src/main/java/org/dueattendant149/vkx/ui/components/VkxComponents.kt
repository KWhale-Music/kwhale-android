package org.dueattendant149.vkx.ui.components

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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.dueattendant149.vkx.model.Track as UiTrack
import org.dueattendant149.vkx.network.VkxApi
import org.dueattendant149.vkx.network.model.Album
import org.dueattendant149.vkx.network.model.Artist
import org.dueattendant149.vkx.network.model.Banner
import org.dueattendant149.vkx.network.model.Playlist
import org.dueattendant149.vkx.network.model.Track as ApiTrack

fun formatDuration(ms: Long): String {
    val total = (ms / 1000L).toInt().coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

fun ApiTrack.artistLine(): String =
    artists.joinToString(", ") { it.name }.ifBlank { "Неизвестный артист" }

fun ApiTrack.imageUrl(api: VkxApi, size: String = "md"): String? = cover?.let { api.imageUrl(it.id, size) }

fun Album.imageUrl(api: VkxApi, size: String = "md"): String? = cover?.let { api.imageUrl(it.id, size) }

fun Playlist.imageUrl(api: VkxApi, size: String = "md"): String? = cover?.let { api.imageUrl(it.id, size) }

fun Artist.imageUrl(api: VkxApi, size: String = "md"): String? = avatar?.let { api.imageUrl(it.id, size) }

@Composable
fun CoverArt(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    iconSize: Int = 20,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(iconSize.dp),
        )
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
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
            .padding(top = 24.dp, bottom = 10.dp),
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

@Composable
fun TrackRow(
    track: ApiTrack,
    api: VkxApi,
    liked: Boolean,
    onClick: () -> Unit,
    onToggleLike: () -> Unit,
    onOverflow: () -> Unit,
    modifier: Modifier = Modifier,
    number: Int? = null,
    showCover: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (number != null) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(26.dp),
            )
        }
        if (showCover) {
            CoverArt(
                imageUrl = track.imageUrl(api, size = "sm"),
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(8.dp),
                iconSize = 20,
            )
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
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
                text = track.artistLine(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = formatDuration(track.durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
        IconButton(onClick = onToggleLike, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = if (liked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (liked) "Убрать из любимых" else "В любимые",
                tint = if (liked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = onOverflow, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "Ещё",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun TrackTile(
    track: ApiTrack,
    api: VkxApi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(132.dp)
            .clickable(onClick = onClick),
    ) {
        CoverArt(
            imageUrl = track.imageUrl(api),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(8.dp),
            iconSize = 28,
        )
        Spacer(Modifier.height(6.dp))
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
    api: VkxApi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Int = 140,
) {
    AlbumCard(
        title = album.title,
        artist = album.artists.joinToString(", ") { it.name }.ifBlank { "Альбом" },
        coverUrl = album.imageUrl(api),
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
) {
    Column(
        modifier = modifier
            .width(width.dp)
            .clickable(onClick = onClick),
    ) {
        CoverArt(
            imageUrl = coverUrl,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(8.dp),
            iconSize = 28,
        )
        Spacer(Modifier.height(6.dp))
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
    api: VkxApi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Int = 140,
) {
    Column(
        modifier = modifier
            .width(width.dp)
            .clickable(onClick = onClick),
    ) {
        CoverArt(
            imageUrl = playlist.imageUrl(api),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(8.dp),
            iconSize = 28,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = playlist.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${playlist.trackCount} треков",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
fun ArtistCard(
    artist: Artist,
    api: VkxApi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 96,
) {
    Column(
        modifier = modifier
            .width(size.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CoverArt(
            imageUrl = artist.imageUrl(api),
            modifier = Modifier.size(size.dp),
            shape = CircleShape,
            iconSize = 28,
        )
        Spacer(Modifier.height(6.dp))
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
fun ContinueCard(
    track: UiTrack,
    progress: Float,
    isPlaying: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
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
                imageUrl = track.coverUrl,
                modifier = Modifier.size(88.dp),
                shape = RoundedCornerShape(12.dp),
                iconSize = 28,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Продолжить",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape),
                )
            }
            Spacer(Modifier.width(10.dp))
            IconButton(
                onClick = onToggle,
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Пауза" else "Играть",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
fun BannerCard(
    banner: Banner,
    api: VkxApi,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit = {},
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
        ) {
            AsyncImage(
                model = banner.image?.let { api.imageUrl(it.id) },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.32f)),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
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
                        color = Color.White.copy(alpha = 0.8f),
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
) {
    Box(modifier.fillMaxSize()) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(42.dp),
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
        }
        val light = MaterialTheme.colorScheme.surface.luminance() > 0.5f
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    if (light) MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                    else Color.Black.copy(alpha = 0.58f),
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
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(16.dp))
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
            Spacer(Modifier.height(20.dp))
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
    )
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.material3.CircularProgressIndicator()
    }
}
