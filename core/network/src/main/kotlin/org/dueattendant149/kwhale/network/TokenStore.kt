package org.dueattendant149.kwhale.network

import android.content.Context
import android.content.SharedPreferences
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

    private val appContext = context.applicationContext

    // Plain prefs are the canonical store. EncryptedSharedPreferences (alpha06)
    // can fail after a reboot/Keystore reset; the old fallback silently switched
    // to an empty plain store and the session disappeared on the next launch.
    private val plain: SharedPreferences =
        appContext.getSharedPreferences("kwhale_session_plain", Context.MODE_PRIVATE)

    private val encrypted: SharedPreferences? by lazy {
        runCatching {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                "kwhale_session",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }.getOrNull()
    }

    private fun read(prefs: SharedPreferences?): String? =
        try {
            prefs?.getString(KEY_SESSION, null)
        } catch (_: Throwable) {
            null
        }

    override fun load(): Session? {
        val plainRaw = read(plain)
        val encryptedRaw = if (plainRaw == null) read(encrypted) else null
        val raw = plainRaw ?: encryptedRaw ?: return null

        if (plainRaw == null && encryptedRaw != null) {
            // Migrate the existing encrypted session to the reliable store.
            plain.edit().putString(KEY_SESSION, encryptedRaw).apply()
        }

        return runCatching { json.decodeFromString(Session.serializer(), raw) }
            .onFailure {
                plain.edit().remove(KEY_SESSION).apply()
                runCatching { encrypted?.edit()?.remove(KEY_SESSION)?.apply() }
            }
            .getOrNull()
    }

    override fun save(session: Session) {
        val raw = json.encodeToString(Session.serializer(), session)
        plain.edit().putString(KEY_SESSION, raw).apply()
        // Best effort: the encrypted copy is a bonus, never the source of truth.
        runCatching { encrypted?.edit()?.putString(KEY_SESSION, raw)?.apply() }
    }

    override fun clear() {
        plain.edit().remove(KEY_SESSION).apply()
        runCatching { encrypted?.edit()?.remove(KEY_SESSION)?.apply() }
    }

    private companion object {
        const val KEY_SESSION = "session"
    }
}
