package io.mobilegraph.a2a.facade

import io.mobilegraph.a2a.A2AConfiguration
import io.mobilegraph.core.environment.MobileGraphEnvironment

/**
 * DSL extension for configuring A2A (Agent-to-Agent) protocol support
 * in MobileGraph initialization.
 *
 * ## Usage
 * ```kotlin
 * MobileGraph.initialize {
 *     a2a {
 *         remoteAgent("https://coding-agent.example.com") {
 *             authProvider = BearerTokenAuthProvider("my-token")
 *         }
 *         remoteAgent("https://analysis-agent.example.com") {
 *             authProvider = ApiKeyAuthProvider("my-api-key")
 *             config = A2AClientConfig(requestTimeoutMs = 120_000L)
 *         }
 *     }
 * }
 * ```
 */
fun MobileGraphEnvironment.Builder.a2a(block: A2AConfiguration.() -> Unit) =
    apply {
        val config = A2AConfiguration()
        config.block()
        component(A2AConfiguration::class, config)
    }
