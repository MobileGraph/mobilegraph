package io.mobilegraph.mcp.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class McpTool(
    val name: String,
    val description: String? = null,
    val inputSchema: JsonObject,
)

@Serializable
data class McpResource(
    val uri: String,
    val name: String,
    val description: String? = null,
    val mimeType: String? = null,
)

@Serializable
data class McpPrompt(
    val name: String,
    val description: String? = null,
    val arguments: List<McpPromptArgument>? = null,
)

@Serializable
data class McpPromptArgument(
    val name: String,
    val description: String? = null,
    val required: Boolean = false,
)

@Serializable
data class McpServerCapabilities(
    val tools: McpCapabilityEntry? = null,
    val resources: McpCapabilityEntry? = null,
    val prompts: McpCapabilityEntry? = null,
    val logging: JsonObject? = null,
)

@Serializable
data class McpCapabilityEntry(
    val listChanged: Boolean = false,
)

@Serializable
data class McpInitializeResult(
    val protocolVersion: String,
    val capabilities: McpServerCapabilities,
    val serverInfo: McpServerInfo,
)

@Serializable
data class McpServerInfo(
    val name: String,
    val version: String,
)
