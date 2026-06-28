package io.mobilegraph.rag

import io.mobilegraph.core.capability.Capability
import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.documents.Document
import io.mobilegraph.models.AssistantMessage
import io.mobilegraph.models.ChatChunk
import io.mobilegraph.models.ChatModel
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.HumanMessage
import io.mobilegraph.models.ModelConfig
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.retrieval.Retriever
import io.mobilegraph.vectorstores.RetrievalFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SimpleRagPipelineTest {
    class MockRetriever(
        val docs: List<Document>,
    ) : Retriever {
        override suspend fun retrieve(
            query: String,
            filter: RetrievalFilter?,
            context: ExecutionContext,
        ): List<Document> = docs
    }

    class MockChatModel : ChatModel {
        override var name: String = "mock"

        override fun supports(capability: Capability): Boolean = true

        override suspend fun invoke(
            prompt: ChatPromptValue,
            config: ModelConfig?,
            context: ExecutionContext,
        ): ModelOutput {
            val content = (prompt.messages.first() as HumanMessage).content
            return ModelOutput.ChatOutput(AssistantMessage("Answer to: $content"))
        }

        override fun stream(
            prompt: ChatPromptValue,
            config: ModelConfig?,
            context: ExecutionContext,
        ): Flow<ChatChunk> = emptyFlow()
    }

    @Test
    fun testExecute() =
        runTest {
            val docs = listOf(Document(id = "1", content = "doc content"))
            val retriever = MockRetriever(docs)
            val contextBuilder = DefaultContextBasedPromptBuilder(prefix = "Context:\n")
            val chatModel = MockChatModel()

            val pipeline = SimpleRagPipeline(retriever, contextBuilder, chatModel)
            val result = pipeline.execute("query")

            val outputText = (result as ModelOutput.ChatOutput).message.content
            assertEquals("Answer to: Context:\ndoc content\n\nUser Question: query", outputText)
        }
}
