package io.mobilegraph.documents.loaders

import io.mobilegraph.documents.Document
import io.mobilegraph.documents.DocumentLoader
import io.mobilegraph.documents.DocumentMetadata

/**
 * Loads a document from a raw text string.
 *
 * @param text The plain text content.
 * @param metadata Additional metadata to attach.
 */
class TextDocumentLoader(
    private val text: String,
    private val metadata: Map<String, String> = emptyMap(),
) : DocumentLoader {
    override suspend fun load(): List<Document> =
        listOf(
            Document(
                id = "text_${text.hashCode()}",
                content = text,
                metadata = metadata + (DocumentMetadata.MIME_TYPE to "text/plain"),
            ),
        )
}

/**
 * Loads a document from Markdown string content.
 *
 * @param content The Markdown formatted text.
 * @param metadata Additional metadata to attach.
 */
class MarkdownDocumentLoader(
    private val content: String,
    private val metadata: Map<String, String> = emptyMap(),
) : DocumentLoader {
    override suspend fun load(): List<Document> =
        listOf(
            Document(
                id = "md_${content.hashCode()}",
                content = content,
                metadata = metadata + (DocumentMetadata.MIME_TYPE to "text/markdown"),
            ),
        )
}
