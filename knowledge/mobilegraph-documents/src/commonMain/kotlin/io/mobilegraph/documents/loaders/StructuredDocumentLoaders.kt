package io.mobilegraph.documents.loaders

import io.mobilegraph.documents.Document
import io.mobilegraph.documents.DocumentLoader
import io.mobilegraph.documents.DocumentMetadata
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Loads documents from a JSON string.
 *
 * If the JSON is an object, it creates a single document. If it's an array, it creates
 * a document for each element in the array.
 *
 * @param jsonContent The raw JSON string.
 * @param contentPath Optional key to extract a specific string field as the document content.
 *                    If null, the entire JSON object/element is used as content.
 * @param metadata Additional metadata to attach to the resulting documents.
 */
class JsonDocumentLoader(
    private val jsonContent: String,
    private val contentPath: String? = null,
    private val metadata: Map<String, String> = emptyMap(),
) : DocumentLoader {
    override suspend fun load(): List<Document> {
        val json = Json.parseToJsonElement(jsonContent)

        return when {
            json is JsonObject -> {
                listOf(createDocument(json, "json_0"))
            }

            json is kotlinx.serialization.json.JsonArray -> {
                json.mapIndexed { index, element ->
                    createDocument(element.jsonObject, "json_$index")
                }
            }

            else -> {
                emptyList()
            }
        }
    }

    private fun createDocument(
        obj: JsonObject,
        defaultId: String,
    ): Document {
        val content =
            contentPath?.let { path ->
                obj[path]?.jsonPrimitive?.content
            } ?: obj.toString()

        return Document(
            id = defaultId,
            content = content,
            metadata = metadata + (DocumentMetadata.MIME_TYPE to "application/json"),
        )
    }
}

/**
 * Loads documents from a CSV string.
 *
 * Each row in the CSV (excluding the header) is converted into a separate [Document].
 * The content of each document is a key-value representation of the row based on headers.
 *
 * @param csvContent The raw CSV data.
 * @param separator The delimiter used in the CSV. Defaults to a comma.
 * @param metadata Additional metadata to attach to all resulting documents.
 */
class CsvDocumentLoader(
    private val csvContent: String,
    private val separator: String = ",",
    private val metadata: Map<String, String> = emptyMap(),
) : DocumentLoader {
    override suspend fun load(): List<Document> {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val headers = lines.first().split(separator).map { it.trim() }

        return lines.drop(1).mapIndexed { index, line ->
            val values = line.split(separator).map { it.trim() }
            val content = headers.zip(values).joinToString("\n") { (h, v) -> "$h: $v" }

            Document(
                id = "csv_$index",
                content = content,
                metadata = metadata + (DocumentMetadata.MIME_TYPE to "text/csv"),
            )
        }
    }
}
