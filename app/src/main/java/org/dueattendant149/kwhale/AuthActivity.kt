package org.dueattendant149.kwhale

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dueattendant149.kwhale.design.KwhaleAccents
import org.dueattendant149.kwhale.design.KwhaleTheme
import org.dueattendant149.kwhale.network.ApiException

class AuthActivity : ComponentActivity() {

    private lateinit var app: KwhaleApp

    override fun onCreate(savedInstanceState: Bundle?) {
        app = application as KwhaleApp
        super.onCreate(savedInstanceState)
        if (app.api.session() != null) {
            openMain()
            return
        }
        enableEdgeToEdge()
        setContent {
            KwhaleTheme(
                mode = app.settings.themeMode,
                accent = KwhaleAccents.byId(app.settings.accentId),
                monet = app.settings.monet,
            ) {
                AuthScreen(app, onLoggedIn = ::openMain, onServerChanged = {
                    recreate()
                })
            }
        }
    }

    private fun openMain() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            },
        )
        finish()
    }
}

@Composable
private fun AuthScreen(
    app: KwhaleApp,
    onLoggedIn: () -> Unit,
    onServerChanged: () -> Unit,
) {
    var registerMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var invite by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var showServer by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        Text(
            text = "KWhale",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Музыка для себя",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = if (registerMode) "Создать аккаунт" else "Вход",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        if (registerMode) {
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Имя") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = invite,
                onValueChange = { invite = it },
                label = { Text("Инвайт-код") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (!loading) {
                    loading = true
                    error = null
                    scope.launch {
                        runCatching {
                            if (registerMode) {
                                app.api.register(email.trim(), password, displayName.trim(), invite.trim())
                            } else {
                                app.api.login(email.trim(), password)
                            }
                        }.onSuccess {
                            app.settings.loggedIn = true
                            if (registerMode) app.settings.onboardingDone = false
                            onLoggedIn()
                        }.onFailure { err ->
                            error = when (err) {
                                is ApiException -> err.problem?.detail
                                    ?: err.problem?.title
                                    ?: err.message
                                else -> err.message
                            } ?: "Ошибка сети"
                            loading = false
                        }
                    }
                }
            }),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                if (loading) return@Button
                if (email.isBlank() || password.isBlank()) {
                    error = "Заполните email и пароль"
                    return@Button
                }
                if (registerMode && (displayName.isBlank() || invite.isBlank())) {
                    error = "Заполните имя и инвайт-код"
                    return@Button
                }
                loading = true
                error = null
                scope.launch {
                    runCatching {
                        if (registerMode) {
                            app.api.register(email.trim(), password, displayName.trim(), invite.trim())
                        } else {
                            app.api.login(email.trim(), password)
                        }
                    }.onSuccess {
                        app.settings.loggedIn = true
                        if (registerMode) app.settings.onboardingDone = false
                        onLoggedIn()
                    }.onFailure { err ->
                        error = when (err) {
                            is ApiException -> err.problem?.detail
                                ?: err.problem?.title
                                ?: err.message
                            else -> err.message
                        } ?: "Ошибка сети"
                        loading = false
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp),
                    strokeWidth = 2.dp,
                )
            }
            Text(if (registerMode) "Создать аккаунт" else "Войти")
        }
        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = { registerMode = !registerMode; error = null }) {
            Text(if (registerMode) "Уже есть аккаунт? Войти" else "Нет аккаунта? Создать")
        }
        TextButton(onClick = { showServer = true }) {
            Text(
                text = "Сервер: ${app.settings.apiBaseUrl}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    if (showServer) {
        var serverUrl by remember { mutableStateOf(app.settings.apiBaseUrl) }
        AlertDialog(
            onDismissRequest = { showServer = false },
            title = { Text("Сервер API") },
            text = {
                Column {
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("Base URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "http://127.0.0.1:19001/v1",
                        "http://10.0.2.2:19001/v1",
                        "https://music.dueattendant149.org/v1",
                    ).forEach { preset ->
                        TextButton(onClick = { serverUrl = preset }) {
                            Text(preset, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    app.reconfigureApi(serverUrl)
                    showServer = false
                    onServerChanged()
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showServer = false }) { Text("Отмена") }
            },
        )
    }
}
