package org.dueattendant149.kwhale.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import org.dueattendant149.kwhale.MainActivity
import org.dueattendant149.kwhale.KwhaleApp

@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var prefetcher: QueuePrefetcher? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as KwhaleApp
        val httpFactory = AuthHttpDataSourceFactory(
            tokenProvider = { app.api.session()?.accessToken },
            allowedHost = { app.api.config.baseHost },
            refreshToken = { stale -> app.api.refreshSession(stale) },
        )
        val upstreamFactory = DefaultDataSource.Factory(this, httpFactory)
        val playbackCache = PlaybackCache.get(this)

        val cachedFactory = CacheDataSource.Factory()
            .setCache(playbackCache.cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            // A full/corrupt cache must never stop playback; skip the cache
            // and stream directly instead.
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val dataSourceFactory = SchemeRoutingDataSourceFactory(
            cached = cachedFactory,
            direct = DefaultDataSource.Factory(this),
        )
        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true,
                )
                setHandleAudioBecomingNoisy(true)
            }

        prefetcher = QueuePrefetcher(
            context = this,
            cache = playbackCache.cache,
            upstreamFactory = upstreamFactory,
            settings = app.settings,
            player = exoPlayer,
        )

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null ||
            !player.playWhenReady ||
            player.mediaItemCount == 0 ||
            player.playbackState == Player.STATE_ENDED
        ) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        prefetcher?.release()
        prefetcher = null
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
