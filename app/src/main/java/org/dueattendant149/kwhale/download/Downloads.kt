package org.dueattendant149.kwhale.download

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.ConcurrentHashMap
import org.dueattendant149.kwhale.model.Track

object Downloads {

    const val WORK_TAG = "kwhale-download"
    const val TRACK_TAG_PREFIX = "track:"
    private val enqueuedTracks = ConcurrentHashMap<String, Track>()

    fun enqueue(context: Context, track: Track, replaceExisting: Boolean = false) {
        val remoteId = track.remoteId ?: return
        enqueuedTracks[remoteId] = track
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                Data.Builder()
                    .putString(DownloadWorker.KEY_ID, remoteId)
                    .putString(DownloadWorker.KEY_STREAM_URL, track.streamUrl)
                    .putString(DownloadWorker.KEY_TITLE, track.title)
                    .putString(DownloadWorker.KEY_ARTIST, track.artist)
                    .putString(DownloadWorker.KEY_COVER_URL, track.coverUrl)
                    .putInt(DownloadWorker.KEY_DURATION_SEC, track.durationSec)
                    .build(),
            )
            .addTag(WORK_TAG)
            .addTag("$TRACK_TAG_PREFIX$remoteId")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "download-$remoteId",
            if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun enqueueAll(context: Context, tracks: List<Track>): Int {
        var queued = 0
        tracks.forEach { track ->
            if (!track.remoteId.isNullOrBlank()) {
                enqueue(context, track)
                queued++
            }
        }
        return queued
    }

    fun cancel(context: Context, remoteId: String) {
        WorkManager.getInstance(context).cancelUniqueWork("download-$remoteId")
    }

    fun pendingMetadata(remoteId: String): Track? = enqueuedTracks[remoteId]
}
