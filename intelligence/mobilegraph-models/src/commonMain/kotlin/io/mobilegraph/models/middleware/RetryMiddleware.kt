/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.models.middleware

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.utils.io.errors.IOException
import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.models.ModelOutput
import kotlinx.coroutines.delay

/**
 * Middleware that automatically retries failed model invocations.
 *
 * Uses exponential backoff to handle transient network issues or server-side
 * rate limiting without overwhelming the provider or draining the mobile battery.
 */
class RetryMiddleware(
    /**
     * Maximum number of retry attempts.
     */
    private val maxRetries: Int = 3,
    /**
     * Initial delay in milliseconds before the first retry.
     */
    private val initialDelayMs: Long = 1000,
    /**
     * Factor by which the delay increases after each failed attempt.
     */
    private val backoffFactor: Double = 2.0,
) : ChatModelMiddleware {
    override suspend fun intercept(
        input: ChatModelInput,
        context: ExecutionContext,
        next: suspend (ChatModelInput, ExecutionContext) -> ModelOutput,
    ): ModelOutput {
        var lastException: Exception? = null
        var currentDelay = initialDelayMs

        repeat(maxRetries + 1) { attempt ->
            try {
                val output = next(input, context)

                // If it's an ErrorOutput, we check if the inner error is transient
                if (attempt < maxRetries && output is ModelOutput.ErrorOutput && isTransient(output.error)) {
                    delay(currentDelay)
                    currentDelay = (currentDelay * backoffFactor).toLong()
                } else {
                    return output
                }
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries && isTransient(e)) {
                    delay(currentDelay)
                    currentDelay = (currentDelay * backoffFactor).toLong()
                } else {
                    return ModelOutput.ErrorOutput(e)
                }
            }
        }

        return ModelOutput.ErrorOutput(lastException ?: Exception("Max retries reached"))
    }

    /**
     * Determines if an error is transient (retryable) or permanent.
     *
     * This production-ready implementation checks for Ktor-specific network
     * exceptions and relevant HTTP status codes (429, 500, 502, 503, 504).
     *
     * @param throwable The exception or error to check.
     * @return True if the error is considered transient and should be retried.
     */
    private fun isTransient(throwable: Throwable): Boolean =
        when (throwable) {
            // 1. Ktor Specific Timeouts & Network Issues
            is HttpRequestTimeoutException -> {
                true
            }

            is ConnectTimeoutException -> {
                true
            }

            is SocketTimeoutException -> {
                true
            }

            // 2. IO Errors (Standard library / Ktor)
            is kotlinx.io.IOException -> {
                true
            }

            // 3. HTTP Response Exceptions (Status Codes)
            is ResponseException -> {
                val status = throwable.response.status.value
                when (status) {
                    429 -> true

                    // Too Many Requests (Rate Limit)
                    500 -> true

                    // Internal Server Error (Sometimes transient)
                    502 -> true

                    // Bad Gateway
                    503 -> true

                    // Service Unavailable
                    504 -> true

                    // Gateway Timeout
                    else -> false
                }
            }

            // 4. Fallback to message inspection for uncaught cases
            else -> {
                val message = throwable.message?.lowercase() ?: ""
                message.contains("timeout") ||
                    message.contains("rate limit") ||
                    message.contains("connection reset") ||
                    message.contains("network unreachable")
            }
        }
}
