package org.dueattendant149.vkx.download

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.dueattendant149.vkx.model.Track

object Downloads {

    fun enqueue(context: Context, track: Track) {
        val remoteId = track.remoteId ?: return
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
            .addTag("vkx-download")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "download-$remoteId",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
