package io.mobilegraph.parsers.exceptions

import io.mobilegraph.core.exceptions.MobileGraphException

/**
 * Base exception for parsing errors.
 */
open class ParserException(
    message: String,
    cause: Throwable? = null,
) : MobileGraphException(message, cause)

/**
 * Thrown when the content to parse is malformed (e.g. invalid JSON).
 */
class MalformedContentException(
    message: String,
    cause: Throwable? = null,
) : ParserException(message, cause)

/**
 * Thrown when the content does not match the expected schema.
 */
class SchemaMismatchException(
    message: String,
    cause: Throwable? = null,
) : ParserException(message, cause)
