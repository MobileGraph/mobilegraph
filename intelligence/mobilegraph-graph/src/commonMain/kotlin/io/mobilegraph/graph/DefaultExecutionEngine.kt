package io.mobilegraph.graph

import io.mobilegraph.checkpoint.Checkpoint
import io.mobilegraph.checkpoint.CheckpointId
import io.mobilegraph.checkpoint.CheckpointMetadata
import io.mobilegraph.checkpoint.CheckpointStore
import io.mobilegraph.core.events.EventPublisher
import io.mobilegraph.core.events.MobileGraphEvent
import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.core.lifecycle.LifecycleEvent
import io.mobilegraph.core.lifecycle.LifecycleRegistry
import io.mobilegraph.core.lifecycle.LifecycleState
import io.mobilegraph.state.GraphState
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlin.coroutines.cancellation.CancellationException

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
        config: ExecutionConfig,
    ): ExecutionResult = runLoop(graph, initialState, listOf(graph.startNodeId), resume = false, config = config)

    override suspend fun resume(
        graph: StateGraph,
        state: GraphState,
        nodeId: String,
        config: ExecutionConfig,
        reExecute: Boolean,
    ): ExecutionResult = runLoop(graph, state, listOf(nodeId), resume = !reExecute, config = config)

    private suspend fun runLoop(
        graph: StateGraph,
        initialState: GraphState,
        startNodeIds: List<String>,
        resume: Boolean,
        config: ExecutionConfig,
    ): ExecutionResult =
        supervisorScope {
            // Use supervisorScope to prevent one node's crash from killing siblings
            var currentState = initialState
            var currentNodes = startNodeIds
            var isResuming = resume
            val eventPublisher = MobileGraph.instance.getComponent(EventPublisher::class)
            val lifecycleRegistry = MobileGraph.instance.getComponent(LifecycleRegistry::class)

            // Flag to signal loop termination from lifecycle events
            var shouldExitLoop = false
            var exitResult: ExecutionResult? = null
            var activeNodes: List<Deferred<Pair<String, ExecutionResult>>> = emptyList()

            // 0. Lifecycle-Aware Durability: Trigger checkpoint on backgrounding
            val lifecycleJob =
                lifecycleRegistry?.let { registry ->
                    launch {
                        registry.currentState.collect { state ->
                            if (state == LifecycleState.Background || state == LifecycleState.Suspended) {
                                // 1. Automatically snapshot the latest "stable" state
                                var autoCheckpointId: String? = null
                                currentNodes.forEach { nodeId ->
                                    autoCheckpointId = saveCheckpoint(nodeId, currentState, isAutoSave = true)
                                }

                                // 2. Handle BackgroundPolicy
                                if (config.backgroundPolicy != BackgroundPolicy.CONTINUE) {
                                    val lastNodeId = currentNodes.firstOrNull() ?: "unknown"

                                    exitResult =
                                        if (config.backgroundPolicy == BackgroundPolicy.PAUSE) {
                                            ExecutionResult.AwaitingReview(
                                                nodeId = lastNodeId,
                                                state = currentState,
                                                checkpointId = autoCheckpointId,
                                            )
                                        } else {
                                            ExecutionResult.Error(currentState)
                                        }

                                    shouldExitLoop = true
                                    // Immediately cancel ongoing LLM/Node executions to save resources
                                    activeNodes.forEach { it.cancel() }
                                }
                            }
                        }
                    }
                }

            try {
                // The engine continues as long as there are active nodes to process
                loop@ while (currentNodes.isNotEmpty() && isActive && !shouldExitLoop) {
                    val nodeResults =
                        try {
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
                                activeNodes = deferred
                                // Wait for all nodes in the current parallel batch to finish
                                deferred.awaitAll()
                            }
                        } catch (e: CancellationException) {
                            if (shouldExitLoop) break@loop
                            throw e
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

                exitResult ?: ExecutionResult.Success(currentState)
            } finally {
                lifecycleJob?.cancel()
            }
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
        isAutoSave: Boolean = false,
    ): String? {
        if (checkpointStore == null) return null

        val now = 0L // Mocked for now

        val suffix = if (isAutoSave) "_auto" else ""
        val checkpointId =
            CheckpointId(
                "${state.executionContext.traceId.value}_${nodeId}_$now$suffix",
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
        return checkpointId.value
    }
}
