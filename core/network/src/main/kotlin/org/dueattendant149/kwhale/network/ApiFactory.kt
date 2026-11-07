package org.dueattendant149.kwhale.network

import android.content.Context
import kotlinx.serialization.json.Json

object ApiFactory {

    val defaultJson: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        isLenient = true
    }

    fun create(
        context: Context,
        config: ApiConfig,
        json: Json = defaultJson,
    ): KwhaleApi {
        val store = EncryptedTokenStore(context.applicationContext, json)
        val client = ApiClient(config, store, json)
        return KwhaleApi(config, client, json)
    }

    fun createInMemory(
        config: ApiConfig,
        session: org.dueattendant149.kwhale.network.model.Session? = null,
        json: Json = defaultJson,
    ): KwhaleApi {
        val client = ApiClient(config, InMemoryTokenStore(session), json)
        return KwhaleApi(config, client, json)
    }
}
