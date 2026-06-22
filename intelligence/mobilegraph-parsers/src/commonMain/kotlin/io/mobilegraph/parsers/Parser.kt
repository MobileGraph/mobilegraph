package io.mobilegraph.parsers

import io.mobilegraph.models.ModelOutput

/**
 * Modern interface for parsing model outputs.
 */
interface Parser<out T> {
    /**
     * Parses the model output into a structured result.
     */
    fun parse(output: ModelOutput): ParseResult<T>

    /**
     * Optional format instructions to be included in the prompt.
     */
    fun formatInstructions(): String = ""
}

/**
 * Factory for creating custom parsers via DSL.
 */
fun <T> parser(block: (ModelOutput) -> ParseResult<T>): Parser<T> =
    object : Parser<T> {
        override fun parse(output: ModelOutput): ParseResult<T> = block(output)
    }
