/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.core.memory

import io.mobilegraph.core.context.ExecutionContext

/**
 * Base interface for managing conversation history and state.
 *
 * This interface is generic to support different types of memory storage,
 * from raw chat messages to summarized entities.
 *
 * @param T The type of items stored in memory.
 */
interface ChatMemory<T> {
    /**
     * Adds an item to the memory store.
     *
     * @param item The item to persist.
     * @param context The current execution context.
     */
    suspend fun add(
        item: T,
        context: ExecutionContext,
    )

    /**
     * Retrieves all items currently held in memory.
     *
     * @param context The current execution context.
     * @return A list of items in the order they were added.
     */
    suspend fun get(context: ExecutionContext): List<T>

    /**
     * Clears all items for a specific session from the memory store.
     *
     * @param context The current execution context (contains the session ID).
     */
    suspend fun clear(context: ExecutionContext)

    /**
     * Clears all items for all sessions from the memory store.
     *
     * @param context The current execution context.
     */
    suspend fun clearAll(context: ExecutionContext = ExecutionContext.Empty)
}
