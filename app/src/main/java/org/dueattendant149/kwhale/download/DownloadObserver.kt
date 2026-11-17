package org.dueattendant149.kwhale.download

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.dueattendant149.kwhale.model.Track

sealed interface TrackDownloadStatus {
    data object NotDownloaded : TrackDownloadStatus
    data object Queued : TrackDownloadStatus
    data class Downloading(val progressPercent: Int) : TrackDownloadStatus
    data class Downloaded(val track: DownloadedTrack) : TrackDownloadStatus
    data object Failed : TrackDownloadStatus
}

data class ActiveDownloadItem(
    val remoteId: String,
    val status: TrackDownloadStatus,
    val track: Track? = null,
)

/**
 * Combines persistent [DownloadStore] entries with live [WorkManager] progress
 * so track menus, collection pages, and DownloadsScreen update in real time.
 */
class DownloadObserver(
    context: Context,
    private val store: DownloadStore,
    scope: CoroutineScope,
) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    val downloadedTracks: StateFlow<List<DownloadedTrack>> = store.tracks

    val statusesByRemoteId: StateFlow<Map<String, TrackDownloadStatus>> = combine(
        store.tracks,
        workManager.getWorkInfosByTagFlow(Downloads.WORK_TAG),
    ) { completed, workInfos ->
        val map = LinkedHashMap<String, TrackDownloadStatus>(completed.size + workInfos.size)
        completed.forEach { track ->
            map[track.remoteId] = TrackDownloadStatus.Downloaded(track)
        }
        workInfos.forEach { info ->
            val remoteId = info.tags
                .firstOrNull { it.startsWith(Downloads.TRACK_TAG_PREFIX) }
                ?.removePrefix(Downloads.TRACK_TAG_PREFIX)
                ?: return@forEach
            if (map[remoteId] is TrackDownloadStatus.Downloaded) return@forEach
            when (info.state) {
                WorkInfo.State.ENQUEUED,
                WorkInfo.State.BLOCKED,
                -> map[remoteId] = TrackDownloadStatus.Queued

                WorkInfo.State.RUNNING -> {
                    val progress = info.progress.getInt(DownloadWorker.KEY_PROGRESS, 0).coerceIn(0, 100)
                    map[remoteId] = TrackDownloadStatus.Downloading(progress)
                }

                WorkInfo.State.FAILED -> map[remoteId] = TrackDownloadStatus.Failed
                WorkInfo.State.SUCCEEDED,
                WorkInfo.State.CANCELLED,
                -> Unit
            }
        }
        map
    }.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    val activeDownloads: StateFlow<List<ActiveDownloadItem>> = statusesByRemoteId
        .map { map ->
            map.entries.mapNotNull { (id, status) ->
                when (status) {
                    TrackDownloadStatus.Queued,
                    is TrackDownloadStatus.Downloading,
                    TrackDownloadStatus.Failed,
                    -> ActiveDownloadItem(
                        remoteId = id,
                        status = status,
                        track = Downloads.pendingMetadata(id),
                    )
                    TrackDownloadStatus.NotDownloaded,
                    is TrackDownloadStatus.Downloaded,
                    -> null
                }
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun statusOf(remoteId: String?, map: Map<String, TrackDownloadStatus> = statusesByRemoteId.value): TrackDownloadStatus =
        if (remoteId.isNullOrBlank()) TrackDownloadStatus.NotDownloaded
        else map[remoteId] ?: TrackDownloadStatus.NotDownloaded
}
