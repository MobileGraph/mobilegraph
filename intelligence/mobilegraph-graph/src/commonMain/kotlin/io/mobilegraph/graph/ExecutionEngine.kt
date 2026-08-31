package io.mobilegraph.graph

import io.mobilegraph.state.GraphState

/**
 * Interface for the engine that traverses and executes a [StateGraph].
 */
interface ExecutionEngine {
    /**
     * Executes the graph starting from the [initialState].
     *
     * @param graph The graph to execute.
     * @param initialState The starting state.
     * @return The result of the graph execution, which can be success or a pause for review.
     */
    suspend fun execute(
        graph: StateGraph,
        initialState: GraphState,
        config: ExecutionConfig = ExecutionConfig(),
    ): ExecutionResult

    /**
     * Resumes a paused execution from a specific node.
     *
     * @param graph The graph to execute.
     * @param state The state to resume with.
     * @param nodeId The ID of the node to resume from.
     * @param config The execution configuration.
     * @return The result of the graph execution.
     */
    suspend fun resume(
        graph: StateGraph,
        state: GraphState,
        nodeId: String,
        config: ExecutionConfig = ExecutionConfig(),
        reExecute: Boolean = false,
    ): ExecutionResult
}
