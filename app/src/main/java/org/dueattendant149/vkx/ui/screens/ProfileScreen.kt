package org.dueattendant149.vkx.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dueattendant149.vkx.VkxApp
import org.dueattendant149.vkx.download.DownloadStore
import org.dueattendant149.vkx.ui.components.CoverArt
import java.io.File

@Composable
fun ProfileScreen(
    app: VkxApp,
    onSettings: () -> Unit,
    onAppearance: () -> Unit,
    onDownloads: () -> Unit,
    onHistory: () -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(app.api.session()?.user?.displayName ?: "Профиль") }
    var email by remember { mutableStateOf(app.api.session()?.user?.email ?: "") }
    var avatar by remember { mutableStateOf<String?>(null) }
    var trackCount by remember { mutableStateOf(0) }
    var playlistCount by remember { mutableStateOf(0) }
    var downloadCount by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }

    val uploadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val display = uri.lastPathSegment?.substringAfterLast('/') ?: "upload.mp3"
                val ext = display.substringAfterLast('.', "mp3")
                val temp = File(context.cacheDir, "upload_${System.currentTimeMillis()}.$ext")
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        temp.outputStream().use { output -> input.copyTo(output) }
                    } ?: error("Не удалось прочитать файл")
                }
                val uploaded = app.api.uploadTrack(temp, title = display.substringBeforeLast('.'))
                temp.delete()
                uploaded
            }.onSuccess {
                Toast.makeText(context, "Трек загружен", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Ошибка загрузки: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        runCatching { app.api.me() }.onSuccess { me ->
            name = me.displayName
            email = me.email.orEmpty()
            avatar = me.avatar?.let { app.api.imageUrl(it.id) }
        }
        trackCount = runCatching { app.api.libraryTracks(limit = 1).total }.getOrNull() ?: 0
        playlistCount = runCatching { app.api.libraryPlaylists(limit = 1).total }.getOrNull() ?: 0
        downloadCount = DownloadStore(context).all().count { it.file().exists() }
        loading = false
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 28.dp),
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(88.dp)) {
                    CoverArt(
                        imageUrl = avatar,
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        iconSize = 34,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                if (email.isNotBlank()) {
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Stat("$trackCount", "треков")
                Stat("$playlistCount", "плейлистов")
                Stat("$downloadCount", "скачано")
            }
        }
        item { HorizontalDivider() }
        item {
            ProfileRow("История", Icons.Rounded.History, onHistory)
        }
        item {
            ProfileRow("Скачано", Icons.Rounded.CloudDownload, onDownloads)
        }
        item {
            ProfileRow("Загрузить трек", Icons.Rounded.Upload, onClick = {
                uploadLauncher.launch(arrayOf("audio/*"))
            })
        }
        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item {
            ProfileRow("Оформление", Icons.Rounded.Palette, onAppearance)
        }
        item {
            ProfileRow("Настройки", Icons.Rounded.Settings, onSettings)
        }
        item {
            ProfileRow("О приложении", Icons.Rounded.Info, onClick = {
                Toast.makeText(context, "VK X · KWhale music", Toast.LENGTH_SHORT).show()
            })
        }
        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item {
            ProfileRow("Выйти", Icons.AutoMirrored.Rounded.Logout, onLogout, destructive = true)
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProfileRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    ListItem(
        headlineContent = {
            Text(
                title,
                color = if (destructive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface,
            )
        },
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                tint = if (destructive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}
