package org.dueattendant149.kwhale.network

import okhttp3.Interceptor
import okhttp3.Response
import org.dueattendant149.kwhale.network.model.Session
import java.io.IOException

class HostPolicyInterceptor(private val allowedHost: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val host = chain.request().url.host
        if (!host.equals(allowedHost, ignoreCase = true)) {
            throw IOException("Blocked non-API host: $host (allowed: $allowedHost)")
        }
        return chain.proceed(chain.request())
    }
}

class AuthInterceptor(
    private val tokenStore: TokenStore,
    private val refresh: (String?) -> Session?,
) : Interceptor {

    private val lock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val session = tokenStore.load()
        val authenticated = if (session != null) {
            original.newBuilder()
                .header("Authorization", "Bearer ${session.accessToken}")
                .build()
        } else {
            original
        }

        val response = chain.proceed(authenticated)
        if (response.code != 401 || session == null || original.url.encodedPath.contains("/auth/")) {
            return response
        }

        response.close()
        val refreshed = synchronized(lock) {
            val latest = tokenStore.load()
            if (latest != null && latest.accessToken != session.accessToken) {
                latest
            } else {
                refresh(session.accessToken)
            }
        } ?: return chain.proceed(original)

        return chain.proceed(
            original.newBuilder()
                .header("Authorization", "Bearer ${refreshed.accessToken}")
                .build(),
        )
    }
}
