package io.mobilegraph.core.tools

import io.mobilegraph.core.exceptions.MobileGraphException

/**
 * Base exception for tool-related errors.
 */
open class ToolException(
    message: String,
    cause: Throwable? = null,
) : MobileGraphException(message, cause)

/**
 * Thrown when an error occurs during tool execution.
 */
class ToolExecutionException(
    message: String,
    cause: Throwable? = null,
) : ToolException(message, cause)

/**
 * Thrown when the tool input is invalid.
 */
class ToolInputException(
    message: String,
    cause: Throwable? = null,
) : ToolException(message, cause)
