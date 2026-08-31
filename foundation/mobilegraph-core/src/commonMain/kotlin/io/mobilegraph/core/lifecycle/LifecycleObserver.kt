package io.mobilegraph.core.lifecycle

/**
 * Interface for platform-specific lifecycle observers.
 */
interface LifecycleObserver {
    /**
     * Starts observing platform lifecycle events.
     */
    fun start()

    /**
     * Stops observing platform lifecycle events and releases resources.
     */
    fun stop()
}
