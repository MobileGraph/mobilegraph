package io.mobilegraph.graph

import io.mobilegraph.state.GraphState

/**
 * Default implementation of [StateGraph].
 */
data class DefaultStateGraph(
    override val nodes: Map<String, GraphNode>,
    override val edges: List<GraphEdge>,
    override val startNodeId: String,
) : StateGraph

/**
 * Default implementation of [GraphEdge].
 */
data class DefaultGraphEdge(
    override val from: String,
    override val to: String,
    private val condition: (suspend (GraphState) -> Boolean)? = null,
) : GraphEdge {
    override suspend fun shouldTransition(state: GraphState): Boolean = condition?.invoke(state) ?: true
}
