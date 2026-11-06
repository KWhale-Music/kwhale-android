package org.dueattendant149.vkx.network

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
    ): VkxApi {
        val store = EncryptedTokenStore(context.applicationContext, json)
        val client = ApiClient(config, store, json)
        return VkxApi(config, client, json)
    }

    fun createInMemory(
        config: ApiConfig,
        session: org.dueattendant149.vkx.network.model.Session? = null,
        json: Json = defaultJson,
    ): VkxApi {
        val client = ApiClient(config, InMemoryTokenStore(session), json)
        return VkxApi(config, client, json)
    }
}
