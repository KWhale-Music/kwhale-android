package org.dueattendant149.kwhale.ui.sheets

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.dueattendant149.kwhale.KwhaleApp
import org.dueattendant149.kwhale.model.Track as UiTrack
import org.dueattendant149.kwhale.network.model.ShareKind
import org.dueattendant149.kwhale.network.model.ShareLink
import org.dueattendant149.kwhale.network.model.SharePlatformLink
import org.dueattendant149.kwhale.ui.components.CoverArt

data class ShareTarget(
    val kind: ShareKind,
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val meta: String? = null,
    val coverUrl: String? = null,
    val fallbackText: String,
    val surface: String,
    val analyticsTrack: UiTrack? = null,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShareSheet(
    app: KwhaleApp,
    target: ShareTarget,
    onDismiss: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var reload by remember { mutableIntStateOf(0) }
    var loading by remember(target.kind, target.id) { mutableStateOf(true) }
    var shareLink by remember(target.kind, target.id) { mutableStateOf<ShareLink?>(null) }
    var error by remember(target.kind, target.id) { mutableStateOf<String?>(null) }
    var justCopied by remember { mutableStateOf(false) }

    LaunchedEffect(target.kind, target.id, reload) {
        if (target.id.isBlank()) {
            shareLink = null
            loading = false
            return@LaunchedEffect
        }
        loading = true
        error = null
        runCatching { app.api.createShare(target.kind, target.id) }
            .onSuccess {
                shareLink = it
                loading = false
            }
            .onFailure {
                error = "Не удалось создать веб-превью"
                loading = false
            }
    }

    LaunchedEffect(justCopied) {
        if (justCopied) {
            delay(1800)
            justCopied = false
        }
    }

    val kindLabel = when (target.kind) {
        ShareKind.TRACK -> "ТРЕК"
        ShareKind.ALBUM -> "АЛЬБОМ"
        ShareKind.ARTIST -> "ИСПОЛНИТЕЛЬ"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
    ) {
        // M3 media identity header without nested card clutter.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverArt(
                imageUrl = target.coverUrl,
                modifier = Modifier.size(64.dp),
                shape = if (target.kind == ShareKind.ARTIST) CircleShape else RoundedCornerShape(14.dp),
                iconSize = 24,
                seed = target.title,
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            text = kindLabel,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Text(
                        text = "Ссылка на ${shareLink?.ttlDays ?: 7} дн.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = target.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                target.subtitle?.takeIf { it.isNotBlank() }?.let { sub ->
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                target.meta?.takeIf { it.isNotBlank() }?.let { m ->
                    Text(
                        text = m,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        Spacer(Modifier.height(16.dp))

        if (loading) {
            Text(
                text = "Собираем превью и ссылки на площадки…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }

        shareLink?.let { link ->
            // Quiet M3 surface container displaying the canonical short URL.
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = link.shareUrl.removePrefix("https://").removePrefix("http://"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.shareUrl)))
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = "Открыть превью в браузере",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(link.shareUrl))
                            justCopied = true
                            onMessage("Ссылка скопирована")
                        },
                    ) {
                        Icon(
                            imageVector = if (justCopied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                            contentDescription = "Скопировать ссылку",
                            tint = if (justCopied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (link.platforms.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                val exactCount = link.platforms.count { it.exact }
                Text(
                    text = if (exactCount > 0) {
                        "Площадки на странице · $exactCount точно"
                    } else {
                        "Площадки на странице · поиск по каталогу"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    link.platforms.forEach { plat ->
                        PlatformBadge(plat)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        if (error != null && shareLink == null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$error — можно отправить текстом",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { reload++ }) {
                        Text("Повторить")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Single clear hierarchy: Primary Filled CTA + secondary Tonal action.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = {
                    target.analyticsTrack?.let { app.events.share(it, target.surface) }
                    val payload = shareLink?.let { "${target.fallbackText}\n${it.shareUrl}" } ?: target.fallbackText
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, payload)
                    }
                    context.startActivity(Intent.createChooser(intent, "Поделиться"))
                    onDismiss()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (shareLink != null) "Отправить" else "Отправить текстом")
            }
            if (shareLink != null) {
                FilledTonalButton(
                    onClick = {
                        target.analyticsTrack?.let { app.events.share(it, target.surface) }
                        clipboard.setText(AnnotatedString(shareLink!!.shareUrl))
                        justCopied = true
                        onMessage("Ссылка скопирована")
                    },
                    modifier = Modifier.height(48.dp),
                ) {
                    Icon(
                        if (justCopied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (justCopied) "Скопировано" else "Копировать")
                }
            }
        }
    }
}

@Composable
private fun PlatformBadge(item: SharePlatformLink) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = if (item.exact) Icons.Rounded.Check else Icons.Rounded.Search,
                contentDescription = if (item.exact) "Точная ссылка" else "Поиск",
                tint = if (item.exact) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelMedium,
                color = if (item.exact) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
