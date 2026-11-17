package org.dueattendant149.kwhale.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.dueattendant149.kwhale.model.Track
import org.dueattendant149.kwhale.network.ApiException
import org.dueattendant149.kwhale.network.model.CensorshipReportResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class TrackReactionStoreTest {

    private class FakeReactionGateway : ReactionGateway {
        var failLike: Throwable? = null
        var failDislike: Throwable? = null
        var failReport: Throwable? = null
        var failCancel: Throwable? = null

        val likes = mutableListOf<String>()
        val unlikes = mutableListOf<String>()
        val dislikes = mutableListOf<String>()
        val undislikes = mutableListOf<String>()
        val reports = mutableListOf<Pair<String, Long?>>()
        val cancels = mutableListOf<String>()
        val events = mutableListOf<String>()

        override suspend fun like(trackId: String) {
            failLike?.let { throw it }
            likes += trackId
        }

        override suspend fun unlike(trackId: String) {
            failLike?.let { throw it }
            unlikes += trackId
        }

        override suspend fun dislike(trackId: String) {
            failDislike?.let { throw it }
            dislikes += trackId
        }

        override suspend fun undislike(trackId: String) {
            failDislike?.let { throw it }
            undislikes += trackId
        }

        override suspend fun reportCensorship(trackId: String, positionMs: Long?): CensorshipReportResponse {
            failReport?.let { throw it }
            reports += trackId to positionMs
            return CensorshipReportResponse(ok = true, reportId = "rep-1", status = "pending")
        }

        override suspend fun cancelCensorshipReport(trackId: String) {
            failCancel?.let { throw it }
            cancels += trackId
        }

        override fun emitLike(track: Track, surface: String) {
            events += "like:${track.remoteId}:$surface"
        }

        override fun emitUnlike(track: Track, surface: String) {
            events += "unlike:${track.remoteId}:$surface"
        }

        override fun emitDislike(track: Track, surface: String) {
            events += "dislike:${track.remoteId}:$surface"
        }
    }

    private fun sampleTrack(
        id: Long = 1L,
        remoteId: String? = "tr-1",
        isLiked: Boolean = false,
        isDisliked: Boolean = false,
        isCensorshipReported: Boolean = false,
    ) = Track(
        id = id,
        title = "Song",
        artist = "Artist",
        durationSec = 180,
        remoteId = remoteId,
        isLiked = isLiked,
        isDisliked = isDisliked,
        isCensorshipReported = isCensorshipReported,
    )

    @Test
    fun toggleLikeOptimisticallyUpdatesAndClearsDislike() = runTest {
        val gateway = FakeReactionGateway()
        val playerUpdates = mutableListOf<Pair<Long, Boolean>>()
        val store = TrackReactionStore(
            gateway = gateway,
            scope = TestScope(StandardTestDispatcher(testScheduler)),
            onPlayerTrackLiked = { id, liked -> playerUpdates += id to liked },
        )
        val track = sampleTrack(isLiked = false, isDisliked = true)

        store.toggleLike(track, surface = "player")

        assertTrue(store.isLiked("tr-1", track.isLiked))
        assertFalse(store.isDisliked("tr-1", track.isDisliked))
        assertEquals(listOf(1L to true), playerUpdates)

        advanceUntilIdle()
        assertEquals(listOf("tr-1"), gateway.likes)
        assertEquals(listOf("like:tr-1:player"), gateway.events)
    }

    @Test
    fun toggleLikeRollsBackStateOnNetworkFailure() = runTest {
        val gateway = FakeReactionGateway().apply { failLike = IOException("timeout") }
        val playerUpdates = mutableListOf<Pair<Long, Boolean>>()
        val errors = mutableListOf<String>()
        val store = TrackReactionStore(
            gateway = gateway,
            scope = TestScope(StandardTestDispatcher(testScheduler)),
            onPlayerTrackLiked = { id, liked -> playerUpdates += id to liked },
        )
        val track = sampleTrack(isLiked = false, isDisliked = true)

        store.toggleLike(track, surface = "home", onError = { errors += it })
        assertTrue(store.isLiked("tr-1", track.isLiked))

        advanceUntilIdle()
        assertFalse(store.isLiked("tr-1", track.isLiked))
        assertTrue(store.isDisliked("tr-1", track.isDisliked))
        assertEquals(listOf(1L to true, 1L to false), playerUpdates)
        assertEquals(listOf("timeout"), errors)
    }

    @Test
    fun toggleDislikeClearsLikeAndEmitsDislike() = runTest {
        val gateway = FakeReactionGateway()
        val playerUpdates = mutableListOf<Pair<Long, Boolean>>()
        val changes = mutableListOf<Boolean>()
        val store = TrackReactionStore(
            gateway = gateway,
            scope = TestScope(StandardTestDispatcher(testScheduler)),
            onPlayerTrackLiked = { id, liked -> playerUpdates += id to liked },
        )
        val track = sampleTrack(isLiked = true, isDisliked = false)

        store.toggleDislike(track, surface = "player", onChanged = { changes += it })
        assertTrue(store.isDisliked("tr-1", track.isDisliked))
        assertFalse(store.isLiked("tr-1", track.isLiked))
        assertEquals(listOf(1L to false), playerUpdates)

        advanceUntilIdle()
        assertEquals(listOf("tr-1"), gateway.dislikes)
        assertEquals(listOf("dislike:tr-1:player"), gateway.events)
        assertEquals(listOf(true), changes)
    }

    @Test
    fun reportCensorshipOneClickAndCancel() = runTest {
        val gateway = FakeReactionGateway()
        val store = TrackReactionStore(
            gateway = gateway,
            scope = TestScope(StandardTestDispatcher(testScheduler)),
        )
        val track = sampleTrack()
        var sentCount = 0
        var alreadyCount = 0
        var cancelledCount = 0

        store.reportCensorshipOneClick(
            track = track,
            positionMs = 42_500L,
            onAlreadyReported = { alreadyCount++ },
            onSent = { sentCount++ },
            onError = { error("unexpected error: $it") },
        )
        assertTrue(store.isCensorshipReported("tr-1", false))
        advanceUntilIdle()
        assertEquals(1, sentCount)
        assertEquals(listOf("tr-1" to 42_500L), gateway.reports)

        store.reportCensorshipOneClick(
            track = track,
            positionMs = 50_000L,
            onAlreadyReported = { alreadyCount++ },
            onSent = { sentCount++ },
            onError = { error("unexpected error: $it") },
        )
        assertEquals(1, alreadyCount)
        assertEquals(1, sentCount)

        store.cancelCensorshipReport(track, onCancelled = { cancelledCount++ })
        assertFalse(store.isCensorshipReported("tr-1", false))
        advanceUntilIdle()
        assertEquals(1, cancelledCount)
        assertEquals(listOf("tr-1"), gateway.cancels)
    }

    @Test
    fun reportCensorshipRollsBackWithFriendlyMessageOn404() = runTest {
        val gateway = FakeReactionGateway().apply {
            failReport = ApiException(statusCode = 404, problem = null, message = "Not Found")
        }
        val store = TrackReactionStore(
            gateway = gateway,
            scope = TestScope(StandardTestDispatcher(testScheduler)),
        )
        val errors = mutableListOf<String>()
        val track = sampleTrack()

        store.reportCensorshipOneClick(
            track = track,
            positionMs = -10L,
            onAlreadyReported = {},
            onSent = {},
            onError = { errors += it },
        )
        advanceUntilIdle()

        assertFalse(store.isCensorshipReported("tr-1", false))
        assertEquals(listOf("На сервере ещё не подключён эндпоинт цензуры"), errors)
    }

    @Test
    fun localTrackWithoutRemoteIdFailsFast() = runTest {
        val gateway = FakeReactionGateway()
        val store = TrackReactionStore(
            gateway = gateway,
            scope = TestScope(StandardTestDispatcher(testScheduler)),
        )
        val errors = mutableListOf<String>()

        store.toggleLike(sampleTrack(remoteId = null), surface = "downloads", onError = { errors += it })
        store.reportCensorshipOneClick(
            track = sampleTrack(remoteId = null),
            positionMs = 1000L,
            onAlreadyReported = {},
            onSent = {},
            onError = { errors += it },
        )
        advanceUntilIdle()

        assertEquals(2, errors.size)
        assertTrue(gateway.likes.isEmpty())
        assertTrue(gateway.reports.isEmpty())
    }
}
