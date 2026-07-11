package io.mobilegraph.models

import io.mobilegraph.core.exceptions.MobileGraphException

/**
 * Base exception for all Model-related errors.
 */
open class ModelException(
    message: String,
    cause: Throwable? = null,
) : MobileGraphException(message, cause)

/**
 * Thrown when there is an authentication error (e.g., invalid API key).
 */
class AuthenticationException(
    message: String,
    cause: Throwable? = null,
) : ModelException(message, cause)

/**
 * Thrown when the rate limit for the model is exceeded.
 */
class RateLimitException(
    message: String,
    cause: Throwable? = null,
) : ModelException(message, cause)

/**
 * Thrown when the requested model is not found or not accessible.
 */
class InvalidModelException(
    message: String,
    cause: Throwable? = null,
) : ModelException(message, cause)

/**
 * Thrown when the context length is exceeded.
 */
class ContextLengthException(
    message: String,
    cause: Throwable? = null,
) : ModelException(message, cause)

/**
 * Thrown when the model output is blocked due to safety or content filters.
 */
class SafetyException(
    message: String,
    cause: Throwable? = null,
) : ModelException(message, cause)
