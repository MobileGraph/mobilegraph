package io.mobilegraph.core.tools

/**
 * Registry for managing and discovering tools.
 */
interface ToolRegistry {
    /**
     * Registers a tool.
     */
    fun register(tool: Tool<*, *>)

    /**
     * Retrieves a tool by its metadata name.
     */
    fun get(name: String): Tool<*, *>?

    /**
     * Retrieves all registered tools.
     */
    fun getAll(): List<Tool<*, *>>
}
