package org.dueattendant149.kwhale.ui.sheets

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dueattendant149.kwhale.KwhaleApp
import org.dueattendant149.kwhale.network.model.Track as ApiTrack
import org.dueattendant149.kwhale.ui.components.CoverArt
import org.dueattendant149.kwhale.ui.components.artistLine
import org.dueattendant149.kwhale.ui.components.imageUrl

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
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var liked by remember(track.id, track.isLiked) { mutableStateOf(track.isLiked) }
    var disliked by remember(track.id, track.isDisliked) { mutableStateOf(track.isDisliked) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
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
                val wasLiked = liked
                liked = !liked
                scope.launch {
                    runCatching { if (liked) app.api.like(track.id) else app.api.unlike(track.id) }
                        .onFailure { liked = wasLiked }
                }
            },
        )
        MenuAction(
            icon = Icons.Rounded.PlaylistAdd,
            title = "В плейлист",
            onClick = onAddToPlaylist,
        )
        MenuAction(
            icon = Icons.Outlined.Download,
            title = "Скачать",
            onClick = onDownload,
        )
        track.artists.firstOrNull()?.id?.takeIf { !it.startsWith("unknown-") }?.let { artistId ->
            MenuAction(
                icon = Icons.Rounded.Person,
                title = "Перейти к артисту",
                onClick = { onOpenArtist(artistId) },
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
            onClick = onShare,
        )
        MenuAction(
            icon = Icons.Outlined.ThumbDown,
            title = if (disliked) "Убрать дизлайк" else "Скрыть из рекомендаций",
            tint = MaterialTheme.colorScheme.error,
            onClick = {
                val wasDisliked = disliked
                disliked = !disliked
                scope.launch {
                    runCatching {
                        if (disliked) app.api.dislike(track.id) else app.api.undislike(track.id)
                    }.onSuccess {
                        // пропускаем только когда скрываем именно играющий сейчас трек
                        if (disliked && app.player.state.value.current?.remoteId == track.id) {
                            app.player.next()
                        }
                        Toast.makeText(
                            context,
                            if (disliked) "Трек скрыт из рекомендаций" else "Дизлайк убран",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }.onFailure {
                        disliked = wasDisliked
                        Toast.makeText(context, "Не удалось изменить", Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                }
            },
        )
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
