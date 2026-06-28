package io.mobilegraph.documents

/**
 * Interface for loading documents from various data sources into the knowledge base.
 *
 * Implementations of [DocumentLoader] handle the retrieval and parsing of raw data from
 * sources like files, web pages, or databases, converting them into one or more [Document] objects.
 */
interface DocumentLoader {
    /**
     * Loads the data from the source and returns it as a list of [Document]s.
     *
     * @return A list of documents parsed from the source.
     */
    suspend fun load(): List<Document>
}
