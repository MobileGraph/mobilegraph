package io.mobilegraph.state

import io.mobilegraph.core.context.ExecutionContext

/**
 * Represents the immutable state of a graph execution.
 *
 * Implementations of this interface should be provided by the app layer to
 * define specific data structures for different agent workflows.
 */
interface GraphState {
    /**
     * Contextual metadata about the execution.
     */
    val executionContext: ExecutionContext

    /**
     * General purpose variables stored in the state.
     */
    val variables: Map<String, Any?>

    /**
     * The original user query that initiated the workflow.
     */
    val userQuery: String

    /**
     * Creates a new instance of the state with updated variables or user query.
     */
    fun copy(
        variables: Map<String, Any?> = this.variables,
        userQuery: String = this.userQuery,
    ): GraphState
}
