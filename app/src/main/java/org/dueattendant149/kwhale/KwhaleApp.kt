package org.dueattendant149.kwhale

import android.app.Application
import android.provider.Settings
import androidx.glance.appwidget.updateAll
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.dueattendant149.kwhale.analytics.EventTracker
import org.dueattendant149.kwhale.data.ApiReactionGateway
import org.dueattendant149.kwhale.data.ContentCache
import org.dueattendant149.kwhale.data.SearchSession
import org.dueattendant149.kwhale.update.AppUpdater
import org.dueattendant149.kwhale.data.TrackReactionStore
import org.dueattendant149.kwhale.download.DownloadObserver
import org.dueattendant149.kwhale.download.DownloadStore
import org.dueattendant149.kwhale.network.ApiConfig
import org.dueattendant149.kwhale.network.ApiFactory
import org.dueattendant149.kwhale.network.HostPolicyInterceptor
import org.dueattendant149.kwhale.network.KwhaleApi
import org.dueattendant149.kwhale.player.Media3Player
import org.dueattendant149.kwhale.widget.PlayerSnapshotStore
import org.dueattendant149.kwhale.widget.PlayerWidget
import java.util.UUID

class KwhaleApp : Application() {

    lateinit var settings: SettingsStore
        private set

    lateinit var player: Media3Player
        private set

    lateinit var downloads: DownloadStore
        private set

    lateinit var downloadObserver: DownloadObserver
        private set

    lateinit var reactions: TrackReactionStore
        private set

    lateinit var events: EventTracker
        private set

    lateinit var api: KwhaleApi
        private set

    lateinit var cache: ContentCache
        private set

    val searchSession: SearchSession = SearchSession()

    val updater: AppUpdater by lazy {
        AppUpdater(
            fetchLatest = { api.appReleaseLatest() },
            downloadApk = { target, onProgress -> api.downloadAppRelease(target, onProgress) },
            scope = appScope,
            currentVersionCode = { BuildConfig.VERSION_CODE },
        )
    }

    lateinit var widgetSnapshot: PlayerSnapshotStore
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        cache = ContentCache(this)
        settings = SettingsStore(this)
        downloads = DownloadStore(this)
        widgetSnapshot = PlayerSnapshotStore(this)
        initApiAndEvents(settings.apiBaseUrl)
        player = Media3Player(this, appScope, downloads, events)
        downloadObserver = DownloadObserver(this, downloads, appScope)
        reactions = TrackReactionStore(
            gateway = ApiReactionGateway(apiProvider = { api }, eventsProvider = { events }),
            scope = appScope,
            onPlayerTrackLiked = { id, liked -> player.updateTrackLiked(id, liked) },
        )

        appScope.launch {
            player.state
                .map { Triple(it.current, it.isPlaying, it.current?.coverUrl) }
                .distinctUntilChanged()
                .collect { (track, isPlaying, _) ->
                    if (track != null) {
                        widgetSnapshot.save(track, isPlaying)
                    } else {
                        // Keep the last track visible on the widget, just paused.
                        widgetSnapshot.setPlaying(false)
                    }
                    runCatching { PlayerWidget().updateAll(this@KwhaleApp) }
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
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache"))
                        .maxSizeBytes(256L * 1024 * 1024)
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
