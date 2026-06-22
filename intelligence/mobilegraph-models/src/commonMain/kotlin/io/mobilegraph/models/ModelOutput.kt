package io.mobilegraph.models

/**
 * Sealed class representing the output of a model invocation.
 */
sealed class ModelOutput {
    /**
     * Successful chat output.
     */
    data class ChatOutput(
        val message: AssistantMessage,
        val usage: Usage? = null,
    ) : ModelOutput()

    /**
     * Error output.
     */
    data class ErrorOutput(
        val error: Throwable,
    ) : ModelOutput()
}
