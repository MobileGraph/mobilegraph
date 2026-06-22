/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.models.middleware

import io.ktor.client.plugins.HttpRequestTimeoutException
import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.models.AssistantMessage
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.ModelOutput
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RetryMiddlewareTest {
    @Test
    fun testSuccessfulRetry() =
        runTest {
            var attempts = 0
            val middleware = RetryMiddleware(maxRetries = 2, initialDelayMs = 1)
            val input = ChatModelInput(ChatPromptValue(emptyList()), null)

            val result =
                middleware.intercept(input, ExecutionContext.Empty) { _, _ ->
                    attempts++
                    if (attempts < 2) {
                        ModelOutput.ErrorOutput(HttpRequestTimeoutException("url", 1000L))
                    } else {
                        ModelOutput.ChatOutput(AssistantMessage("success"))
                    }
                }

            assertEquals(2, attempts)
            assertTrue(result is ModelOutput.ChatOutput)
            assertEquals("success", result.message.content)
        }

    @Test
    fun testPermanentFailureNoRetry() =
        runTest {
            var attempts = 0
            val middleware = RetryMiddleware(maxRetries = 3, initialDelayMs = 1)
            val input = ChatModelInput(ChatPromptValue(emptyList()), null)

            val result =
                middleware.intercept(input, ExecutionContext.Empty) { _, _ ->
                    attempts++
                    ModelOutput.ErrorOutput(Exception("Permanent Error"))
                }

            assertEquals(1, attempts)
            assertTrue(result is ModelOutput.ErrorOutput)
        }

    @Test
    fun testMaxRetriesReached() =
        runTest {
            var attempts = 0
            val middleware = RetryMiddleware(maxRetries = 2, initialDelayMs = 1)
            val input = ChatModelInput(ChatPromptValue(emptyList()), null)

            val result =
                middleware.intercept(input, ExecutionContext.Empty) { _, _ ->
                    attempts++
                    ModelOutput.ErrorOutput(HttpRequestTimeoutException("url", 1000L))
                }

            assertEquals(3, attempts) // Initial + 2 retries
            assertTrue(result is ModelOutput.ErrorOutput)
        }
}
