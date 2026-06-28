package io.mobilegraph.core.events

/**
 * Interface for publishing MobileGraph events.
 */
interface EventPublisher {
    suspend fun publish(event: MobileGraphEvent)
}
