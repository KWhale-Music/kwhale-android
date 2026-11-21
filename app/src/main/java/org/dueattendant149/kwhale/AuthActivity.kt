package org.dueattendant149.kwhale

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dueattendant149.kwhale.design.KwhaleAccents
import org.dueattendant149.kwhale.design.KwhaleTheme
import org.dueattendant149.kwhale.network.ApiConfig
import org.dueattendant149.kwhale.network.ApiException
import org.dueattendant149.kwhale.network.ApiFactory
import org.dueattendant149.kwhale.network.ServerUrl
import java.net.ConnectException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

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
                AuthScreen(app, onLoggedIn = ::openMain)
            }
        }
    }

    private fun openMain() {
        val pendingDeepLink = intent.getStringExtra(MainActivity.EXTRA_DEEP_LINK_URI)
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                if (!pendingDeepLink.isNullOrBlank()) {
                    putExtra(MainActivity.EXTRA_DEEP_LINK_URI, pendingDeepLink)
                }
            },
        )
        finish()
    }
}

@Composable
private fun AuthScreen(
    app: KwhaleApp,
    onLoggedIn: () -> Unit,
) {
    var registerMode by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    // Password is deliberately not rememberSaveable: it survives errors and
    // configuration changes, but never lands in a saved instance state bundle.
    var password by remember { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
    var invite by rememberSaveable { mutableStateOf("") }
    var server by rememberSaveable { mutableStateOf(app.settings.apiBaseUrl) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var serverStatus by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var showPresets by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun normalizeServer(): String? {
        val normalized = ServerUrl.normalize(server, app.settings.apiBaseUrl)
        if (normalized == null) {
            error = "Некорректный адрес сервера"
            return null
        }
        if (!BuildConfig.DEBUG && ServerUrl.isHttp(normalized)) {
            error = "В release-сборке сервер должен работать по https://"
            return null
        }
        return normalized
    }

    fun submit() {
        if (loading) return
        error = null
        if (email.isBlank() || password.isBlank()) {
            error = "Заполните email и пароль"
            return
        }
        if (registerMode && displayName.isBlank()) {
            error = "Укажите имя"
            return
        }
        if (registerMode && invite.isBlank()) {
            error = "Введите токен авторизации"
            return
        }
        if (registerMode && password.length < 8) {
            error = "Пароль: минимум 8 символов"
            return
        }
        val baseUrl = normalizeServer() ?: return
        if (baseUrl != app.settings.apiBaseUrl) {
            // No recreate(): the form stays exactly as the user typed it.
            app.reconfigureApi(baseUrl)
        }
        loading = true
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
                error = authErrorMessage(err)
                // The server answered (wrong password/token/email): clear only
                // the password. A network blip keeps everything, including it.
                if (err is ApiException && err.statusCode in 400..499) {
                    password = ""
                }
                loading = false
            }
        }
    }

    fun checkServer() {
        val baseUrl = normalizeServer() ?: return
        serverStatus = "Проверяем…"
        scope.launch {
            val result = runCatching {
                ApiFactory.createInMemory(
                    ApiConfig(
                        baseUrl = baseUrl,
                        clientVersion = BuildConfig.VERSION_NAME,
                        deviceId = "server-check",
                    ),
                ).health()
            }
            serverStatus = result.fold(
                onSuccess = { "Сервер доступен" },
                onFailure = { "Не отвечает: ${it.message ?: "ошибка"}" },
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
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
        Spacer(Modifier.height(28.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Подключение",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = server,
                    onValueChange = {
                        server = it
                        serverStatus = null
                    },
                    label = { Text("Сервер") },
                    placeholder = { Text("music.example.org") },
                    singleLine = true,
                    supportingText = {
                        Text("Домен или полный адрес. https:// добавится автоматически.")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { checkServer() }) { Text("Проверить") }
                    TextButton(onClick = { showPresets = true }) { Text("Пресеты") }
                }
                serverStatus?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (it.startsWith("Сервер доступен")) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = if (registerMode) "Создать аккаунт" else "Вход",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

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
                label = { Text("Токен авторизации") },
                supportingText = { Text("Одноразовый токен, его выдаёт администратор") },
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
            visualTransformation = if (showPassword) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) {
                            Icons.Rounded.VisibilityOff
                        } else {
                            Icons.Rounded.Visibility
                        },
                        contentDescription = if (showPassword) {
                            "Скрыть пароль"
                        } else {
                            "Показать пароль"
                        },
                    )
                }
            },
            supportingText = if (registerMode) {
                { Text("Минимум 8 символов") }
            } else {
                null
            },
            isError = registerMode && password.isNotEmpty() && password.length < 8,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { submit() },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(18.dp),
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
        TextButton(onClick = {
            registerMode = !registerMode
            error = null
        }) {
            Text(if (registerMode) "Уже есть аккаунт? Войти" else "Нет аккаунта? Создать")
        }
            }
        }
    }

    if (showPresets) {
        val presets = buildList {
            add("https://music.dueattendant149.org/v1")
            if (BuildConfig.DEBUG) {
                add("http://192.168.1.229:19001/v1")
                add("http://10.0.2.2:19001/v1")
                add("http://127.0.0.1:19001/v1")
            }
        }
        AlertDialog(
            onDismissRequest = { showPresets = false },
            title = { Text("Серверы") },
            text = {
                Column {
                    presets.forEach { preset ->
                        TextButton(onClick = {
                            server = preset
                            serverStatus = null
                            showPresets = false
                        }) {
                            Text(preset, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPresets = false }) { Text("Закрыть") }
            },
        )
    }
}

private fun authErrorMessage(err: Throwable): String? {
    if (err is ApiException) {
        val problem = err.problem
        return when {
            problem?.code == "invalid_invite" -> "Токен авторизации недействителен или истёк"
            problem?.code == "conflict" -> "Этот email уже зарегистрирован"
            problem?.code == "invalid_credentials" -> "Неверный email или пароль"
            problem?.fields?.containsKey("password") == true -> "Пароль: минимум 8 символов"
            problem?.fields?.containsKey("email") == true -> "Проверьте email"
            problem?.fields?.containsKey("inviteToken") == true -> "Проверьте токен авторизации"
            problem?.fields?.containsKey("displayName") == true -> "Укажите имя"
            else -> problem?.detail ?: problem?.title ?: err.message
        }
    }
    return when (err) {
        is UnknownHostException -> "Сервер не найден. Проверьте домен и интернет."
        is ConnectException -> "Не удалось подключиться к серверу."
        is SSLException -> "Ошибка HTTPS. Проверьте адрес сервера."
        else -> err.message
    } ?: "Ошибка сети"
}
