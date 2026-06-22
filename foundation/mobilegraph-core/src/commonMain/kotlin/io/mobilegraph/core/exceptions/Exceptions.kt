package io.mobilegraph.core.exceptions

/**
 * Base exception for all MobileGraph errors.
 */
open class MobileGraphException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Thrown when there is a configuration error.
 */
class ConfigurationException(
    message: String,
    cause: Throwable? = null,
) : MobileGraphException(message, cause)

/**
 * Thrown when an error occurs during execution.
 */
class ExecutionException(
    message: String,
    cause: Throwable? = null,
) : MobileGraphException(message, cause)

/**
 * Thrown when an operation times out.
 */
class TimeoutException(
    message: String,
    cause: Throwable? = null,
) : MobileGraphException(message, cause)

/**
 * Thrown when an operation is cancelled.
 */
class CancellationException(
    message: String,
    cause: Throwable? = null,
) : MobileGraphException(message, cause)
