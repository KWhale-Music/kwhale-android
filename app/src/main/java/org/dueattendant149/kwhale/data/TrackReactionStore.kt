package org.dueattendant149.kwhale.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.dueattendant149.kwhale.analytics.EventTracker
import org.dueattendant149.kwhale.model.Track as UiTrack
import org.dueattendant149.kwhale.network.ApiException
import org.dueattendant149.kwhale.network.KwhaleApi
import org.dueattendant149.kwhale.network.model.CensorshipReportResponse
import org.dueattendant149.kwhale.network.model.Track as ApiTrack

interface ReactionGateway {
    suspend fun like(trackId: String)
    suspend fun unlike(trackId: String)
    suspend fun dislike(trackId: String)
    suspend fun undislike(trackId: String)
    suspend fun reportCensorship(trackId: String, positionMs: Long?): CensorshipReportResponse
    suspend fun cancelCensorshipReport(trackId: String)
    fun emitLike(track: UiTrack, surface: String)
    fun emitUnlike(track: UiTrack, surface: String)
    fun emitDislike(track: UiTrack, surface: String)
}

class ApiReactionGateway(
    private val apiProvider: () -> KwhaleApi,
    private val eventsProvider: () -> EventTracker,
) : ReactionGateway {
    override suspend fun like(trackId: String) = apiProvider().like(trackId)
    override suspend fun unlike(trackId: String) = apiProvider().unlike(trackId)
    override suspend fun dislike(trackId: String) = apiProvider().dislike(trackId)
    override suspend fun undislike(trackId: String) = apiProvider().undislike(trackId)
    override suspend fun reportCensorship(trackId: String, positionMs: Long?) =
        apiProvider().reportCensorship(trackId, reason = "censored", positionMs = positionMs)

    override suspend fun cancelCensorshipReport(trackId: String) =
        apiProvider().cancelCensorshipReport(trackId)

    override fun emitLike(track: UiTrack, surface: String) {
        eventsProvider().like(track, surface)
    }

    override fun emitUnlike(track: UiTrack, surface: String) {
        eventsProvider().unlike(track, surface)
    }

    override fun emitDislike(track: UiTrack, surface: String) {
        eventsProvider().dislike(track, surface)
    }
}

/**
 * Single source of truth for optimistic track reactions (like, dislike, censorship)
 * shared across Home, Search, Library, Collections, History, and Player.
 */
class TrackReactionStore(
    private val gateway: ReactionGateway,
    private val scope: CoroutineScope,
    private val onPlayerTrackLiked: (trackLocalId: Long, isLiked: Boolean) -> Unit = { _, _ -> },
) {
    private val _likedOverrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val likedOverrides: StateFlow<Map<String, Boolean>> = _likedOverrides.asStateFlow()

    private val _dislikedOverrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val dislikedOverrides: StateFlow<Map<String, Boolean>> = _dislikedOverrides.asStateFlow()

    private val _censorshipOverrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val censorshipOverrides: StateFlow<Map<String, Boolean>> = _censorshipOverrides.asStateFlow()

    private val _censorshipInFlight = MutableStateFlow<Set<String>>(emptySet())
    val censorshipInFlight: StateFlow<Set<String>> = _censorshipInFlight.asStateFlow()

    fun isLiked(trackId: String, serverLiked: Boolean, overrides: Map<String, Boolean> = _likedOverrides.value): Boolean =
        overrides[trackId] ?: serverLiked

    fun isDisliked(trackId: String, serverDisliked: Boolean, overrides: Map<String, Boolean> = _dislikedOverrides.value): Boolean =
        overrides[trackId] ?: serverDisliked

    fun isCensorshipReported(
        trackId: String,
        serverReported: Boolean,
        overrides: Map<String, Boolean> = _censorshipOverrides.value,
    ): Boolean = overrides[trackId] ?: serverReported

    fun toggleLike(
        track: ApiTrack,
        api: KwhaleApi,
        surface: String,
        onError: (String) -> Unit = {},
    ) = toggleLike(track.toUi(api), surface, onError)

    fun toggleLike(
        track: UiTrack,
        surface: String,
        onError: (String) -> Unit = {},
    ) {
        val remoteId = track.remoteId
        if (remoteId.isNullOrBlank()) {
            onError("Локальный трек нельзя добавить в избранное сервера")
            return
        }
        val prevLikedEntry = _likedOverrides.value[remoteId]
        val prevLiked = prevLikedEntry ?: track.isLiked
        val nextLiked = !prevLiked
        val prevDislikedEntry = _dislikedOverrides.value[remoteId]

        _likedOverrides.update { it + (remoteId to nextLiked) }
        if (nextLiked) {
            _dislikedOverrides.update { it + (remoteId to false) }
        }
        onPlayerTrackLiked(track.id, nextLiked)

        scope.launch {
            try {
                if (nextLiked) gateway.like(remoteId) else gateway.unlike(remoteId)
                if (nextLiked) gateway.emitLike(track, surface) else gateway.emitUnlike(track, surface)
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                _likedOverrides.update { current ->
                    if (prevLikedEntry == null) current - remoteId else current + (remoteId to prevLikedEntry)
                }
                if (nextLiked) {
                    _dislikedOverrides.update { current ->
                        if (prevDislikedEntry == null) current - remoteId else current + (remoteId to prevDislikedEntry)
                    }
                }
                onPlayerTrackLiked(track.id, prevLiked)
                onError(t.message ?: "Не удалось обновить лайк")
            }
        }
    }

    fun toggleDislike(
        track: UiTrack,
        surface: String = "player",
        onChanged: (Boolean) -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        val remoteId = track.remoteId
        if (remoteId.isNullOrBlank()) {
            onError("Локальный трек нельзя скрыть на сервере")
            return
        }
        val prevDislikedEntry = _dislikedOverrides.value[remoteId]
        val prevDisliked = prevDislikedEntry ?: track.isDisliked
        val nextDisliked = !prevDisliked
        val prevLikedEntry = _likedOverrides.value[remoteId]
        val prevLiked = prevLikedEntry ?: track.isLiked

        _dislikedOverrides.update { it + (remoteId to nextDisliked) }
        if (nextDisliked) {
            _likedOverrides.update { it + (remoteId to false) }
            onPlayerTrackLiked(track.id, false)
        }

        scope.launch {
            try {
                if (nextDisliked) gateway.dislike(remoteId) else gateway.undislike(remoteId)
                if (nextDisliked) gateway.emitDislike(track, surface)
                onChanged(nextDisliked)
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                _dislikedOverrides.update { current ->
                    if (prevDislikedEntry == null) current - remoteId else current + (remoteId to prevDislikedEntry)
                }
                if (nextDisliked) {
                    _likedOverrides.update { current ->
                        if (prevLikedEntry == null) current - remoteId else current + (remoteId to prevLikedEntry)
                    }
                    onPlayerTrackLiked(track.id, prevLiked)
                }
                onError(t.message ?: "Не удалось обновить реакцию")
            }
        }
    }

    /**
     * One-click report to send a track for censorship review in admin-ui.
     */
    fun reportCensorshipOneClick(
        track: UiTrack,
        positionMs: Long?,
        onAlreadyReported: () -> Unit,
        onSent: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val remoteId = track.remoteId
        if (remoteId.isNullOrBlank()) {
            onError("Локальный трек не связан с каталогом сервера")
            return
        }
        if (isCensorshipReported(remoteId, track.isCensorshipReported)) {
            onAlreadyReported()
            return
        }
        if (remoteId in _censorshipInFlight.value) return

        val prevEntry = _censorshipOverrides.value[remoteId]
        _censorshipInFlight.update { it + remoteId }
        _censorshipOverrides.update { it + (remoteId to true) }

        scope.launch {
            try {
                gateway.reportCensorship(remoteId, positionMs?.coerceAtLeast(0L))
                onSent()
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                _censorshipOverrides.update { current ->
                    if (prevEntry == null) current - remoteId else current + (remoteId to prevEntry)
                }
                val msg = if (t is ApiException && t.isNotFound) {
                    "На сервере ещё не подключён эндпоинт цензуры"
                } else {
                    t.message ?: "Не удалось отправить трек на пересмотр"
                }
                onError(msg)
            } finally {
                _censorshipInFlight.update { it - remoteId }
            }
        }
    }

    fun cancelCensorshipReport(
        track: UiTrack,
        onCancelled: () -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        val remoteId = track.remoteId ?: return
        val prevEntry = _censorshipOverrides.value[remoteId]
        _censorshipOverrides.update { it + (remoteId to false) }
        scope.launch {
            try {
                gateway.cancelCensorshipReport(remoteId)
                onCancelled()
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                _censorshipOverrides.update { current ->
                    if (prevEntry == null) current - remoteId else current + (remoteId to prevEntry)
                }
                onError(t.message ?: "Не удалось отменить заявку")
            }
        }
    }
}
