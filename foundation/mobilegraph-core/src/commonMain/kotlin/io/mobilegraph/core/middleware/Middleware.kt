package io.mobilegraph.core.middleware

import io.mobilegraph.core.context.ExecutionContext

/**
 * Interface for cross-cutting concerns. A "hook" system that intercepts requests and responses to add behaviors like logging, retries, or security.
 * It Decouples "business logic" (AI) from "infrastructure logic" (logging). It allows you to add features like automatic retries to any model without modifying its source code.
 */
interface Middleware<I, O> {
    suspend fun intercept(
        input: I,
        context: ExecutionContext,
        next: suspend (I, ExecutionContext) -> O,
    ): O
}
