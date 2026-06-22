package io.mobilegraph.core.context

import io.mobilegraph.core.cancellation.CancellationToken
import io.mobilegraph.core.ids.RequestId
import io.mobilegraph.core.ids.SessionId
import io.mobilegraph.core.ids.TraceId
import io.mobilegraph.core.metadata.Metadata
import io.mobilegraph.core.registry.ComponentProvider
import kotlin.reflect.KClass

data class SimpleExecutionContext(
    override val traceId: TraceId,
    override val sessionId: SessionId? = null,
    override val requestId: RequestId,
    override val metadata: Metadata = Metadata(),
    override val locale: String? = null,
    override val deadline: Long? = null,
    override val cancellationToken: CancellationToken = CancellationToken.None,
    private val componentProvider: ComponentProvider? = null,
) : ExecutionContext {
    override fun <T : Any> getComponent(clazz: KClass<T>): T? = componentProvider?.getComponent(clazz)

    override fun withMetadata(metadata: Metadata): ExecutionContext = copy(metadata = metadata)

    override fun withRequestId(requestId: RequestId): ExecutionContext = copy(requestId = requestId)
}
