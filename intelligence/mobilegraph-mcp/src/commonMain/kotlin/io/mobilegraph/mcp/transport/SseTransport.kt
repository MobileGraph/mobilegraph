package io.mobilegraph.mcp.transport

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/**
 * SSE Transport implementation for MCP.
 * Connects to a remote MCP server via Server-Sent Events.
 */
class SseTransport(
    private val client: HttpClient,
    private val sseUrl: String,
    private val postUrl: String = sseUrl, // Usually the same base, but might differ
    private val isPost: Boolean = true,
    private val headers: Map<String, String> = emptyMap(),
) : McpTransport {
    private var initialMessage: String? = null
    private var sessionId: String? = null

    fun setInitialMessage(message: String) {
        this.initialMessage = message
    }

    private val messages =
        MutableSharedFlow<String>(
            replay = 1,
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    init {
        start()
    }

    private fun start() {
        job =
            scope.launch {
                // Wait for initial message if we are doing a POST handshake
                if (isPost) {
                    var attempts = 0
                    while (initialMessage == null && attempts < 50) {
                        delay(10)
                        attempts++
                    }
                }

                try {
                    val block: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {
                        header("Accept", "text/event-stream, application/json")
                        this@SseTransport.headers.forEach { (key, value) ->
                            header(key, value)
                        }
                        if (isPost) {
                            contentType(ContentType.Application.Json)
                            setBody(TextContent(initialMessage ?: "{}", ContentType.Application.Json))
                        }
                    }

                    val statement =
                        if (isPost) {
                            client.preparePost(sseUrl, block)
                        } else {
                            client.prepareGet(sseUrl, block)
                        }

                    statement.execute { response ->
                        // Capture session ID for future POST requests
                        response.headers["mcp-session-id"]?.let {
                            sessionId = it
                        }

                        if (response.status.value !in 200..299) {
                            messages.emit(
                                "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32000,\"message\":\"Transport error: Server returned ${response.status}\"}}",
                            )
                            return@execute
                        }
                        val channel = response.body<ByteReadChannel>()
                        processStream(channel)
                    }
                } catch (e: Exception) {
                    println("SSE Transport Error: ${e.message}")
                    messages.emit("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32000,\"message\":\"Transport error: ${e.message}\"}}")
                }
            }
    }

    private suspend fun processStream(channel: ByteReadChannel) {
        var currentData = StringBuilder()
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

        // Flush remaining
        if (currentData.isNotEmpty()) {
            messages.emit(currentData.toString())
        }
    }

    override suspend fun send(message: String) {
        try {
            client.post(postUrl) {
                header("Accept", "application/json")
                this@SseTransport.headers.forEach { (key, value) ->
                    header(key, value)
                }
                sessionId?.let { header("mcp-session-id", it) }
                setBody(TextContent(message, ContentType.Application.Json))
            }
        } catch (e: Exception) {
            println("SSE Send Error: ${e.message}")
        }
    }

    override fun receive(): Flow<String> = messages

    override suspend fun close() {
        job?.cancelAndJoin()
    }
}
