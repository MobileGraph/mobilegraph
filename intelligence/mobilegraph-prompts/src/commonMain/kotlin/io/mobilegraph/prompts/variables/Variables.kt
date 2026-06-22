package io.mobilegraph.prompts.variables

import kotlin.jvm.JvmInline

/**
 * Represents a value that can be passed into a prompt template.
 */
sealed interface PromptValue {
    fun asString(): String
}

@JvmInline
value class StringValue(
    val value: String,
) : PromptValue {
    override fun asString(): String = value
}

@JvmInline
value class IntValue(
    val value: Int,
) : PromptValue {
    override fun asString(): String = value.toString()
}

@JvmInline
value class BooleanValue(
    val value: Boolean,
) : PromptValue {
    override fun asString(): String = value.toString()
}

/**
 * A collection of variables for prompt rendering.
 */
@JvmInline
value class Variables(
    val values: Map<String, PromptValue> = emptyMap(),
) {
    operator fun get(name: String): PromptValue? = values[name]

    fun with(
        name: String,
        value: String,
    ): Variables = Variables(values + (name to StringValue(value)))

    fun with(
        name: String,
        value: Int,
    ): Variables = Variables(values + (name to IntValue(value)))

    fun with(
        name: String,
        value: Boolean,
    ): Variables = Variables(values + (name to BooleanValue(value)))

    fun merge(other: Variables): Variables = Variables(values + other.values)

    companion object {
        val Empty = Variables()

        fun of(vararg pairs: Pair<String, String>): Variables = Variables(pairs.toMap().mapValues { StringValue(it.value) })
    }
}
