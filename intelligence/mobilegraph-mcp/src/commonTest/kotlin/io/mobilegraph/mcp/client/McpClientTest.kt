package io.mobilegraph.mcp.client

import io.mobilegraph.mcp.transport.McpTransport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class McpClientTest {
    private class MockTransport : McpTransport {
        val sentMessages = mutableListOf<String>()
        private val receiveFlow = MutableSharedFlow<String>(replay = 1)

        override suspend fun send(message: String) {
            sentMessages.add(message)
        }

        override fun receive(): Flow<String> = receiveFlow

        suspend fun simulateReceive(message: String) {
            receiveFlow.emit(message)
        }

        override suspend fun close() {}
    }

    @Test
    fun testInitialize() =
        runTest(timeout = 60.seconds) {
            val transport = MockTransport()
            val client = McpClient(transport)

            val initializeResponse =
                """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "result": {
                        "protocolVersion": "2024-11-05",
                        "capabilities": {},
                        "serverInfo": { "name": "TestServer", "version": "1.0" }
                    }
                }
                """.trimIndent()

            launch {
                // Wait for client to send initialize request
                while (transport.sentMessages.isEmpty()) {
                    yield()
                }
                transport.simulateReceive(initializeResponse)
            }

            val result = client.initialize()
            assertEquals("2024-11-05", result.protocolVersion)

            // Check initialized notification
            while (transport.sentMessages.size < 2) {
                yield()
            }
            assertTrue(transport.sentMessages.any { it.contains("notifications/initialized") })
        }

    @Test
    fun testListTools() =
        runTest {
            val transport = MockTransport()
            val client = McpClient(transport)

            val toolsResponse =
                """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "result": {
                        "tools": [
                            { "name": "test_tool", "description": "A test tool", "inputSchema": { "type": "object" } }
                        ]
                    }
                }
                """.trimIndent()

            val job =
                launch {
                    val tools = client.listTools()
                    assertEquals(1, tools.size)
                    assertEquals("test_tool", tools[0].name)
                }

            while (transport.sentMessages.isEmpty()) {
                yield()
            }
            transport.simulateReceive(toolsResponse)
            job.join()
        }

    @Test
    fun testCallTool() =
        runTest {
            val transport = MockTransport()
            val client = McpClient(transport)

            val callResponse =
                """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "result": {
                        "content": "Success"
                    }
                }
                """.trimIndent()

            val job =
                launch {
                    val result = client.callTool("test_tool", buildJsonObject { put("arg", 1) })
                    assertEquals("Success", result["content"]?.toString()?.replace("\"", ""))
                }

            while (transport.sentMessages.isEmpty()) {
                yield()
            }
            transport.simulateReceive(callResponse)
            job.join()
        }
}
