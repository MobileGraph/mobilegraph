package io.mobilegraph.tools.facade

import io.mobilegraph.core.environment.MobileGraphEnvironment
import io.mobilegraph.core.tools.Tool
import io.mobilegraph.core.tools.ToolRegistry
import io.mobilegraph.tools.registry.DefaultToolRegistry

/**
 * DSL for registering tools during initialization.
 */
class ToolsBuilder {
    private val registry = DefaultToolRegistry()

    fun register(tool: Tool<*, *>) {
        registry.register(tool)
    }

    internal fun build(): ToolRegistry = registry
}

/**
 * Use it to register custom tools
 */
fun MobileGraphEnvironment.Builder.withTools(block: ToolsBuilder.() -> Unit) =
    apply {
        val builder = ToolsBuilder()
        builder.block()
        component(ToolRegistry::class, builder.build())
    }
