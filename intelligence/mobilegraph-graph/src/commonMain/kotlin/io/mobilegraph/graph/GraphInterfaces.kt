package io.mobilegraph.graph

import io.mobilegraph.state.GraphState

/**
 * Represents a node in the [StateGraph].
 *
 * A node is a unit of work that processes a [GraphState] and returns a new [GraphState].
 */
interface GraphNode {
    /**
     * Unique identifier for the node within the graph.
     */
    val id: String

    /**
     * Executes the node's logic.
     *
     * @param state The current state entering the node.
     * @return The result of the node's execution, which can be success or a pause for review.
     */
    suspend fun execute(state: GraphState): ExecutionResult
}

/**
 * Represents a transition between two nodes in a [StateGraph].
 */
interface GraphEdge {
    /**
     * The ID of the source node.
     */
    val from: String

    /**
     * The ID of the destination node.
     */
    val to: String

    /**
     * Determines if this transition should be followed based on the current state.
     */
    suspend fun shouldTransition(state: GraphState): Boolean
}

/**
 * A directed graph that represents an agentic workflow or state machine.
 */
interface StateGraph {
    /**
     * All nodes defined in the graph.
     */
    val nodes: Map<String, GraphNode>

    /**
     * All edges (transitions) between nodes.
     */
    val edges: List<GraphEdge>

    /**
     * The ID of the node where execution starts.
     */
    val startNodeId: String
}

/**
 * Represents the result of a graph execution.
 */
sealed interface ExecutionResult {
    /**
     * The state at the point of completion or pause.
     */
    val state: GraphState

    /**
     * Execution completed successfully.
     */
    data class Success(
        override val state: GraphState,
    ) : ExecutionResult

    /**
     * Execution is paused waiting for human review.
     * @property nodeId The ID of the node that requested review.
     * @property state The current state of the graph.
     */
    data class AwaitingReview(
        val nodeId: String,
        override val state: GraphState,
    ) : ExecutionResult

    /**
     * Execution is partially complete for a parallel branch and is waiting for other branches.
     * This is internal to the engine.
     */
    data class AwaitingJoin(
        val nodeId: String,
        override val state: GraphState,
    ) : ExecutionResult

    data class Error(
        override val state: GraphState,
    ) : ExecutionResult
}
