package io.mobilegraph.agents

import io.mobilegraph.core.tools.ToolRegistry
import io.mobilegraph.graph.StateGraph
import io.mobilegraph.models.ChatModel
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.models.SystemMessage
import io.mobilegraph.models.UserMessage
import io.mobilegraph.state.GraphState

/**
 * Represents a high-level Agent entity.
 *
 * An Agent combines a [StateGraph] (the logic), a [ChatModel] (the brain),
 * and a [ToolRegistry] (the capabilities).
 *
 * App developers should implement this interface to define custom agents and
 * control how state data is formatted into LLM instructions.
 */
interface Agent {
    val name: String
    val description: String
    val model: ChatModel
    val tools: ToolRegistry?
    val rolePrompt: SystemMessage
    val graph: StateGraph?

    /**
     * Whether this agent should have access to the global tool registry
     * configured during SDK initialization. Defaults to false.
     */
    val useGlobalTools: Boolean get() = false

    /**
     * Formats the agent's instructions based on the current state.
     * This is where the app layer can inject state variables into the prompt.
     */
    fun formatAgentInstruction(state: GraphState): UserMessage

    /**
     * Handles the output from the LLM and returns the updated state.
     */
    suspend fun handleLlmOutput(
        output: ModelOutput,
        state: GraphState,
    ): GraphState
}
