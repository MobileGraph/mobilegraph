package io.mobilegraph.core.tools

import io.mobilegraph.core.context.ExecutionContext

/**
 * Interface for selecting a subset of tools based on a query or context.
 */
interface ToolSelector {
    /**
     * Selects relevant tools from the given [registry] for the provided [query].
     */
    suspend fun selectTools(
        query: String,
        registry: ToolRegistry,
        context: ExecutionContext,
    ): List<Tool<*, *>>
}

/**
 * A tool selector that returns all tools (default behavior).
 */
class AllToolSelector : ToolSelector {
    override suspend fun selectTools(
        query: String,
        registry: ToolRegistry,
        context: ExecutionContext,
    ): List<Tool<*, *>> = registry.getAll()
}
