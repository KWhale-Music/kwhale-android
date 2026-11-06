package org.dueattendant149.vkx.download

import android.content.Context
import android.net.Uri
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class DownloadedTrack(
    val remoteId: String,
    val streamUrl: String,
    val title: String,
    val artist: String,
    val filePath: String,
    val coverUrl: String? = null,
    val durationSec: Int = 0,
    val mimeType: String = "audio/mpeg",
    val bytes: Long = 0L,
    val downloadedAt: Long = System.currentTimeMillis(),
) {
    fun file(): File = File(filePath)
    fun uri(): Uri = Uri.fromFile(file())
}

class DownloadStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("vkx_downloads", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Any()

    fun all(): List<DownloadedTrack> = synchronized(lock) {
        prefs.all.values.mapNotNull { raw ->
            (raw as? String)?.let {
                runCatching { json.decodeFromString(DownloadedTrack.serializer(), it) }.getOrNull()
            }
        }
    }

    fun get(remoteId: String): DownloadedTrack? = synchronized(lock) {
        prefs.getString(remoteId, null)?.let {
            runCatching { json.decodeFromString(DownloadedTrack.serializer(), it) }.getOrNull()
        }
    }

    fun findByStreamUrl(streamUrl: String?): DownloadedTrack? {
        if (streamUrl.isNullOrBlank()) return null
        return all().firstOrNull { it.streamUrl == streamUrl && it.file().exists() }
    }

    fun localUriFor(streamUrl: String?): Uri? = findByStreamUrl(streamUrl)?.uri()

    fun save(track: DownloadedTrack) = synchronized(lock) {
        prefs.edit()
            .putString(track.remoteId, json.encodeToString(DownloadedTrack.serializer(), track))
            .apply()
    }

    fun remove(remoteId: String) = synchronized(lock) {
        get(remoteId)?.file()?.delete()
        prefs.edit().remove(remoteId).apply()
    }

    fun downloadsDir(context: Context): File =
        File(context.filesDir, "downloads").apply { mkdirs() }
}
