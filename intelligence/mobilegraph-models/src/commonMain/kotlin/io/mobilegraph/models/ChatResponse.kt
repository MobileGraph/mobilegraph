package io.mobilegraph.models

import kotlinx.serialization.Serializable

/**
 * Represents a response from a ChatModel.
 */
@Serializable
data class ChatResponse(
    val message: Message,
    val finishReason: String? = null,
    val usage: Usage? = null,
)

/**
 * Represents token usage in a model response.
 */
@Serializable
data class Usage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
)
