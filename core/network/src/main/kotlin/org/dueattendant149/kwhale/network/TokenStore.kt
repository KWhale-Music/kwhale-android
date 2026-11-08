package org.dueattendant149.kwhale.network

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.json.Json
import org.dueattendant149.kwhale.network.model.Session

interface TokenStore {
    fun load(): Session?
    fun save(session: Session)
    fun clear()
}

class InMemoryTokenStore(initial: Session? = null) : TokenStore {
    @Volatile
    private var session: Session? = initial

    override fun load(): Session? = session
    override fun save(session: Session) { this.session = session }
    override fun clear() { session = null }
}

@Suppress("DEPRECATION")
class EncryptedTokenStore(
    context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : TokenStore {

    private val prefs = runCatching {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            "kwhale_session",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }.getOrElse {
        context.applicationContext.getSharedPreferences("kwhale_session_plain", Context.MODE_PRIVATE)
    }

    init {
        migrateLegacy(context)
    }

    /** Carry the session over from the pre-rename vkx_session store once. */
    private fun migrateLegacy(context: Context) {
        if (prefs.getString(KEY_SESSION, null) != null) return
        val old = runCatching {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context.applicationContext,
                "vkx_session",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }.getOrNull() ?: return
        val raw = old.getString(KEY_SESSION, null) ?: return
        prefs.edit().putString(KEY_SESSION, raw).apply()
        old.edit().remove(KEY_SESSION).apply()
    }

    override fun load(): Session? {
        val raw = prefs.getString(KEY_SESSION, null) ?: return null
        return runCatching { json.decodeFromString(Session.serializer(), raw) }
            .onFailure { prefs.edit().remove(KEY_SESSION).apply() }
            .getOrNull()
    }

    override fun save(session: Session) {
        prefs.edit()
            .putString(KEY_SESSION, json.encodeToString(Session.serializer(), session))
            .apply()
    }

    override fun clear() {
        prefs.edit().remove(KEY_SESSION).apply()
    }

    private companion object {
        const val KEY_SESSION = "session"
    }
}
