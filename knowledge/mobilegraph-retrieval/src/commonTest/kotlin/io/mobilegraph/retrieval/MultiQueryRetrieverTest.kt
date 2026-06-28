package io.mobilegraph.retrieval

import io.mobilegraph.core.capability.Capability
import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.documents.Document
import io.mobilegraph.models.AssistantMessage
import io.mobilegraph.models.ChatChunk
import io.mobilegraph.models.ChatModel
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.ModelConfig
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.vectorstores.RetrievalFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MultiQueryRetrieverTest {
    class MockChatModel(
        val variations: String,
    ) : ChatModel {
        override val name: String = "mock"

        override fun supports(capability: Capability): Boolean = true

        override suspend fun invoke(
            prompt: ChatPromptValue,
            config: ModelConfig?,
            context: ExecutionContext,
        ): ModelOutput = ModelOutput.ChatOutput(AssistantMessage(variations))

        override fun stream(
            prompt: ChatPromptValue,
            config: ModelConfig?,
            context: ExecutionContext,
        ): Flow<ChatChunk> = emptyFlow()
    }

    class MockRetriever : Retriever {
        val queries = mutableListOf<String>()

        override suspend fun retrieve(
            query: String,
            filter: RetrievalFilter?,
            context: ExecutionContext,
        ): List<Document> {
            queries.add(query)
            return listOf(Document(id = query, content = "content for $query"))
        }
    }

    @Test
    fun testMultiQuery() =
        runTest {
            val chatModel = MockChatModel("query 1\nquery 2")
            val delegate = MockRetriever()
            val retriever = MultiQueryRetriever(delegate, chatModel, queryCount = 2)

            val results = retriever.retrieve("original")

            // Should have "original", "query 1", "query 2"
            assertEquals(3, delegate.queries.size)
            assertTrue(delegate.queries.contains("original"))
            assertTrue(delegate.queries.contains("query 1"))
            assertTrue(delegate.queries.contains("query 2"))

            assertEquals(3, results.size)
        }
}
