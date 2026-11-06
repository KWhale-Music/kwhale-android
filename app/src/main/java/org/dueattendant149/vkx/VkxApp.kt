package org.dueattendant149.vkx

import android.app.Application
import android.provider.Settings
import androidx.glance.appwidget.updateAll
import coil.Coil
import coil.ImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.dueattendant149.vkx.analytics.EventTracker
import org.dueattendant149.vkx.download.DownloadStore
import org.dueattendant149.vkx.network.ApiConfig
import org.dueattendant149.vkx.network.ApiFactory
import org.dueattendant149.vkx.network.HostPolicyInterceptor
import org.dueattendant149.vkx.network.VkxApi
import org.dueattendant149.vkx.player.Media3Player
import org.dueattendant149.vkx.widget.PlayerWidget
import java.util.UUID

class VkxApp : Application() {

    lateinit var settings: SettingsStore
        private set

    lateinit var player: Media3Player
        private set

    lateinit var downloads: DownloadStore
        private set

    lateinit var events: EventTracker
        private set

    lateinit var api: VkxApi
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        settings = SettingsStore(this)
        downloads = DownloadStore(this)
        initApiAndEvents(settings.apiBaseUrl)
        player = Media3Player(this, appScope, downloads, events)

        appScope.launch {
            player.state
                .map { it.current?.id to it.isPlaying }
                .distinctUntilChanged()
                .collect {
                    runCatching { PlayerWidget().updateAll(this@VkxApp) }
                }
        }
    }

    fun reconfigureApi(newBaseUrl: String) {
        settings.apiBaseUrl = newBaseUrl
        initApiAndEvents(settings.apiBaseUrl)
    }

    private fun initApiAndEvents(baseUrl: String) {
        api = ApiFactory.create(
            context = this,
            config = ApiConfig(
                baseUrl = baseUrl,
                clientVersion = BuildConfig.VERSION_NAME,
                deviceId = deviceId(),
            ),
        )
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .okHttpClient {
                    OkHttpClient.Builder()
                        .addInterceptor(HostPolicyInterceptor(api.config.baseHost))
                        .build()
                }
                .build(),
        )
        events = EventTracker(api, appScope)
    }

    private fun deviceId(): String {
        val androidId = runCatching {
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()
        return androidId
            ?.takeIf { it.isNotBlank() && it != "9774d56d682e549c" }
            ?: UUID.randomUUID().toString()
    }
}
