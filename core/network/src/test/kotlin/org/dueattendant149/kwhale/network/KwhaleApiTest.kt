package org.dueattendant149.kwhale.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.dueattendant149.kwhale.network.model.Session
import org.dueattendant149.kwhale.network.model.ShareKind
import org.dueattendant149.kwhale.network.model.User
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.io.IOException

class KwhaleApiTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun config() = ApiConfig(
        baseUrl = server.url("/v1").toString(),
        clientVersion = "test",
        deviceId = "test-device",
    )

    private fun authJson(access: String, refresh: String = "refresh") = """
        {
          "accessToken": "$access",
          "refreshToken": "$refresh",
          "expiresIn": 900,
          "tokenType": "Bearer",
          "user": {
            "id": "user_1",
            "email": "demo@kwhale.local",
            "displayName": "Demo"
          }
        }
    """.trimIndent()

    @Test
    fun loginStoresSessionAndUsesSingleOrigin() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(authJson("access-1", "refresh-1")),
        )

        val api = ApiFactory.createInMemory(config())
        val session = api.login("demo@kwhale.local", "password")

        assertEquals("access-1", session.accessToken)
        assertEquals("refresh-1", session.refreshToken)
        assertEquals(session, api.session())

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1/auth/login", request.path)
        assertTrue(request.body.readUtf8().contains("\"email\":\"demo@kwhale.local\""))
        assertEquals("test", request.getHeader("X-Client-Version"))
        assertEquals("test-device", request.getHeader("X-Device-Id"))
    }

    @Test
    fun concurrentRefreshUsesOneServerRotation() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(authJson("access-2", "refresh-2")),
        )
        val initial = Session(
            accessToken = "access-1",
            refreshToken = "refresh-1",
            expiresAtEpochSec = Long.MAX_VALUE,
            user = User(id = "user_1", displayName = "Demo"),
        )
        val api = ApiFactory.createInMemory(config(), initial)

        val (first, second) = runBlocking {
            coroutineScope {
                val a = async(Dispatchers.IO) { api.refreshSession("access-1") }
                val b = async(Dispatchers.IO) { api.refreshSession("access-1") }
                a.await() to b.await()
            }
        }

        assertEquals("access-2", first?.accessToken)
        assertEquals("access-2", second?.accessToken)
        assertEquals(1, server.requestCount)
        assertEquals("/v1/auth/refresh", server.takeRequest().path)
    }

    @Test
    fun hostPolicyRejectsForeignHost() {
        val client = OkHttpClient.Builder()
            .addInterceptor(HostPolicyInterceptor("api.example.com"))
            .build()

        val request = Request.Builder()
            .url("http://127.0.0.1:${server.port}/v1/home")
            .build()

        val error = assertThrows(IOException::class.java) {
            client.newCall(request).execute()
        }
        assertTrue(error.message.orEmpty().contains("Blocked non-API host"))
    }

    @Test
    fun refreshesOnceOn401AndRetries() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/problem+json")
                .setBody("""{"status":401,"title":"Unauthorized","code":"token_expired"}"""),
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(authJson("access-2", "refresh-2")),
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"sections":[]}"""),
        )

        val oldSession = Session(
            accessToken = "access-1",
            refreshToken = "refresh-1",
            expiresAtEpochSec = 1L,
            user = User(id = "user_1", displayName = "Demo"),
        )
        val api = ApiFactory.createInMemory(config(), oldSession)

        val home = api.home()
        assertTrue(home.sections.isEmpty())

        val first = server.takeRequest()
        assertEquals("/v1/home", first.path)
        assertEquals("Bearer access-1", first.getHeader("Authorization"))

        val refresh = server.takeRequest()
        assertEquals("/v1/auth/refresh", refresh.path)
        assertEquals("POST", refresh.method)
        assertTrue(refresh.body.readUtf8().contains("\"refreshToken\":\"refresh-1\""))

        val retry = server.takeRequest()
        assertEquals("/v1/home", retry.path)
        assertEquals("Bearer access-2", retry.getHeader("Authorization"))

        assertEquals("access-2", api.session()?.accessToken)
    }

    @Test
    fun networkErrorDuringRefreshDoesNotClearSession() = runBlocking {
        // 401 on the main call triggers a refresh; the refresh itself hits a
        // dropped connection (IOException). The stored session must survive —
        // a network blip must never log the user out.
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/problem+json")
                .setBody("""{"status":401,"title":"Unauthorized","code":"token_expired"}"""),
        )
        server.enqueue(
            MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START),
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"sections":[]}"""),
        )

        val oldSession = Session(
            accessToken = "access-1",
            refreshToken = "refresh-1",
            expiresAtEpochSec = 1L,
            user = User(id = "user_1", displayName = "Demo"),
        )
        val api = ApiFactory.createInMemory(config(), oldSession)

        runCatching { api.home() }

        assertEquals("access-1", api.session()?.accessToken)
    }

    @Test
    fun parsesHomeFeedWithTypedItems() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "sections": [
                        {
                          "id": "for-you",
                          "kind": "recommendation",
                          "layout": "carousel",
                          "title": "Для вас",
                          "items": [
                            {
                              "type": "track",
                              "track": {
                                "id": "track_1",
                                "title": "Night Drive",
                                "artists": [{"id": "artist_1", "name": "Neon"}],
                                "durationMs": 214000,
                                "explicit": false,
                                "cover": {"id": "img_1", "width": 600, "height": 600},
                                "isLiked": false,
                                "isDisliked": false,
                                "hasLyrics": true
                              }
                            }
                          ]
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val session = Session(
            accessToken = "access",
            refreshToken = "refresh",
            expiresAtEpochSec = Long.MAX_VALUE,
            user = User(id = "user_1", displayName = "Demo"),
        )
        val api = ApiFactory.createInMemory(config(), session)
        val home = api.home()

        assertEquals(1, home.sections.size)
        val item = home.sections.first().items.first()
        assertEquals("track", item.type)
        assertEquals("Night Drive", item.track?.title)
        assertEquals("img_1", item.track?.cover?.id)
    }

    @Test
    fun imageAndStreamUrlsStayOnApiOrigin() {
        val api = ApiFactory.createInMemory(config())

        val image = api.imageUrl("img_1", "sm")
        val stream = api.streamUrl("track_1")

        assertTrue(image.startsWith(server.url("/v1").toString().trimEnd('/')))
        assertTrue(image.contains("/images/img_1?size=sm"))
        assertTrue(stream.startsWith(server.url("/v1").toString().trimEnd('/')))
        assertTrue(stream.endsWith("/tracks/track_1/stream"))
    }

    @Test
    fun supportsSearchHistorySuggestHistoryAndProfileUpdate() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"items":["kino"]}"""),
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"queries":["kino"],"items":[{"type":"query","text":"kino"}]}"""),
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"id":"user_1","email":"demo@kwhale.local","displayName":"New Name"}"""),
        )

        val session = Session(
            accessToken = "access",
            refreshToken = "refresh",
            expiresAtEpochSec = Long.MAX_VALUE,
            user = User(id = "user_1", displayName = "Demo"),
        )
        val api = ApiFactory.createInMemory(config(), session)

        val hist = api.searchHistory()
        assertEquals(1, hist.items.size)
        assertEquals("kino", hist.items.first())

        val suggest = api.suggestions("ki")
        assertEquals("kino", suggest.items.first().text)

        val updatedUser = api.updateMe("New Name")
        assertEquals("New Name", updatedUser.displayName)
    }

    @Test
    fun reportsAndCancelsCensorshipReview() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(202)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"ok":true,"reportId":"rep_42","status":"pending"}"""),
        )
        server.enqueue(MockResponse().setResponseCode(204))

        val session = Session(
            accessToken = "access",
            refreshToken = "refresh",
            expiresAtEpochSec = Long.MAX_VALUE,
            user = User(id = "user_1", displayName = "Demo"),
        )
        val api = ApiFactory.createInMemory(config(), session)

        val response = api.reportCensorship("track_99", reason = "censored", positionMs = 42_000L)
        assertTrue(response.ok)
        assertEquals("rep_42", response.reportId)
        assertEquals("pending", response.status)

        val postRequest = server.takeRequest()
        assertEquals("POST", postRequest.method)
        assertTrue(postRequest.path.orEmpty().endsWith("/tracks/track_99/censorship"))
        assertTrue(!postRequest.getHeader("Idempotency-Key").isNullOrBlank())
        val reqBody = postRequest.body.readUtf8()
        assertTrue(reqBody.contains("\"reason\":\"censored\""))
        assertTrue(reqBody.contains("\"positionMs\":42000"))

        api.cancelCensorshipReport("track_99")
        val deleteRequest = server.takeRequest()
        assertEquals("DELETE", deleteRequest.method)
        assertTrue(deleteRequest.path.orEmpty().endsWith("/tracks/track_99/censorship"))
    }

    @Test
    fun createsAndResolvesShareLinks() = runBlocking {
        val shareJson = """
            {
              "id": "b8b9f730",
              "kind": "album",
              "entityId": "42",
              "title": "Этажи",
              "subtitle": "Молчат Дома",
              "meta": "2018 · 9 треков",
              "imageId": "img_42",
              "shareUrl": "https://music.dueattendant149.org/share/s/b8b9f730",
              "cardUrl": "https://music.dueattendant149.org/share/card/b8b9f730.png",
              "coverUrl": "https://music.dueattendant149.org/share/cover/b8b9f730",
              "deepLink": "kwhale://album/42",
              "expiresAt": "2026-11-28T19:00:00Z",
              "ttlDays": 7,
              "clicksTotal": 0,
              "platforms": [
                {
                  "platform": "apple",
                  "label": "Apple Music",
                  "url": "https://music.apple.com/album/1484820476",
                  "goUrl": "https://music.dueattendant149.org/share/go/b8b9f730/apple",
                  "exact": true
                }
              ]
            }
        """.trimIndent()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(shareJson),
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(shareJson),
        )

        val session = Session(
            accessToken = "access",
            refreshToken = "refresh",
            expiresAtEpochSec = Long.MAX_VALUE,
            user = User(id = "user_1", displayName = "Demo"),
        )
        val api = ApiFactory.createInMemory(config(), session)

        val created = api.createShare(ShareKind.ALBUM, "42")
        assertEquals("b8b9f730", created.id)
        assertEquals(ShareKind.ALBUM, created.kind)
        assertEquals("kwhale://album/42", created.deepLink)
        assertEquals(1, created.platforms.size)
        assertTrue(created.platforms.first().exact)

        val postReq = server.takeRequest()
        assertEquals("POST", postReq.method)
        assertTrue(postReq.path.orEmpty().endsWith("/share"))
        val body = postReq.body.readUtf8()
        assertTrue(body.contains("\"kind\":\"album\""))
        assertTrue(body.contains("\"id\":\"42\""))

        val resolved = api.shareDetails("b8b9f730")
        assertEquals("42", resolved.entityId)
        val getReq = server.takeRequest()
        assertEquals("GET", getReq.method)
        assertTrue(getReq.path.orEmpty().endsWith("/share/b8b9f730"))
    }
}
