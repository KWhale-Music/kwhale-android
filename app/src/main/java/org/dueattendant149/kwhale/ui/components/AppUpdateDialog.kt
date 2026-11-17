package org.dueattendant149.kwhale.ui.components

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.dueattendant149.kwhale.update.AppUpdater

/**
 * One dialog for the whole update flow: available → downloading → install.
 * Reduced-motion friendly by construction (no decorative animation, just
 * progress).
 */
@Composable
fun AppUpdateDialog(updater: AppUpdater, currentVersionName: String) {
    val state by updater.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    when (val s = state) {
        is AppUpdater.State.Available -> AlertDialog(
            onDismissRequest = { updater.dismiss() },
            icon = { Icon(Icons.Rounded.SystemUpdate, contentDescription = null) },
            title = { Text("Доступно обновление ${s.release.versionName}") },
            text = {
                Column {
                    Text(
                        "Сейчас установлена версия $currentVersionName",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    s.release.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 8,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    activity?.let { updater.download(s.release, it) }
                }) { Text("Обновить") }
            },
            dismissButton = {
                TextButton(onClick = { updater.dismiss() }) { Text("Позже") }
            },
        )

        is AppUpdater.State.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Скачиваем обновление…") },
            text = {
                Column {
                    val progress =
                        if (s.total > 0) s.bytesRead.toFloat() / s.total else 0f
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    if (s.total > 0) {
                        Text(
                            "${s.bytesRead / 1024} / ${s.total / 1024} КБ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {},
        )

        is AppUpdater.State.ReadyToInstall -> {
            // Fire the installer once and fall back to Idle.
            val file = s.file
            androidx.compose.runtime.LaunchedEffect(file) {
                if (activity != null) {
                    if (AppUpdater.canInstallPackages(activity)) {
                        AppUpdater.install(activity, file)
                    } else {
                        AppUpdater.openInstallPermissionSettings(activity)
                    }
                }
                updater.dismiss()
            }
        }

        is AppUpdater.State.Checking -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Проверяем обновления…") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Секунду", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {},
        )

        is AppUpdater.State.Failed -> AlertDialog(
            onDismissRequest = { updater.dismiss() },
            title = { Text("Обновление недоступно") },
            text = { Text(s.message) },
            confirmButton = {
                TextButton(onClick = { updater.dismiss() }) { Text("Закрыть") }
            },
        )

        is AppUpdater.State.UpToDate -> AlertDialog(
            onDismissRequest = { updater.dismiss() },
            title = { Text("У вас последняя версия") },
            text = { Text("Версия $currentVersionName — новее нет.") },
            confirmButton = {
                TextButton(onClick = { updater.dismiss() }) { Text("Отлично") }
            },
        )

        else -> Unit // Idle: no dialog
    }
}
