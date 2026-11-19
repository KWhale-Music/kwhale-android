package org.dueattendant149.kwhale.analytics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.dueattendant149.kwhale.model.Track
import org.dueattendant149.kwhale.network.KwhaleApi
import org.dueattendant149.kwhale.network.model.Event
import org.dueattendant149.kwhale.network.model.EventContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Buffers recommendation signals, persists pending batches to disk when
 * [storageDir] is provided, and sends them to POST /events.
 * The API treats event ids as idempotency keys, so retries are safe.
 */
class EventTracker(
    private val api: KwhaleApi,
    private val scope: CoroutineScope,
    private val storageDir: File? = null,
) {
    private val pending = CopyOnWriteArrayList<Event>()
    private val flushing = AtomicBoolean(false)
    private val diskFile: File? = storageDir?.let { File(it, "pending_events.json") }
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val sessionId = UUID.randomUUID().toString()
    private var playbackId: String? = null
    private var startedAt: String? = null
    private var trackDurationMs: Long? = null

    @Volatile
    var activeSurface: String = "player"
        private set

    @Volatile
    var activeMixId: String? = null
        private set

    init {
        loadFromDisk()
        if (pending.isNotEmpty()) {
            flush()
        }
    }

    fun setPlaybackContext(surface: String, mixId: String?) {
        activeSurface = normalizeSurface(surface)
        activeMixId = mixId?.takeIf { it.isNotBlank() }
    }

    fun play(track: Track, surface: String = "player", mixId: String? = null) {
        playbackId = UUID.randomUUID().toString()
        startedAt = Instant.now().toString()
        trackDurationMs = track.durationSec.toLong() * 1000L
        add(track, "play", surface, positionMs = 0L, mixId = mixId ?: activeMixId)
    }

    fun pause(track: Track, surface: String = "player", positionMs: Long = 0L, mixId: String? = null) =
        add(track, "pause", surface, positionMs, listenedMs = positionMs, mixId = mixId ?: activeMixId)

    fun resume(track: Track, surface: String = "player", positionMs: Long = 0L, mixId: String? = null) =
        add(track, "resume", surface, positionMs, mixId = mixId ?: activeMixId)

    fun seek(track: Track, surface: String = "player", positionMs: Long = 0L, mixId: String? = null) =
        add(track, "seek", surface, positionMs, mixId = mixId ?: activeMixId)

    fun skip(
        track: Track,
        surface: String = "player",
        positionMs: Long? = null,
        listenedMs: Long? = null,
        mixId: String? = null,
    ) {
        add(
            track,
            "skip",
            surface,
            positionMs = positionMs,
            ended = true,
            listenedMs = listenedMs ?: positionMs,
            mixId = mixId ?: activeMixId,
        )
    }

    fun complete(
        track: Track,
        surface: String = "player",
        durationMs: Long? = null,
        listenedMs: Long? = null,
        mixId: String? = null,
    ) {
        val total = durationMs?.takeIf { it > 0L } ?: trackDurationMs ?: (track.durationSec.toLong() * 1000L)
        val listened = listenedMs?.takeIf { it > 0L } ?: total
        add(
            track,
            "complete",
            surface,
            positionMs = total,
            ended = true,
            listenedMs = listened,
            mixId = mixId ?: activeMixId,
        )
    }

    fun repeat(track: Track, surface: String = "player", positionMs: Long = 0L, mixId: String? = null) =
        add(track, "repeat", surface, positionMs, mixId = mixId ?: activeMixId)

    fun like(track: Track, surface: String = "player", mixId: String? = null) =
        add(track, "like", surface, mixId = mixId ?: activeMixId)

    fun unlike(track: Track, surface: String = "player", mixId: String? = null) =
        add(track, "unlike", surface, mixId = mixId ?: activeMixId)

    fun dislike(track: Track, surface: String = "player", mixId: String? = null) =
        add(track, "dislike", surface, mixId = mixId ?: activeMixId)

    fun playlistAdd(track: Track, playlistId: String? = null, surface: String = "playlist") =
        add(track, "playlist_add", surface, playlistId = playlistId)

    fun share(track: Track, surface: String = "player") =
        add(track, "share", surface)

    private fun normalizeSurface(raw: String): String {
        val clean = raw.trim().lowercase()
        return if (clean in VALID_SURFACES) clean else "player"
    }

    fun impression(
        track: Track?,
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
        while (pending.size > MAX_PENDING) {
            pending.removeAt(0)
        }
        if (ended) {
            playbackId = null
            startedAt = null
            trackDurationMs = null
        }
        saveToDiskAsync()
        flush()
    }

    fun flush() {
        if (pending.isEmpty() || api.session() == null) return
        if (!flushing.compareAndSet(false, true)) return
        scope.launch {
            try {
                while (pending.isNotEmpty() && api.session() != null) {
                    val batch = pending.toList().take(50)
                    if (batch.isEmpty()) break
                    val ok = runCatching { api.sendEvents(batch) }.isSuccess
                    if (!ok) break
                    pending.removeAll(batch.toSet())
                    saveToDiskSync()
                }
            } finally {
                flushing.set(false)
            }
        }
    }

    private fun loadFromDisk() {
        val f = diskFile ?: return
        runCatching {
            if (!f.isFile) return
            val items = json.decodeFromString(ListSerializer(Event.serializer()), f.readText())
            pending.addAll(items.takeLast(MAX_PENDING))
        }
    }

    private fun saveToDiskAsync() {
        if (diskFile == null) return
        scope.launch(Dispatchers.IO) {
            saveToDiskSync()
        }
    }

    private fun saveToDiskSync() {
        val f = diskFile ?: return
        runCatching {
            f.parentFile?.mkdirs()
            val snapshot = pending.toList().takeLast(MAX_PENDING)
            if (snapshot.isEmpty()) {
                f.delete()
            } else {
                val tmp = File(f.parentFile, "${f.name}.tmp")
                tmp.writeText(json.encodeToString(ListSerializer(Event.serializer()), snapshot))
                tmp.renameTo(f)
            }
        }
    }

    private companion object {
        const val MAX_PENDING = 500
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
