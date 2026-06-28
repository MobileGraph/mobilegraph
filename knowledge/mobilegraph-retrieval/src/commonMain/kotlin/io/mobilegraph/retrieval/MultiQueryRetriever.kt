package io.mobilegraph.retrieval

import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.documents.Document
import io.mobilegraph.models.ChatModel
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.HumanMessage
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.vectorstores.RetrievalFilter

/**
 * A retriever that uses an LLM to generate multiple variations of the user's query to
 * improve retrieval recall.
 *
 * This implementation is useful when the user's initial query might be poorly worded
 * or too brief to find good semantic matches. It uses a [ChatModel] to rewrite the query
 * into [queryCount] variations, retrieves documents for all of them, and returns the unique set.
 *
 * @param delegate The base retriever used to fetch documents for each query variation.
 * @param chatModel The LLM used to generate query variations.
 * @param queryCount The number of additional query variations to generate. Defaults to 3.
 */
class MultiQueryRetriever(
    private val delegate: Retriever,
    private val chatModel: ChatModel,
    private val queryCount: Int = 3,
) : Retriever {
    override suspend fun retrieve(
        query: String,
        filter: RetrievalFilter?,
        context: ExecutionContext,
    ): List<Document> {
        val variations = generateVariations(query)
        val allQueries = (variations + query).distinct()

        val allDocs =
            allQueries.flatMap { q ->
                delegate.retrieve(q, filter, context)
            }

        return allDocs.distinctBy { it.id }
    }

    private suspend fun generateVariations(query: String): List<String> {
        val prompt =
            """
            You are an AI assistant. Your task is to generate $queryCount different versions 
            of the given user query to help retrieve relevant documents from a vector database. 
            
            Original query: $query
            
            Provide only the queries, one per line, without numbers or any other text.
            """.trimIndent()

        val output = chatModel.invoke(ChatPromptValue(listOf(HumanMessage(prompt))))

        val text =
            when (output) {
                is ModelOutput.ChatOutput -> output.message.content
                is ModelOutput.ErrorOutput -> ""
            }

        return text
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("Original query") }
            .take(queryCount)
    }
}
