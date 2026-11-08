package org.dueattendant149.kwhale

import android.content.Context
import org.dueattendant149.kwhale.design.ThemeMode

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("kwhale_settings", Context.MODE_PRIVATE)

    init {
        migrateLegacy(context)
    }

    /**
     * The pre-rename build used vkx_settings / vkx_session / vkx_downloads.
     * Updating that package must not silently log the user out or reset the
     * theme, so copy the old values once. api_base_url is deliberately not
     * copied: the debug build now points at the LAN host.
     */
    private fun migrateLegacy(context: Context) {
        if (prefs.all.isNotEmpty()) return
        val old = context.getSharedPreferences("vkx_settings", Context.MODE_PRIVATE)
        if (old.all.isEmpty()) return
        prefs.edit().apply {
            old.all.forEach { (key, value) ->
                if (key == "api_base_url") return@forEach
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                    is Set<*> -> putStringSet(key, value.filterIsInstance<String>().toSet())
                }
            }
        }.apply()
    }

    var apiBaseUrl: String
        get() = normalizeBaseUrl(prefs.getString("api_base_url", BuildConfig.API_BASE_URL) ?: BuildConfig.API_BASE_URL)
        set(value) {
            prefs.edit().putString("api_base_url", normalizeBaseUrl(value)).apply()
        }

    var loggedIn: Boolean
        get() = prefs.getBoolean("logged_in", false)
        set(value) { prefs.edit().putBoolean("logged_in", value).apply() }

    var themeMode: ThemeMode
        get() = runCatching {
            ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.DARK.name)!!)
        }.getOrDefault(ThemeMode.DARK)
        set(value) { prefs.edit().putString("theme_mode", value.name).apply() }

    var themeId: Int
        get() = prefs.getInt("theme_id", 0)
        set(value) { prefs.edit().putInt("theme_id", value).apply() }

    var accentId: Int
        get() = prefs.getInt("accent_id", 2)
        set(value) { prefs.edit().putInt("accent_id", value).apply() }

    var monet: Boolean
        get() = prefs.getBoolean("monet", false)
        set(value) { prefs.edit().putBoolean("monet", value).apply() }

    var onboardingDone: Boolean
        get() = prefs.getBoolean("onboarding_done", false)
        set(value) { prefs.edit().putBoolean("onboarding_done", value).apply() }

    // ── LLM (OpenAI-compatible) for AI prompt-mixes ─────────────────────
    var llmApiKey: String
        get() = prefs.getString("llm_api_key", "").orEmpty()
        set(value) { prefs.edit().putString("llm_api_key", value.trim()).apply() }

    var llmEndpoint: String
        get() = prefs.getString("llm_endpoint", "").orEmpty()
        set(value) { prefs.edit().putString("llm_endpoint", value.trim().trimEnd('/')).apply() }

    var llmModel: String
        get() = prefs.getString("llm_model", "").orEmpty()
        set(value) { prefs.edit().putString("llm_model", value.trim()).apply() }

    companion object {
        fun normalizeBaseUrl(raw: String): String {
            val trimmed = raw.trim().trimEnd('/')
            if (trimmed.isEmpty()) return BuildConfig.API_BASE_URL
            return if (trimmed.endsWith("/v1")) trimmed else "$trimmed/v1"
        }
    }
}
