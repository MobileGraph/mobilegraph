package io.mobilegraph.core.events

import io.mobilegraph.core.ids.RequestId
import io.mobilegraph.core.ids.SessionId
import io.mobilegraph.core.ids.TraceId
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Base interface for all events published by the MobileGraph runtime.
 */
sealed interface MobileGraphEvent {
    val traceId: TraceId
    val requestId: RequestId
    val sessionId: SessionId?
    val timestamp: Instant

    data class RequestStarted(
        override val traceId: TraceId,
        override val requestId: RequestId,
        override val sessionId: SessionId? = null,
        override val timestamp: Instant = Clock.System.now(),
    ) : MobileGraphEvent

    data class RequestCompleted(
        override val traceId: TraceId,
        override val requestId: RequestId,
        override val sessionId: SessionId? = null,
        override val timestamp: Instant = Clock.System.now(),
    ) : MobileGraphEvent

    data class RequestFailed(
        override val traceId: TraceId,
        override val requestId: RequestId,
        val errorMessage: String,
        override val sessionId: SessionId? = null,
        override val timestamp: Instant = Clock.System.now(),
    ) : MobileGraphEvent
}
