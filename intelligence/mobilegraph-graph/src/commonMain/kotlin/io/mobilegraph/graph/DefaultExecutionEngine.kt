package io.mobilegraph.graph

import io.mobilegraph.checkpoint.Checkpoint
import io.mobilegraph.checkpoint.CheckpointId
import io.mobilegraph.checkpoint.CheckpointMetadata
import io.mobilegraph.checkpoint.CheckpointStore
import io.mobilegraph.core.events.EventPublisher
import io.mobilegraph.core.events.MobileGraphEvent
import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.state.GraphState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.isActive
import kotlinx.coroutines.supervisorScope

/**
 * Core engine responsible for traversing and executing a [StateGraph].
 *
 * It manages the lifecycle of node execution, conditional branching,
 * and automatic checkpointing for durability.
 */
class DefaultExecutionEngine(
    private val checkpointStore: CheckpointStore? = null,
) : ExecutionEngine {
    override suspend fun execute(
        graph: StateGraph,
        initialState: GraphState,
    ): ExecutionResult = runLoop(graph, initialState, listOf(graph.startNodeId), resume = false)

    override suspend fun resume(
        graph: StateGraph,
        state: GraphState,
        nodeId: String,
    ): ExecutionResult = runLoop(graph, state, listOf(nodeId), resume = true)

    private suspend fun runLoop(
        graph: StateGraph,
        initialState: GraphState,
        startNodeIds: List<String>,
        resume: Boolean,
    ): ExecutionResult =
        supervisorScope {
            // Use supervisorScope to prevent one node's crash from killing siblings
            var currentState = initialState
            var currentNodes = startNodeIds
            var isResuming = resume
            val eventPublisher = MobileGraph.instance.getComponent(EventPublisher::class)

            // The engine continues as long as there are active nodes to process
            loop@ while (currentNodes.isNotEmpty() && isActive) {
                val nodeResults =
                    if (isResuming) {
                        // If we are resuming, we skip the execution of the current nodes
                        // and treat them as if they just completed with the current state.
                        isResuming = false
                        currentNodes.map { it to ExecutionResult.Success(currentState) }
                    } else {
                        // Execute all current level nodes (can be one or many in parallel)
                        val deferred =
                            currentNodes.map { nodeId ->
                                async {
                                    val node =
                                        graph.nodes[nodeId]
                                            ?: throw IllegalStateException("Node with ID '$nodeId' not found in graph")

                                    val context = currentState.executionContext

                                    // Emit NodeStarted
                                    eventPublisher?.publish(
                                        MobileGraphEvent.NodeStarted(
                                            traceId = context.traceId,
                                            requestId = context.requestId,
                                            nodeId = nodeId,
                                        ),
                                    )

                                    try {
                                        // 1. Execute node logic
                                        val result = node.execute(currentState)

                                        // Emit NodeCompleted
                                        eventPublisher?.publish(
                                            MobileGraphEvent.NodeCompleted(
                                                traceId = context.traceId,
                                                requestId = context.requestId,
                                                nodeId = nodeId,
                                            ),
                                        )

                                        // 2. Save Checkpoint (Durability)
                                        saveCheckpoint(nodeId, result.state)

                                        nodeId to result
                                    } catch (e: Exception) {
                                        // Error Strategy: Isolated Failure
                                        // If a node crashes, we catch it here so that its siblings in a
                                        // parallel Fan-Out can still finish their work for this "tick".
                                        nodeId to ExecutionResult.Error(currentState)
                                    }
                                }
                            }
                        // Wait for all nodes in the current parallel batch to finish
                        deferred.awaitAll()
                    }

                // 3. Merge states and check for interrupts
                val pendingNextNodes = mutableListOf<String>()

                for ((nodeId, result) in nodeResults) {
                    when (result) {
                        is ExecutionResult.AwaitingReview -> {
                            eventPublisher?.publish(
                                MobileGraphEvent.WaitingForReview(
                                    traceId = currentState.executionContext.traceId,
                                    requestId = currentState.executionContext.requestId,
                                    nodeId = nodeId,
                                ),
                            )
                            return@supervisorScope result
                        }

                        is ExecutionResult.AwaitingJoin -> {
                            // This node is waiting for other parallel branches.
                            // We do not transition from it yet.
                        }

                        is ExecutionResult.Success -> {
                            // Merge variables from parallel branches to prevent data loss
                            currentState =
                                result.state.copy(
                                    variables = currentState.variables + result.state.variables,
                                )
                            val node = graph.nodes[nodeId]
                            if (node !is EndNode) {
                                pendingNextNodes.addAll(findNextNodeIds(graph, nodeId, currentState))
                            }
                        }

                        is ExecutionResult.Error -> {
                            // Error Strategy: Short-Circuit
                            // If any node in the batch returns an Error, we stop the whole graph
                            // AFTER the current batch of parallel nodes has finished.
                            return@supervisorScope result
                        }
                    }
                }

                currentNodes = pendingNextNodes
            }

            ExecutionResult.Success(currentState)
        }

    private suspend fun findNextNodeIds(
        graph: StateGraph,
        currentNodeId: String,
        state: GraphState,
    ): List<String> =
        graph.edges
            .filter { it.from == currentNodeId && it.shouldTransition(state) }
            .map { it.to }

    private suspend fun saveCheckpoint(
        nodeId: String,
        state: GraphState,
    ) {
        if (checkpointStore == null) return

        val now = 0L // Mocked for now

        val checkpointId =
            CheckpointId(
                "${state.executionContext.traceId.value}_${nodeId}_$now",
            )

        val checkpoint =
            Checkpoint(
                id = checkpointId,
                state = state,
                metadata =
                    CheckpointMetadata(
                        timestamp = now,
                        nodeId = nodeId,
                    ),
            )

        checkpointStore.save(checkpoint)
    }
}
