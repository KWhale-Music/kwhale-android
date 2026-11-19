package org.dueattendant149.kwhale.ui.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dueattendant149.kwhale.KwhaleApp
import org.dueattendant149.kwhale.network.model.Track as ApiTrack
import org.dueattendant149.kwhale.network.model.TrackInfo as ApiTrackInfo
import org.dueattendant149.kwhale.ui.components.ErrorState

/**
 * «О треке»: metadata, measured audio quality (track_quality) and sources.
 *
 * The feedback asked for a way to see what the library actually stores for a
 * track — format, bitrate, size, where it plays from.
 */
@Composable
fun TrackInfoSheet(
    app: KwhaleApp,
    track: ApiTrack,
    onDismiss: () -> Unit,
) {
    var info by remember(track.id) { mutableStateOf<ApiTrackInfo?>(null) }
    var failed by remember(track.id) { mutableStateOf(false) }

    LaunchedEffect(track.id) {
        runCatching { app.api.trackInfo(track.id) }
            .onSuccess { info = it; failed = false }
            .onFailure { failed = true }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp),
    ) {
        Text(
            text = "О треке",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        when {
            failed -> ErrorState(
                message = "Не удалось загрузить сведения о треке",
                onRetry = {
                    failed = false
                    info = null
                },
                modifier = Modifier.fillMaxWidth(),
            )
            info == null -> CircularProgressIndicator(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            )
            else -> {
                val data = info!!
                val meta = data.track
                InfoRow("Название", meta.title)
                InfoRow("Исполнитель", meta.artists.joinToString(", ") { it.name }.ifBlank { "—" })
                meta.album?.let { InfoRow("Альбом", it.title) }
                meta.year?.let { InfoRow("Год", it.toString()) }
                meta.genre?.let { InfoRow("Жанр", it.name) }
                data.isrc?.let { InfoRow("ISRC", it) }
                val measured = data.quality?.measuredDurationMs
                InfoRow(
                    "Длительность",
                    formatDuration(measured ?: meta.durationMs),
                )

                val quality = data.quality
                if (quality != null) {
                    val format = buildString {
                        append(quality.codec?.uppercase() ?: "—")
                        if (quality.lossless) append(" · lossless")
                    }
                    InfoRow("Формат", format)
                    quality.bitrateKbps?.let { InfoRow("Битрейт", "$it кбит/с") }
                    quality.sampleRate?.let { InfoRow("Частота", "${it / 1000} кГц") }
                    quality.bitDepth?.let { InfoRow("Разрядность", "$it бит") }
                    quality.channels?.let {
                        InfoRow(
                            "Каналы",
                            when (it) {
                                1 -> "моно"
                                2 -> "стерео"
                                else -> "$it"
                            },
                        )
                    }
                    quality.bytes?.takeIf { it > 0 }?.let { InfoRow("Размер", formatBytes(it)) }
                    quality.error?.let { InfoRow("Проблема", it) }
                } else {
                    InfoRow("Качество", "файл ещё не сканировался")
                }

                InfoRow(
                    "В библиотеке",
                    when (meta.downloadStatus) {
                        "ready" -> "да, играет с сервера"
                        "downloading" -> "скачивается"
                        "failed" -> "ошибка загрузки"
                        else -> "нет, играет из источника"
                    },
                )
                if (data.sources.isNotEmpty()) {
                    InfoRow(
                        "Источники",
                        data.sources.joinToString(", ") { source ->
                            source.connector + if (source.available) "" else " (недоступен)"
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(130.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun formatDuration(ms: Long): String {
    val total = (ms / 1000L).toInt().coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> "%.1f ГБ".format(bytes / (1024.0 * 1024.0 * 1024.0))
    bytes >= 1024L * 1024L -> "%.1f МБ".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "%.0f КБ".format(bytes / 1024.0)
    else -> "$bytes Б"
}
