package io.mobilegraph.vectorstores

import io.mobilegraph.core.capability.Capability
import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.documents.Document
import io.mobilegraph.models.EmbeddingModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InMemoryVectorStoreTest {
    class MockEmbeddingModel : EmbeddingModel {
        override val name: String = "mock"

        override fun supports(capability: Capability): Boolean = true

        override suspend fun embed(
            text: String,
            context: ExecutionContext,
        ): FloatArray =
            when {
                text.contains("quick") -> floatArrayOf(1.0f, 0.0f)
                text.contains("lazy") -> floatArrayOf(0.0f, 1.0f)
                else -> floatArrayOf(0.5f, 0.5f)
            }

        override suspend fun embed(
            texts: List<String>,
            context: ExecutionContext,
        ): List<FloatArray> = texts.map { embed(it, context) }
    }

    @Test
    fun testAddAndSearch() =
        runTest {
            val model = MockEmbeddingModel()
            val store = InMemoryVectorStore(model)

            val docs =
                listOf(
                    Document(id = "1", content = "the quick brown fox"),
                    Document(id = "2", content = "the lazy dog"),
                )

            store.add(docs)

            val results = store.similaritySearch("quick", k = 1)
            assertEquals(1, results.size)
            assertEquals("1", results[0].document.id)
            assertTrue(results[0].score > 0.9f)
        }

    @Test
    fun testMetadataFilter() =
        runTest {
            val model = MockEmbeddingModel()
            val store = InMemoryVectorStore(model)

            val docs =
                listOf(
                    Document(id = "1", content = "quick", metadata = mapOf("cat" to "a")),
                    Document(id = "2", content = "quick", metadata = mapOf("cat" to "b")),
                )

            store.add(docs)

            val results = store.similaritySearch("quick", k = 10, filter = RetrievalFilter.Metadata("cat", "b"))
            assertEquals(1, results.size)
            assertEquals("2", results[0].document.id)
        }

    @Test
    fun testDelete() =
        runTest {
            val model = MockEmbeddingModel()
            val store = InMemoryVectorStore(model)

            store.add(listOf(Document(id = "1", content = "quick")))
            store.delete(listOf("1"))

            val results = store.similaritySearch("quick")
            assertTrue(results.isEmpty())
        }
}
