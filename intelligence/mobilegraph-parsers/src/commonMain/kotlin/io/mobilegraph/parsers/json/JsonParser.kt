package io.mobilegraph.parsers.json

import io.mobilegraph.parsers.exceptions.MalformedContentException
import io.mobilegraph.parsers.exceptions.SchemaMismatchException
import io.mobilegraph.parsers.parsers.OutputParser
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * A parser that converts JSON text into strongly typed Kotlin objects using kotlinx.serialization.
 */
class JsonParser<T>(
    private val serializer: KSerializer<T>,
    private val json: Json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        },
) : OutputParser<T> {
    override fun parse(text: String): T =
        try {
            json.decodeFromString(serializer, text)
        } catch (e: SerializationException) {
            // Distinguish between malformed JSON and schema mismatches if possible,
            // though kotlinx.serialization often throws SerializationException for both.
            throw MalformedContentException("Failed to parse JSON: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            throw SchemaMismatchException("JSON schema mismatch: ${e.message}", e)
        }
}
