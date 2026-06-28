package io.mobilegraph.retrieval

import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.documents.Document
import io.mobilegraph.vectorstores.RetrievalFilter
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HybridRetrieverTest {
    class MockRetriever(
        val docs: List<Document>,
    ) : Retriever {
        override suspend fun retrieve(
            query: String,
            filter: RetrievalFilter?,
            context: ExecutionContext,
        ): List<Document> = docs
    }

    @Test
    fun testHybridRetrieve() =
        runTest {
            val r1 = MockRetriever(listOf(Document(id = "1", content = "doc1"), Document(id = "2", content = "doc2")))
            val r2 = MockRetriever(listOf(Document(id = "2", content = "doc2"), Document(id = "3", content = "doc3")))

            val hybrid = HybridRetriever(listOf(r1, r2))
            val results = hybrid.retrieve("query")

            // RRF should rank doc2 first as it appears in both
            assertEquals(3, results.size)
            assertEquals("2", results[0].id)
        }
}
