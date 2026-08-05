package io.mobilegraph.mcp.transport

import kotlinx.coroutines.flow.Flow

/**
 * Interface for MCP transport layers (e.g., SSE, Stdio, Local).
 */
interface McpTransport {
    /**
     * Sends a message (usually a JSON string) to the server.
     */
    suspend fun send(message: String)

    /**
     * Returns a flow of messages received from the server.
     */
    fun receive(): Flow<String>

    /**
     * Closes the transport.
     */
    suspend fun close()
}
