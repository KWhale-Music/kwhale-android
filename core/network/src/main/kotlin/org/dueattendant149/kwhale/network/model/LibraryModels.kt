package org.dueattendant149.kwhale.network.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class PlaylistCreate(
    val title: String,
    val description: String? = null,
    val visibility: String = "private",
)

@Serializable
data class PlaylistUpdate(
    val title: String? = null,
    val description: String? = null,
    val visibility: String? = null,
)

@Serializable
data class PlaylistTracksAdd(
    val trackIds: List<String>,
    val position: Int? = null,
)

@Serializable
data class PlaylistReorder(
    val trackIds: List<String>,
)

@Serializable
data class LikeAllResult(
    val requested: Int = 0,
    val liked: Int = 0,
    val alreadyLiked: Int = 0,
    val queued: Int = 0,
    val failed: Int = 0,
)

@Serializable
data class HealthResponse(
    val status: String = "ok",
)

@Serializable
data class HistoryEntry(
    val track: Track,
    val playedAt: String,
    val context: String? = null,
)

@Serializable
data class HistoryPage(
    val items: List<HistoryEntry> = emptyList(),
    val nextCursor: String? = null,
    val total: Int? = null,
)

@Serializable
data class GenresResponse(
    val items: List<Genre> = emptyList(),
)

@Serializable
data class ProposedMetadata(
    val isrc: String? = null,
    val album: String? = null,
    val year: Int? = null,
    val genre: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
)

@Serializable
data class EnrichmentProposal(
    val id: Long,
    @SerialName("trackId") val trackId: Long? = null,
    val title: String? = null,
    val artists: List<String> = emptyList(),
    val proposed: ProposedMetadata = ProposedMetadata(),
    val source: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
)

@Serializable
data class EnrichmentListPage(
    val items: List<EnrichmentProposal> = emptyList(),
)
