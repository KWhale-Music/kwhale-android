package org.dueattendant149.kwhale.player

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Process-wide ExoPlayer cache for streamed audio.
 *
 * It lives outside [PlaybackService] so it survives a service restart and can
 * be inspected/cleared from Settings. Manual downloads are separate files in
 * filesDir and are never touched by this cache.
 */
@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
class PlaybackCache private constructor(context: Context) {

    val cache: SimpleCache
    private val dir = File(context.cacheDir, "playback-cache")

    init {
        val databaseProvider = StandaloneDatabaseProvider(context.applicationContext)
        cache = runCatching {
            SimpleCache(
                dir,
                LeastRecentlyUsedCacheEvictor(MAX_BYTES),
                databaseProvider,
            )
        }.getOrElse {
            // A stale lock (process killed mid-write) must not brick playback.
            SimpleCache.delete(dir, databaseProvider)
            SimpleCache(
                dir,
                LeastRecentlyUsedCacheEvictor(MAX_BYTES),
                databaseProvider,
            )
        }
    }

    val usedBytes: Long get() = runCatching { cache.cacheSpace }.getOrDefault(0L)

    val usedMb: Long get() = usedBytes / (1024 * 1024)

    val maxMb: Long get() = MAX_BYTES / (1024 * 1024)

    fun clear() {
        runCatching {
            cache.keys.toList().forEach(cache::removeResource)
        }
    }

    companion object {
        const val MAX_BYTES: Long = 1024L * 1024 * 1024

        @Volatile
        private var instance: PlaybackCache? = null

        fun get(context: Context): PlaybackCache =
            instance ?: synchronized(this) {
                instance ?: PlaybackCache(context.applicationContext).also { instance = it }
            }
    }
}
