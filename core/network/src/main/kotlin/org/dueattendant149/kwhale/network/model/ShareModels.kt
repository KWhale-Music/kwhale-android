package org.dueattendant149.kwhale.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ShareKind {
    @SerialName("track")
    TRACK,

    @SerialName("album")
    ALBUM,

    @SerialName("artist")
    ARTIST,
}

@Serializable
data class SharePlatformLink(
    val platform: String,
    val label: String,
    val url: String,
    val goUrl: String,
    val exact: Boolean = false,
)

@Serializable
data class ShareCreateRequest(
    val kind: ShareKind,
    val id: String,
)

@Serializable
data class ShareLink(
    val id: String,
    val kind: ShareKind,
    val entityId: String,
    val title: String,
    val subtitle: String? = null,
    val meta: String? = null,
    val imageId: String = "placeholder",
    val shareUrl: String,
    val cardUrl: String,
    val coverUrl: String,
    val deepLink: String,
    val expiresAt: String,
    val ttlDays: Int = 7,
    val clicksTotal: Long = 0L,
    val platforms: List<SharePlatformLink> = emptyList(),
)
