/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.models.middleware

import io.mobilegraph.core.context.SimpleExecutionContext
import io.mobilegraph.core.ids.RequestId
import io.mobilegraph.core.ids.SessionId
import io.mobilegraph.core.ids.TraceId
import io.mobilegraph.models.AssistantMessage
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.models.UserMessage
import io.mobilegraph.models.memory.InMemoryChatMessageHistory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatMemoryMiddlewareTest {
    @Test
    fun testAutomaticHistoryManagement() =
        runTest {
            val history = InMemoryChatMessageHistory()
            val context = SimpleExecutionContext(TraceId("t1"), SessionId("s1"), RequestId("r1"))

            // Manual component registration for context (since middleware looks there first)
            val contextWithMemory =
                SimpleExecutionContext(
                    TraceId("t1"),
                    SessionId("s1"),
                    RequestId("r1"),
                    componentProvider =
                        object : io.mobilegraph.core.registry.ComponentProvider {
                            override fun <T : Any> getComponent(clazz: kotlin.reflect.KClass<T>): T? =
                                if (clazz == io.mobilegraph.models.memory.ChatMessageHistory::class) history as T else null
                        },
                )

            val middleware = ChatMemoryMiddleware()
            val input1 = ChatModelInput(ChatPromptValue(listOf(UserMessage("user msg 1", id = "msg1"))), null)

            // First interaction
            middleware.intercept(input1, contextWithMemory) { input, _ ->
                assertEquals(1, input.prompt.messages.size)
                ModelOutput.ChatOutput(AssistantMessage("ai resp 1", id = "ai1"))
            }

            // Second interaction - history should be prepended
            val input2 = ChatModelInput(ChatPromptValue(listOf(UserMessage("user msg 2", id = "msg2"))), null)
            middleware.intercept(input2, contextWithMemory) { enrichedInput, _ ->
                // History: [Human(1), Assistant(1)] + Current: [Human(2)] = 3 messages
                assertEquals(3, enrichedInput.prompt.messages.size)
                assertTrue(enrichedInput.prompt.messages[0] is UserMessage)
                assertEquals("user msg 1", enrichedInput.prompt.messages[0].content)
                assertTrue(enrichedInput.prompt.messages[1] is AssistantMessage)
                assertEquals("ai resp 1", enrichedInput.prompt.messages[1].content)

                ModelOutput.ChatOutput(AssistantMessage("ai resp 2", id = "ai2"))
            }

            // Check final history state
            val saved = history.get(contextWithMemory)
            assertEquals(4, saved.size) // [H1, A1, H2, A2]
        }
}
