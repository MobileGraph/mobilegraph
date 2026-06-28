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

    // --- Phase 2: Knowledge & RAG Events ---

    data class DocumentsLoaded(
        override val traceId: TraceId,
        override val requestId: RequestId,
        val count: Int,
        override val sessionId: SessionId? = null,
        override val timestamp: Instant = Clock.System.now(),
    ) : MobileGraphEvent

    data class ChunkingCompleted(
        override val traceId: TraceId,
        override val requestId: RequestId,
        val originalCount: Int,
        val chunkCount: Int,
        override val sessionId: SessionId? = null,
        override val timestamp: Instant = Clock.System.now(),
    ) : MobileGraphEvent

    data class EmbeddingsGenerated(
        override val traceId: TraceId,
        override val requestId: RequestId,
        val count: Int,
        override val sessionId: SessionId? = null,
        override val timestamp: Instant = Clock.System.now(),
    ) : MobileGraphEvent

    data class RetrievalStarted(
        override val traceId: TraceId,
        override val requestId: RequestId,
        val query: String,
        override val sessionId: SessionId? = null,
        override val timestamp: Instant = Clock.System.now(),
    ) : MobileGraphEvent

    data class RetrievalCompleted(
        override val traceId: TraceId,
        override val requestId: RequestId,
        val filteredDocumentCount: Int,
        val vectorSearchedResult: List<Pair<String, Float>> = emptyList(), // ID and Score
        override val sessionId: SessionId? = null,
        override val timestamp: Instant = Clock.System.now(),
    ) : MobileGraphEvent

    data class RagResponseGenerated(
        override val traceId: TraceId,
        override val requestId: RequestId,
        override val sessionId: SessionId? = null,
        override val timestamp: Instant = Clock.System.now(),
    ) : MobileGraphEvent
}
