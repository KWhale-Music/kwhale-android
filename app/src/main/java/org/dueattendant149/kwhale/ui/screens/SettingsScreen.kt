package org.dueattendant149.kwhale.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.dueattendant149.kwhale.BuildConfig
import org.dueattendant149.kwhale.KwhaleApp
import org.dueattendant149.kwhale.design.LocalMiniPlayerBottomPadding
import org.dueattendant149.kwhale.player.PlaybackCache
import org.dueattendant149.kwhale.ui.components.LocalSnackbarHost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    app: KwhaleApp,
    onBack: () -> Unit,
    onAppearance: () -> Unit,
    onDownloads: () -> Unit,
    onHistory: () -> Unit,
) {
    val context = LocalContext.current
    val snackbar = LocalSnackbarHost.current
    val miniPlayerBottom = LocalMiniPlayerBottomPadding.current
    val showAbout = remember { mutableStateOf(false) }
    val showCache = remember { mutableStateOf(false) }
    val showAccount = remember { mutableStateOf(false) }
    val settings = app.settings
    var prefetchEnabled by remember { mutableStateOf(settings.prefetchEnabled) }
    val scope = rememberCoroutineScope()
    val llmEndpoint = remember { mutableStateOf(settings.llmEndpoint) }
    val llmApiKey = remember { mutableStateOf(settings.llmApiKey) }
    val llmModel = remember { mutableStateOf(settings.llmModel) }

    var accountEmail by remember { mutableStateOf("") }
    var displayNameInput by remember { mutableStateOf("") }
    var savingAccount by remember { mutableStateOf(false) }
    var accountError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching { app.api.me() }.onSuccess { user ->
            accountEmail = user.email.orEmpty()
            displayNameInput = user.displayName
        }
    }

    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        scope.launch {
            snackbar?.showSnackbar(if (granted) "Уведомления включены" else "Уведомления отклонены")
        }
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
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp + miniPlayerBottom),
        ) {
            SettingsRow(
                title = "Аккаунт",
                subtitle = displayNameInput.ifBlank { accountEmail.ifBlank { null } },
                icon = Icons.Rounded.AccountCircle,
            ) {
                accountError = null
                showAccount.value = true
            }
            SettingsRow("Оформление", icon = Icons.Rounded.Palette, onClick = onAppearance)
            SettingsRow("Загрузки", icon = Icons.Rounded.CloudDownload, onClick = onDownloads)
            SettingsToggleRow(
                title = "Предзагрузка очереди",
                subtitle = "5 треков вперёд. Wi-Fi — целиком, мобильная сеть — 4 МБ на трек",
                checked = prefetchEnabled,
                onCheckedChange = {
                    prefetchEnabled = it
                    settings.prefetchEnabled = it
                },
            )
            SettingsRow("Кэш воспроизведения", icon = Icons.Rounded.PlayCircle) {
                showCache.value = true
            }
            SettingsRow("Уведомления", icon = Icons.Rounded.Notifications) {
                val granted = Build.VERSION.SDK_INT < 33 ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
                if (granted) {
                    scope.launch { snackbar?.showSnackbar("Уведомления включены") }
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
            SettingsRow("История", icon = Icons.Rounded.History, onClick = onHistory)
            SettingsRow("О приложении", icon = Icons.Rounded.Info) {
                showAbout.value = true
            }
            SettingsRow(
                title = "Сервер API",
                subtitle = settings.apiBaseUrl,
                icon = Icons.Rounded.Code,
            ) {
                scope.launch { snackbar?.showSnackbar("API: ${settings.apiBaseUrl}") }
            }
        }
    }

    if (showAccount.value) {
        AlertDialog(
            onDismissRequest = { if (!savingAccount) showAccount.value = false },
            title = { Text("Профиль аккаунта") },
            text = {
                Column {
                    if (accountEmail.isNotBlank()) {
                        Text(
                            text = accountEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    OutlinedTextField(
                        value = displayNameInput,
                        onValueChange = { displayNameInput = it },
                        label = { Text("Отображаемое имя") },
                        singleLine = true,
                        enabled = !savingAccount,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    accountError?.let { msg ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !savingAccount && displayNameInput.trim().isNotEmpty(),
                    onClick = {
                        val trimmed = displayNameInput.trim()
                        if (trimmed.isEmpty()) return@TextButton
                        savingAccount = true
                        accountError = null
                        scope.launch {
                            runCatching { app.api.updateMe(trimmed) }
                                .onSuccess { updated ->
                                    displayNameInput = updated.displayName
                                    app.cache.invalidateProfile()
                                    savingAccount = false
                                    showAccount.value = false
                                    snackbar?.showSnackbar("Имя обновлено")
                                }
                                .onFailure { err ->
                                    savingAccount = false
                                    accountError = err.message ?: "Не удалось обновить профиль"
                                }
                        }
                    },
                ) {
                    if (savingAccount) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Сохранить")
                    }
                }
            },
            dismissButton = {
                TextButton(enabled = !savingAccount, onClick = { showAccount.value = false }) {
                    Text("Отмена")
                }
            },
        )
    }

    if (showAbout.value) {
        AlertDialog(
            onDismissRequest = { showAbout.value = false },
            title = { Text("О приложении") },
            text = {
                Text(
                    "KWhale ${BuildConfig.VERSION_NAME} (сборка ${BuildConfig.VERSION_CODE})\n" +
                        "Музыкальный клиент KWhale",
                )
            },
            confirmButton = {
                TextButton(onClick = { showAbout.value = false }) { Text("OK") }
            },
        )
    }

    if (showCache.value) {
        val playbackCache = remember { PlaybackCache.get(context) }
        AlertDialog(
            onDismissRequest = { showCache.value = false },
            title = { Text("Кэш воспроизведения") },
            text = {
                Text(
                    "${playbackCache.usedMb} МБ из ${playbackCache.maxMb} МБ.\n\n" +
                        "Сюда попадают следующие 5 треков очереди и уже прослушанное. " +
                        "Ручные загрузки здесь не лежат и не удаляются.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showCache.value = false
                    scope.launch(Dispatchers.IO) {
                        playbackCache.clear()
                        launch(Dispatchers.Main) {
                            snackbar?.showSnackbar("Кэш воспроизведения очищен")
                        }
                    }
                }) { Text("Очистить") }
            },
            dismissButton = {
                TextButton(onClick = { showCache.value = false }) { Text("Закрыть") }
            },
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = null)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
    )
}
