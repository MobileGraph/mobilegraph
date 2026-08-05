package io.mobilegraph.mcp.tools

import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.core.tools.Tool
import io.mobilegraph.core.tools.ToolMetadata
import io.mobilegraph.mcp.client.McpClient
import io.mobilegraph.mcp.models.McpTool
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject

/**
 * A bridge that exposes MCP tools as MobileGraph Tools.
 */
class McpToolProvider(
    private val client: McpClient,
) {
    /**
     * Fetches tools from the MCP server and wraps them.
     */
    suspend fun getTools(): List<Tool<JsonObject, JsonObject>> {
        val mcpTools = client.listTools()
        return mcpTools.map { mcpTool ->
            McpToolWrapper(mcpTool, client)
        }
    }

    private class McpToolWrapper(
        private val mcpTool: McpTool,
        private val client: McpClient,
    ) : Tool<JsonObject, JsonObject> {
        override val metadata =
            ToolMetadata(
                name = mcpTool.name,
                description = mcpTool.description ?: "",
            )

        override val inputSerializer: KSerializer<JsonObject> = JsonObject.serializer()

        override suspend fun invoke(
            input: JsonObject,
            context: ExecutionContext,
        ): JsonObject = client.callTool(mcpTool.name, input)
    }
}
