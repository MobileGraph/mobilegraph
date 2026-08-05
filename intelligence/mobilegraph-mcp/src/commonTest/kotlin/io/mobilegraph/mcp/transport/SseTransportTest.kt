package io.mobilegraph.mcp.transport

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SseTransportTest {
    @Test
    fun testPersistentConnection() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    if (request.method.value == "POST") {
                        respond("ok", headers = headersOf("mcp-session-id", "test-session"))
                    } else {
                        respond(
                            content = "data: {\"jsonrpc\":\"2.0\",\"id\":1,\"result\":\"ok\"}\n\n",
                            headers = headersOf("Content-Type", "text/event-stream"),
                        )
                    }
                }
            val client = HttpClient(mockEngine)
            val transport = SseTransport(client, "http://test.com", isPost = false)

            // SseTransport connects in init { start() }
            val response = transport.receive().first()
            assertTrue(response.contains("ok"))

            transport.close()
        }

    @Test
    fun testSendWithSessionId() =
        runTest {
            var capturedSessionId: String? = null
            val mockEngine =
                MockEngine { request ->
                    if (request.method.value == "POST") {
                        capturedSessionId = request.headers["mcp-session-id"]
                        respond("ok")
                    } else {
                        respond(
                            content = "data: ok\n\n",
                            headers =
                                headersOf(
                                    "Content-Type" to listOf("text/event-stream"),
                                    "mcp-session-id" to listOf("test-session"),
                                ),
                        )
                    }
                }
            val client = HttpClient(mockEngine)
            val transport = SseTransport(client, "http://test.com", isPost = false)

            // Wait for connection and message to be processed
            val firstMsg = transport.receive().first()
            assertEquals("ok", firstMsg)

            transport.send("test-message")
            delay(100)

            assertEquals("test-session", capturedSessionId)
            transport.close()
        }
}
