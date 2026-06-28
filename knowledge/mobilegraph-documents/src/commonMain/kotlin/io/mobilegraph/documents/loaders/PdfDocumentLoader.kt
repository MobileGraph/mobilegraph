package io.mobilegraph.documents.loaders

import io.mobilegraph.documents.Document
import io.mobilegraph.documents.DocumentLoader
import io.mobilegraph.documents.DocumentMetadata

/**
 * Interface for platform-specific PDF text extraction.
 *
 * PDF parsing is highly platform-dependent and often requires large third-party libraries
 * (e.g., PDFBox on JVM, Android's PdfRenderer, or PDFKit on iOS). To keep the core SDK
 * lightweight and flexible, extraction logic is delegated to the application layer.
 */
interface PdfExtractor {
    /**
     * Extracts text content from a PDF file at the given path.
     */
    suspend fun extractText(path: String): String
}

/**
 * Loads documents from a PDF file.
 *
 * This loader requires a [PdfExtractor] to be provided by the application to handle
 * the platform-specific parsing logic.
 */
class PdfDocumentLoader(
    private val path: String,
    private val extractor: PdfExtractor,
    private val metadata: Map<String, String> = emptyMap(),
) : DocumentLoader {
    override suspend fun load(): List<Document> {
        val text = extractor.extractText(path)
        return listOf(
            Document(
                id = "pdf_${path.hashCode()}",
                content = text,
                metadata = metadata + (DocumentMetadata.MIME_TYPE to "application/pdf") + (DocumentMetadata.SOURCE to path),
            ),
        )
    }
}
