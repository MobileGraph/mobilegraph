package io.mobilegraph.retrieval

import io.mobilegraph.core.annotations.ExperimentalMobileGraphApi
import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.documents.Document
import io.mobilegraph.documents.DocumentMetadata
import io.mobilegraph.vectorstores.RetrievalFilter
import kotlin.math.log10
import kotlin.properties.Delegates

/**
 * A keyword-based retriever that implements the BM25 (Best Matching 25) ranking function.
 *
 * BM25 is a state-of-the-art ranking function used by search engines to estimate the relevance
 * of documents to a given search query. It is particularly effective for keyword-based search
 * where exact matches are important. Read more https://huggingface.co/blog/xhluca/bm25s
 *
 * @param documents The initial set of documents to index for keyword search.
 * @property k1 Controls non-linear term frequency scaling (saturation). Defaults to 1.5.
 * @property b Controls to what degree document length normalizes the tf values. Defaults to 0.75.
 */
class BM25Retriever(
    private var documents: List<Document>,
    private val k1: Float = 1.5f,
    private val b: Float = 0.75f,
) : Retriever {
    private var docCount = documents.size.toFloat()
    private var avgDocLength by Delegates.notNull<Float>()

    private lateinit var docTermFreqs: List<Map<String, Int>>
    private lateinit var docLengths: List<Int>
    private lateinit var idf: Map<String, Float>

    init {
        updateDocumentList(documents)
    }

    fun updateDocumentList(newDocuments: List<Document>) {
        this.documents = newDocuments
        this.docCount = newDocuments.size.toFloat()
        this.docTermFreqs =
            newDocuments.map { doc ->
                tokenize(doc.content).groupingBy { it }.eachCount()
            }
        this.docLengths = docTermFreqs.map { it.values.sum() }
        this.avgDocLength = if (docLengths.isNotEmpty()) docLengths.average().toFloat() else 0f

        val df = mutableMapOf<String, Int>()
        docTermFreqs.forEach { freqs ->
            freqs.keys.forEach { term ->
                df[term] = (df[term] ?: 0) + 1
            }
        }

        this.idf =
            df.mapValues { (_, count) ->
                log10((docCount - count + 0.5f) / (count + 0.5f) + 1.0f).toFloat()
            }
    }

    override suspend fun retrieve(
        query: String,
        filter: RetrievalFilter?,
        context: ExecutionContext,
    ): List<Document> {
        val filteredDocs =
            if (filter == null) {
                documents.indices.toList()
            } else {
                documents.indices.filter { i ->
                    val doc = documents[i]
                    when (filter) {
                        is RetrievalFilter.DocumentId -> doc.metadata[DocumentMetadata.DOC_ID] == filter.id
                        is RetrievalFilter.Metadata -> doc.metadata[filter.key] == filter.value
                    }
                }
            }

        if (filteredDocs.isEmpty()) return emptyList()

        val queryTerms = tokenize(query)
        if (queryTerms.isEmpty()) return emptyList()

        val scores =
            filteredDocs.map { i ->
                var score = 0f
                val docFreqs = docTermFreqs[i]
                val docLen = docLengths[i].toFloat()

                queryTerms.forEach { term ->
                    val f = docFreqs[term]?.toFloat() ?: 0f
                    val termIdf = idf[term] ?: 0f
                    score += termIdf * (f * (k1 + 1f)) / (f + k1 * (1f - b + b * (docLen / avgDocLength)))
                }
                i to score
            }

        return scores
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { documents[it.first] }
    }

    /**
     * Internal utility to clean and tokenize text into keywords.
     */
    private fun tokenize(text: String): List<String> =
        text
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 1 }
}
