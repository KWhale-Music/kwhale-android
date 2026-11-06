package org.dueattendant149.vkx.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import org.dueattendant149.vkx.R
import org.dueattendant149.vkx.VkxApp
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

        val app = applicationContext as VkxApp
        val store = DownloadStore(applicationContext)
        val safeId = remoteId.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val destination = File(store.downloadsDir(applicationContext), "$safeId.mp3")

        setForeground(createForegroundInfo(title, 0))
        return try {
            app.api.downloadTrack(remoteId, destination) { downloaded, total ->
                val percent = if (total != null && total > 0) {
                    ((downloaded * 100) / total).toInt()
                } else {
                    0
                }
                setProgressAsync(workDataOf(KEY_PROGRESS to percent))
            }
            store.save(
                DownloadedTrack(
                    remoteId = remoteId,
                    streamUrl = streamUrl,
                    title = title,
                    artist = artist,
                    filePath = destination.absolutePath,
                    coverUrl = coverUrl,
                    durationSec = durationSec,
                    bytes = destination.length(),
                ),
            )
            Result.success(workDataOf(KEY_PATH to destination.absolutePath))
        } catch (error: Exception) {
            destination.delete()
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun createForegroundInfo(title: String, progress: Int): ForegroundInfo {
        val channelId = "vkx_downloads"
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
    }
}
