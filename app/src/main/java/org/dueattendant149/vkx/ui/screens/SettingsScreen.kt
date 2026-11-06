package org.dueattendant149.vkx.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.dueattendant149.vkx.ui.components.CoverArt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAppearance: () -> Unit,
    onDownloads: () -> Unit,
    onHistory: () -> Unit,
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Настройки") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                }
            },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsRow("Аккаунт", Icons.Rounded.AccountCircle) {
                Toast.makeText(context, "Аккаунт управляется на сервере", Toast.LENGTH_SHORT).show()
            }
            SettingsRow("Оформление", Icons.Rounded.Palette, onAppearance)
            SettingsRow("Воспроизведение", Icons.Rounded.PlayCircle) {
                Toast.makeText(context, "Настройки воспроизведения скоро", Toast.LENGTH_SHORT).show()
            }
            SettingsRow("Загрузки", Icons.Rounded.CloudDownload, onDownloads)
            SettingsRow("Уведомления", Icons.Rounded.Notifications) {
                val granted = Build.VERSION.SDK_INT < 33 ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
                Toast.makeText(
                    context,
                    if (granted) "Уведомления включены" else "Уведомления отключены (разрешите в системе)",
                    Toast.LENGTH_SHORT,
                ).show()
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SettingsRow("История", Icons.Rounded.History, onHistory)
            SettingsRow("О приложении", Icons.Rounded.Info) {
                Toast.makeText(context, "VK X · KWhale music", Toast.LENGTH_SHORT).show()
            }
            SettingsRow("Для разработчика", Icons.Rounded.Code) {
                Toast.makeText(context, "API: music.dueattendant149.org/v1", Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}
