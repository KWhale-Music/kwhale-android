package org.dueattendant149.kwhale.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dueattendant149.kwhale.network.KwhaleApi
import org.dueattendant149.kwhale.network.model.AppRelease
import java.io.File

/**
 * In-app updater: check → prompt (caller) → download → system installer.
 *
 * Check runs in the background at startup and on demand from the profile
 * screen. Download streams to cacheDir/updates (FileProvider-visible) and the
 * install intent goes through the platform package installer — the only way a
 * sideloaded app can update itself.
 */
class AppUpdater(
    private val apiProvider: () -> KwhaleApi,
    private val scope: CoroutineScope,
    private val currentVersionCode: () -> Int,
) {

    sealed interface State {
        data object Idle : State
        data object Checking : State
        data class Available(val release: AppRelease) : State
        data object UpToDate : State
        data class Downloading(val bytesRead: Long, val total: Long) : State
        data class ReadyToInstall(val file: File) : State
        data class Failed(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    private var lastCheckAt = 0L

    /** Silent unless something is available; safe to call on every start. */
    fun checkOnStartup() {
        val now = System.currentTimeMillis()
        if (now - lastCheckAt < 60_000) return // debounce: don't re-check on quick returns
        lastCheckAt = now
        scope.launch {
            if (_state.value is State.Available || _state.value is State.Downloading) return@launch
            val release = runCatching { apiProvider().appReleaseLatest() }.getOrNull() ?: return@launch
            if (release.versionCode > currentVersionCode()) {
                _state.value = State.Available(release)
            }
        }
    }

    /** Explicit user action: always reports the outcome. */
    fun checkManual() {
        scope.launch {
            _state.value = State.Checking
            val release = runCatching { apiProvider().appReleaseLatest() }.getOrElse {
                _state.value = State.Failed(it.message ?: "Не удалось проверить обновления")
                return@launch
            }
            _state.value = if (release == null || release.versionCode <= currentVersionCode()) {
                State.UpToDate
            } else {
                State.Available(release)
            }
        }
    }

    fun download(release: AppRelease, context: Context) {
        scope.launch {
            _state.value = State.Downloading(0, release.sizeBytes)
            runCatching {
                val dir = File(context.cacheDir, "updates").apply { mkdirs() }
                val target = File(dir, "kwhale-${release.versionName}.apk")
                withContext(Dispatchers.IO) {
                    apiProvider().downloadAppRelease(target) { read, total ->
                        _state.value = State.Downloading(read, total)
                    }
                }
            }.onSuccess { file ->
                _state.value = State.ReadyToInstall(file)
            }.onFailure {
                _state.value = State.Failed(it.message ?: "Не удалось скачать обновление")
            }
        }
    }

    fun dismiss() {
        if (_state.value !is State.Downloading) _state.value = State.Idle
    }

    companion object {
        /** Hand the APK to the system installer via FileProvider. */
        fun install(activity: Activity, file: File) {
            val uri: Uri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                file,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        }

        fun canInstallPackages(context: Context): Boolean =
            context.packageManager.canRequestPackageInstalls()

        fun openInstallPermissionSettings(context: Context) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
