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
import io.mobilegraph.core.facade.MobileGraph

/**
 * Accesses memory-related functionality through the MobileGraph facade.
 */
val MobileGraph.memory: MobileGraphMemory
    get() = MobileGraphMemory(this)

/**
 * Entry point for memory operations.
 */
class MobileGraphMemory(
    private val mobileGraph: MobileGraph,
) {
    /**
     * Clears the chat history for the global session.
     */
    suspend fun clearGlobal() {
        getMemory()?.clear(ExecutionContext.Empty)
    }

    /**
     * Clears the chat history for all sessions.
     */
    suspend fun clearAll() {
        getMemory()?.clearAll()
    }

    private fun getMemory(): ChatMemory<*>? = mobileGraph.environment.getComponent(ChatMemory::class)
}
