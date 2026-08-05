package io.mobilegraph.mcp.transport

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/**
 * Implementation of the "Streamable HTTP" transport for MCP.
 *
 * In this model, every request is a POST. The server may respond with a
 * standard JSON-RPC response OR a text/event-stream (SSE) that contains
 * the response and potentially further messages.
 */
class StreamableHttpTransport(
    private val client: HttpClient,
    private val url: String,
) : McpTransport {
    private val messages =
        MutableSharedFlow<String>(
            replay = 1,
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    private var sessionId: String? = null
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    override suspend fun send(message: String) {
        scope.launch {
            try {
                client
                    .post(url) {
                        header("Accept", "application/json, text/event-stream")
                        sessionId?.let { header("mcp-session-id", it) }
                        setBody(TextContent(message, ContentType.Application.Json))
                    }.let { response ->
                        // Capture session ID if provided by the server
                        response.headers["mcp-session-id"]?.let {
                            sessionId = it
                        }

                        val contentType = response.contentType()
                        if (contentType?.match(ContentType.Text.EventStream) == true) {
                            // Handle SSE stream
                            val channel = response.body<ByteReadChannel>()
                            processSse(channel)
                        } else {
                            // Handle standard JSON response
                            if (response.status.value !in 200..299) {
                                messages.emit(
                                    "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32000,\"message\":\"Transport error: Server returned ${response.status}\"}}",
                                )
                                return@let
                            }
                            val body = response.body<String>()
                            if (body.isNotBlank()) {
                                println("MCP RECV (JSON): $body")
                                messages.emit(body)
                            } else {
                                println("MCP RECV: Empty body (Status: ${response.status})")
                            }
                        }
                    }
            } catch (e: Exception) {
                messages.emit("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32000,\"message\":\"Transport error: ${e.message}\"}}")
            }
        }
    }

    private suspend fun processSse(channel: ByteReadChannel) {
        var currentData = StringBuilder()

        try {
            while (true) {
                val line = channel.readUTF8Line() ?: break
                if (line.isEmpty()) {
                    if (currentData.isNotEmpty()) {
                        val message = currentData.toString()
                        println("MCP EMIT (SSE): $message")
                        messages.emit(message)
                        currentData = StringBuilder()
                    }
                    continue
                }

                if (line.startsWith("data:")) {
                    val data = line.substring(5).trim()
                    println("MCP RECV (SSE DATA): $data")
                    currentData.append(data)
                }
            }

            // Emit any remaining data after stream closes
            if (currentData.isNotEmpty()) {
                val message = currentData.toString()
                println("MCP EMIT (SSE END): $message")
                messages.emit(message)
            }
        } catch (e: Exception) {
            println("SSE Processing Error: ${e.message}")
        }
    }

    /**
     * Backward compatibility for McpClient.initialize
     */
    fun setInitialMessage(message: String) {
        // No-op in this model, initialize will call send()
    }

    override fun receive(): Flow<String> = messages

    override suspend fun close() {
        // No long-running job to cancel
    }
}
