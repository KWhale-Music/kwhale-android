package org.dueattendant149.kwhale.network

import java.net.URI

/**
 * User-facing server address normalisation.
 *
 * People type `music.example.org`, `https://music.example.org/`, or a full
 * `https://host/kwhale` — all of them must end up as an absolute base URL with
 * the `/v1` suffix the API expects. A scheme is added when missing so a bare
 * domain never turns into a relative path (the old bug: `host/v1` without
 * `https://`).
 */
object ServerUrl {

    /** Returns a normalised base URL, or null when the input cannot be a server. */
    fun normalize(raw: String, fallback: String? = null): String? {
        var value = raw.trim()
        if (value.isEmpty()) return fallback

        // Reject non-HTTP schemes early instead of turning them into `https://ftp://...`.
        if (value.contains("://") &&
            !value.startsWith("http://") &&
            !value.startsWith("https://")
        ) {
            return null
        }
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            value = "https://$value"
        }

        val uri = runCatching { URI(value) }.getOrNull()
        if (uri?.host.isNullOrBlank()) return null
        if (uri.scheme != "http" && uri.scheme != "https") return null

        value = value.trimEnd('/')
        if (!value.endsWith("/v1")) value += "/v1"
        return value
    }

    fun isHttp(url: String): Boolean = url.startsWith("http://")
}
