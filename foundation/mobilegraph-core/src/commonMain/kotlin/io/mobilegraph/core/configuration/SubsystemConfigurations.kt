package io.mobilegraph.core.configuration

import io.mobilegraph.core.environment.MobileGraphEnvironment
import io.mobilegraph.core.facade.MobileGraphPlugin

/**
 * Placeholder configurations for various MobileGraph subsystems.
 * In the Foundation Phase, these serve as DSL entry points.
 */

class ModelsConfiguration {
    // Add logic for model registration in the intelligence layer extensions
}

class PromptsConfiguration {
    // Add logic for prompt registration in the intelligence layer extensions
}

class KnowledgeConfiguration {
    // Add logic for vector stores and retrieval
}

class MemoryConfiguration {
    var history: Any? = null

    /**
     * Sets the chat message history implementation.
     */
    fun history(history: Any) {
        this.history = history
    }
}

class ToolsConfiguration {
    // Add logic for tool registration and policies
}

class ObservabilityConfiguration {
    // Add logic for tracing, metrics, and audit
}

class SecurityConfiguration {
    // Add logic for credential storage and PII filtering
}

class StorageConfiguration {
    // Add logic for checkpointing and persistence
}

class LifecycleConfiguration {
    // Add logic for platform lifecycle integration
}

class PluginsConfiguration(
    private val environmentBuilder: MobileGraphEnvironment.Builder,
) {
    /**
     * Installs a plugin into the MobileGraph environment.
     */
    fun <TConfig : Any, TPlugin : Any> install(
        plugin: MobileGraphPlugin<TConfig, TPlugin>,
        configure: TConfig.() -> Unit = {},
    ): TPlugin = plugin.install(environmentBuilder, configure)
}
