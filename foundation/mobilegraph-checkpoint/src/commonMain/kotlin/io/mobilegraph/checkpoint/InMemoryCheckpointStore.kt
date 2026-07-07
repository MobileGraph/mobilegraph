package io.mobilegraph.checkpoint

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Simple in-memory implementation of [CheckpointStore].
 * Useful for testing and transient sessions.
 */
class InMemoryCheckpointStore : CheckpointStore {
    private val checkpoints = mutableMapOf<CheckpointId, Checkpoint>()
    private val mutex = Mutex()

    override suspend fun save(checkpoint: Checkpoint) =
        mutex.withLock {
            checkpoints[checkpoint.id] = checkpoint
        }

    override suspend fun load(id: CheckpointId): Checkpoint? =
        mutex.withLock {
            checkpoints[id]
        }

    override suspend fun list(executionId: String): List<Checkpoint> =
        mutex.withLock {
            // Simple implementation: filter by some logic if executionId is encoded in CheckpointId
            checkpoints.values
                .filter { it.id.value.startsWith(executionId) }
                .sortedBy { it.metadata.timestamp }
        }

    override suspend fun delete(id: CheckpointId) =
        mutex.withLock {
            checkpoints.remove(id)
            Unit
        }
}
