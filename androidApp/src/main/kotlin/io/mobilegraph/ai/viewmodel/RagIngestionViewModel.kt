package io.mobilegraph.ai.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.documents.RecursiveTextSplitter
import io.mobilegraph.documents.loaders.CsvDocumentLoader
import io.mobilegraph.documents.loaders.HtmlDocumentLoader
import io.mobilegraph.documents.loaders.JsonDocumentLoader
import io.mobilegraph.documents.loaders.MarkdownDocumentLoader
import io.mobilegraph.documents.loaders.PdfDocumentLoader
import io.mobilegraph.documents.loaders.PdfExtractor
import io.mobilegraph.documents.loaders.TextDocumentLoader
import io.mobilegraph.documents.loaders.WebScraper
import io.mobilegraph.documents.loaders.WebsiteLoader
import io.mobilegraph.vectorstores.facade.knowledge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jsoup.Jsoup

/**
 * ViewModel dedicated to handling the ingestion of various document types into the MobileGraph Knowledge Base.
 */
class RagIngestionViewModel : ViewModel() {
    var uiState by mutableStateOf("Ready")
    var isLoading by mutableStateOf(false)

    private val _ingestionSuccess = MutableStateFlow<String?>(null)
    val ingestionSuccess: StateFlow<String?> = _ingestionSuccess

    private val _eventLog = MutableStateFlow<List<String>>(emptyList())
    val eventLog: StateFlow<List<String>> = _eventLog

    private val httpClient = HttpClient()
    private var applicationContext: Context? = null

    /**
     * Initializes the context for asset loading.
     */
    fun initialize(context: Context) {
        this.applicationContext = context.applicationContext
    }

    /**
     * Ingests a plain text file from assets.
     */
    fun ingestText() {
        ingest("txt-doc", "Text Sample") {
            val content = readAsset("sample.txt") ?: "MobileGraph SDK provides powerful on-device AI capabilities."
            TextDocumentLoader(content)
        }
    }

    /**
     * Ingests a Markdown file from assets with specific chunking parameters.
     */
    fun ingestMarkdown() {
        ingest("md-doc", "Markdown Sample", 500, 200) {
            val content = readAsset("sample.md") ?: "# MobileGraph\n\nSupporting RAG features."
            MarkdownDocumentLoader(content)
        }
    }

    /**
     * Ingests an HTML file from assets.
     */
    fun ingestHtml() {
        ingest("html-doc", "HTML Sample") {
            val content = readAsset("sample.html") ?: "<html><body><h1>MobileGraph</h1><p>Local RAG systems.</p></body></html>"
            HtmlDocumentLoader(content)
        }
    }

    /**
     * Ingests a JSON file from assets.
     */
    fun ingestJson() {
        ingest("json-doc", "JSON Sample") {
            val content = readAsset("sample.json") ?: "{\"name\": \"MobileGraph\", \"status\": \"Active\"}"
            JsonDocumentLoader(content)
        }
    }

    /**
     * Ingests a CSV file from assets.
     */
    fun ingestCsv() {
        ingest("csv-doc", "CSV Sample") {
            val content = readAsset("sample.csv") ?: "ID,Feature\n1,Embeddings\n2,VectorStore"
            CsvDocumentLoader(content)
        }
    }

    /**
     * Ingests a PDF file from assets using a custom PdfExtractor.
     */
    fun ingestPdf(assetName: String) {
        val extractor =
            object : PdfExtractor {
                override suspend fun extractText(path: String): String = extractPdfTextFromAssets(applicationContext!!, path)
            }
        ingest("pdf-doc", "PDF Sample") {
            PdfDocumentLoader(assetName, extractor)
        }
    }

    /**
     * Scrapes a website and ingests its text content.
     */
    fun ingestWebsite(url: String) {
        val scraper =
            object : WebScraper {
                override suspend fun scrape(url: String): String =
                    try {
                        val response = httpClient.get(url).bodyAsText()
                        val doc = Jsoup.parse(response)
                        doc.text()
                    } catch (e: Exception) {
                        "Error scraping $url: ${e.message}"
                    }
            }
        ingest("web-doc", url) { WebsiteLoader(url, scraper) }
    }

    /**
     * Core ingestion method using the MobileGraph Knowledge DSL.
     *
     * SDK Action: MobileGraph.knowledge.ingest
     * This handles the full pipeline: Load -> Split -> Embed -> Store.
     */
    private fun ingest(
        id: String,
        name: String,
        chunkSize: Int = 200,
        chunkOverlap: Int = 50,
        loaderProvider: () -> io.mobilegraph.documents.DocumentLoader,
    ) {
        viewModelScope.launch {
            isLoading = true
            uiState = "Ingesting $id..."
            try {
                // SDK USE: knowledge.ingest DSL to process the document
                MobileGraph.knowledge.ingest {
                    docId(id)
                    docName(name)
                    loader(loaderProvider())
                    // SDK USE: RecursiveTextSplitter to maintain semantic context during chunking
                    splitter(RecursiveTextSplitter(chunkSize, chunkOverlap))
                }
                uiState = "Ingestion Successful: $id"
                addEvent("Ingested $id successfully")
                _ingestionSuccess.value = id
                // _ingestionSuccess.value = null
            } catch (e: Exception) {
                uiState = "Ingestion Failed: ${e.message}"
                addEvent("Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    private fun readAsset(fileName: String): String? =
        try {
            applicationContext
                ?.assets
                ?.open(fileName)
                ?.bufferedReader()
                ?.use { it.readText() }
        } catch (e: Exception) {
            Log.e("RagIngestionVM", "Error reading asset $fileName", e)
            null
        }

    private fun addEvent(event: String) {
        _eventLog.value = _eventLog.value + event
    }

    /**
     * Helper to extract text from a PDF asset using PDFBox.
     */
    private fun extractPdfTextFromAssets(
        context: Context,
        assetFileName: String,
    ): String =
        context.assets.open(assetFileName).use { inputStream ->
            PDDocument.load(inputStream).use { document ->
                val stripper = PDFTextStripper()
                stripper.sortByPosition = true
                stripper.getText(document)
            }
        }
}
