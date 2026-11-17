package org.dueattendant149.kwhale.ui.screens

import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dueattendant149.kwhale.BuildConfig
import org.dueattendant149.kwhale.KwhaleApp
import org.dueattendant149.kwhale.data.ContentCache
import org.dueattendant149.kwhale.data.ProfileSnapshot
import org.dueattendant149.kwhale.design.KwhalePlural
import org.dueattendant149.kwhale.design.LocalMiniPlayerBottomPadding
import org.dueattendant149.kwhale.network.model.EnrichmentProposal
import org.dueattendant149.kwhale.ui.components.CoverArt
import org.dueattendant149.kwhale.ui.components.LocalSnackbarHost
import org.dueattendant149.kwhale.ui.components.ShimmerBox
import java.io.File

@Composable
fun ProfileScreen(
    app: KwhaleApp,
    onSettings: () -> Unit,
    onAppearance: () -> Unit,
    onDownloads: () -> Unit,
    onHistory: () -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHost = LocalSnackbarHost.current
    val cached = remember { app.cache.read(ContentCache.PROFILE, ProfileSnapshot.serializer()) }
    var name by remember {
        mutableStateOf(cached?.name ?: app.api.session()?.user?.displayName ?: "Профиль")
    }
    var email by remember { mutableStateOf(cached?.email ?: app.api.session()?.user?.email ?: "") }
    var avatar by remember { mutableStateOf(cached?.avatar) }
    val miniPlayerBottom = LocalMiniPlayerBottomPadding.current
    val downloadedTracks by app.downloadObserver.downloadedTracks.collectAsState()
    val downloadCount = downloadedTracks.count { it.file().exists() }
    var trackCount by remember { mutableStateOf(cached?.trackCount ?: 0) }
    var playlistCount by remember { mutableStateOf(cached?.playlistCount ?: 0) }
    var enrichments by remember { mutableStateOf(cached?.enrichments.orEmpty()) }
    var loading by remember { mutableStateOf(cached == null) }
    var uploading by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }

    val uploadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null || uploading) return@rememberLauncherForActivityResult
        uploading = true
        scope.launch {
            runCatching {
                val display = uri.lastPathSegment?.substringAfterLast('/') ?: "upload.mp3"
                val ext = display.substringAfterLast('.', "mp3")
                val temp = File(context.cacheDir, "upload_${System.currentTimeMillis()}.$ext")
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            temp.outputStream().use { output -> input.copyTo(output) }
                        } ?: error("Не удалось прочитать файл")
                    }
                    app.api.uploadTrack(temp, title = display.substringBeforeLast('.'))
                } finally {
                    temp.delete()
                }
            }.onSuccess {
                app.cache.invalidateLibrary()
                app.cache.invalidateProfile()
                trackCount += 1
                reloadKey += 1
                snackbarHost?.showSnackbar("Трек загружен в библиотеку")
            }.onFailure { err ->
                snackbarHost?.showSnackbar("Ошибка загрузки: ${err.message ?: "неизвестная ошибка"}")
            }
            uploading = false
        }
    }

    LaunchedEffect(reloadKey) {
        if (reloadKey == 0 && cached != null && app.cache.isFresh(ContentCache.PROFILE, 60_000)) {
            loading = false
            return@LaunchedEffect
        }
        runCatching { app.api.me() }.onSuccess { me ->
            name = me.displayName
            email = me.email.orEmpty()
            avatar = me.avatar?.let { app.api.imageUrl(it.id) }
        }
        trackCount = runCatching { app.api.libraryTracks(limit = 1).total }.getOrNull() ?: trackCount
        playlistCount = runCatching { app.api.libraryPlaylists(limit = 1).total }.getOrNull() ?: playlistCount
        enrichments = runCatching { app.api.enrichments().items }.getOrDefault(enrichments)
        app.cache.write(
            ContentCache.PROFILE,
            ProfileSnapshot.serializer(),
            ProfileSnapshot(
                name = name,
                email = email,
                avatar = avatar,
                trackCount = trackCount,
                playlistCount = playlistCount,
                downloadCount = downloadCount,
                enrichments = enrichments,
            ),
        )
        loading = false
    }

    if (loading) {
        ProfileSkeleton()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(
            top = 24.dp,
            bottom = 28.dp + miniPlayerBottom,
        ),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(88.dp)) {
                    CoverArt(
                        imageUrl = avatar,
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        iconSize = 34,
                        seed = name,
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
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Stat(KwhalePlural.formatted(trackCount), "в библиотеке")
                Stat(
                    KwhalePlural.formatted(playlistCount),
                    KwhalePlural.playlists(playlistCount).substringAfter(' '),
                )
                Stat(KwhalePlural.formatted(downloadCount), "скачано")
            }
        }
        if (enrichments.isNotEmpty()) {
            item {
                Text(
                    "Предложения по метаданным",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            items(enrichments, key = { it.id }) { prop ->
                EnrichmentCard(
                    proposal = prop,
                    onConfirm = {
                        scope.launch {
                            runCatching { app.api.confirmEnrichment(prop.id) }
                                .onSuccess {
                                    enrichments = enrichments.filterNot { it.id == prop.id }
                                    app.cache.invalidateProfile()
                                    app.cache.invalidateLibrary()
                                    snackbarHost?.showSnackbar("Метаданные подтверждены")
                                }
                                .onFailure {
                                    snackbarHost?.showSnackbar("Не удалось подтвердить изменения")
                                }
                        }
                    },
                    onReject = {
                        scope.launch {
                            runCatching { app.api.rejectEnrichment(prop.id) }
                                .onSuccess {
                                    enrichments = enrichments.filterNot { it.id == prop.id }
                                    app.cache.invalidateProfile()
                                }
                                .onFailure {
                                    snackbarHost?.showSnackbar("Не удалось отклонить предложение")
                                }
                        }
                    },
                )
            }
        }
        item { HorizontalDivider(Modifier.padding(horizontal = 20.dp)) }
        item {
            ProfileRow("История", Icons.Rounded.History, onHistory)
        }
        item {
            ProfileRow("Скачано", Icons.Rounded.CloudDownload, onDownloads)
        }
        item {
            Column {
                ProfileRow(
                    title = if (uploading) "Загружаем трек…" else "Загрузить трек",
                    icon = Icons.Rounded.Upload,
                    onClick = {
                        if (!uploading) uploadLauncher.launch(arrayOf("audio/*"))
                    },
                    enabled = !uploading,
                    loading = uploading,
                )
                if (uploading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 2.dp),
                    )
                }
            }
        }
        item { HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
        item {
            ProfileRow("Оформление", Icons.Rounded.Palette, onAppearance)
        }
        item {
            ProfileRow("Настройки", Icons.Rounded.Settings, onSettings)
        }
        item {
            ProfileRow(
                title = "Проверить обновления",
                icon = Icons.Rounded.SystemUpdate,
                onClick = { app.updater.checkManual() },
            )
        }
        item {
            ProfileRow("О приложении", Icons.Rounded.Info, onClick = { showAbout = true })
        }
        item { HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
        item {
            ProfileRow(
                title = "Выйти",
                icon = Icons.AutoMirrored.Rounded.Logout,
                onClick = { confirmLogout = true },
                destructive = true,
            )
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("KWhale") },
            text = {
                Text(
                    "Версия ${BuildConfig.VERSION_NAME} (сборка ${BuildConfig.VERSION_CODE})\n" +
                        "Сервер: ${app.settings.apiBaseUrl}",
                )
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("Закрыть") }
            },
        )
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Выйти из аккаунта?") },
            text = { Text("Вы сможете снова войти по email или выбрать другой сервер.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmLogout = false
                        onLogout()
                    },
                ) {
                    Text("Выйти", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Отмена") }
            },
        )
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
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    ListItem(
        headlineContent = {
            Text(
                title,
                color = when {
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                    destructive -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        },
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                tint = when {
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                    destructive -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        },
        trailingContent = if (loading) {
            {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            }
        } else {
            null
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp),
    )
}

@Composable
private fun EnrichmentCard(
    proposal: EnrichmentProposal,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
) {
    val p = proposal.proposed
    val changes = buildList {
        p.isrc?.let { add("ISRC: $it") }
        p.album?.let { add("Альбом: $it") }
        p.year?.let { add("Год: $it") }
        p.genre?.let { add("Жанр: $it") }
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "${proposal.title ?: "Трек"} · ${proposal.artists.joinToString()}",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(4.dp))
            changes.forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row {
                Button(onClick = onConfirm) { Text("Подтвердить") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onReject) { Text("Отклонить") }
            }
        }
    }
}

@Composable
private fun ProfileSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ShimmerBox(Modifier.size(88.dp), CircleShape)
        }
        Spacer(Modifier.size(12.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ShimmerBox(Modifier.width(170.dp).height(26.dp))
        }
        Spacer(Modifier.size(8.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ShimmerBox(Modifier.width(210.dp).height(14.dp))
        }
        Spacer(Modifier.size(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            repeat(3) {
                ShimmerBox(Modifier.width(72.dp).height(40.dp))
            }
        }
        Spacer(Modifier.size(28.dp))
        repeat(6) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShimmerBox(Modifier.size(24.dp), CircleShape)
                Spacer(Modifier.size(14.dp))
                ShimmerBox(Modifier.width(150.dp).height(16.dp))
            }
        }
    }
}
