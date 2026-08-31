package io.mobilegraph.agents

import io.mobilegraph.graph.ExecutionConfig
import io.mobilegraph.graph.ExecutionResult
import io.mobilegraph.graph.StateGraph
import io.mobilegraph.state.GraphState

/**
 * The high-level runtime for managing and executing durable agents.
 *
 * It bridges the gap between the raw [StateGraph] execution and the application lifecycle,
 * handling persistence, pausing/resuming, and observability.
 */
interface AgentRuntime {
    /**
     * Starts or resumes an agent's execution.
     */
    suspend fun run(
        graph: StateGraph,
        initialState: GraphState,
        config: ExecutionConfig = ExecutionConfig(),
    ): ExecutionResult

    /**
     * Resumes an agent's execution from a specific checkpoint and node.
     *
     * @param graph The graph to execute.
     * @param checkpointId The ID of the checkpoint to load.
     * @param nodeId The ID of the node to resume from.
     * @param input Optional additional input or state updates.
     * @param config The execution configuration.
     * @param reExecute Whether to re-execute the node at [nodeId] or skip it.
     */
    suspend fun resume(
        graph: StateGraph,
        checkpointId: String,
        nodeId: String,
        input: Map<String, Any?> = emptyMap(),
        config: ExecutionConfig = ExecutionConfig(),
        reExecute: Boolean = false,
    ): ExecutionResult

    /**
     * Pauses the current execution, ensuring a checkpoint is saved.
     */
    suspend fun pause()

    /**
     * Cancels the execution.
     */
    suspend fun cancel()
}
