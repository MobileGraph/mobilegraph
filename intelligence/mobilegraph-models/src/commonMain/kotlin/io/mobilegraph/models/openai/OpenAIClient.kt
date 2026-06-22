package io.mobilegraph.models.openai

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal fun createOpenAIHttpClient(engine: io.ktor.client.engine.HttpClientEngine? = null): HttpClient {
    val config: io.ktor.client.HttpClientConfig<*>.() -> Unit = {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                    encodeDefaults = true
                },
            )
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    return if (engine != null) HttpClient(engine, config) else HttpClient(config)
}
