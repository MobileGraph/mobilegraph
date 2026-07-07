/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.models.middleware

import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.models.AssistantMessage
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.models.UserMessage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoggingMiddlewareTest {
    private class MockLogger : MobileGraphLogger {
        val messages = mutableListOf<String>()

        override fun log(
            message: String,
            severity: MobileGraphLogger.Severity,
        ) {
            messages.add(message)
        }
    }

    @Test
    fun testPromptAndOutputLogging() =
        runTest {
            val logger = MockLogger()
            val middleware = LoggingMiddleware(logger = logger)
            val input = ChatModelInput(ChatPromptValue(listOf(UserMessage("hello"))), null)

            middleware.intercept(input, ExecutionContext.Empty) { _, _ ->
                ModelOutput.ChatOutput(AssistantMessage("hi"))
            }

            assertEquals(2, logger.messages.size)
            assertTrue(logger.messages[0].contains("Invoking model"))
            assertTrue(logger.messages[1].contains("Model response"))
        }

    @Test
    fun testErrorLogging() =
        runTest {
            val logger = MockLogger()
            val middleware = LoggingMiddleware(logger = logger)
            val input = ChatModelInput(ChatPromptValue(emptyList()), null)

            middleware.intercept(input, ExecutionContext.Empty) { _, _ ->
                ModelOutput.ErrorOutput(Exception("failed"))
            }

            assertEquals(2, logger.messages.size)
            assertTrue(logger.messages[1].contains("Model error"))
        }
}
