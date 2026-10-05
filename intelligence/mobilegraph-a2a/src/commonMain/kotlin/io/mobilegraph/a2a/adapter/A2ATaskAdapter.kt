package io.mobilegraph.a2a.adapter

import io.mobilegraph.a2a.errors.A2AValidationException
import io.mobilegraph.a2a.models.A2ATask
import io.mobilegraph.a2a.models.A2ATaskState
import io.mobilegraph.a2a.models.TextPart
import io.mobilegraph.state.GraphState

/**
 * Adapts between A2A protocol tasks and MobileGraph graph state.
 *
 * This adapter acts as the **trust and protocol boundary** between
 * external A2A responses (probabilistic/untrusted input) and the
 * deterministic MobileGraph graph execution.
 *
 * All remote responses are validated before being mapped into
 * graph state variables.
 *
 * ## State Mapping
 * ```
 * A2A Task State     MobileGraph Mapping
 * ─────────────────  ─────────────────────────
 * submitted          Task created, queued
 * working            Execution in progress
 * input-required     AwaitingReview (checkpoint)
 * completed          ExecutionResult.Success
 * failed             ExecutionResult.Error
 * canceled           Cancellation
 * ```
 */
object A2ATaskAdapter {
    /** State key where the A2A task result is stored. */
    const val STATE_KEY_A2A_RESULT = "a2a_result"

    /** State key where A2A artifacts are stored. */
    const val STATE_KEY_A2A_ARTIFACTS = "a2a_artifacts"

    /** State key where the A2A task status is stored. */
    const val STATE_KEY_A2A_STATUS = "a2a_status"

    /** State key where the A2A task ID is stored. */
    const val STATE_KEY_A2A_TASK_ID = "a2a_task_id"

    /** State key where the A2A context ID is stored. */
    const val STATE_KEY_A2A_CONTEXT_ID = "a2a_context_id"

    /** State key where the remote agent name is stored. */
    const val STATE_KEY_A2A_AGENT = "a2a_remote_agent"

    /** State key where A2A error details are stored (on failure). */
    const val STATE_KEY_A2A_ERROR = "a2a_error"

    /** State key where the input-required message is stored. */
    const val STATE_KEY_A2A_INPUT_REQUIRED_MESSAGE = "a2a_input_required_message"

    /**
     * Validates an A2A task response from a remote agent.
     *
     * Ensures the task has a valid ID, artifacts, or status message.
     * Remote agents are treated as untrusted external systems.
     *
     * @param task The A2A task to validate.
     * @throws A2AValidationException if the task is invalid.
     */
    fun validateTask(task: A2ATask) {
        if (task.id.isBlank() && task.artifacts.isEmpty() && task.status.message == null) {
            throw A2AValidationException("A2A task response is empty (missing task ID, artifacts, and status message)")
        }
    }

    /**
     * Maps a completed A2A task into a MobileGraph [GraphState].
     *
     * Extracts artifacts and text content from the task and
     * stores them as graph state variables for downstream nodes.
     *
     * @param task The completed A2A task.
     * @param currentState The current graph state to merge into.
     * @param agentName The remote agent's name (for observability).
     * @return Updated [GraphState] with A2A results.
     */
    fun mapTaskToState(
        task: A2ATask,
        currentState: GraphState,
        agentName: String? = null,
    ): GraphState {
        validateTask(task)

        val variables = mutableMapOf<String, Any?>()

        // Store task metadata
        variables[STATE_KEY_A2A_TASK_ID] = task.id
        if (!task.contextId.isNullOrBlank()) {
            variables[STATE_KEY_A2A_CONTEXT_ID] = task.contextId
        }
        variables[STATE_KEY_A2A_STATUS] = task.status.state.name

        if (agentName != null) {
            variables[STATE_KEY_A2A_AGENT] = agentName
        }

        // Extract text content from artifacts, status message, and history
        val artifactTexts =
            task.artifacts.flatMap { artifact ->
                artifact.parts.filterIsInstance<TextPart>().map { it.text }
            }

        val statusMessageTexts =
            task.status.message
                ?.parts
                ?.filterIsInstance<TextPart>()
                ?.map { it.text } ?: emptyList()

        val historyAgentTexts =
            task.history
                .filter { it.role == io.mobilegraph.a2a.models.A2ARole.AGENT }
                .flatMap { msg -> msg.parts.filterIsInstance<TextPart>().map { it.text } }

        val combinedResultTexts = (artifactTexts + statusMessageTexts + historyAgentTexts).distinct()

        if (combinedResultTexts.isNotEmpty()) {
            variables[STATE_KEY_A2A_RESULT] = combinedResultTexts.joinToString("\n\n")
        }

        // Store structured artifact data
        if (task.artifacts.isNotEmpty()) {
            variables[STATE_KEY_A2A_ARTIFACTS] =
                task.artifacts.map { artifact ->
                    mapOf(
                        "id" to (artifact.artifactId ?: ""),
                        "name" to (artifact.name ?: ""),
                        "description" to (artifact.description ?: ""),
                        "parts" to
                            artifact.parts.map { part ->
                                when (part) {
                                    is TextPart -> {
                                        mapOf("type" to "text", "text" to part.text)
                                    }

                                    is io.mobilegraph.a2a.models.FilePart -> {
                                        mapOf(
                                            "type" to "file",
                                            "name" to (part.file.name ?: ""),
                                            "mimeType" to (part.file.mimeType ?: ""),
                                            "uri" to (part.file.uri ?: ""),
                                        )
                                    }

                                    is io.mobilegraph.a2a.models.DataPart -> {
                                        mapOf(
                                            "type" to "data",
                                            "data" to part.data.toString(),
                                        )
                                    }
                                }
                            },
                    )
                }
        }

        // Handle error state
        if (task.status.state == A2ATaskState.FAILED) {
            val errorMessage =
                task.status.message
                    ?.parts
                    ?.filterIsInstance<TextPart>()
                    ?.joinToString(" ") { it.text }
                    ?: "Remote agent task failed"
            variables[STATE_KEY_A2A_ERROR] = errorMessage
        }

        // Handle input-required state
        if (task.status.state == A2ATaskState.INPUT_REQUIRED) {
            val inputMessage =
                task.status.message
                    ?.parts
                    ?.filterIsInstance<TextPart>()
                    ?.joinToString(" ") { it.text }
                    ?: "Remote agent requires additional input"
            variables[STATE_KEY_A2A_INPUT_REQUIRED_MESSAGE] = inputMessage
        }

        return currentState.copy(
            variables = currentState.variables + variables,
        )
    }

    /**
     * Checks if a task state indicates that the graph should pause
     * for human/external input.
     */
    fun isInputRequired(task: A2ATask): Boolean = task.status.state == A2ATaskState.INPUT_REQUIRED

    /**
     * Checks if a task has reached a terminal state.
     */
    fun isTerminal(task: A2ATask): Boolean = task.status.state.isTerminal()
}
