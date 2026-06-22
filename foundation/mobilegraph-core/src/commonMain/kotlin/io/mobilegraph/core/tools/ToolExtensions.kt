package io.mobilegraph.core.tools

import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.core.registry.getComponent
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

/**
 * Accesses the tool registry from the execution context.
 */
val ExecutionContext.toolRegistry: ToolRegistry?
    get() = getComponent<ToolRegistry>()

/**
 * Accesses the tool selector from the execution context.
 */
val ExecutionContext.toolSelector: ToolSelector
    get() = getComponent<ToolSelector>() ?: AllToolSelector()

/**
 * Executes a tool using a JSON string as input.
 */
@Suppress("UNCHECKED_CAST")
suspend fun Tool<*, *>.executeFromJson(
    input: String,
    context: ExecutionContext,
): String {
    val tool = this as Tool<Any, Any>
    val serializer = inputSerializer

    val decodedInput =
        if (serializer != null) {
            try {
                json.decodeFromString(serializer, input)
            } catch (e: Exception) {
                throw e
            }
        } else {
            input
        }

    val result = tool.invoke(decodedInput, context)
    return result.toString()
}

/**
 * Converts a Tool to its Model-compatible ToolDefinition.
 */
fun Tool<*, *>.asDefinition(): ToolDefinition =
    ToolDefinition(
        name = metadata.name,
        description = metadata.description,
        parameters = null,
    )
