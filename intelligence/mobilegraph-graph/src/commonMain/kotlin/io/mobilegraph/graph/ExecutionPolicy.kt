package io.mobilegraph.graph

/**
 * Defines how the execution engine behaves when the application lifecycle state changes.
 */
enum class BackgroundPolicy {
    /**
     * Continue execution regardless of background state.
     */
    CONTINUE,

    /**
     * Automatically pause execution when entering background.
     * The engine will checkpoint and return [ExecutionResult.AwaitingReview] or a similar signal.
     */
    PAUSE,

    /**
     * Cancel execution immediately when entering background.
     */
    CANCEL,
}

/**
 * Configuration for graph execution.
 */
data class ExecutionConfig(
    val backgroundPolicy: BackgroundPolicy = BackgroundPolicy.CONTINUE,
)
