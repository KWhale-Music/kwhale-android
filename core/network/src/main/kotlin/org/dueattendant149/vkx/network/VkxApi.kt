package org.dueattendant149.vkx.network

import kotlinx.serialization.json.Json
import org.dueattendant149.vkx.network.model.Album
import org.dueattendant149.vkx.network.model.AlbumsPage
import org.dueattendant149.vkx.network.model.EnrichmentListPage
import org.dueattendant149.vkx.network.model.Artist
import org.dueattendant149.vkx.network.model.ArtistsPage
import org.dueattendant149.vkx.network.model.AuthResponse
import org.dueattendant149.vkx.network.model.Config
import org.dueattendant149.vkx.network.model.Event
import org.dueattendant149.vkx.network.model.EventBatch
import org.dueattendant149.vkx.network.model.GenresResponse
import org.dueattendant149.vkx.network.model.HealthResponse
import org.dueattendant149.vkx.network.model.HistoryPage
import org.dueattendant149.vkx.network.model.HomeResponse
import org.dueattendant149.vkx.network.model.LikeAllResult
import org.dueattendant149.vkx.network.model.LoginRequest
import org.dueattendant149.vkx.network.model.LogoutRequest
import org.dueattendant149.vkx.network.model.Lyrics
import org.dueattendant149.vkx.network.model.Mix
import org.dueattendant149.vkx.network.model.MixesPage
import org.dueattendant149.vkx.network.model.Onboarding
import org.dueattendant149.vkx.network.model.PromptMixRequest
import org.dueattendant149.vkx.network.model.Playlist
import org.dueattendant149.vkx.network.model.PlaylistCreate
import org.dueattendant149.vkx.network.model.PlaylistReorder
import org.dueattendant149.vkx.network.model.PlaylistTracksAdd
import org.dueattendant149.vkx.network.model.PlaylistUpdate
import org.dueattendant149.vkx.network.model.PlaylistsPage
import org.dueattendant149.vkx.network.model.RegisterRequest
import org.dueattendant149.vkx.network.model.SearchHistory
import org.dueattendant149.vkx.network.model.SearchResponse
import org.dueattendant149.vkx.network.model.Session
import org.dueattendant149.vkx.network.model.Suggestions
import org.dueattendant149.vkx.network.model.Track
import org.dueattendant149.vkx.network.model.TrackRecommendationsPage
import org.dueattendant149.vkx.network.model.TracksPage
import org.dueattendant149.vkx.network.model.User
import org.dueattendant149.vkx.network.model.UserUpdate

class VkxApi(
    val config: ApiConfig,
    private val client: ApiClient,
    val json: Json = client.json,
) {

    val tokenStore: TokenStore get() = client.tokenStore

    fun session(): Session? = tokenStore.load()
    fun saveSession(session: Session) = tokenStore.save(session)
    fun clearSession() = tokenStore.clear()

    // ---- auth -------------------------------------------------------------

    suspend fun register(
        email: String,
        password: String,
        displayName: String,
        inviteToken: String,
        deviceId: String? = config.deviceId,
    ): Session {
        val body = json.encodeToString(
            RegisterRequest.serializer(),
            RegisterRequest(email, password, displayName, inviteToken, deviceId),
        )
        val auth = client.post("/auth/register", body, AuthResponse.serializer())
        return auth.toSession().also(tokenStore::save)
    }

    suspend fun login(
        email: String,
        password: String,
        deviceId: String? = config.deviceId,
    ): Session {
        val body = json.encodeToString(
            LoginRequest.serializer(),
            LoginRequest(email, password, deviceId),
        )
        val auth = client.post("/auth/login", body, AuthResponse.serializer())
        return auth.toSession().also(tokenStore::save)
    }

    suspend fun logout() {
        val current = tokenStore.load()
        try {
            if (current != null) {
                val body = json.encodeToString(
                    LogoutRequest.serializer(),
                    LogoutRequest(current.refreshToken),
                )
                client.postUnit("/auth/logout", body)
            }
        } finally {
            tokenStore.clear()
        }
    }

    suspend fun me(): User = client.get("/me", User.serializer())

    suspend fun updateMe(displayName: String): User {
        val body = json.encodeToString(UserUpdate.serializer(), UserUpdate(displayName))
        return client.patch("/me", body, User.serializer())
    }

    // ---- home / search ----------------------------------------------------

    suspend fun home(cursor: String? = null, limit: Int? = null): HomeResponse =
        client.get(
            "/home",
            HomeResponse.serializer(),
            query(cursor, limit),
        )

    suspend fun search(
        query: String,
        type: String = "all",
        limit: Int? = null,
        wait: Boolean = false,
    ): SearchResponse = client.get(
        "/search",
        SearchResponse.serializer(),
        mapOf(
            "q" to query,
            "type" to type,
            "limit" to limit?.toString(),
            "wait" to if (wait) "true" else null,
        ),
    )

    suspend fun suggestions(query: String): Suggestions =
        client.get("/search/suggestions", Suggestions.serializer(), mapOf("q" to query))

    suspend fun searchHistory(): SearchHistory =
        client.get("/search/history", SearchHistory.serializer())

    suspend fun clearSearchHistory() = client.deleteUnit("/search/history")

    // ---- catalog ----------------------------------------------------------

    suspend fun track(id: String): Track = client.get("/tracks/$id", Track.serializer())
    suspend fun album(id: String): Album = client.get("/albums/$id", Album.serializer())
    suspend fun albumTracks(
        id: String,
        cursor: String? = null,
        limit: Int? = null,
    ): TracksPage = client.get("/albums/$id/tracks", TracksPage.serializer(), query(cursor, limit))

    suspend fun artist(id: String): Artist = client.get("/artists/$id", Artist.serializer())
    suspend fun artistAlbums(
        id: String,
        cursor: String? = null,
        limit: Int? = null,
    ): AlbumsPage = client.get("/artists/$id/albums", AlbumsPage.serializer(), query(cursor, limit))

    suspend fun artistTopTracks(id: String, limit: Int? = null): TracksPage =
        client.get("/artists/$id/top-tracks", TracksPage.serializer(), limit(limit))

    suspend fun playlist(id: String): Playlist = client.get("/playlists/$id", Playlist.serializer())

    suspend fun playlists(
        owner: String = "me",
        cursor: String? = null,
        limit: Int? = null,
    ): PlaylistsPage = client.get(
        "/playlists",
        PlaylistsPage.serializer(),
        mapOf("owner" to owner, "cursor" to cursor, "limit" to limit?.toString()),
    )

    suspend fun libraryTracks(
        cursor: String? = null,
        limit: Int? = null,
        sort: String = "default",
    ): TracksPage = client.get(
        "/library/tracks",
        TracksPage.serializer(),
        mapOf("cursor" to cursor, "limit" to limit?.toString(), "sort" to sort),
    )

    suspend fun libraryPlaylists(cursor: String? = null, limit: Int? = null): PlaylistsPage =
        client.get("/library/playlists", PlaylistsPage.serializer(), query(cursor, limit))

    suspend fun history(cursor: String? = null, limit: Int? = null): HistoryPage =
        client.get("/history", HistoryPage.serializer(), query(cursor, limit))

    suspend fun clearHistory() = client.deleteUnit("/history")

    // ---- playlists --------------------------------------------------------

    suspend fun createPlaylist(create: PlaylistCreate): Playlist {
        val body = json.encodeToString(PlaylistCreate.serializer(), create)
        return client.post(
            "/playlists",
            body,
            Playlist.serializer(),
            headers = idempotencyHeader(),
        )
    }

    suspend fun updatePlaylist(id: String, update: PlaylistUpdate): Playlist {
        val body = json.encodeToString(PlaylistUpdate.serializer(), update)
        return client.patch("/playlists/$id", body, Playlist.serializer())
    }

    suspend fun deletePlaylist(id: String) = client.deleteUnit("/playlists/$id")

    suspend fun playlistTracks(
        id: String,
        cursor: String? = null,
        limit: Int? = null,
    ): TracksPage = client.get("/playlists/$id/tracks", TracksPage.serializer(), query(cursor, limit))

    suspend fun addPlaylistTracks(
        id: String,
        trackIds: List<String>,
        position: Int? = null,
    ): Playlist {
        val body = json.encodeToString(
            PlaylistTracksAdd.serializer(),
            PlaylistTracksAdd(trackIds, position),
        )
        return client.post(
            "/playlists/$id/tracks",
            body,
            Playlist.serializer(),
            headers = idempotencyHeader(),
        )
    }

    suspend fun removePlaylistTrack(id: String, trackId: String) =
        client.deleteUnit("/playlists/$id/tracks/$trackId")

    suspend fun reorderPlaylistTracks(id: String, trackIds: List<String>): Playlist {
        val body = json.encodeToString(
            PlaylistReorder.serializer(),
            PlaylistReorder(trackIds),
        )
        return client.put("/playlists/$id/tracks/order", body, Playlist.serializer())
    }

    suspend fun likeAlbumTracks(id: String): LikeAllResult {
        return client.put(
            "/albums/$id/tracks/like",
            null,
            LikeAllResult.serializer(),
            headers = idempotencyHeader(),
        )
    }

    suspend fun likePlaylistTracks(id: String): LikeAllResult {
        return client.put(
            "/playlists/$id/tracks/like",
            null,
            LikeAllResult.serializer(),
            headers = idempotencyHeader(),
        )
    }

    suspend fun followPlaylist(id: String) = client.putUnit("/playlists/$id/follow")
    suspend fun unfollowPlaylist(id: String) = client.deleteUnit("/playlists/$id/follow")

    // ---- likes ------------------------------------------------------------

    suspend fun like(trackId: String) = client.putUnit("/tracks/$trackId/like")
    suspend fun unlike(trackId: String) = client.deleteUnit("/tracks/$trackId/like")
    suspend fun dislike(trackId: String) = client.putUnit("/tracks/$trackId/dislike")
    suspend fun undislike(trackId: String) = client.deleteUnit("/tracks/$trackId/dislike")

    suspend fun lyrics(trackId: String): Lyrics =
        client.get("/tracks/$trackId/lyrics", Lyrics.serializer())

    // ---- recommendations --------------------------------------------------

    suspend fun topArtists(limit: Int? = null): ArtistsPage =
        client.get("/recommendations/top-artists", ArtistsPage.serializer(), limit(limit))

    suspend fun genres(): GenresResponse =
        client.get("/recommendations/genres", GenresResponse.serializer())

    suspend fun onboarding(): Onboarding =
        client.get("/recommendations/onboarding", Onboarding.serializer())

    suspend fun updateOnboarding(onboarding: Onboarding) {
        client.putUnit(
            "/recommendations/onboarding",
            json.encodeToString(Onboarding.serializer(), onboarding),
        )
    }

    // ---- mixes ------------------------------------------------------------

    suspend fun mixes(
        kind: String? = null,
        cursor: String? = null,
        limit: Int? = null,
    ): MixesPage = client.get(
        "/mixes",
        MixesPage.serializer(),
        mapOf("kind" to kind, "cursor" to cursor, "limit" to limit?.toString()),
    )

    suspend fun mixTracks(
        id: String,
        cursor: String? = null,
        limit: Int? = null,
    ): TrackRecommendationsPage =
        client.get("/mixes/$id/tracks", TrackRecommendationsPage.serializer(), query(cursor, limit))

    suspend fun promptMix(
        prompt: String,
        size: Int = 40,
        llmApiKey: String? = null,
        llmEndpoint: String? = null,
        llmModel: String? = null,
    ): TrackRecommendationsPage =
        client.post(
            "/mixes/prompt",
            json.encodeToString(
                PromptMixRequest.serializer(),
                PromptMixRequest(prompt, size, llmApiKey, llmEndpoint, llmModel),
            ),
            TrackRecommendationsPage.serializer(),
        )

    suspend fun enrichments(): EnrichmentListPage =
        client.get("/uploads/enrichments", EnrichmentListPage.serializer())

    suspend fun confirmEnrichment(id: Long) =
        client.postUnit("/uploads/enrichments/$id/confirm", null)

    suspend fun rejectEnrichment(id: Long) =
        client.postUnit("/uploads/enrichments/$id/reject", null)

    // ---- events / config --------------------------------------------------

    suspend fun sendEvents(events: List<Event>) {
        if (events.isEmpty()) return
        client.postUnit(
            "/events",
            json.encodeToString(EventBatch.serializer(), EventBatch(events)),
        )
    }

    suspend fun config(): Config = client.get("/config", Config.serializer())

    suspend fun health(): HealthResponse = client.get("/health", HealthResponse.serializer())

    suspend fun uploadTrack(
        file: java.io.File,
        mimeType: String = "audio/mpeg",
        title: String? = null,
        artist: String? = null,
        album: String? = null,
    ): Track = client.postMultipart(
        path = "/uploads/tracks",
        file = file,
        mimeType = mimeType,
        formFields = mapOf("title" to title, "artist" to artist, "album" to album),
        serializer = Track.serializer(),
        headers = idempotencyHeader(),
    )

    // ---- media ------------------------------------------------------------

    fun streamUrl(trackId: String): String = config.url("/tracks/$trackId/stream")
    fun downloadUrl(trackId: String): String = config.url("/tracks/$trackId/download")
    fun imageUrl(imageId: String, size: String = "lg"): String =
        config.url("/images/$imageId?size=$size")

    suspend fun downloadTrack(
        trackId: String,
        destination: java.io.File,
        onProgress: (downloaded: Long, total: Long?) -> Unit = { _, _ -> },
    ) = client.download("/tracks/$trackId/download", destination, onProgress)

    private fun query(cursor: String?, limit: Int?): Map<String, String?> =
        mapOf("cursor" to cursor, "limit" to limit?.toString())

    private fun limit(limit: Int?): Map<String, String?> =
        mapOf("limit" to limit?.toString())

    private fun idempotencyHeader(): Map<String, String> =
        mapOf("Idempotency-Key" to java.util.UUID.randomUUID().toString())
}
