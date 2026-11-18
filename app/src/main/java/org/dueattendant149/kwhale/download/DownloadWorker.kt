package org.dueattendant149.kwhale.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import org.dueattendant149.kwhale.R
import org.dueattendant149.kwhale.KwhaleApp
import java.io.File

class DownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val remoteId = inputData.getString(KEY_ID) ?: return Result.failure()
        val streamUrl = inputData.getString(KEY_STREAM_URL) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE).orEmpty()
        val artist = inputData.getString(KEY_ARTIST).orEmpty()
        val coverUrl = inputData.getString(KEY_COVER_URL)
        val durationSec = inputData.getInt(KEY_DURATION_SEC, 0)

        val app = applicationContext as KwhaleApp
        val store = app.downloads
        val safeId = remoteId.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val dir = store.downloadsDir(applicationContext)
        val destination = File(dir, "$safeId.mp3")

        setForeground(createForegroundInfo(title, 0))
        return try {
            val mime = app.api.downloadTrack(remoteId, destination) { downloaded, total ->
                val percent = if (total != null && total > 0) {
                    ((downloaded * 100) / total).toInt()
                } else {
                    0
                }
                setProgressAsync(workDataOf(KEY_PROGRESS to percent))
            }
            // keep the real format's extension (a FLAC stream saved as .mp3 is misleading)
            var finalDest = destination
            val ext = extForMime(mime)
            if (ext != "mp3") {
                val renamed = File(dir, "$safeId.$ext")
                if (destination.renameTo(renamed)) finalDest = renamed
            }
            // Content identity check: a mislabeled server source can deliver a
            // different recording under the right name (the
            // «body double»/«amy» mixup, 2026-11-18) — measure the file and
            // refuse to keep it when it contradicts the requested duration.
            val measuredSec = measuredDurationSec(finalDest)
            if (measuredSec > 0 && durationSec > MIN_RELIABLE_DURATION_SEC &&
                kotlin.math.abs(measuredSec - durationSec) > DURATION_TOLERANCE_SEC
            ) {
                finalDest.delete()
                return Result.failure()
            }
            store.save(
                DownloadedTrack(
                    remoteId = remoteId,
                    streamUrl = streamUrl,
                    title = title,
                    artist = artist,
                    filePath = finalDest.absolutePath,
                    coverUrl = coverUrl,
                    // measured beats metadata: playback uses it to spot files
                    // that drifted away from the track they belong to
                    durationSec = if (measuredSec > 0) measuredSec else durationSec,
                    bytes = finalDest.length(),
                ),
            )
            Result.success(workDataOf(KEY_PATH to finalDest.absolutePath))
        } catch (error: Exception) {
            destination.delete()
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun measuredDurationSec(file: File): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val ms = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            (ms / 1000L).toInt()
        } catch (_: Exception) {
            0
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun extForMime(mime: String?): String = when (mime?.substringBefore(";")?.lowercase()) {
        "audio/flac", "audio/x-flac" -> "flac"
        "audio/mp4", "audio/m4a", "audio/x-m4a" -> "m4a"
        "audio/ogg", "application/ogg" -> "ogg"
        "audio/wav", "audio/x-wav" -> "wav"
        "audio/aac" -> "aac"
        else -> "mp3"
    }

    private fun createForegroundInfo(title: String, progress: Int): ForegroundInfo {
        val channelId = "kwhale_downloads"
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Скачивание",
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_download_outline_28)
            .setContentTitle(title.ifBlank { "Скачивание трека" })
            .setContentText(if (progress > 0) "$progress%" else "Загрузка…")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, progress <= 0)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val KEY_ID = "remote_id"
        const val KEY_STREAM_URL = "stream_url"
        const val KEY_TITLE = "title"
        const val KEY_ARTIST = "artist"
        const val KEY_COVER_URL = "cover_url"
        const val KEY_DURATION_SEC = "duration_sec"
        const val KEY_PROGRESS = "progress"
        const val KEY_PATH = "path"
        private const val NOTIFICATION_ID = 4101

        // Mirror of server services/downloader.py: rows with junk preview
        // durations (<= 40 s canonical) cannot judge content identity.
        private const val MIN_RELIABLE_DURATION_SEC = 40
        private const val DURATION_TOLERANCE_SEC = 30
    }
}
