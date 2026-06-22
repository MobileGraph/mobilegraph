package io.mobilegraph.core.execution

import io.mobilegraph.core.context.ExecutionContext

/**
 * Interface for components that can be executed.
 */
interface Executable<in I, out O> {
    suspend fun execute(
        input: I,
        context: ExecutionContext,
    ): O
}
