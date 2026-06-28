/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.models.openai

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.mobilegraph.core.capability.Capability
import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.models.EmbeddingModel

/**
 * OpenAI implementation of [EmbeddingModel].
 */
class OpenAIEmbeddingModel(
    override val name: String = "text-embedding-3-small",
    private val apiKey: String,
    private val baseUrl: String = "https://api.openai.com/v1",
    private val httpClient: HttpClient = createOpenAIHttpClient(),
) : EmbeddingModel {
    override fun supports(capability: Capability): Boolean = capability == Capability.Embedding

    override suspend fun embed(
        text: String,
        context: ExecutionContext,
    ): FloatArray = embed(listOf(text), context).first()

    override suspend fun embed(
        texts: List<String>,
        context: ExecutionContext,
    ): List<FloatArray> {
        val request = OpenAIEmbeddingRequest(model = name, input = texts)
        val response: OpenAIEmbeddingResponse =
            httpClient
                .post("$baseUrl/embeddings") {
                    header("Authorization", "Bearer $apiKey")
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body()

        return response.data.sortedBy { it.index }.map { it.embedding.toFloatArray() }
    }
}
