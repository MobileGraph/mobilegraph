package io.mobilegraph.core.lifecycle

/**
 * Represents events that trigger changes in the application lifecycle.
 */
sealed interface LifecycleEvent {
    /**
     * Application has entered the foreground.
     */
    data object EnterForeground : LifecycleEvent

    /**
     * Application has entered the background.
     */
    data object EnterBackground : LifecycleEvent

    /**
     * Network connectivity has been lost.
     */
    data object NetworkLost : LifecycleEvent

    /**
     * Network connectivity has been restored.
     */
    data object NetworkRestored : LifecycleEvent

    /**
     * Application execution is being suspended (e.g., system-initiated pause).
     */
    data object Suspended : LifecycleEvent

    /**
     * Application has been restored from a previous state.
     */
    data object Restored : LifecycleEvent
}
