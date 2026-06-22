package io.mobilegraph.parsers

import io.mobilegraph.models.ChatModel
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.ModelOutput
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * A parser that extracts structured data from JSON output.
 */
class StructuredOutputParser<T>(
    val serializer: KSerializer<T>,
) : Parser<T> {
    private val json = Json { ignoreUnknownKeys = true }

    override fun parse(output: ModelOutput): ParseResult<T> {
        val text = output.asText()
        return try {
            // Find first '{' and last '}' to handle potential prose around JSON
            val startIndex = text.indexOf('{')
            val endIndex = text.lastIndexOf('}')

            if ((startIndex == -1) || (endIndex == -1)) {
                return ParseResult.Failure(ParseError("No JSON object found in output"))
            }

            val jsonText = text.substring(startIndex, endIndex + 1)
            val value = json.decodeFromString(serializer, jsonText)
            ParseResult.Success(value)
        } catch (e: Exception) {
            ParseResult.Failure(ParseError("Failed to parse JSON: ${e.message}", cause = e))
        }
    }

    override fun formatInstructions(): String {
        val schema = generateSchema(serializer.descriptor)
        return "Return a JSON object matching this schema:\n$schema\nDo not include prose in your response."
    }

    private fun generateSchema(descriptor: SerialDescriptor): String {
        val type =
            when (descriptor.kind) {
                StructureKind.CLASS, StructureKind.OBJECT -> {
                    val fields =
                        (0 until descriptor.elementsCount).joinToString(", ") { i ->
                            "\"${descriptor.getElementName(i)}\": ${generateSchema(descriptor.getElementDescriptor(i))}"
                        }
                    "{$fields}"
                }

                StructureKind.LIST -> {
                    "[${generateSchema(descriptor.getElementDescriptor(0))}]"
                }

                StructureKind.MAP -> {
                    "{\"key\": ${generateSchema(descriptor.getElementDescriptor(1))}}"
                }

                PrimitiveKind.STRING -> {
                    "\"string\""
                }

                PrimitiveKind.INT, PrimitiveKind.LONG, PrimitiveKind.SHORT, PrimitiveKind.BYTE,
                PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE,
                -> {
                    "\"number\""
                }

                PrimitiveKind.BOOLEAN -> {
                    "\"boolean\""
                }

                else -> {
                    "\"${descriptor.serialName}\""
                }
            }
        return if (descriptor.isNullable) "$type | null" else type
    }
}

/**
 * DSL entry point for creating a structured output parser.
 */
inline fun <reified T> structuredOutputParser(): StructuredOutputParser<T> = StructuredOutputParser(serializer<T>())

/**
 * A parser that handles a list of strings.
 */
class ListParser : Parser<List<String>> {
    override fun parse(output: ModelOutput): ParseResult<List<String>> {
        val text = output.asText()
        val items =
            text
                .split("\n")
                .map { it.trim().removePrefix("-").trim() }
                .filter { it.isNotEmpty() }
        return ParseResult.Success(items)
    }
}

fun listParser(): ListParser = ListParser()

/**
 * A parser that can retry with corrections if initial parsing fails.
 */
class RetryWithCorrectionParser<T>(
    val delegate: Parser<T>,
    val model: ChatModel,
    val maxRetries: Int = 2,
) : Parser<T> {
    override fun parse(output: ModelOutput): ParseResult<T> {
        // This is a simplified version; real implementation would require
        // the original prompt to provide feedback to the model.
        return delegate.parse(output)
    }

    // Complex parse that includes the original prompt and model for retries
    suspend fun parse(
        output: ModelOutput,
        originalPrompt: ChatPromptValue,
    ): ParseResult<T> {
        val currentOutput = output
        val result = delegate.parse(currentOutput)

        var retries = 0
        while (result is ParseResult.Failure && retries < maxRetries) {
            // Logic to feed back error to the model would go here
            retries++
        }

        return result
    }
}

fun <T> retryWithCorrectionParser(
    parser: Parser<T>,
    model: ChatModel,
    maxRetries: Int = 2,
): RetryWithCorrectionParser<T> = RetryWithCorrectionParser(parser, model, maxRetries)
