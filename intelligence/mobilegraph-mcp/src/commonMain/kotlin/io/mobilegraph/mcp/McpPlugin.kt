package io.mobilegraph.mcp

import io.mobilegraph.core.environment.MobileGraphEnvironment
import io.mobilegraph.core.facade.MobileGraphPlugin
import io.mobilegraph.core.tools.ToolRegistry
import io.mobilegraph.mcp.facade.McpConfiguration
import io.mobilegraph.mcp.tools.McpToolProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * A plugin that integrates MCP servers into the MobileGraph ecosystem.
 */
class McpPlugin : MobileGraphPlugin<McpConfiguration, McpPlugin.McpIntegration> {
    override fun install(
        environmentBuilder: MobileGraphEnvironment.Builder,
        configure: McpConfiguration.() -> Unit,
    ): McpIntegration {
        val configuration = McpConfiguration()
        configuration.configure()

        val integration = McpIntegration(configuration)
        environmentBuilder.component(McpIntegration::class, integration)

        return integration
    }

    class McpIntegration(
        val configuration: McpConfiguration,
    ) {
        private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        /**
         * Connects to all configured MCP servers and registers their tools.
         * This should be called after MobileGraph is initialized.
         */
        fun initialize(toolRegistry: ToolRegistry) {
            configuration.getClients().forEach { client ->
                scope.launch {
                    try {
                        client.initialize()
                        val provider = McpToolProvider(client)
                        val tools = provider.getTools()
                        tools.forEach { tool ->
                            toolRegistry.register(tool)
                        }
                    } catch (e: Exception) {
                        println("MCP Error: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    companion object {
        val Mcp = McpPlugin()
    }
}
