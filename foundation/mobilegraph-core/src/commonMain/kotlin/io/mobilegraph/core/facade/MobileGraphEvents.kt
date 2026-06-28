package io.mobilegraph.core.facade

import io.mobilegraph.core.events.MobileGraphEvent
import kotlinx.coroutines.flow.Flow

/**
 * Provides access to the global stream of events from the MobileGraph runtime.
 */
val MobileGraph.events: Flow<MobileGraphEvent>
    get() = runtime.events

/**
 * Convenience static access to the global event stream.
 */
val MobileGraph.Companion.events: Flow<MobileGraphEvent>
    get() = MobileGraph.instance.events
