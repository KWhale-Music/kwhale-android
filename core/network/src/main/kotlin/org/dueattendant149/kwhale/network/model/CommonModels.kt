package org.dueattendant149.kwhale.network.model

import kotlinx.serialization.Serializable

@Serializable
data class Problem(
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val instance: String? = null,
    val code: String? = null,
    val fields: Map<String, String> = emptyMap(),
)

@Serializable
data class ImageRef(
    val id: String,
    val width: Int? = null,
    val height: Int? = null,
    val blurHash: String? = null,
) {
    fun path(size: String = "md"): String = "/images/$id?size=$size"
}

@Serializable
data class ArtistRef(
    val id: String,
    val name: String,
)

@Serializable
data class AlbumRef(
    val id: String,
    val title: String,
    val cover: ImageRef? = null,
)

@Serializable
data class UserRef(
    val id: String,
    val displayName: String,
    val avatar: ImageRef? = null,
)

@Serializable
data class Genre(
    val id: String,
    val name: String,
    val image: ImageRef? = null,
)

@Serializable
data class User(
    val id: String,
    val displayName: String,
    val email: String? = null,
    val avatar: ImageRef? = null,
    val createdAt: String? = null,
)

@Serializable
data class UserUpdate(
    val displayName: String,
)

@Serializable
data class Session(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSec: Long,
    val user: User,
) {
    fun isAccessTokenExpired(nowEpochSec: Long = System.currentTimeMillis() / 1000): Boolean =
        nowEpochSec >= expiresAtEpochSec - 30
}

@Serializable
data class Config(
    val apiVersion: String,
    val minClientVersion: String,
    val features: Features = Features(),
    val limits: Limits = Limits(),
    val media: MediaConfig = MediaConfig(),
) {
    @Serializable
    data class Features(
        val recommendations: Boolean = false,
        val lyrics: Boolean = false,
        val downloads: Boolean = false,
        val playlists: Boolean = true,
        val offline: Boolean = false,
    )

    @Serializable
    data class Limits(
        val maxPlaylistTracks: Int = 5000,
        val maxUploadBytes: Long = 10L * 1024 * 1024,
        val eventBatchSize: Int = 100,
    )

    @Serializable
    data class MediaConfig(
        val supportedFormats: List<String> = emptyList(),
        val streamChunkSize: Long = 256L * 1024,
    )
}
