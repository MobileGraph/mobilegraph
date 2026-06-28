package io.mobilegraph.documents

import kotlinx.serialization.Serializable

/**
 * Represents a single piece of information within the knowledge base.
 *
 * A [Document] can be an entire file (like a text file) or a specific part of a larger
 * source (like a page from a PDF or a chunk of text after splitting).
 *
 * @property id A unique identifier for this document/chunk.
 * @property content The raw text content of the document.
 * @property metadata A map of additional information associated with the document (e.g., source, page number).
 */
@Serializable
data class Document(
    val id: String,
    val content: String,
    val metadata: Map<String, String> = emptyMap(),
)

/**
 * Standard keys for [Document] metadata fields.
 */
object DocumentMetadata {
    /** The origin of the document (e.g., a file path or URL). */
    const val SOURCE = "source"

    /** The page number within the source document (primarily for PDFs). */
    const val PAGE = "page"

    /** The original filename of the document. */
    const val FILENAME = "filename"

    /** The MIME type of the original document content. */
    const val MIME_TYPE = "mimeType"

    /** The timestamp when the document was first created or ingested. */
    const val CREATED_AT = "createdAt"

    /** A unique identifier for the parent document (shared across all its chunks). */
    const val DOC_ID = "doc_id"

    /** The human-readable name or title of the parent document. */
    const val DOC_NAME = "doc_name"
}
