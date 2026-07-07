/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.models.memory

import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.models.AssistantMessage
import io.mobilegraph.models.ChatChunk
import io.mobilegraph.models.ChatModel
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.ModelConfig
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.models.SystemMessage
import io.mobilegraph.models.UserMessage
import io.mobilegraph.models.facade.chat
import io.mobilegraph.models.facade.withModels
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdvancedMemoryTest {
    @Test
    fun testWindowMemoryEviction() =
        runTest {
            val memory = ConversationBufferWindowMemory(k = 1) // Keep only 1 turn
            val context = ExecutionContext.Empty

            // Turn 1
            memory.add(UserMessage("q1", id = "m1"), context)
            memory.add(AssistantMessage("a1", id = "m2"), context)

            // Turn 2 - should evict Turn 1
            memory.add(UserMessage("q2", id = "m3"), context)
            memory.add(AssistantMessage("a2", id = "m4"), context)

            val history = memory.get(context)
            assertEquals(2, history.size)
            assertEquals("q2", history[0].content)
            assertEquals("a2", history[1].content)
        }

    @Test
    fun testSummaryBufferMemory() =
        runTest {
            // Mock model for summarization
            val mockModel =
                object : ChatModel {
                    override val name: String = "mock"

                    override fun supports(capability: io.mobilegraph.core.capability.Capability) = false

                    override suspend fun invoke(
                        prompt: ChatPromptValue,
                        config: ModelConfig?,
                        context: ExecutionContext,
                    ): ModelOutput = ModelOutput.ChatOutput(AssistantMessage("Summed up"))

                    override fun stream(
                        prompt: ChatPromptValue,
                        config: ModelConfig?,
                        context: ExecutionContext,
                    ) = kotlinx.coroutines.flow.emptyFlow<ChatChunk>()

                    override fun readModelConfig(): ModelConfig? = null
                }

            val memory = ConversationSummaryBufferMemory(maxBufferMessages = 2, summarizeModel = "mock")

            // Setup global registry for the mock model lookup
            io.mobilegraph.core.facade.MobileGraph.initialize {
                withModels {
                    chat("mock", mockModel)
                }
            }

            val context = ExecutionContext.Empty

            // Add 3 messages (exceeds buffer of 2)
            memory.add(UserMessage("m1", id = "1"), context)
            memory.add(AssistantMessage("m2", id = "2"), context)
            memory.add(UserMessage("m3", id = "3"), context)

            val history = memory.get(context)
            // Should have 1 SystemMessage (summary) + remaining buffer
            // Since we clear half of buffer on summarize (2 - 2/2 = 1 left)
            // Total should be around 1 (summary) + 1 (m3) = 2 messages
            assertEquals(2, history.size)
            assertTrue(history[0] is SystemMessage)
            assertEquals("Previous conversation summary: Summed up", history[0].content)
            assertEquals("m3", history[1].content)
        }
}
