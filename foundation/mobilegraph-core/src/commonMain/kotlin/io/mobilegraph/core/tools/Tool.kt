package io.mobilegraph.core.tools

import io.mobilegraph.core.context.ExecutionContext
import kotlinx.serialization.KSerializer

/**
 * Represents an external action or capability that can be executed.
 *
 * @param I The type of the input accepted by the tool.
 * @param O The type of the output produced by the tool.
 */
interface Tool<I, out O> {
    /**
     * Metadata describing the tool.
     */
    val metadata: ToolMetadata

    /**
     * Serializer for the input type [I].
     * Required for dynamic execution from JSON.
     */
    val inputSerializer: KSerializer<I>? get() = null

    /**
     * Executes the tool with the given [input] and [context].
     *
     * @param input The tool input.
     * @param context The execution context.
     * @return The tool output.
     */
    suspend fun invoke(
        input: I,
        context: ExecutionContext,
    ): O
}
