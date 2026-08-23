package io.mobilegraph.mcp.transport

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StreamableHttpTransportTest {
    @Test
    fun testJsonResponse() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    respond(
                        content = """{"jsonrpc":"2.0","id":1,"result":"ok"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "application/json"),
                    )
                }
            val client = HttpClient(mockEngine)
            val transport = StreamableHttpTransport(client, "http://test.com")

            transport.send("test")
            val response = transport.receive().first()
            assertTrue(response.contains("ok"))
        }

    @Test
    fun testSseResponse() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    respond(
                        content = "data: {\"jsonrpc\":\"2.0\",\"id\":1,\"result\":\"sse-ok\"}\n\n",
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "text/event-stream"),
                    )
                }
            val client = HttpClient(mockEngine)
            val transport = StreamableHttpTransport(client, "http://test.com")

            transport.send("test")
            val response = transport.receive().first()
            assertTrue(response.contains("sse-ok"))
        }

    @Test
    fun testSessionIdHandling() =
        runTest {
            var capturedSessionId: String? = null
            val mockEngine =
                MockEngine { request ->
                    val content = request.body as OutgoingContent
                    // We can't easily read the body here without complex setup, so we'll just check if session ID is set
                    // The transport only sets session ID AFTER the first response is received.
                    if (request.headers["mcp-session-id"] != null) {
                        capturedSessionId = request.headers["mcp-session-id"]
                    }

                    respond(
                        content = "ok",
                        status = HttpStatusCode.OK,
                        headers = headersOf("mcp-session-id", "test-session"),
                    )
                }
            val client = HttpClient(mockEngine)
            val transport = StreamableHttpTransport(client, "http://test.com")

            // First call triggers session ID capture
            transport.send("first")

            // Poll for session ID in subsequent sends
            var attempts = 0
            while (capturedSessionId == null && attempts < 20) {
                transport.send("ping")
                delay(50)
                attempts++
            }

            assertEquals("test-session", capturedSessionId)
        }

    @Test
    fun testErrorHandling() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    respond(
                        content = "Error occurred",
                        status = HttpStatusCode.InternalServerError,
                    )
                }
            val client = HttpClient(mockEngine)
            val transport = StreamableHttpTransport(client, "http://test.com")

            transport.send("test")

            val response = transport.receive().first()
            assertTrue(response.contains("jsonrpc"), "Expected JSON-RPC error: $response")
        }

    @Test
    fun testCustomHeaders() =
        runTest {
            var capturedAuth: String? = null
            var capturedCustom: String? = null
            val mockEngine =
                MockEngine { request ->
                    capturedAuth = request.headers["Authorization"]
                    capturedCustom = request.headers["X-Custom-Header"]
                    respond(
                        content = """{"jsonrpc":"2.0","id":1,"result":"ok"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "application/json"),
                    )
                }
            val client = HttpClient(mockEngine)
            val transport =
                StreamableHttpTransport(
                    client,
                    "http://test.com",
                    headers =
                        mapOf(
                            "Authorization" to "Bearer test-token",
                            "X-Custom-Header" to "CustomValue",
                        ),
                )

            transport.send("test")
            val response = transport.receive().first()
            assertTrue(response.contains("ok"))
            assertEquals("Bearer test-token", capturedAuth)
            assertEquals("CustomValue", capturedCustom)
        }
}
