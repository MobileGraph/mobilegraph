package io.mobilegraph.core.ids

import kotlin.jvm.JvmInline

@JvmInline
value class TraceId(
    val value: String,
)

@JvmInline
value class SessionId(
    val value: String,
)

@JvmInline
value class RequestId(
    val value: String,
)

@JvmInline
value class ExecutionId(
    val value: String,
)

@JvmInline
value class NodeId(
    val value: String,
)

@JvmInline
value class GraphId(
    val value: String,
)
