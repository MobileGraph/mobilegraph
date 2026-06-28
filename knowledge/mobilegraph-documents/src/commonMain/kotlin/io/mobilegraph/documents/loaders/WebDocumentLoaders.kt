package io.mobilegraph.documents.loaders

import io.mobilegraph.core.annotations.ExperimentalMobileGraphApi
import io.mobilegraph.documents.Document
import io.mobilegraph.documents.DocumentLoader
import io.mobilegraph.documents.DocumentMetadata

/**
 * Interface for a web scraper used by [WebsiteLoader].
 *
 * Web scraping involves network requests and HTML parsing, which are handled
 * differently across platforms (e.g., Jsoup on Android/JVM vs. native frameworks on iOS).
 * To keep the core SDK lightweight, the scraping logic is delegated to the application layer.
 */
interface WebScraper {
    /**
     * Scrapes the content from the given URL and returns it as a plain text string.
     *
     * @param url The website URL to scrape.
     * @return The cleaned text content of the website.
     */
    suspend fun scrape(url: String): String
}

/**
 * Loads a document from a website URL.
 *
 * This loader requires a [WebScraper] to be provided by the application to handle
 * the actual network fetch and HTML-to-text conversion.
 *
 * @param url The URL of the website to ingest.
 * @param scraper The scraper implementation.
 * @param metadata Additional metadata to attach.
 */
@ExperimentalMobileGraphApi
class WebsiteLoader(
    private val url: String,
    private val scraper: WebScraper,
    private val metadata: Map<String, String> = emptyMap(),
) : DocumentLoader {
    override suspend fun load(): List<Document> {
        val content = scraper.scrape(url)
        return listOf(
            Document(
                id = "url_${url.hashCode()}",
                content = content,
                metadata =
                    metadata +
                        mapOf(
                            DocumentMetadata.SOURCE to url,
                            DocumentMetadata.MIME_TYPE to "text/html",
                        ),
            ),
        )
    }
}

/**
 * Loads a document from an HTML string.
 *
 * This loader performs a basic regex-based tag stripping to convert HTML to plain text.
 * For production use with complex HTML, consider using a proper HTML parser in the app layer.
 *
 * @param html The raw HTML string.
 * @param metadata Additional metadata to attach.
 */
class HtmlDocumentLoader(
    private val html: String,
    private val metadata: Map<String, String> = emptyMap(),
) : DocumentLoader {
    override suspend fun load(): List<Document> {
        // Simple regex-based tag stripping as a convenience.
        val textOnly = html.replace(Regex("<[^>]*>"), " ").trim()

        return listOf(
            Document(
                id = "html_${html.hashCode()}",
                content = textOnly,
                metadata = metadata + (DocumentMetadata.MIME_TYPE to "text/html"),
            ),
        )
    }
}
