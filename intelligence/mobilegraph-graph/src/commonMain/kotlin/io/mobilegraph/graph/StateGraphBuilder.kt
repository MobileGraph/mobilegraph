package io.mobilegraph.graph

import io.mobilegraph.state.GraphState
import kotlin.reflect.KClass

/**
 * DSL Builder for creating [StateGraph] instances.
 */
class StateGraphBuilder {
    private val nodes = mutableMapOf<String, GraphNode>()
    private val edges = mutableListOf<GraphEdge>()
    private var startNodeId: String? = null

    /**
     * Adds a node to the graph.
     */
    fun node(node: GraphNode) {
        nodes[node.id] = node
        if (startNodeId == null) startNodeId = node.id
    }

    /**
     * Defines the starting node.
     */
    fun start(nodeId: String) {
        startNodeId = nodeId
    }

    /**
     * Adds an edge between two nodes.
     */
    fun edge(
        from: String,
        to: String,
    ) {
        edges.add(DefaultGraphEdge(from, to))
    }

    /**
     * Adds a conditional edge.
     */
    fun conditionalEdge(
        from: String,
        to: String,
        condition: suspend (GraphState) -> Boolean,
    ) {
        edges.add(DefaultGraphEdge(from, to, condition))
    }

    /**
     * Adds a join node to the graph for synchronization.
     */
    fun join(
        id: String,
        incomingCount: Int,
        mergeStrategy: (List<GraphState>) -> GraphState = { it.last() },
    ) {
        node(JoinNode(id, incomingCount, mergeStrategy))
    }

    fun build(): StateGraph {
        val startId = startNodeId ?: throw IllegalStateException("Start node must be defined")
        return DefaultStateGraph(nodes.toMap(), edges.toList(), startId)
    }
}

/**
 * DSL entry point for building a [StateGraph].
 */
fun stateGraph(block: StateGraphBuilder.() -> Unit): StateGraph = StateGraphBuilder().apply(block).build()
