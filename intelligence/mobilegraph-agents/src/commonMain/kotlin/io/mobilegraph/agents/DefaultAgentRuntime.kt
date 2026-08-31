package io.mobilegraph.agents

import io.mobilegraph.checkpoint.CheckpointId
import io.mobilegraph.checkpoint.CheckpointStore
import io.mobilegraph.core.events.EventPublisher
import io.mobilegraph.core.events.MobileGraphEvent
import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.graph.ExecutionConfig
import io.mobilegraph.graph.ExecutionEngine
import io.mobilegraph.graph.ExecutionResult
import io.mobilegraph.graph.StateGraph
import io.mobilegraph.state.GraphState
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope

/**
 * Concrete implementation of [AgentRuntime].
 */
class DefaultAgentRuntime(
    private val executionEngine: ExecutionEngine,
    private val checkpointStore: CheckpointStore? = null,
) : AgentRuntime {
    private var currentJob: Job? = null

    override suspend fun run(
        graph: StateGraph,
        initialState: GraphState,
        config: ExecutionConfig,
    ): ExecutionResult =
        coroutineScope {
            val eventPublisher = MobileGraph.instance.getComponent(EventPublisher::class)
            val context = initialState.executionContext

            // Publish event
            eventPublisher?.publish(
                MobileGraphEvent.AgentStarted(
                    traceId = context.traceId,
                    requestId = context.requestId,
                ),
            )

            try {
                val result = executionEngine.execute(graph, initialState, config)

                if (result is ExecutionResult.Success) {
                    eventPublisher?.publish(
                        MobileGraphEvent.AgentCompleted(
                            traceId = context.traceId,
                            requestId = context.requestId,
                        ),
                    )
                }

                result
            } catch (e: Exception) {
                eventPublisher?.publish(
                    MobileGraphEvent.AgentFailed(
                        traceId = context.traceId,
                        requestId = context.requestId,
                        error = e.message ?: "Unknown error",
                    ),
                )
                throw e
            }
        }

    override suspend fun resume(
        graph: StateGraph,
        checkpointId: String,
        nodeId: String,
        input: Map<String, Any?>,
        config: ExecutionConfig,
        reExecute: Boolean,
    ): ExecutionResult =
        coroutineScope {
            val store = checkpointStore ?: throw IllegalStateException("CheckpointStore is required for resumption")
            val checkpoint =
                store.load(CheckpointId(checkpointId))
                    ?: throw IllegalArgumentException("Checkpoint with ID '$checkpointId' not found")

            val eventPublisher = MobileGraph.instance.getComponent(EventPublisher::class)

            // Merge input into state
            val resumedState =
                checkpoint.state.copy(
                    variables = checkpoint.state.variables + input,
                )

            try {
                val result = executionEngine.resume(graph, resumedState, nodeId, config, reExecute)

                if (result is ExecutionResult.Success) {
                    eventPublisher?.publish(
                        MobileGraphEvent.AgentCompleted(
                            traceId = resumedState.executionContext.traceId,
                            requestId = resumedState.executionContext.requestId,
                        ),
                    )
                }

                result
            } catch (e: Exception) {
                eventPublisher?.publish(
                    MobileGraphEvent.AgentFailed(
                        traceId = resumedState.executionContext.traceId,
                        requestId = resumedState.executionContext.requestId,
                        error = e.message ?: "Unknown error",
                    ),
                )
                throw e
            }
        }

    override suspend fun pause() {
        currentJob?.cancel()
    }

    override suspend fun cancel() {
        currentJob?.cancel()
    }
}
