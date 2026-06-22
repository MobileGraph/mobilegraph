package io.mobilegraph.models

import io.mobilegraph.core.tools.ToolDefinition
import kotlinx.serialization.Serializable

/**
 * Configuration for model execution.
 */
@Serializable
data class ModelConfig(
    // val model: String? = null,
    val temperature: Float? = null,
    val maxTokens: Int? = null,
    val stop: List<String>? = null,
    val tools: List<ToolDefinition>? = null,
    val toolChoice: String? = null,
)
