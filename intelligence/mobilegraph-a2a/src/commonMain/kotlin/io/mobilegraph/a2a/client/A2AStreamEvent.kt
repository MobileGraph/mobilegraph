package io.mobilegraph.a2a.client

import io.mobilegraph.a2a.models.A2AArtifact
import io.mobilegraph.a2a.models.A2AJsonRpcError
import io.mobilegraph.a2a.models.A2ATask
import io.mobilegraph.a2a.models.A2ATaskStatus

/**
 * Represents an event received during A2A SSE streaming.
 *
 * When using `message/stream`, the remote agent sends back
 * Server-Sent Events (SSE) with incremental task updates.
 * These events are parsed and exposed as [A2AStreamEvent] instances.
 */
sealed interface A2AStreamEvent {
    /**
     * The remote agent reported a task status change.
     *
     * @property taskId The task this update applies to.
     * @property status The updated task status.
     * @property isFinal Whether this is the final event (task reached terminal state).
     */
    data class TaskStatusUpdate(
        val taskId: String,
        val status: A2ATaskStatus,
        val isFinal: Boolean,
    ) : A2AStreamEvent

    /**
     * The remote agent produced an artifact (or artifact chunk).
     *
     * For streaming, artifacts may arrive in chunks with
     * `append=true` and `lastChunk=true/false`.
     *
     * @property taskId The task this artifact belongs to.
     * @property artifact The artifact data.
     */
    data class TaskArtifactUpdate(
        val taskId: String,
        val artifact: A2AArtifact,
    ) : A2AStreamEvent

    /**
     * The task completed and the full task object is available.
     *
     * @property task The completed task with all artifacts and history.
     */
    data class TaskComplete(
        val task: A2ATask,
    ) : A2AStreamEvent

    /**
     * An error occurred during streaming.
     *
     * @property error The JSON-RPC error from the stream.
     * @property taskId The task ID if available.
     */
    data class StreamError(
        val error: A2AJsonRpcError,
        val taskId: String? = null,
    ) : A2AStreamEvent
}
