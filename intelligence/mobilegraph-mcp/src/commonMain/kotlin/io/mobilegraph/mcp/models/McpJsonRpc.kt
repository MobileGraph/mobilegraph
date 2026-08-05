package io.mobilegraph.mcp.models

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class McpRequest(
    @EncodeDefault val jsonrpc: String = "2.0",
    val id: JsonElement, // Can be Int or String
    val method: String,
    val params: JsonObject? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class McpResponse(
    @EncodeDefault val jsonrpc: String = "2.0",
    val id: JsonElement?,
    val result: JsonElement? = null,
    val error: McpError? = null,
)

@Serializable
data class McpError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class McpNotification(
    @EncodeDefault val jsonrpc: String = "2.0",
    val method: String,
    val params: JsonObject? = null,
)

@Serializable
data class McpMeta(
    val protocolVersion: String,
    val clientCapabilities: JsonObject? = null,
    val clientInfo: McpClientInfo? = null,
)

@Serializable
data class McpClientInfo(
    val name: String,
    val version: String,
)
