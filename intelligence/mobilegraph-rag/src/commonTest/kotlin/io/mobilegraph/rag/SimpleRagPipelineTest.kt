package io.mobilegraph.rag

import io.mobilegraph.core.capability.Capability
import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.documents.Document
import io.mobilegraph.models.AssistantMessage
import io.mobilegraph.models.ChatChunk
import io.mobilegraph.models.ChatModel
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.ModelConfig
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.models.UserMessage
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
            val content = (prompt.messages.first() as UserMessage).content
            return ModelOutput.ChatOutput(AssistantMessage("Answer to: $content"))
        }

        override fun stream(
            prompt: ChatPromptValue,
            config: ModelConfig?,
            context: ExecutionContext,
        ): Flow<ChatChunk> = emptyFlow()

        override fun readModelConfig(): ModelConfig?= null
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
            val expectedPrompt = """
Use the following context to answer the user's question. 
If the context doesn't contain the answer, say "Not enough context to answer the query."

Context:
Context:
doc content

Question: query
            """.trimIndent()
            assertEquals("Answer to: $expectedPrompt", outputText)
        }
}
