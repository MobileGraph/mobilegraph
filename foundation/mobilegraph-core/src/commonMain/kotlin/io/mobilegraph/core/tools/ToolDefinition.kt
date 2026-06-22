package io.mobilegraph.core.tools

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Represents the definition of a tool that can be called by a model.
 */
@Serializable
data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: JsonObject? = null,
)
