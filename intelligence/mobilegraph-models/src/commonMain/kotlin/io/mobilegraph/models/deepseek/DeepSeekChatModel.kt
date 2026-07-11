package io.mobilegraph.models.deepseek

import io.ktor.client.HttpClient
import io.mobilegraph.models.openai.OpenAIChatModel
import io.mobilegraph.models.openai.createOpenAIHttpClient

/**
 * Chat model implementation for DeepSeek.
 * Uses OpenAI-compatible API.
 */
class DeepSeekChatModel(
    override val name: String = "deepseek-v4-flash",
    private val apiKey: String,
    private val baseUrl: String = "https://api.deepseek.com",
    private val httpClient: HttpClient = createOpenAIHttpClient(),
) : OpenAIChatModel(name, apiKey, baseUrl, httpClient)
