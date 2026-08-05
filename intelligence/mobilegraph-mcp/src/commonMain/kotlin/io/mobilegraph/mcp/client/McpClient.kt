package io.mobilegraph.mcp.client

import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.mcp.models.McpClientInfo
import io.mobilegraph.mcp.models.McpInitializeResult
import io.mobilegraph.mcp.models.McpNotification
import io.mobilegraph.mcp.models.McpRequest
import io.mobilegraph.mcp.models.McpResponse
import io.mobilegraph.mcp.models.McpTool
import io.mobilegraph.mcp.transport.McpTransport
import io.mobilegraph.mcp.transport.SseTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * A client for the Model Context Protocol (MCP).
 * Manages communication with an MCP server over a transport.
 */
class McpClient(
    private val transport: McpTransport,
    private val clientInfo: McpClientInfo = McpClientInfo("MobileGraph", MobileGraph.VERSION),
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var nextId = 1
    private val idMutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Initializes the connection and exchanges capabilities.
     */
    suspend fun initialize(): McpInitializeResult {
        val params =
            buildJsonObject {
                put("protocolVersion", "2024-11-05")
                put("clientInfo", json.encodeToJsonElement(clientInfo))
                put(
                    "capabilities",
                    buildJsonObject {
                        put("tools", buildJsonObject { put("listChanged", true) })
                        put("resources", buildJsonObject { put("listChanged", true) })
                        put("prompts", buildJsonObject { put("listChanged", true) })
                    },
                )
            }

        val id = idMutex.withLock { nextId++ }
        // Force jsonrpc field to be included by constructing request explicitly
        val request =
            McpRequest(
                jsonrpc = "2.0",
                id = JsonPrimitive(id),
                method = "initialize",
                params = params,
            )
        val requestString = json.encodeToString(request)
        println("MCP SEND: $requestString")

        val responseFlow =
            transport
                .receive()
                .map { json.decodeFromString<McpResponse>(it) }
                .filter { it.id?.jsonPrimitive?.intOrNull == id }

        // Start listening for response BEFORE sending
        val responseDeferred =
            scope.async {
                responseFlow.first()
            }

        // If transport supports early handshake, set the message
        // This is still needed for SseTransport which follows the open-SSE-then-POST pattern
        if (transport is SseTransport) {
            transport.setInitialMessage(requestString)
        } else {
            transport.send(requestString)
        }

        val response = responseDeferred.await()

        val result = json.decodeFromJsonElement<McpInitializeResult>(response.result ?: JsonNull)

        // Send initialized notification
        notify("notifications/initialized")

        return result
    }

    /**
     * Lists tools provided by the server.
     */
    suspend fun listTools(): List<McpTool> {
        val params =
            buildJsonObject {
                put(
                    "_meta",
                    buildJsonObject {
                        put("progressToken", 1)
                    },
                )
            }
        val response = call("tools/list", params)
        val toolsElement = response.result?.jsonObject?.get("tools") ?: return emptyList()
        return json.decodeFromJsonElement<List<McpTool>>(toolsElement)
    }

    /**
     * Calls a tool on the server.
     */
    suspend fun callTool(
        name: String,
        arguments: JsonObject,
    ): JsonObject {
        val params =
            buildJsonObject {
                put("name", name)
                put("arguments", arguments)
            }
        val response = call("tools/call", params)
        return response.result?.jsonObject ?: buildJsonObject {}
    }

    /**
     * Sends a JSON-RPC request and waits for the response.
     */
    private suspend fun call(
        method: String,
        params: JsonObject? = null,
    ): McpResponse {
        val id = idMutex.withLock { nextId++ }
        val request = McpRequest(id = JsonPrimitive(id), method = method, params = params)
        val requestString = json.encodeToString(request)

        val responseFlow =
            transport
                .receive()
                .map { json.decodeFromString<McpResponse>(it) }
                .filter { it.id?.jsonPrimitive?.intOrNull == id }

        // Start listening BEFORE sending to avoid missing the response
        val responseDeferred =
            scope.async {
                responseFlow.first()
            }

        transport.send(requestString)

        return responseDeferred.await()
    }

    /**
     * Sends a JSON-RPC notification.
     */
    private suspend fun notify(
        method: String,
        params: JsonObject? = null,
    ) {
        val notification = McpNotification(method = method, params = params)
        val notificationString = json.encodeToString(notification)
        transport.send(notificationString)
    }

    fun close() {
        scope.cancel()
    }
}
