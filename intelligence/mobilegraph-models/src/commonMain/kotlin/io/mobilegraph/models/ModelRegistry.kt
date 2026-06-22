package io.mobilegraph.models

import io.mobilegraph.core.capability.Capability

/**
 * Registry for managing and discovering models at runtime.
 */
interface ModelRegistry {
    /**
     * Retrieves a chat model by its registered name.
     */
    fun chat(name: String): ChatModel?

    /**
     * Retrieves the default chat model.
     */
    fun chat(): ChatModel?

    /**
     * Retrieves a chat model that supports the specified capability.
     */
    fun chatFor(capability: Capability): ChatModel?

    /**
     * Retrieves an embedding model by its registered name.
     */
    fun embedding(name: String): EmbeddingModel?

    /**
     * Retrieves the default embedding model.
     */
    fun embedding(): EmbeddingModel?
}
