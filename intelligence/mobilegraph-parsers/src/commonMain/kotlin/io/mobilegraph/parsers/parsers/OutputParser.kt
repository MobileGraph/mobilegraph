package io.mobilegraph.parsers.parsers

/**
 * Interface for components that transform textual model output into structured data.
 * @param T The type of the structured data.
 */
interface OutputParser<out T> {
    /**
     * Parses the given text into a structured object of type [T].
     * Throws ParserException if parsing fails.
     */
    fun parse(text: String): T
}
