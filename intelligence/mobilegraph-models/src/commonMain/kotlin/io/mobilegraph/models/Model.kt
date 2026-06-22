package io.mobilegraph.models

import io.mobilegraph.core.capability.Capability

/**
 * Base interface for all models in MobileGraph.
 */
interface Model {
    /**
     * The unique name of the model (e.g., "gpt-4o", "gemini-1.5-pro").
     */
    val name: String

    /**
     * Checks if the model supports a specific capability.
     */
    fun supports(capability: Capability): Boolean
}
