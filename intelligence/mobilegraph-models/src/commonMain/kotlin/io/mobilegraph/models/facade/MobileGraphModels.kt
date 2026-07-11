/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.models.facade

import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.models.ChatModel
import io.mobilegraph.models.EmbeddingModel
import io.mobilegraph.models.ModelRegistry

/**
 * Provides access to model-related functionality through the MobileGraph facade.
 *
 * Usage: `MobileGraph.models.chat()`
 */
val MobileGraph.models: MobileGraphModels
    get() = MobileGraphModels(this)

/**
 * Provides access to model-related functionality through the MobileGraph companion (global instance).
 */
val MobileGraph.Companion.models: MobileGraphModels
    get() = MobileGraph.instance.models

/**
 * Entry point for model operations.
 *
 * This class provides access to the global model registry and default models.
 * It is primarily intended for stateless utility tasks like summarization,
 * translation, or background processing where conversation history is not needed.
 */
class MobileGraphModels(
    private val mobileGraph: MobileGraph,
) {
    /**
     * Retrieves the default chat model from the registry.
     *
     * **Usage Recommendation**: Use this for **Stateless Utility Tasks**
     * (e.g., summarizing a single document, translating a sentence).
     * For user-facing chat interactions with history, use `session.model()` instead.
     *
     * @return The default [ChatModel].
     * @throws IllegalStateException if no ChatModel is registered.
     */
    fun chat(): ChatModel = registry().chat() ?: throw IllegalStateException("No ChatModel registered")

    /**
     * Retrieves a named chat model from the registry.
     */
    fun chat(name: String): ChatModel = registry().chat(name) ?: throw IllegalStateException("ChatModel '$name' not found")

    /**
     * Retrieves the default embedding model from the registry.
     */
    fun embedding(): EmbeddingModel = registry().embedding() ?: throw IllegalStateException("No EmbeddingModel registered")

    /**
     * Accesses the model registry for advanced selection.
     */
    fun registry(): ModelRegistry =
        mobileGraph.environment.getComponent(ModelRegistry::class)
            ?: throw IllegalStateException("ModelRegistry not found in environment")
}
