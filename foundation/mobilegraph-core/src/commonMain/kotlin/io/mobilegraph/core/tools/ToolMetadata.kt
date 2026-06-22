package io.mobilegraph.core.tools

/**
 * Metadata describing a tool.
 */
data class ToolMetadata(
    /**
     * Unique name of the tool.
     */
    val name: String,
    /**
     * Human-readable description of what the tool does.
     */
    val description: String,
    /**
     * Optional tags for categorizing or filtering tools.
     */
    val tags: Set<String> = emptySet(),
)
