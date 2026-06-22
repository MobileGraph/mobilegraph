package io.mobilegraph.core.cancellation

/**
 * Represents a token that can be checked for cancellation.
 */
interface CancellationToken {
    val isCancelled: Boolean

    fun throwIfCancelled() {
        if (isCancelled) {
            throw io.mobilegraph.core.exceptions
                .CancellationException("Operation was cancelled")
        }
    }

    companion object {
        val None =
            object : CancellationToken {
                override val isCancelled: Boolean = false
            }
    }
}
