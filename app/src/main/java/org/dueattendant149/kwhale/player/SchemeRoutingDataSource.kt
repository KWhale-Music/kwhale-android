package org.dueattendant149.kwhale.player

import android.net.Uri
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener

/**
 * Routes streamed HTTP(S) audio through the cache and local files directly.
 *
 * Wrapping every source in CacheDataSource would copy downloaded tracks into
 * the playback cache (doubling storage) and add a pointless layer for offline
 * files. This factory chooses per request.
 */
@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
class SchemeRoutingDataSourceFactory(
    private val cached: DataSource.Factory,
    private val direct: DataSource.Factory,
) : DataSource.Factory {

    override fun createDataSource(): DataSource = RoutingDataSource(cached, direct)

    private class RoutingDataSource(
        private val cached: DataSource.Factory,
        private val direct: DataSource.Factory,
    ) : DataSource {

        private var delegate: DataSource? = null
        private val listeners = mutableListOf<TransferListener>()

        override fun addTransferListener(transferListener: TransferListener) {
            listeners += transferListener
        }

        override fun open(dataSpec: DataSpec): Long {
            val factory = if (dataSpec.uri.scheme == "file" || dataSpec.uri.scheme == "content") {
                direct
            } else {
                cached
            }
            val source = factory.createDataSource()
            listeners.forEach(source::addTransferListener)
            delegate = source
            return source.open(dataSpec)
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            checkNotNull(delegate) { "DataSource is not open" }.read(buffer, offset, length)

        override fun getUri(): Uri? = delegate?.uri

        override fun getResponseHeaders(): Map<String, List<String>> =
            delegate?.responseHeaders ?: emptyMap()

        override fun close() {
            delegate?.close()
            delegate = null
        }
    }
}
