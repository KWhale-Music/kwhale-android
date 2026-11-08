package org.dueattendant149.kwhale.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Small disk cache for screen payloads.
 *
 * Tab switches dispose the composition and the old code refetched everything,
 * which looked like lag. Screens now render the last snapshot immediately and
 * refresh in the background; only a cold start shows a skeleton.
 *
 * Files live in filesDir/ui-cache and are versioned by key ("home-v1").
 * Writes are atomic and happen off the main thread.
 */
class ContentCache(context: Context) {

    private val dir = File(context.filesDir, "ui-cache").apply { mkdirs() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val memory = mutableMapOf<String, Any?>()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> read(key: String, serializer: KSerializer<T>): T? {
        if (memory.containsKey(key)) return memory[key] as T?
        return runCatching {
            val file = file(key)
            if (!file.isFile) return null
            json.decodeFromString(serializer, file.readText()).also { memory[key] = it }
        }.getOrNull()
    }

    fun <T> write(key: String, serializer: KSerializer<T>, value: T) {
        memory[key] = value
        scope.launch {
            runCatching {
                val target = file(key)
                val tmp = File(dir, "${target.name}.tmp")
                tmp.writeText(json.encodeToString(serializer, value))
                if (!tmp.renameTo(target)) {
                    target.delete()
                    tmp.renameTo(target)
                }
            }
        }
    }

    fun isFresh(key: String, ttlMs: Long): Boolean {
        val file = file(key)
        return file.isFile && System.currentTimeMillis() - file.lastModified() < ttlMs
    }

    fun invalidate(key: String) {
        memory.remove(key)
        file(key).delete()
    }

    fun invalidateHome() = invalidate(HOME)
    fun invalidateLibrary() = invalidate(LIBRARY)
    fun invalidateProfile() = invalidate(PROFILE)
    fun invalidateSearchBootstrap() = invalidate(SEARCH_BOOTSTRAP)

    fun invalidateCollection(kind: String, id: String) =
        invalidate(collection(kind, id))

    fun invalidateArtist(id: String) = invalidate(artist(id))

    private fun file(key: String) = File(dir, "$key.json")

    companion object {
        const val HOME = "home-v1"
        const val LIBRARY = "library-v1"
        const val PROFILE = "profile-v1"
        const val SEARCH_BOOTSTRAP = "search-bootstrap-v1"

        fun collection(kind: String, id: String) = "collection-v1-$kind-$id"
        fun artist(id: String) = "artist-v1-$id"
    }
}
