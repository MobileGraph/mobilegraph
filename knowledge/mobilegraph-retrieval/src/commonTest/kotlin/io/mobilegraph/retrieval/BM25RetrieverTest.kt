package io.mobilegraph.retrieval

import io.mobilegraph.core.annotations.ExperimentalMobileGraphApi
import io.mobilegraph.documents.Document
import io.mobilegraph.documents.DocumentMetadata
import io.mobilegraph.vectorstores.RetrievalFilter
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalMobileGraphApi::class)
class BM25RetrieverTest {
    @Test
    fun testRetrieve() =
        runTest {
            val docs =
                listOf(
                    Document(id = "1", content = "the quick brown fox"),
                    Document(id = "2", content = "jumped over the lazy dog"),
                    Document(id = "3", content = "the quick brown jumps"),
                )
            val retriever = BM25Retriever(docs)

            val results = retriever.retrieve("quick brown")

            assertEquals(2, results.size)
            assertTrue(results.any { it.id == "1" })
            assertTrue(results.any { it.id == "3" })
        }

    @Test
    fun testNoResults() =
        runTest {
            val docs =
                listOf(
                    Document(id = "1", content = "the quick brown fox"),
                )
            val retriever = BM25Retriever(docs)

            val results = retriever.retrieve("nonexistent")

            assertTrue(results.isEmpty())
        }

    @Test
    fun testTargetedRetrieve() =
        runTest {
            val docs =
                listOf(
                    Document(id = "1", content = "the quick brown fox", metadata = mapOf(DocumentMetadata.DOC_ID to "docA")),
                    Document(id = "2", content = "the quick brown dog", metadata = mapOf(DocumentMetadata.DOC_ID to "docB")),
                )
            val retriever = BM25Retriever(docs)

            val results = retriever.retrieve("quick brown", filter = RetrievalFilter.DocumentId("docA"))

            assertEquals(1, results.size)
            assertEquals("1", results[0].id)
        }
}
