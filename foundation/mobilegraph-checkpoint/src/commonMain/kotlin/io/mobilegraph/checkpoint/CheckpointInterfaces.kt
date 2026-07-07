package io.mobilegraph.checkpoint

import io.mobilegraph.state.GraphState

/**
 * Unique identifier for a checkpoint.
 */
data class CheckpointId(
    val value: String,
)

/**
 * Metadata associated with a checkpoint for auditing and recovery.
 */
data class CheckpointMetadata(
    val timestamp: Long,
    val nodeId: String,
    val threadId: String? = null,
)

/**
 * Represents a saved point in a graph's execution.
 */
data class Checkpoint(
    val id: CheckpointId,
    val state: GraphState,
    val metadata: CheckpointMetadata,
)

/**
 * Interface for persisting and retrieving checkpoints.
 *
 * This is the core of "Durable Execution" in MobileGraph.
 */
interface CheckpointStore {
    /**
     * Persists a checkpoint.
     */
    suspend fun save(checkpoint: Checkpoint)

    /**
     * Retrieves a checkpoint by its ID.
     */
    suspend fun load(id: CheckpointId): Checkpoint?

    /**
     * Lists all checkpoints for a given execution context.
     */
    suspend fun list(executionId: String): List<Checkpoint>

    /**
     * Deletes a specific checkpoint.
     */
    suspend fun delete(id: CheckpointId)
}
