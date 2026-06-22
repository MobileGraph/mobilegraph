/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.core.context

import io.mobilegraph.core.cancellation.CancellationToken
import io.mobilegraph.core.ids.RequestId
import io.mobilegraph.core.ids.SessionId
import io.mobilegraph.core.ids.TraceId
import io.mobilegraph.core.metadata.Metadata
import io.mobilegraph.core.registry.ComponentProvider

/**
 * Universal context for all MobileGraph operations.
 *
 * Provides tracing, session management, cancellation, and component resolution
 * capabilities across the SDK layers.
 */
interface ExecutionContext : ComponentProvider {
    /**
     * Unique identifier for the overall trace/operation.
     */
    val traceId: TraceId

    /**
     * Optional session identifier for grouped interactions.
     */
    val sessionId: SessionId?

    /**
     * Unique identifier for the specific request within a trace.
     */
    val requestId: RequestId

    /**
     * Arbitrary metadata associated with the context.
     */
    val metadata: Metadata

    /**
     * Preferred locale for the operation.
     */
    val locale: String?

    /**
     * Epoch timestamp (ms) after which the operation should time out.
     */
    val deadline: Long?

    /**
     * Token used to signal cancellation of the operation.
     */
    val cancellationToken: CancellationToken

    /**
     * Returns a new context with the updated metadata.
     */
    fun withMetadata(metadata: Metadata): ExecutionContext

    /**
     * Returns a new context with the updated request ID.
     */
    fun withRequestId(requestId: RequestId): ExecutionContext

    companion object {
        /**
         * A default empty context for initial or independent operations.
         */
        val Empty: ExecutionContext by lazy {
            SimpleExecutionContext(
                traceId = TraceId("empty-trace"),
                requestId = RequestId("empty-req"),
            )
        }
    }
}
