package io.mobilegraph.mcp.facade

import io.ktor.client.HttpClient
import io.mobilegraph.core.environment.MobileGraphEnvironment
import io.mobilegraph.core.tools.ToolRegistry
import io.mobilegraph.mcp.client.McpClient
import io.mobilegraph.mcp.tools.McpToolProvider
import io.mobilegraph.mcp.transport.McpTransport
import io.mobilegraph.mcp.transport.SseTransport
import io.mobilegraph.mcp.transport.StreamableHttpTransport
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class McpConfiguration {
    private val clients = mutableListOf<McpClient>()

    /**
     * Connects to a remote MCP server via SSE.
     */
    fun sseServer(
        url: String,
        client: HttpClient = HttpClient(),
        isPost: Boolean = true,
    ) {
        val transport = SseTransport(client, url, isPost = isPost)
        val mcpClient = McpClient(transport)
        clients.add(mcpClient)
    }

    /**
     * Connects to a remote MCP server using the "Streamable HTTP" (POST) transport.
     */
    fun streamableHttpServer(
        url: String,
        client: HttpClient = HttpClient(),
    ) {
        val transport = StreamableHttpTransport(client, url)
        val mcpClient = McpClient(transport)
        clients.add(mcpClient)
    }

    /**
     * Connects to an MCP server using a custom transport.
     */
    fun server(transport: McpTransport) {
        val mcpClient = McpClient(transport)
        clients.add(mcpClient)
    }

    internal fun getClients() = clients
}

/**
 * Configures MCP (Model Context Protocol) support.
 */
fun MobileGraphEnvironment.Builder.mcp(block: McpConfiguration.() -> Unit) =
    apply {
        val config = McpConfiguration()
        config.block()
        component(McpConfiguration::class, config)
    }
