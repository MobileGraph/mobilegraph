package io.mobilegraph.core.metadata

import kotlin.jvm.JvmInline

/**
 * Immutable metadata container.
 */
@JvmInline
value class Metadata(
    val values: Map<String, Any> = emptyMap(),
) {
    operator fun get(key: String): Any? = values[key]

    fun plus(
        key: String,
        value: Any,
    ): Metadata = Metadata(values + (key to value))

    fun plus(other: Metadata): Metadata = Metadata(values + other.values)
}
