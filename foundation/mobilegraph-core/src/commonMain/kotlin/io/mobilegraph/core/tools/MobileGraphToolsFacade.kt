package io.mobilegraph.core.tools

import io.mobilegraph.core.facade.MobileGraph

/**
 * Provides access to tool-related functionality through the MobileGraph facade.
 */
val MobileGraph.tools: MobileGraphTools
    get() = MobileGraphTools(this)

/**
 * Provides access to tool-related functionality through the MobileGraph companion (global instance).
 */
val MobileGraph.Companion.tools: MobileGraphTools
    get() = MobileGraph.instance.tools

/**
 * Entry point for tool operations.
 */
class MobileGraphTools(
    private val mobileGraph: MobileGraph,
) {
    /**
     * Accesses the tool registry.
     */
    fun registry(): ToolRegistry =
        mobileGraph.environment.getComponent(ToolRegistry::class)
            ?: throw IllegalStateException("ToolRegistry not found in environment")

    /**
     * Accesses the tool selector.
     */
    fun selector(): ToolSelector =
        mobileGraph.environment.getComponent(ToolSelector::class)
            ?: AllToolSelector()
}
