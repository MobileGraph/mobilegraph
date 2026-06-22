package io.mobilegraph.core.environment

import io.mobilegraph.core.configuration.KnowledgeConfiguration
import io.mobilegraph.core.configuration.LifecycleConfiguration
import io.mobilegraph.core.configuration.MemoryConfiguration
import io.mobilegraph.core.configuration.MobileGraphConfiguration
import io.mobilegraph.core.configuration.ModelsConfiguration
import io.mobilegraph.core.configuration.ObservabilityConfiguration
import io.mobilegraph.core.configuration.PluginsConfiguration
import io.mobilegraph.core.configuration.PromptsConfiguration
import io.mobilegraph.core.configuration.RuntimeConfiguration
import io.mobilegraph.core.configuration.SecurityConfiguration
import io.mobilegraph.core.configuration.StorageConfiguration
import io.mobilegraph.core.configuration.ToolsConfiguration
import io.mobilegraph.core.middleware.Middleware
import kotlin.reflect.KClass

/**
 * Immutable shared dependencies for the MobileGraph runtime.
 */
class MobileGraphEnvironment private constructor(
    val configuration: MobileGraphConfiguration,
    val runtimeConfig: RuntimeConfiguration,
    private val components: Map<KClass<*>, Any>,
    val middleware: List<Middleware<*, *>>,
) {
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getComponent(clazz: KClass<T>): T? = components[clazz] as? T

    class Builder {
        private var configuration = MobileGraphConfiguration()
        private var runtimeConfig = RuntimeConfiguration()
        private val components = mutableMapOf<KClass<*>, Any>()
        private val middleware = mutableListOf<Middleware<*, *>>()

        fun configuration(config: MobileGraphConfiguration) = apply { this.configuration = config }

        fun runtime(block: RuntimeConfiguration.() -> Unit) =
            apply {
                runtimeConfig.block()
            }

        fun models(block: ModelsConfiguration.() -> Unit) =
            apply {
                val config = ModelsConfiguration()
                config.block()
                // In a future phase, config will be used to register models automatically
            }

        fun prompts(block: PromptsConfiguration.() -> Unit) =
            apply {
                val config = PromptsConfiguration()
                config.block()
            }

        fun knowledge(block: KnowledgeConfiguration.() -> Unit) =
            apply {
                KnowledgeConfiguration().block()
            }

        fun memory(block: MemoryConfiguration.() -> Unit) =
            apply {
                MemoryConfiguration().block()
            }

        fun tools(block: ToolsConfiguration.() -> Unit) =
            apply {
                ToolsConfiguration().block()
            }

        fun observability(block: ObservabilityConfiguration.() -> Unit) =
            apply {
                ObservabilityConfiguration().block()
            }

        fun security(block: SecurityConfiguration.() -> Unit) =
            apply {
                SecurityConfiguration().block()
            }

        fun storage(block: StorageConfiguration.() -> Unit) =
            apply {
                StorageConfiguration().block()
            }

        fun lifecycle(block: LifecycleConfiguration.() -> Unit) =
            apply {
                LifecycleConfiguration().block()
            }

        fun plugins(block: PluginsConfiguration.() -> Unit) =
            apply {
                PluginsConfiguration(this).block()
            }

        fun <T : Any> component(
            clazz: KClass<T>,
            instance: T,
        ) = apply {
            components[clazz] = instance
        }

        fun middleware(middleware: Middleware<*, *>) =
            apply {
                this.middleware.add(middleware)
            }

        fun build() =
            MobileGraphEnvironment(
                configuration = configuration,
                runtimeConfig = runtimeConfig,
                components = components.toMap(),
                middleware = middleware.toList(),
            )
    }
}
