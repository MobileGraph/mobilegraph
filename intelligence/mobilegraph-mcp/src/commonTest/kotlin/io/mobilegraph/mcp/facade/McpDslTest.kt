package io.mobilegraph.mcp.facade

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mobilegraph.mcp.McpPlugin
import kotlin.test.Test
import kotlin.test.assertEquals

class McpDslTest {
    @Test
    fun testMcpConfigurationBuilder() {
        val config = McpConfiguration()
        val mockEngine =
            MockEngine { request ->
                respond(
                    content = """{"jsonrpc":"2.0","id":1,"result":"ok"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", "application/json"),
                )
            }
        val client = HttpClient(mockEngine)

        config.streamableHttpServer("http://localhost:8080/mcp", client = client) {
            header("Authorization", "Bearer token123")
            header("X-Custom", "Val")
        }

        config.sseServer("http://localhost:8080/sse", client = client, isPost = false) {
            header("Authorization", "Bearer token456")
            headers(mapOf("X-Another" to "AnotherVal"))
        }

        val clients = config.getClients()
        assertEquals(2, clients.size)
    }
}
