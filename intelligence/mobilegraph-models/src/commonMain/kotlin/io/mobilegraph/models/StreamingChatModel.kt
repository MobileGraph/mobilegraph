package io.mobilegraph.models

import kotlinx.serialization.Serializable

/**
 * Interface for chat-based models that support streaming.
 */
interface StreamingChatModel : ChatModel

/**
 * Represents a chunk of a streaming chat response.
 */
@Serializable
data class ChatChunk(
    val delta: String,
    val finishReason: String? = null,
) {
    /**
     * Compatibility property to match the sample code's chunk.text usage.
     */
    val text: String get() = delta
}
