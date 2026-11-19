package org.dueattendant149.kwhale.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.core.content.ContextCompat
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.dueattendant149.kwhale.analytics.EventTracker
import org.dueattendant149.kwhale.download.DownloadStore
import org.dueattendant149.kwhale.model.Track

/**
 * UI-side handle for the Media3 playback service. Playback itself lives in
 * [PlaybackService] so it survives screen changes and shows the media notification.
 */
class Media3Player(
    private val context: Context,
    private val scope: CoroutineScope,
    private val downloads: DownloadStore,
    private val events: EventTracker,
) {

    private val sessionToken = SessionToken(
        context,
        ComponentName(context, PlaybackService::class.java),
    )

    private val controllerFuture: ListenableFuture<MediaController> =
        MediaController.Builder(context, sessionToken).buildAsync()

    private var controller: MediaController? = null
    private var queue: List<Track> = emptyList()
    private var pendingQueue: List<Track>? = null
    private var pendingIndex: Int = 0
    private var currentSurface: String = "player"
    private var currentMixId: String? = null
    private var lastIndex: Int = 0
    private var playRequestedAt: Long = 0L
    private var loggedReadyKey: String? = null
    private var retriedErrorKey: String? = null
    private var accumulatedListenedMs: Long = 0L
    private var activePlayStartedElapsedMs: Long? = null
    private var completedForIdx: Int = -1
    private var finalizedForIdx: Int = -1

    val activeSurface: String get() = currentSurface
    val activeMixId: String? get() = currentMixId

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private fun syncListenedClock(isPlayingNow: Boolean) {
        val started = activePlayStartedElapsedMs
        if (started != null) {
            accumulatedListenedMs += (SystemClock.elapsedRealtime() - started).coerceAtLeast(0L)
            activePlayStartedElapsedMs = null
        }
        if (isPlayingNow) {
            activePlayStartedElapsedMs = SystemClock.elapsedRealtime()
        }
    }

    private fun currentListenedMs(): Long {
        val active = activePlayStartedElapsedMs?.let { (SystemClock.elapsedRealtime() - it).coerceAtLeast(0L) } ?: 0L
        return accumulatedListenedMs + active
    }

    private fun resetTrackTelemetry(index: Int, playing: Boolean = controller?.isPlaying == true) {
        accumulatedListenedMs = 0L
        activePlayStartedElapsedMs = if (playing) SystemClock.elapsedRealtime() else null
        completedForIdx = -1
        finalizedForIdx = -1
        lastIndex = index
    }

    private fun finalizeTrack(index: Int, endedNaturally: Boolean, durationHintMs: Long? = null) {
        if (index == finalizedForIdx || index !in queue.indices) return
        finalizedForIdx = index
        syncListenedClock(false)
        val track = queue[index]
        val listened = currentListenedMs().coerceAtLeast(0L)
        val durMs = durationHintMs?.takeIf { it > 0L }
            ?: controller?.duration?.takeIf { it > 0L }
            ?: (track.durationSec.toLong() * 1000L)
        val thresholdMs = if (durMs > 0L) {
            minOf(180_000L, (durMs * 0.80).toLong()).coerceAtLeast(15_000L)
        } else {
            30_000L
        }
        if (endedNaturally || listened >= thresholdMs) {
            if (index != completedForIdx) {
                completedForIdx = index
                events.complete(
                    track = track,
                    surface = currentSurface,
                    durationMs = durMs.takeIf { it > 0L },
                    listenedMs = if (endedNaturally && listened == 0L) durMs else listened,
                    mixId = currentMixId,
                )
            }
        } else {
            val pos = controller?.currentPosition?.coerceAtLeast(0L) ?: listened
            events.skip(
                track = track,
                surface = currentSurface,
                positionMs = pos,
                listenedMs = listened,
                mixId = currentMixId,
            )
        }
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish()

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            syncListenedClock(isPlaying)
            publish()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                val ctrl = controller
                val idx = ctrl?.currentMediaItemIndex ?: lastIndex
                finalizeTrack(idx, endedNaturally = true, durationHintMs = ctrl?.duration)
            }
            if (playbackState == Player.STATE_READY && playRequestedAt > 0L) {
                val idx = controller?.currentMediaItemIndex ?: -1
                val track = queue.getOrNull(idx)
                val key = track?.let { it.remoteId ?: it.id.toString() }
                if (track != null && key != null && key != loggedReadyKey) {
                    val elapsed = SystemClock.elapsedRealtime() - playRequestedAt
                    Log.i("KWhalePrefetch", "first-frame ${elapsed}ms (${track.title})")
                    loggedReadyKey = key
                }
            }
            publish()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            playRequestedAt = SystemClock.elapsedRealtime()
            loggedReadyKey = null
            retriedErrorKey = null
            val ctrl = controller
            val prevIdx = lastIndex
            val newIdx = ctrl?.currentMediaItemIndex ?: 0
            when (reason) {
                Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> {
                    finalizeTrack(prevIdx, endedNaturally = true)
                    resetTrackTelemetry(newIdx)
                    queue.getOrNull(newIdx)?.let { events.play(it, currentSurface, currentMixId) }
                }
                Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> {
                    if (prevIdx != newIdx) {
                        finalizeTrack(prevIdx, endedNaturally = false)
                        resetTrackTelemetry(newIdx)
                        queue.getOrNull(newIdx)?.let { events.play(it, currentSurface, currentMixId) }
                    }
                }
                Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> {
                    finalizeTrack(prevIdx, endedNaturally = true)
                    resetTrackTelemetry(newIdx)
                    queue.getOrNull(newIdx)?.let { events.repeat(it, currentSurface, 0L, currentMixId) }
                }
                else -> {
                    lastIndex = newIdx
                }
            }
            publish()
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e("KWhale", "playback error: ${error.errorCodeName}", error)
            _playbackError.value = readablePlaybackError(error)
            val ctrl = controller
            val track = queue.getOrNull(ctrl?.currentMediaItemIndex ?: -1)
            val key = track?.let { it.remoteId ?: it.id.toString() }
            val retryableIoError = error.errorCode in setOf(
                PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
                PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED,
                PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
            )
            if (ctrl != null && key != null && retryableIoError && retriedErrorKey != key) {
                // One quick retry per item: the stream proxy can hit a transient
                // upstream read error, and the token refresh path may have just
                // rotated the Authorization header.
                retriedErrorKey = key
                val pos = if (error.errorCode == PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE) {
                    0L
                } else {
                    ctrl.currentPosition.coerceAtLeast(0L)
                }
                playRequestedAt = SystemClock.elapsedRealtime()
                loggedReadyKey = null
                ctrl.prepare()
                ctrl.seekTo(pos)
                ctrl.play()
                _playbackError.value = null
                return
            }
            publish()
        }
    }

    init {
        controllerFuture.addListener(
            {
                controller = runCatching { controllerFuture.get() }.getOrNull()
                controller?.addListener(listener)
                pendingQueue?.let { playQueue(it, pendingIndex, currentSurface, currentMixId) }
                publish()
            },
            ContextCompat.getMainExecutor(context),
        )

        scope.launch {
            while (isActive) {
                publish()
                delay(250)
            }
        }
    }

    fun playQueue(
        tracks: List<Track>,
        startIndex: Int = 0,
        surface: String = "player",
        mixId: String? = null,
    ) {
        if (tracks.isEmpty()) return
        val previous = controller
        if (queue.isNotEmpty() && previous != null) {
            finalizeTrack(previous.currentMediaItemIndex, endedNaturally = false)
        }
        currentSurface = surface
        currentMixId = mixId?.takeIf { it.isNotBlank() }
        events.setPlaybackContext(currentSurface, currentMixId)
        _playbackError.value = null
        val current = controller
        if (current == null) {
            pendingQueue = tracks
            pendingIndex = startIndex
            return
        }
        pendingQueue = null
        queue = tracks
        playRequestedAt = SystemClock.elapsedRealtime()
        loggedReadyKey = null
        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
        resetTrackTelemetry(safeIndex, playing = true)
        val items = tracks.map(::mediaItemFor)
        current.setMediaItems(items, safeIndex, 0L)
        current.prepare()
        current.play()
        queue.getOrNull(safeIndex)?.let { events.play(it, currentSurface, currentMixId) }
        publish()
    }

    /** Insert [track] right after the currently playing item (or start playback if idle). */
    fun playNext(track: Track, surface: String = "player", mixId: String? = null) {
        val current = controller
        if (queue.isEmpty() || current == null) {
            playQueue(listOf(track), 0, surface, mixId)
            return
        }
        val insertIndex = (current.currentMediaItemIndex + 1).coerceIn(0, queue.size)
        val mutable = queue.toMutableList()
        mutable.add(insertIndex, track)
        queue = mutable
        current.addMediaItem(insertIndex, mediaItemFor(track))
        publish()
    }

    fun clearError() {
        _playbackError.value = null
    }

    fun retryCurrent() {
        val current = controller ?: return
        _playbackError.value = null
        playRequestedAt = SystemClock.elapsedRealtime()
        loggedReadyKey = null
        retriedErrorKey = null
        current.prepare()
        current.play()
        publish()
    }

    /** Append [tracks] to the end of the queue without interrupting playback. */
    fun addQueue(tracks: List<Track>, surface: String = "player", mixId: String? = null) {
        if (tracks.isEmpty()) return
        if (queue.isEmpty()) {
            currentSurface = surface
            currentMixId = mixId?.takeIf { it.isNotBlank() }
            events.setPlaybackContext(currentSurface, currentMixId)
        }
        val current = controller
        if (current == null) {
            pendingQueue = (pendingQueue ?: emptyList()) + tracks
            return
        }
        val wasEmpty = queue.isEmpty()
        queue = queue + tracks
        current.addMediaItems(tracks.map(::mediaItemFor))
        if (wasEmpty) {
            playRequestedAt = SystemClock.elapsedRealtime()
            loggedReadyKey = null
            resetTrackTelemetry(0, playing = true)
            current.prepare()
            current.play()
            queue.getOrNull(0)?.let { events.play(it, currentSurface, currentMixId) }
        }
        publish()
    }

    fun toggle() {
        val current = controller ?: return
        val track = queue.getOrNull(current.currentMediaItemIndex)
        val pos = current.currentPosition.coerceAtLeast(0L)
        if (current.isPlaying) {
            current.pause()
            if (track != null) events.pause(track, currentSurface, pos, currentMixId)
        } else {
            current.play()
            if (track != null) events.resume(track, currentSurface, pos, currentMixId)
        }
    }

    fun next() {
        val current = controller ?: return
        val curIdx = current.currentMediaItemIndex
        finalizeTrack(curIdx, endedNaturally = false)
        if (current.hasNextMediaItem()) {
            val singleItem = queue.size <= 1
            current.seekToNextMediaItem()
            if (singleItem) {
                resetTrackTelemetry(0, playing = true)
                queue.getOrNull(0)?.let { events.play(it, currentSurface, currentMixId) }
            }
        } else if (queue.isNotEmpty()) {
            val alreadyFirst = curIdx == 0
            current.seekToDefaultPosition(0)
            if (alreadyFirst) {
                resetTrackTelemetry(0, playing = true)
                queue.getOrNull(0)?.let { events.play(it, currentSurface, currentMixId) }
            }
        }
    }

    fun previous() {
        val current = controller ?: return
        val curIdx = current.currentMediaItemIndex
        if (current.currentPosition > 3_000L) {
            current.seekTo(0L)
            queue.getOrNull(curIdx)?.let {
                events.seek(it, currentSurface, 0L, currentMixId)
            }
        } else if (current.hasPreviousMediaItem()) {
            finalizeTrack(curIdx, endedNaturally = false)
            val singleItem = queue.size <= 1
            current.seekToPreviousMediaItem()
            if (singleItem) {
                resetTrackTelemetry(0, playing = true)
                queue.getOrNull(0)?.let { events.play(it, currentSurface, currentMixId) }
            }
        } else {
            current.seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        val current = controller ?: return
        val target = positionMs.coerceAtLeast(0L)
        current.seekTo(target)
        queue.getOrNull(current.currentMediaItemIndex)?.let {
            events.seek(it, currentSurface, target, currentMixId)
        }
    }

    fun jumpTo(index: Int) {
        val current = controller ?: return
        if (index !in queue.indices) return
        val curIdx = current.currentMediaItemIndex
        finalizeTrack(curIdx, endedNaturally = false)
        playRequestedAt = SystemClock.elapsedRealtime()
        loggedReadyKey = null
        current.seekToDefaultPosition(index)
        current.play()
        if (curIdx == index) {
            resetTrackTelemetry(index, playing = true)
            queue.getOrNull(index)?.let { events.play(it, currentSurface, currentMixId) }
        }
        publish()
    }

    fun removeFromQueue(index: Int) {
        val current = controller ?: return
        if (index !in queue.indices) return
        if (queue.size <= 1) {
            stop()
            return
        }
        val mutable = queue.toMutableList()
        mutable.removeAt(index)
        queue = mutable
        current.removeMediaItem(index)
        publish()
    }

    fun moveInQueue(fromIndex: Int, toIndex: Int) {
        val current = controller ?: return
        if (fromIndex !in queue.indices || toIndex !in queue.indices || fromIndex == toIndex) return
        val mutable = queue.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        queue = mutable
        current.moveMediaItem(fromIndex, toIndex)
        publish()
    }

    fun updateTrackLiked(trackId: Long, isLiked: Boolean) {
        queue = queue.map { if (it.id == trackId) it.copy(isLiked = isLiked) else it }
        publish()
    }

    fun toggleShuffle() {
        val current = controller ?: return
        current.shuffleModeEnabled = !current.shuffleModeEnabled
        publish()
    }

    fun toggleRepeat() {
        val current = controller ?: return
        val nextMode = when (current.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        current.repeatMode = nextMode
        if (nextMode != Player.REPEAT_MODE_OFF) {
            queue.getOrNull(current.currentMediaItemIndex)?.let {
                events.repeat(it, currentSurface, current.currentPosition.coerceAtLeast(0L), currentMixId)
            }
        }
        publish()
    }

    fun stop() {
        val current = controller
        if (queue.isNotEmpty()) {
            val idx = current?.currentMediaItemIndex ?: lastIndex
            finalizeTrack(idx, endedNaturally = false)
        }
        current?.run {
            stop()
            clearMediaItems()
        }
        queue = emptyList()
        pendingQueue = null
        resetTrackTelemetry(0, playing = false)
        _state.value = PlaybackState()
    }

    fun release() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        MediaController.releaseFuture(controllerFuture)
    }

    private fun publish() {
        if (queue.isEmpty()) return
        val current = controller ?: return
        val duration = current.duration.takeIf { it > 0L }
            ?: queue.getOrNull(current.currentMediaItemIndex)?.let { it.durationSec * 1000L }
            ?: 0L
        val mappedRepeat = when (current.repeatMode) {
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            else -> RepeatMode.OFF
        }
        _state.value = PlaybackState(
            queue = queue,
            index = current.currentMediaItemIndex.coerceIn(0, queue.lastIndex),
            isPlaying = current.isPlaying,
            isBuffering = current.playbackState == Player.STATE_BUFFERING,
            positionMs = current.currentPosition.coerceAtLeast(0L),
            shuffle = current.shuffleModeEnabled,
            repeatMode = mappedRepeat,
            durationMs = duration,
        )
    }

    private fun readablePlaybackError(error: PlaybackException): String = when (error.errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        -> "Нет связи с сервером"
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
        -> "Аудиопоток сейчас недоступен"
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        -> "Не удалось декодировать аудиофайл"
        else -> "Ошибка воспроизведения (${error.errorCodeName})"
    }

    @OptIn(UnstableApi::class)
    private fun mediaItemFor(track: Track): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            // Car head units often show the album tag where the app shows the
            // title. Broadcast the title as the album as well; the in-app UI
            // renders Track.album, so nothing changes on screen.
            .setAlbumTitle(track.title)
            .apply {
                track.coverUrl?.takeIf { it.isNotBlank() }?.let {
                    setArtworkUri(Uri.parse(it))
                }
            }
            .build()

        return MediaItem.Builder()
            .setMediaId(track.id.toString())
            // Key by the stable remote id, so the playback cache survives a
            // base-URL change and prefetch/playback share the same entry.
            // The v2 prefix is a one-time cache generation bump: v1 entries
            // were written before the server verified downloaded content and
            // can hold another recording's audio under a correct title (the
            // «body double»/«amy» mixup, 2026-11-18) — they must never play.
            .setCustomCacheKey("v2:track:${track.remoteId ?: track.id}")
            .setUri(uriFor(track))
            .setMediaMetadata(metadata)
            .build()
    }

    private fun uriFor(track: Track): Uri {
        // Offline: prefer the downloaded file, keyed by stable remoteId so playback
        // survives base-URL changes and works in airplane mode. streamUrl matching is
        // a legacy fallback (O(N)); remoteId lookup is O(1).
        track.remoteId?.let { rid ->
            downloads.get(rid)?.let { d ->
                // A downloaded file whose recorded duration contradicts the track
                // is a different recording (mislabeled source, 2026-11-18) — never
                // play it; stream the verified server copy instead. durationSec is
                // measured at download time (new records), so old records with the
                // copied metadata duration simply pass through.
                val mismatch = d.durationSec > 40 && track.durationSec > 40 &&
                    kotlin.math.abs(d.durationSec - track.durationSec) > 30
                if (!mismatch && d.file().exists()) return d.uri()
            }
        }
        downloads.localUriFor(track.streamUrl)?.let { return it }
        return Uri.parse(track.streamUrl.orEmpty())
    }
}
