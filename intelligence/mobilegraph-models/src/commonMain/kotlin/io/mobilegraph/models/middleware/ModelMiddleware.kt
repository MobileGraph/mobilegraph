package io.mobilegraph.models.middleware

import io.mobilegraph.core.middleware.Middleware
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.ModelConfig
import io.mobilegraph.models.ModelOutput

/**
 * Input for ChatModel middleware.
 */
data class ChatModelInput(
    val prompt: ChatPromptValue,
    val config: ModelConfig?,
)

/**
 * Middleware specialized for ChatModels.
 */
interface ChatModelMiddleware : Middleware<ChatModelInput, ModelOutput>
