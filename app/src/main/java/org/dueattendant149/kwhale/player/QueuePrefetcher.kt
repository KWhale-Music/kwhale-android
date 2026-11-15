package org.dueattendant149.kwhale.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.dueattendant149.kwhale.SettingsStore

/**
 * Warms the next few queue items into [PlaybackCache].
 *
 * On unmetered networks it caches the whole file (capped at 80 MB), on
 * metered networks only the first few seconds. It deliberately does not use a
 * PriorityTaskManager: Media3's PriorityDataSource requires an explicit
 * add()/remove() registration, and without it every open throws
 * PriorityTooLowException before the upstream request (0.1.8 regression).
 */
@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
class QueuePrefetcher(
    context: Context,
    private val cache: Cache,
    upstreamFactory: DataSource.Factory,
    private val settings: SettingsStore,
    private val player: Player,
) : Player.Listener {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    @Volatile
    private var activeWriter: CacheWriter? = null

    @Volatile
    private var lastWindow: String? = null

    private val prefetchFactory = CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(upstreamFactory)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    init {
        player.addListener(this)
        schedule()
    }

    override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
        schedule()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        schedule()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        schedule()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        schedule()
    }

    fun release() {
        player.removeListener(this)
        job?.cancel()
        activeWriter?.cancel()
        scope.cancel()
    }

    private fun schedule() {
        if (!settings.prefetchEnabled) {
            lastWindow = null
            job?.cancel()
            activeWriter?.cancel()
            return
        }
        val count = player.mediaItemCount
        if (count == 0) {
            lastWindow = null
            return
        }
        val start = (player.currentMediaItemIndex + 1).coerceAtLeast(0)
        if (start >= count) {
            lastWindow = null
            return
        }
        val end = minOf(count, start + LOOKAHEAD)
        val items = (start until end).map(player::getMediaItemAt)
        val unmetered = isUnmetered()
        val window = "${if (unmetered) "wifi" else "cell"}:" +
            items.joinToString("|") { it.localConfiguration?.customCacheKey ?: it.mediaId }
        if (window == lastWindow) return
        lastWindow = window

        job?.cancel()
        activeWriter?.cancel()
        job = scope.launch {
            for (item in items) {
                if (!isActive || !settings.prefetchEnabled) break
                val configuration = item.localConfiguration ?: continue
                if (configuration.uri.scheme == "file") continue
                val key = configuration.customCacheKey ?: item.mediaId
                val limit = if (unmetered) UNMETERED_CAP_BYTES else METERED_CAP_BYTES
                val dataSpec = DataSpec.Builder()
                    .setUri(configuration.uri)
                    .setKey(key)
                    .setLength(limit)
                    .build()
                val writer = CacheWriter(
                    prefetchFactory.createDataSourceForDownloading(),
                    dataSpec,
                    ByteArray(BUFFER_BYTES),
                    null,
                )
                activeWriter = writer
                try {
                    Log.i(TAG, "prefetch start $key (cap ${limit / (1024 * 1024)} MB)")
                    writer.cache()
                    Log.i(TAG, "prefetch done $key")
                } catch (error: Exception) {
                    if (isActive) Log.w(TAG, "prefetch failed $key: ${error.message}")
                } finally {
                    activeWriter = null
                }
            }
        }
    }

    private fun isUnmetered(): Boolean {
        val manager = appContext.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    companion object {
        private const val TAG = "KWhalePrefetch"
        private const val LOOKAHEAD = 5
        private const val METERED_CAP_BYTES = 4L * 1024 * 1024
        private const val UNMETERED_CAP_BYTES = 80L * 1024 * 1024
        private const val BUFFER_BYTES = 64 * 1024
    }
}
