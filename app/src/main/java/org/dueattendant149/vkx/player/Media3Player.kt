package org.dueattendant149.vkx.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
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
import org.dueattendant149.vkx.analytics.EventTracker
import org.dueattendant149.vkx.download.DownloadStore
import org.dueattendant149.vkx.model.Track

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
    private var lastIndex: Int = 0

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish()

        override fun onIsPlayingChanged(isPlaying: Boolean) = publish()

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                val ctrl = controller
                val idx = ctrl?.currentMediaItemIndex ?: lastIndex
                queue.getOrNull(idx)?.let { track ->
                    events.complete(track, currentSurface, ctrl?.duration)
                }
            }
            publish()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val ctrl = controller
            val newIdx = ctrl?.currentMediaItemIndex ?: 0
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                queue.getOrNull(lastIndex)?.let { prev ->
                    events.complete(prev, currentSurface)
                }
                queue.getOrNull(newIdx)?.let { next ->
                    events.play(next, currentSurface)
                }
            } else if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                queue.getOrNull(newIdx)?.let { track ->
                    events.repeat(track, currentSurface, 0L)
                }
            }
            lastIndex = newIdx
            publish()
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e("KWhale", "playback error: ${error.errorCodeName}", error)
            _playbackError.value = error.errorCodeName
            publish()
        }
    }

    init {
        controllerFuture.addListener(
            {
                controller = runCatching { controllerFuture.get() }.getOrNull()
                controller?.addListener(listener)
                pendingQueue?.let { playQueue(it, pendingIndex, currentSurface) }
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

    fun playQueue(tracks: List<Track>, startIndex: Int = 0, surface: String = "player") {
        if (tracks.isEmpty()) return
        currentSurface = surface
        _playbackError.value = null
        val current = controller
        if (current == null) {
            pendingQueue = tracks
            pendingIndex = startIndex
            return
        }
        pendingQueue = null
        queue = tracks
        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
        lastIndex = safeIndex
        val items = tracks.map(::mediaItemFor)
        current.setMediaItems(items, safeIndex, 0L)
        current.prepare()
        current.play()
        queue.getOrNull(safeIndex)?.let { events.play(it, currentSurface) }
        publish()
    }

    /** Append [tracks] to the end of the queue without interrupting playback. */
    fun addQueue(tracks: List<Track>, surface: String = "player") {
        if (tracks.isEmpty()) return
        currentSurface = surface
        val current = controller
        if (current == null) {
            pendingQueue = (pendingQueue ?: emptyList()) + tracks
            return
        }
        val wasEmpty = queue.isEmpty()
        queue = queue + tracks
        current.addMediaItems(tracks.map(::mediaItemFor))
        if (wasEmpty) {
            current.prepare()
            current.play()
            queue.getOrNull(0)?.let { events.play(it, currentSurface) }
        }
        publish()
    }

    fun toggle() {
        val current = controller ?: return
        val track = queue.getOrNull(current.currentMediaItemIndex)
        val pos = current.currentPosition.coerceAtLeast(0L)
        if (current.isPlaying) {
            current.pause()
            if (track != null) events.pause(track, currentSurface, pos)
        } else {
            current.play()
            if (track != null) events.resume(track, currentSurface, pos)
        }
    }

    fun next() {
        val current = controller ?: return
        val curIdx = current.currentMediaItemIndex
        queue.getOrNull(curIdx)?.let {
            events.skip(it, currentSurface, positionMs = current.currentPosition.coerceAtLeast(0L))
        }
        if (current.hasNextMediaItem()) {
            current.seekToNextMediaItem()
            queue.getOrNull(current.currentMediaItemIndex)?.let { events.play(it, currentSurface) }
        } else if (queue.isNotEmpty()) {
            current.seekToDefaultPosition(0)
            queue.getOrNull(0)?.let { events.play(it, currentSurface) }
        }
    }

    fun previous() {
        val current = controller ?: return
        if (current.currentPosition > 3_000L) {
            current.seekTo(0L)
            queue.getOrNull(current.currentMediaItemIndex)?.let {
                events.seek(it, currentSurface, 0L)
            }
        } else if (current.hasPreviousMediaItem()) {
            current.seekToPreviousMediaItem()
            queue.getOrNull(current.currentMediaItemIndex)?.let { events.play(it, currentSurface) }
        } else {
            current.seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        val current = controller ?: return
        val target = positionMs.coerceAtLeast(0L)
        current.seekTo(target)
        queue.getOrNull(current.currentMediaItemIndex)?.let {
            events.seek(it, currentSurface, target)
        }
    }

    fun jumpTo(index: Int) {
        val current = controller ?: return
        if (index !in queue.indices) return
        current.seekToDefaultPosition(index)
        current.play()
        lastIndex = index
        queue.getOrNull(index)?.let { events.play(it, currentSurface) }
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
        val enableRepeat = current.repeatMode != Player.REPEAT_MODE_ONE
        current.repeatMode = if (enableRepeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        if (enableRepeat) {
            queue.getOrNull(current.currentMediaItemIndex)?.let {
                events.repeat(it, currentSurface, current.currentPosition.coerceAtLeast(0L))
            }
        }
        publish()
    }

    fun stop() {
        controller?.run {
            stop()
            clearMediaItems()
        }
        queue = emptyList()
        pendingQueue = null
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
        _state.value = PlaybackState(
            queue = queue,
            index = current.currentMediaItemIndex.coerceIn(0, queue.lastIndex),
            isPlaying = current.isPlaying,
            positionMs = current.currentPosition.coerceAtLeast(0L),
            shuffle = current.shuffleModeEnabled,
            repeat = current.repeatMode == Player.REPEAT_MODE_ONE,
            durationMs = duration,
        )
    }

    private fun mediaItemFor(track: Track): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .apply {
                track.album?.takeIf { it.isNotBlank() }?.let { setAlbumTitle(it) }
                track.coverUrl?.takeIf { it.isNotBlank() }?.let {
                    setArtworkUri(Uri.parse(it))
                }
            }
            .build()

        return MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(uriFor(track))
            .setMediaMetadata(metadata)
            .build()
    }

    private fun uriFor(track: Track): Uri {
        // Offline: prefer the downloaded file, keyed by stable remoteId so playback
        // survives base-URL changes and works in airplane mode. streamUrl matching is
        // a legacy fallback (O(N)); remoteId lookup is O(1).
        track.remoteId?.let { rid ->
            downloads.get(rid)?.let { d -> if (d.file().exists()) return d.uri() }
        }
        downloads.localUriFor(track.streamUrl)?.let { return it }
        return Uri.parse(track.streamUrl.orEmpty())
    }
}
