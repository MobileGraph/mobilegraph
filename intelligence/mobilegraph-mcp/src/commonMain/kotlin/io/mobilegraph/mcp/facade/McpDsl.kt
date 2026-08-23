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

class McpHttpServerBuilder {
    private val _headers = mutableMapOf<String, String>()
    val headers: Map<String, String> get() = _headers

    fun header(
        name: String,
        value: String,
    ) {
        _headers[name] = value
    }

    fun headers(headers: Map<String, String>) {
        _headers.putAll(headers)
    }

    fun headers(vararg pairs: Pair<String, String>) {
        _headers.putAll(pairs)
    }
}

class McpConfiguration {
    private val clients = mutableListOf<McpClient>()

    /**
     * Connects to a remote MCP server via SSE.
     */
    fun sseServer(
        url: String,
        client: HttpClient = HttpClient(),
        isPost: Boolean = true,
        headers: Map<String, String> = emptyMap(),
        block: (McpHttpServerBuilder.() -> Unit)? = null,
    ) {
        val builder = McpHttpServerBuilder()
        builder.headers(headers)
        block?.invoke(builder)

        val transport = SseTransport(client, url, isPost = isPost, headers = builder.headers)
        val mcpClient = McpClient(transport)
        clients.add(mcpClient)
    }

    /**
     * Connects to a remote MCP server using the "Streamable HTTP" (POST) transport.
     */
    fun streamableHttpServer(
        url: String,
        client: HttpClient = HttpClient(),
        headers: Map<String, String> = emptyMap(),
        block: (McpHttpServerBuilder.() -> Unit)? = null,
    ) {
        val builder = McpHttpServerBuilder()
        builder.headers(headers)
        block?.invoke(builder)

        val transport = StreamableHttpTransport(client, url, headers = builder.headers)
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
