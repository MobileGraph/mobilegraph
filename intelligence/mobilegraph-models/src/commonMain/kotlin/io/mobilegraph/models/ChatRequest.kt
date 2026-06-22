package io.mobilegraph.models

import io.mobilegraph.core.tools.ToolDefinition
import kotlinx.serialization.Serializable

/**
 * Represents a request to a ChatModel.
 */
@Serializable
data class ChatRequest(
    val messages: List<Message>,
    val temperature: Double? = 0.7,
    val maxTokens: Int? = 1024,
    val stop: List<String>? = null,
    val tools: List<ToolDefinition>? = null,
    val toolChoice: String? = null,
)
