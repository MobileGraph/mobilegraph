package io.mobilegraph.core.session

import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.core.environment.MobileGraphEnvironment
import io.mobilegraph.core.events.MobileGraphEvent
import io.mobilegraph.core.registry.ComponentProvider
import kotlinx.coroutines.flow.Flow

/**
 * Represents an active unit of interaction with MobileGraph.
 */
interface MobileGraphSession {
    /**
     * The environment this session is running in.
     */
    val environment: MobileGraphEnvironment

    /**
     * The unique identifier for this session.
     */
    val sessionId: String

    /**
     * The name of the specific model bound to this session, if any.
     */
    val modelName: String?

    /**
     * Stream of events published during this session.
     */
    val events: Flow<MobileGraphEvent>

    /**
     * Closes the session and releases associated resources.
     */
    fun close()

    /**
     * Internal access to the runtime for executing actions.
     */
    interface Internal :
        ComponentProvider,
        ExecutionContext

    val internal: Internal
}
