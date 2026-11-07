package org.dueattendant149.kwhale.network.model

import kotlinx.serialization.Serializable

@Serializable
data class SeedRef(
    val type: String,
    val id: String,
)

@Serializable
data class Mix(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val cover: ImageRef? = null,
    val kind: String = "personal",
    val seed: SeedRef? = null,
    val trackCount: Int = 0,
    val isFollowed: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class TrackRecommendation(
    val track: Track,
    val reason: String? = null,
    val score: Double? = null,
)

@Serializable
data class TrackRecommendationsPage(
    val items: List<TrackRecommendation> = emptyList(),
    val nextCursor: String? = null,
)

@Serializable
data class MixesPage(
    val items: List<Mix> = emptyList(),
    val nextCursor: String? = null,
)

@Serializable
data class PromptMixRequest(
    val prompt: String,
    val size: Int = 40,
    val llmApiKey: String? = null,
    val llmEndpoint: String? = null,
    val llmModel: String? = null,
)

@Serializable
data class Onboarding(
    val genres: List<Genre> = emptyList(),
    val artists: List<ArtistRef> = emptyList(),
)

@Serializable
data class EventContext(
    val source: String? = null,
    val mixId: String? = null,
    val playlistId: String? = null,
)

@Serializable
data class Event(
    val id: String,
    val type: String,
    val trackId: String? = null,
    val playbackId: String? = null,
    val context: EventContext? = null,
    val positionMs: Long? = null,
    val listenedMs: Long? = null,
    val trackDurationMs: Long? = null,
    val startedAt: String? = null,
    val endedAt: String? = null,
    val timezoneOffsetMinutes: Int? = null,
    val surface: String? = null,
    val sessionId: String? = null,
    val deviceId: String? = null,
    val query: String? = null,
    val resultCount: Int? = null,
    val clickedPosition: Int? = null,
    val occurredAt: String,
)

@Serializable
data class EventBatch(
    val events: List<Event>,
)
