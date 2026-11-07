package org.dueattendant149.kwhale.analytics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dueattendant149.kwhale.model.Track
import org.dueattendant149.kwhale.network.KwhaleApi
import org.dueattendant149.kwhale.network.model.Event
import org.dueattendant149.kwhale.network.model.EventContext
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Buffers recommendation signals and sends them to POST /events.
 * The API treats event ids as idempotency keys, so retries are safe.
 */
class EventTracker(
    private val api: KwhaleApi,
    private val scope: CoroutineScope,
) {
    private val pending = CopyOnWriteArrayList<Event>()
    private val sessionId = UUID.randomUUID().toString()
    private var playbackId: String? = null
    private var startedAt: String? = null
    private var trackDurationMs: Long? = null

    fun play(track: Track, surface: String = "player") {
        playbackId = UUID.randomUUID().toString()
        startedAt = Instant.now().toString()
        trackDurationMs = track.durationSec.toLong() * 1000L
        add(track, "play", surface, positionMs = 0L)
    }

    fun pause(track: Track, surface: String = "player", positionMs: Long = 0L) =
        add(track, "pause", surface, positionMs, listenedMs = positionMs)

    fun resume(track: Track, surface: String = "player", positionMs: Long = 0L) =
        add(track, "resume", surface, positionMs)

    fun seek(track: Track, surface: String = "player", positionMs: Long = 0L) =
        add(track, "seek", surface, positionMs)

    fun skip(track: Track, surface: String = "player", positionMs: Long? = null) {
        add(track, "skip", surface, positionMs, ended = true, listenedMs = positionMs)
    }

    fun complete(track: Track, surface: String = "player", durationMs: Long? = null) {
        val total = durationMs?.takeIf { it > 0L } ?: trackDurationMs ?: (track.durationSec.toLong() * 1000L)
        add(track, "complete", surface, total, ended = true, listenedMs = total)
    }

    fun repeat(track: Track, surface: String = "player", positionMs: Long = 0L) =
        add(track, "repeat", surface, positionMs)

    fun like(track: Track, surface: String = "player") =
        add(track, "like", surface)

    fun unlike(track: Track, surface: String = "player") =
        add(track, "unlike", surface)

    fun dislike(track: Track, surface: String = "player") =
        add(track, "dislike", surface)

    fun playlistAdd(track: Track, playlistId: String? = null, surface: String = "playlist") =
        add(track, "playlist_add", surface, playlistId = playlistId)

    fun share(track: Track, surface: String = "player") =
        add(track, "share", surface)

    private fun normalizeSurface(raw: String): String {
        val clean = raw.trim().lowercase()
        return if (clean in VALID_SURFACES) clean else "player"
    }

    fun impression(
        track: Track,
        surface: String = "recommendation",
        mixId: String? = null,
        position: Int? = null,
    ) = add(track, "impression", surface, mixId = mixId, clickedPosition = position)

    fun click(
        track: Track?,
        surface: String = "home",
        mixId: String? = null,
        position: Int? = null,
    ) = add(track, "click", surface, mixId = mixId, clickedPosition = position)

    fun mixClick(mixId: String) = click(track = null, surface = "home", mixId = mixId)

    private fun add(
        track: Track?,
        type: String,
        surface: String,
        positionMs: Long? = null,
        ended: Boolean = false,
        listenedMs: Long? = null,
        playlistId: String? = null,
        mixId: String? = null,
        clickedPosition: Int? = null,
    ) {
        val remoteId = track?.remoteId
        if (track != null && remoteId == null) return
        if (api.session() == null) return
        val validSurface = normalizeSurface(surface)
        val now = Instant.now()
        val offset = ZoneId.systemDefault().rules.getOffset(now).totalSeconds / 60
        pending.add(
            Event(
                id = UUID.randomUUID().toString(),
                type = type,
                trackId = remoteId,
                playbackId = playbackId,
                context = EventContext(source = validSurface, playlistId = playlistId, mixId = mixId),
                positionMs = positionMs,
                listenedMs = listenedMs,
                trackDurationMs = trackDurationMs ?: track?.durationSec?.toLong()?.times(1000L),
                startedAt = startedAt,
                endedAt = if (ended) now.toString() else null,
                timezoneOffsetMinutes = offset,
                surface = validSurface,
                sessionId = sessionId,
                deviceId = api.config.deviceId,
                clickedPosition = clickedPosition,
                occurredAt = now.toString(),
            ),
        )
        if (ended) {
            playbackId = null
            startedAt = null
            trackDurationMs = null
        }
        flush()
    }

    fun flush() {
        if (pending.isEmpty()) return
        val batch = pending.toList().take(50)
        scope.launch {
            runCatching { api.sendEvents(batch) }
                .onSuccess { pending.removeAll(batch.toSet()) }
        }
    }

    private companion object {
        val VALID_SURFACES = setOf(
            "home",
            "search",
            "library",
            "playlist",
            "album",
            "artist",
            "mix",
            "player",
            "recommendation",
            "notification",
            "external",
        )
    }
}
