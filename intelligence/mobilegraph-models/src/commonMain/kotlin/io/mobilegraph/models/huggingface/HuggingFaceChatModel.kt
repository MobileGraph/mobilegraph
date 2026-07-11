package io.mobilegraph.models.huggingface

import io.ktor.client.HttpClient
import io.mobilegraph.models.openai.OpenAIChatModel
import io.mobilegraph.models.openai.createOpenAIHttpClient

/**
 * Chat model implementation for Hugging Face.
 * Uses OpenAI-compatible API provided by Hugging Face Inference Endpoints.
 * Default URL is for Serverless Inference API.
 */
class HuggingFaceChatModel(
    override val name: String, // e.g., "meta-llama/Llama-3.1-8B-Instruct"
    private val apiKey: String,
    private val baseUrl: String = "https://api-inference.huggingface.co/v1",
    private val httpClient: HttpClient = createOpenAIHttpClient(),
) : OpenAIChatModel(name, apiKey, baseUrl, httpClient)
