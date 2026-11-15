package org.dueattendant149.kwhale.widget

import android.content.Context
import org.dueattendant149.kwhale.model.Track

/**
 * Persistent snapshot of the last player state for the home-screen widget.
 *
 * The widget can be started by the launcher in a fresh process where the
 * MediaController has not connected yet; reading [org.dueattendant149.kwhale.player.Media3Player.state]
 * there yields an empty queue and the widget used to render only placeholders.
 * The player writes this snapshot on every track/play-state change and the
 * widget reads it even when the app process was killed.
 */
data class PlayerSnapshot(
    val trackId: Long,
    val remoteId: String?,
    val title: String,
    val artist: String,
    val coverUrl: String?,
    val isPlaying: Boolean,
    val updatedAt: Long,
)

class PlayerSnapshotStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): PlayerSnapshot? {
        if (!prefs.contains(KEY_TITLE)) return null
        return PlayerSnapshot(
            trackId = prefs.getLong(KEY_TRACK_ID, -1L),
            remoteId = prefs.getString(KEY_REMOTE_ID, null),
            title = prefs.getString(KEY_TITLE, "") ?: "",
            artist = prefs.getString(KEY_ARTIST, "") ?: "",
            coverUrl = prefs.getString(KEY_COVER, null),
            isPlaying = prefs.getBoolean(KEY_PLAYING, false),
            updatedAt = prefs.getLong(KEY_UPDATED, 0L),
        )
    }

    fun save(track: Track, isPlaying: Boolean) {
        prefs.edit()
            .putLong(KEY_TRACK_ID, track.id)
            .putString(KEY_REMOTE_ID, track.remoteId)
            .putString(KEY_TITLE, track.title)
            .putString(KEY_ARTIST, track.artist)
            .putString(KEY_COVER, track.coverUrl)
            .putBoolean(KEY_PLAYING, isPlaying)
            .putLong(KEY_UPDATED, System.currentTimeMillis())
            .apply()
    }

    fun setPlaying(isPlaying: Boolean) {
        if (!prefs.contains(KEY_TITLE)) return
        prefs.edit().putBoolean(KEY_PLAYING, isPlaying).apply()
    }

    private companion object {
        const val PREFS = "kwhale_player_widget"
        const val KEY_TRACK_ID = "track_id"
        const val KEY_REMOTE_ID = "remote_id"
        const val KEY_TITLE = "title"
        const val KEY_ARTIST = "artist"
        const val KEY_COVER = "cover_url"
        const val KEY_PLAYING = "is_playing"
        const val KEY_UPDATED = "updated_at"
    }
}
