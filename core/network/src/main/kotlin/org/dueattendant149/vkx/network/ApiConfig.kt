package org.dueattendant149.vkx.network

import java.net.URI

data class ApiConfig(
    val baseUrl: String,
    val clientVersion: String = "1.0.0",
    val deviceId: String = "android",
) {
    val normalizedBaseUrl: String = baseUrl.trim().trimEnd('/')

    val baseHost: String = runCatching { URI(normalizedBaseUrl).host }
        .getOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: error("ApiConfig.baseUrl must be an absolute URL: $baseUrl")

    fun url(path: String): String {
        val p = if (path.startsWith('/')) path else "/$path"
        return normalizedBaseUrl + p
    }
}
