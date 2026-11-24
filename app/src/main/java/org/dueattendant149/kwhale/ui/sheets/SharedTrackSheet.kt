package org.dueattendant149.kwhale.ui.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.dueattendant149.kwhale.KwhaleApp
import org.dueattendant149.kwhale.network.model.Track as ApiTrack
import org.dueattendant149.kwhale.ui.components.CoverArt
import org.dueattendant149.kwhale.ui.components.artistLine
import org.dueattendant149.kwhale.ui.components.imageUrl

/**
 * Preview sheet shown when opening a shared track deep link (`kwhale://share/...` or
 * `kwhale://track/...`) so opening a link lets the listener choose whether to replace
 * current playback, queue next, or browse the artist/album instead of immediately
 * interrupting whatever is playing.
 */
@Composable
fun SharedTrackSheet(
    app: KwhaleApp,
    track: ApiTrack,
    hasActiveQueue: Boolean,
    onPlayNow: () -> Unit,
    onPlayNext: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onMoreActions: () -> Unit,
) {
    val validArtists = remember(track.artists) {
        track.artists.filter { !it.id.startsWith("unknown-") && it.name.isNotBlank() }
    }
    var showArtistPicker by remember { mutableStateOf(false) }

    val metaLine = remember(track.album, track.durationMs) {
        val parts = buildList {
            track.album?.title?.takeIf { it.isNotBlank() }?.let(::add)
            if (track.durationMs > 0L) add(formatSharedDuration(track.durationMs))
        }
        parts.joinToString(" · ")
    }

    if (showArtistPicker) {
        AlertDialog(
            onDismissRequest = { showArtistPicker = false },
            title = { Text("Выберите исполнителя") },
            text = {
                Column {
                    validArtists.forEach { artist ->
                        ListItem(
                            headlineContent = { Text(artist.name) },
                            leadingContent = { Icon(Icons.Rounded.Person, contentDescription = null) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showArtistPicker = false
                                    onOpenArtist(artist.id)
                                },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showArtistPicker = false }) { Text("Закрыть") }
            },
        )
    }

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
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(16.dp),
                iconSize = 28,
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = track.artistLine(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (metaLine.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = metaLine,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onPlayNow,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Слушать", maxLines = 1)
            }
            if (hasActiveQueue) {
                FilledTonalButton(
                    onClick = onPlayNext,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.PlaylistPlay,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Следующим", maxLines = 1)
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        val colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        track.album?.let { album ->
            ListItem(
                headlineContent = { Text("Открыть альбом") },
                supportingContent = { Text(album.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingContent = { Icon(Icons.Rounded.Album, contentDescription = null) },
                colors = colors,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAlbum(album.id) },
            )
        }
        val primaryArtist = validArtists.firstOrNull()
        if (primaryArtist != null) {
            ListItem(
                headlineContent = { Text("Открыть исполнителя") },
                supportingContent = {
                    Text(
                        validArtists.joinToString(", ") { it.name },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingContent = { Icon(Icons.Rounded.Person, contentDescription = null) },
                colors = colors,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (validArtists.size == 1) {
                            onOpenArtist(primaryArtist.id)
                        } else {
                            showArtistPicker = true
                        }
                    },
            )
        }
        ListItem(
            headlineContent = { Text("Ещё действия") },
            supportingContent = { Text("В избранное, в плейлист, скачать") },
            leadingContent = { Icon(Icons.Rounded.MoreHoriz, contentDescription = null) },
            colors = colors,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onMoreActions),
        )
    }
}

private fun formatSharedDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return "%d:%02d".format(min, sec)
}
