package io.mobilegraph.a2a

import io.mobilegraph.a2a.client.A2AAuthProvider
import io.mobilegraph.a2a.client.A2AClient
import io.mobilegraph.a2a.client.A2AClientConfig
import io.mobilegraph.a2a.client.NoAuthProvider
import io.mobilegraph.core.environment.MobileGraphEnvironment
import io.mobilegraph.core.facade.MobileGraphPlugin

/**
 * A plugin that integrates A2A (Agent-to-Agent) protocol support
 * into the MobileGraph ecosystem.
 *
 * A2A enables MobileGraph agents to discover and communicate with
 * remote A2A-compatible agents. This plugin manages the lifecycle
 * of A2A clients configured during initialization.
 *
 * ## Usage
 * ```kotlin
 * MobileGraph.initialize {
 *     plugins {
 *         install(A2APlugin.A2a) {
 *             remoteAgent("https://coding-agent.example.com") {
 *                 authProvider = BearerTokenAuthProvider("my-token")
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Architecture
 * ```
 * MobileGraph Runtime
 *        │
 *        ├── MCP Layer (tools/resources)
 *        │
 *        └── A2A Layer (this plugin)
 *              │
 *              v
 *        Remote A2A Agents
 * ```
 */
class A2APlugin : MobileGraphPlugin<A2AConfiguration, A2APlugin.A2AIntegration> {
    override fun install(
        environmentBuilder: MobileGraphEnvironment.Builder,
        configure: A2AConfiguration.() -> Unit,
    ): A2AIntegration {
        val configuration = A2AConfiguration()
        configuration.configure()

        val integration = A2AIntegration(configuration)
        environmentBuilder.component(A2AIntegration::class, integration)

        return integration
    }

    /**
     * Provides runtime access to A2A integration capabilities.
     */
    class A2AIntegration(
        val configuration: A2AConfiguration,
    ) {
        /**
         * Returns all configured A2A clients.
         */
        fun getClients(): List<A2AClient> = configuration.getClients()

        /**
         * Returns the client configured for a specific remote agent URL.
         */
        fun getClient(agentUrl: String): A2AClient? = configuration.getClientForUrl(agentUrl)

        /**
         * Returns the default client, or null if none configured.
         */
        fun getDefaultClient(): A2AClient? = configuration.getClients().firstOrNull()

        /**
         * Closes all configured A2A clients.
         */
        fun close() {
            configuration.getClients().forEach { it.close() }
        }
    }

    companion object {
        /** Singleton instance for plugin registration. */
        val A2a = A2APlugin()
    }
}

/**
 * Configuration DSL for A2A integration.
 *
 * Allows configuring multiple remote A2A agents, each with
 * its own authentication provider and client settings.
 */
class A2AConfiguration {
    private val clients = mutableListOf<A2AClient>()
    private val urlClientMap = mutableMapOf<String, A2AClient>()

    /** Default client configuration. */
    var defaultConfig: A2AClientConfig = A2AClientConfig()

    /**
     * Configures a remote A2A agent to connect to.
     *
     * @param url The base URL of the remote agent.
     * @param block Configuration block for this remote agent.
     */
    fun remoteAgent(
        url: String,
        block: RemoteAgentBuilder.() -> Unit = {},
    ) {
        val builder = RemoteAgentBuilder(defaultConfig)
        builder.block()
        val client = builder.build()
        clients.add(client)
        urlClientMap[url] = client
    }

    /**
     * Adds a pre-configured A2A client.
     */
    fun client(client: A2AClient) {
        clients.add(client)
    }

    internal fun getClients(): List<A2AClient> = clients.toList()

    internal fun getClientForUrl(url: String): A2AClient? = urlClientMap[url]
}

/**
 * Builder for configuring a single remote A2A agent connection.
 */
class RemoteAgentBuilder(
    private val defaultConfig: A2AClientConfig,
) {
    /** Authentication provider for this remote agent. */
    var authProvider: A2AAuthProvider = NoAuthProvider()

    /** Client configuration overrides for this remote agent. */
    var config: A2AClientConfig = defaultConfig

    /** Custom Ktor HttpClient (null = create default). */
    var httpClient: io.ktor.client.HttpClient? = null

    fun build(): A2AClient {
        val client = httpClient ?: io.ktor.client.HttpClient()
        return A2AClient(
            httpClient = client,
            config = config,
            authProvider = authProvider,
        )
    }
}
