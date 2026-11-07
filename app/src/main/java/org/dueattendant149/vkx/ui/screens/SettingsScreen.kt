package org.dueattendant149.vkx.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import org.dueattendant149.vkx.VkxApp
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
    val showAbout = remember { mutableStateOf(false) }
    val settings = (context.applicationContext as VkxApp).settings
    val llmEndpoint = remember { mutableStateOf(settings.llmEndpoint) }
    val llmApiKey = remember { mutableStateOf(settings.llmApiKey) }
    val llmModel = remember { mutableStateOf(settings.llmModel) }
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        Toast.makeText(
            context,
            if (granted) "Уведомления включены" else "Уведомления отклонены",
            Toast.LENGTH_SHORT,
        ).show()
    }
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
            SettingsRow("Загрузки", Icons.Rounded.CloudDownload, onDownloads)
            SettingsRow("Уведомления", Icons.Rounded.Notifications) {
                val granted = Build.VERSION.SDK_INT < 33 ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
                if (granted) {
                    Toast.makeText(context, "Уведомления включены", Toast.LENGTH_SHORT).show()
                } else if (Build.VERSION.SDK_INT >= 33) {
                    notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(
                "AI-микс (LLM)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            OutlinedTextField(
                value = llmEndpoint.value,
                onValueChange = { llmEndpoint.value = it; settings.llmEndpoint = it },
                label = { Text("Эндпоинт (OpenAI-совместимый)") },
                placeholder = { Text("https://api.deepseek.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            )
            OutlinedTextField(
                value = llmApiKey.value,
                onValueChange = { llmApiKey.value = it; settings.llmApiKey = it },
                label = { Text("API-ключ") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            )
            OutlinedTextField(
                value = llmModel.value,
                onValueChange = { llmModel.value = it; settings.llmModel = it },
                label = { Text("Модель (необязательно)") },
                placeholder = { Text("deepseek-chat") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SettingsRow("История", Icons.Rounded.History, onHistory)
            SettingsRow("О приложении", Icons.Rounded.Info) {
                showAbout.value = true
            }
            SettingsRow("Для разработчика", Icons.Rounded.Code) {
                Toast.makeText(context, "API: music.dueattendant149.org/v1", Toast.LENGTH_LONG).show()
            }
        }
    }

    if (showAbout.value) {
        AlertDialog(
            onDismissRequest = { showAbout.value = false },
            title = { Text("О приложении") },
            text = { Text("VK X · KWhale music\nМузыкальный сервис KWhale") },
            confirmButton = {
                TextButton(onClick = { showAbout.value = false }) { Text("OK") }
            },
        )
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
