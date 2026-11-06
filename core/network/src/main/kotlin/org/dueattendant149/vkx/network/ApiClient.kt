package org.dueattendant149.vkx.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.dueattendant149.vkx.network.model.AuthResponse
import org.dueattendant149.vkx.network.model.Problem
import org.dueattendant149.vkx.network.model.RefreshRequest
import org.dueattendant149.vkx.network.model.Session
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiClient(
    val config: ApiConfig,
    val tokenStore: TokenStore,
    val json: Json,
) {

    private val plainClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(HostPolicyInterceptor(config.baseHost))
        .build()

    private val authenticatedClient: OkHttpClient = plainClient.newBuilder()
        .addInterceptor(AuthInterceptor(tokenStore) { refreshBlocking() })
        .build()

    suspend fun <T> get(
        path: String,
        serializer: KSerializer<T>,
        query: Map<String, String?> = emptyMap(),
    ): T = execute(authenticatedClient, buildRequest("GET", path, null, query), serializer)

    suspend fun <T> post(
        path: String,
        body: String?,
        serializer: KSerializer<T>,
        query: Map<String, String?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): T = execute(authenticatedClient, buildRequest("POST", path, body, query, headers), serializer)

    suspend fun <T> put(
        path: String,
        body: String?,
        serializer: KSerializer<T>,
        query: Map<String, String?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): T = execute(authenticatedClient, buildRequest("PUT", path, body, query, headers), serializer)

    suspend fun <T> patch(
        path: String,
        body: String?,
        serializer: KSerializer<T>,
        query: Map<String, String?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): T = execute(authenticatedClient, buildRequest("PATCH", path, body, query, headers), serializer)

    suspend fun <T> postMultipart(
        path: String,
        file: File,
        mimeType: String,
        formFields: Map<String, String?>,
        serializer: KSerializer<T>,
        headers: Map<String, String> = emptyMap(),
    ): T {
        val url = config.normalizedBaseUrl.toHttpUrl().newBuilder()
            .addPathSegments(path.trimStart('/'))
            .build()
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody(mimeType.toMediaType()),
            )
            .apply {
                formFields.forEach { (k, v) ->
                    if (!v.isNullOrBlank()) addFormDataPart(k, v)
                }
            }
            .build()
        val builder = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("X-Client-Version", config.clientVersion)
            .header("X-Device-Id", config.deviceId)
        headers.forEach { (k, v) -> builder.header(k, v) }
        val request = builder.post(multipart).build()
        return execute(authenticatedClient, request, serializer)
    }

    suspend fun postUnit(
        path: String,
        body: String? = null,
        query: Map<String, String?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ) = executeUnit(authenticatedClient, buildRequest("POST", path, body, query, headers))

    suspend fun putUnit(
        path: String,
        body: String? = null,
        query: Map<String, String?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ) = executeUnit(authenticatedClient, buildRequest("PUT", path, body, query, headers))

    suspend fun deleteUnit(
        path: String,
        query: Map<String, String?> = emptyMap(),
    ) = executeUnit(authenticatedClient, buildRequest("DELETE", path, null, query))

    suspend fun download(
        path: String,
        destination: File,
        onProgress: (downloaded: Long, total: Long?) -> Unit = { _, _ -> },
    ) = withContext(Dispatchers.IO) {
        val request = buildRequest("GET", path, null, emptyMap())
        authenticatedClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw problem(response.code, response.body?.string().orEmpty())
            }
            val body = response.body ?: throw IOException("Empty download body")
            val total = body.contentLength().takeIf { it > 0L }
            destination.parentFile?.mkdirs()
            body.byteStream().use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var copied = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        onProgress(copied, total)
                    }
                }
            }
        }
    }

    private suspend fun <T> execute(
        client: OkHttpClient,
        request: Request,
        serializer: KSerializer<T>,
    ): T = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw problem(response.code, text)
            json.decodeFromString(serializer, text)
        }
    }

    private suspend fun executeUnit(
        client: OkHttpClient,
        request: Request,
    ): Unit = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw problem(response.code, text)
        }
    }

    private fun problem(statusCode: Int, text: String): ApiException {
        val problem = runCatching { json.decodeFromString(Problem.serializer(), text) }.getOrNull()
        val message = problem?.detail
            ?: problem?.title
            ?: "HTTP $statusCode"
        return ApiException(statusCode, problem, message)
    }

    private fun buildRequest(
        method: String,
        path: String,
        body: String?,
        query: Map<String, String?>,
        headers: Map<String, String> = emptyMap(),
    ): Request {
        val url = config.normalizedBaseUrl.toHttpUrl().newBuilder()
            .addPathSegments(path.trimStart('/'))
            .apply {
                query.forEach { (key, value) ->
                    if (value != null) addQueryParameter(key, value)
                }
            }
            .build()

        val builder = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("X-Client-Version", config.clientVersion)
            .header("X-Device-Id", config.deviceId)
        headers.forEach { (k, v) -> builder.header(k, v) }

        return when (method) {
            "GET", "DELETE" -> builder.method(method, null).build()
            else -> builder.method(
                method,
                (body ?: "").toRequestBody(JSON_MEDIA_TYPE),
            ).build()
        }
    }

    /**
     * Called from OkHttp's dispatcher thread by [AuthInterceptor]. Must stay blocking.
     */
    private fun refreshBlocking(): Session? {
        val current = tokenStore.load() ?: return null
        val body = json.encodeToString(RefreshRequest.serializer(), RefreshRequest(current.refreshToken))
        val request = buildRequest("POST", "/auth/refresh", body, emptyMap())
        return runCatching {
            plainClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    tokenStore.clear()
                    return null
                }
                val text = response.body?.string().orEmpty()
                val auth = json.decodeFromString(AuthResponse.serializer(), text)
                val session = auth.toSession()
                tokenStore.save(session)
                session
            }
        }.getOrElse {
            tokenStore.clear()
            null
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
