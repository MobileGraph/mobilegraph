package io.mobilegraph.tools.registry

import io.mobilegraph.core.tools.Tool
import io.mobilegraph.core.tools.ToolRegistry

/**
 * Default implementation of [ToolRegistry].
 */
internal class DefaultToolRegistry : ToolRegistry {
    private val tools = mutableMapOf<String, Tool<*, *>>()

    override fun register(tool: Tool<*, *>) {
        tools[tool.metadata.name] = tool
    }

    override fun get(name: String): Tool<*, *>? = tools[name]

    override fun getAll(): List<Tool<*, *>> = tools.values.toList()
}
