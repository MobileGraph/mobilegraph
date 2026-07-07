package io.mobilegraph.graph

import io.mobilegraph.state.GraphState

class EndNode(
    override val id: String = "end",
) : GraphNode {
    override suspend fun execute(state: GraphState) = ExecutionResult.Success(state)
}
