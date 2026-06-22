package io.mobilegraph.core.runtime

import io.mobilegraph.core.events.MobileGraphEvent
import io.mobilegraph.core.session.MobileGraphSession

/**
 * Internal helper to publish events from within a session's execution flow.
 */
suspend fun MobileGraphSession.publishEvent(event: MobileGraphEvent) {
    (this as? MobileGraphRuntimeAccessor)?.runtime?.publishEvent(event)
}

/**
 * Interface to allow access to the runtime from the session.
 */
internal interface MobileGraphRuntimeAccessor {
    val runtime: MobileGraphRuntime
}
