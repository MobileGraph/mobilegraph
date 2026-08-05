# MCP Integration Guide

The **Model Context Protocol (MCP)** allows MobileGraph to connect to remote or local servers to discover and use tools, resources, and prompts dynamically.

---

## 1. Installation

Add the `mobilegraph-mcp` module to your dependencies.

```kotlin
dependencies {
    implementation("io.github.mobilegraph:mobilegraph-mcp:0.1.0-beta")
}
```

## 2. Configuration

MCP is integrated via a plugin. You install it during the standard `MobileGraph.initialize` block. You **must** also initialize the Tool Registry using `withTools { }` for MCP to register its discovered tools.

```kotlin
MobileGraph.initialize {
    // Required: Initialize Tool Registry
    withTools { }

    plugins {
        install(McpPlugin.Mcp) {
            // Modern HTTP-based transport (optimized for Serverless/Cloudflare)
            streamableHttpServer("https://mcp-server.example.com/mcp")
            
            // Or use classic persistent SSE transport
            // sseServer("https://mcp-server.example.com/sse", isPost = true)
        }
    }
}

// After SDK initialization, trigger the MCP handshake
val mcpIntegration = MobileGraph.instance.getComponent(McpPlugin.McpIntegration::class)
mcpIntegration?.initialize(MobileGraph.tools.registry())
```

## 3. Tool Discovery & Usage

MobileGraph automatically bridges MCP tools into the native `Tool` interface. 

### Global Tool Registration
When the `initialize()` method of the `McpIntegration` is called, it:
1.  Establishes the connection using the configured transport.
2.  Performs the JSON-RPC `initialize` handshake.
3.  Fetches the list of available tools from the server.
4.  Registers them with the provided `ToolRegistry`.

### Autonomous Agent Integration
Any agent configured to use global tools will automatically have access to these remote tools.

```kotlin
class MyMcpAgent(
    override val model: ChatModel,
    override val tools: ToolRegistry
) : Agent {
    override val useGlobalTools = true // Opt-in to MCP tools
    // ...
}
```

## 4. Technical Architecture

### Transports
MobileGraph supports two primary remote transports, both optimized for mobile stability:

*   **StreamableHttpTransport**: Implements the "Modern" MCP HTTP pattern. Every request is an HTTP POST that can optionally return an SSE stream. It handles session persistence via the `mcp-session-id` header automatically. Best for Cloudflare Workers and serverless environments.
*   **SseTransport**: Implements the "Classic" MCP SSE pattern. It opens a single long-lived connection for receiving events and uses separate POST requests for sending messages. Optimized with `mcp-session-id` support for reliable session linking.

### JSON-RPC 2.0 Compliance
Our implementation strictly follows the JSON-RPC 2.0 specification:
*   **Handshake**: Sends `jsonrpc: "2.0"` and validates protocol version `2024-11-05`.
*   **Session Management**: Automatically captures and propagates session IDs to link independent HTTP requests to the same session.
*   **Raw Formatting**: Uses raw `TextContent` to prevent double-encoding of JSON strings.

---

## 📱 Sample Application
For a complete working example, see `McpActivity.kt` and `McpViewModel.kt` in the `androidApp` module. It demonstrates connecting to a live demo server and executing a multi-step agentic workflow using remote tools.
