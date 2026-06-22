/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.core.runtime

import io.mobilegraph.core.cancellation.CancellationToken
import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.core.environment.MobileGraphEnvironment
import io.mobilegraph.core.events.MobileGraphEvent
import io.mobilegraph.core.ids.RequestId
import io.mobilegraph.core.ids.SessionId
import io.mobilegraph.core.ids.TraceId
import io.mobilegraph.core.metadata.Metadata
import io.mobilegraph.core.session.MobileGraphSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * Internal runtime kernel for MobileGraph.
 */
internal class MobileGraphRuntime(
    val environment: MobileGraphEnvironment,
) {
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _events =
        MutableSharedFlow<MobileGraphEvent>(
            replay = 1,
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val events = _events.asSharedFlow()

    fun createSession(modelName: String? = null): MobileGraphSession = DefaultMobileGraphSession(this, modelName)

    suspend fun publishEvent(event: MobileGraphEvent) {
        println("SDK Runtime: Publishing event: $event")
        _events.emit(event)
    }
}

internal class DefaultMobileGraphSession(
    override val runtime: MobileGraphRuntime,
    override val modelName: String? = null,
) : MobileGraphSession,
    MobileGraphRuntimeAccessor {
    override val environment: MobileGraphEnvironment = runtime.environment
    override val sessionId: String = "session-${kotlin.random.Random.nextInt()}"
    override val events: kotlinx.coroutines.flow.Flow<MobileGraphEvent> = runtime.events

    override val internal: MobileGraphSession.Internal =
        object : MobileGraphSession.Internal {
            override val traceId: TraceId = TraceId("trace-${this@DefaultMobileGraphSession.sessionId}")
            override val sessionId: SessionId = SessionId(this@DefaultMobileGraphSession.sessionId)
            override val requestId: RequestId get() = RequestId("req-${kotlin.random.Random.nextInt()}")
            override val metadata: Metadata = Metadata()
            override val locale: String? = null
            override val deadline: Long? = null
            override val cancellationToken: CancellationToken = CancellationToken.None

            override fun <T : Any> getComponent(clazz: KClass<T>): T? = runtime.environment.getComponent(clazz)

            @Suppress("ktlint:standard:max-line-length")
            override fun withMetadata(metadata: Metadata): ExecutionContext =
                throw UnsupportedOperationException("Immutable internal context")

            @Suppress("ktlint:standard:max-line-length")
            override fun withRequestId(requestId: RequestId): ExecutionContext =
                throw UnsupportedOperationException("Immutable internal context")
        }

    override fun close() {
        val memory = internal.getComponent(io.mobilegraph.core.memory.ChatMemory::class)
        memory?.let {
            runtime.scope.launch {
                it.clear(internal)
            }
        }
    }
}
