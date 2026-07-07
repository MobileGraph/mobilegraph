package io.mobilegraph.graph

import io.mobilegraph.state.GraphState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A specialized node that acts as a synchronization point for multiple parallel paths.
 *
 * It will only proceed once all expected incoming transitions have reached this node.
 * Implementations should define how states from different paths are merged.
 */
class JoinNode(
    override val id: String,
    private val expectedIncomingCount: Int,
    private val mergeStrategy: (List<GraphState>) -> GraphState = { states -> states.last() },
) : GraphNode {
    private val receivedStates = mutableListOf<GraphState>()
    private val mutex = Mutex()

    override suspend fun execute(state: GraphState): ExecutionResult =
        mutex.withLock {
            receivedStates.add(state)
            if (receivedStates.size < expectedIncomingCount) {
                // Not all branches have arrived yet
                ExecutionResult.AwaitingJoin(id, state)
            } else {
                val mergedState = mergeStrategy(receivedStates.toList())
                receivedStates.clear()
                ExecutionResult.Success(mergedState)
            }
        }
}
