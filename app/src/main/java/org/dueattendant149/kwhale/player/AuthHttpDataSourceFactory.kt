package org.dueattendant149.kwhale.player

import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource

/**
 * Adds the current Bearer token to every ExoPlayer HTTP request.
 *
 * The API is single-origin; stream/download URLs are built from the same base URL.
 * Auth is read lazily so a refreshed token is used for the next request.
 */
@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
class AuthHttpDataSourceFactory(
    private val tokenProvider: () -> String?,
    private val allowedHost: () -> String? = { null },
    /** Called once on HTTP 401 with the stale token, then the open is retried. */
    private val refreshToken: ((String?) -> Unit)? = null,
    private val delegate: HttpDataSource.Factory = DefaultHttpDataSource.Factory(),
) : HttpDataSource.Factory {

    override fun createDataSource(): HttpDataSource {
        val source = delegate.createDataSource()
        return object : HttpDataSource by source {
            override fun open(dataSpec: DataSpec): Long {
                val host = allowedHost()
                if (host != null &&
                    !dataSpec.uri.host.equals(host, ignoreCase = true)
                ) {
                    throw java.io.IOException(
                        "Blocked non-API media host: ${dataSpec.uri.host}",
                    )
                }
                fun authorized(spec: DataSpec): DataSpec {
                    val token = tokenProvider()
                    return if (token.isNullOrBlank()) {
                        spec
                    } else {
                        spec.withAdditionalHeaders(
                            mapOf("Authorization" to "Bearer $token"),
                        )
                    }
                }

                val staleToken = tokenProvider()
                return try {
                    source.open(authorized(dataSpec))
                } catch (e: HttpDataSource.InvalidResponseCodeException) {
                    if (e.responseCode != 401 || refreshToken == null) throw e
                    refreshToken(staleToken)
                    source.open(authorized(dataSpec))
                }
            }
        }
    }

    override fun setDefaultRequestProperties(
        defaultRequestProperties: Map<String, String>,
    ): HttpDataSource.Factory {
        delegate.setDefaultRequestProperties(defaultRequestProperties)
        return this
    }
}
