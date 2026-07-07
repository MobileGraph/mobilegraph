package io.mobilegraph.graph

import io.mobilegraph.state.GraphState

/**
 * A specialized node that indicates human intervention is required.
 * When the engine reaches this node, it will pause execution and save a checkpoint.
 */
class HumanReviewNode(
    override val id: String,
    private val block: suspend (GraphState) -> ExecutionResult = { ExecutionResult.AwaitingReview(id, it) },
) : GraphNode {
    override suspend fun execute(state: GraphState): ExecutionResult = block(state)
}
