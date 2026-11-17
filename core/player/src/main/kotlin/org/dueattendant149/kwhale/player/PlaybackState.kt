package org.dueattendant149.kwhale.player

import org.dueattendant149.kwhale.model.Track

enum class RepeatMode { OFF, ALL, ONE }

data class PlaybackState(
    val queue: List<Track> = emptyList(),
    val index: Int = 0,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val durationMs: Long = 0L,
) {
    val repeat: Boolean get() = repeatMode != RepeatMode.OFF
    val current: Track? get() = queue.getOrNull(index)
    val progress: Float
        get() = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}
