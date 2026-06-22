package io.mobilegraph.parsers

/**
 * Represents the result of a parsing operation.
 */
sealed class ParseResult<out T> {
    /**
     * Successful parse result.
     */
    data class Success<T>(
        val value: T,
    ) : ParseResult<T>()

    /**
     * Partial parse result - some fields succeeded, but others failed.
     */
    data class Partial<T>(
        val partialValue: T?,
        val errors: List<ParseError>,
    ) : ParseResult<T>()

    /**
     * Complete failure.
     */
    data class Failure(
        val error: ParseError,
    ) : ParseResult<Nothing>()
}

/**
 * Details of a parsing error.
 */
data class ParseError(
    val message: String,
    val path: String? = null,
    val cause: Throwable? = null,
)
