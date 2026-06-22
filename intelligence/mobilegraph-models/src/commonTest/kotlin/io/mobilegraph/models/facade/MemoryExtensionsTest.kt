/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.models.facade

import io.mobilegraph.core.environment.MobileGraphEnvironment
import io.mobilegraph.models.memory.ChatMessageHistory
import io.mobilegraph.models.memory.ConversationBufferWindowMemory
import io.mobilegraph.models.memory.ConversationSummaryBufferMemory
import io.mobilegraph.models.memory.InMemoryChatMessageHistory
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MemoryExtensionsTest {
    @Test
    fun testUseChatMemory() {
        val env =
            MobileGraphEnvironment
                .Builder()
                .withMemory {
                    useChatMemory()
                }.build()

        val history = env.getComponent(ChatMessageHistory::class)
        assertNotNull(history)
        assertTrue(history is InMemoryChatMessageHistory)
    }

    @Test
    fun testUseWindowChatMemory() {
        val env =
            MobileGraphEnvironment
                .Builder()
                .withMemory {
                    useWindowChatMemory(k = 10)
                }.build()

        val history = env.getComponent(ChatMessageHistory::class)
        assertNotNull(history)
        assertTrue(history is ConversationBufferWindowMemory)
    }

    @Test
    fun testUseSummaryBufferMemory() {
        val env =
            MobileGraphEnvironment
                .Builder()
                .withMemory {
                    useSummaryBufferMemory(modelName = "mini")
                }.build()

        val history = env.getComponent(ChatMessageHistory::class)
        assertNotNull(history)
        assertTrue(history is ConversationSummaryBufferMemory)
    }
}
