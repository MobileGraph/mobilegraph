package io.mobilegraph.prompts.exceptions

import io.mobilegraph.core.exceptions.MobileGraphException

/**
 * Base exception for prompt-related errors.
 */
open class PromptException(
    message: String,
    cause: Throwable? = null,
) : MobileGraphException(message, cause)

/**
 * Thrown when a required variable is missing during prompt rendering.
 */
class MissingVariableException(
    val variableName: String,
) : PromptException("Missing required variable: $variableName")

/**
 * Thrown when a template is malformed or invalid.
 */
class InvalidTemplateException(
    message: String,
) : PromptException(message)

/**
 * Thrown when prompt rendering fails.
 */
class RenderingException(
    message: String,
    cause: Throwable? = null,
) : PromptException(message, cause)
