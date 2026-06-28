package io.mobilegraph.rag

import io.mobilegraph.documents.Document

/**
 * Interface for augmenting a user query with retrieved documents to create a final LLM prompt.
 *
 * This component is responsible for the "Augmentation" step of RAG. It formats the
 * retrieved [Document]s into a context block and combines it with the user's question
 * and specific instructions (e.g., guardrails).
 */
interface ContextBasedPromptBuilder {
    /**
     * Constructs the full prompt string to be sent to the LLM.
     *
     * @param documents The list of documents to use as context. Can be null or empty.
     * @param userQuery The original question asked by the user.
     * @return A formatted prompt containing the context and the question.
     */
    fun buildContextAndPrompt(
        documents: List<Document>?,
        userQuery: String,
    ): String
}

/**
 * Default implementation of [ContextBasedPromptBuilder].
 *
 * It combines the documents into a single block and appends the user's question,
 * along with a basic guardrail instruction to prevent hallucinations.
 *
 * @property separator The string used to join multiple documents. Defaults to double newline.
 * @property prefix A header added before the combined document content.
 * @property suffix A footer added after the combined document content.
 */
class DefaultContextBasedPromptBuilder(
    private val separator: String = "\n\n",
    private val prefix: String = "Relevant context:\n",
    private val suffix: String = "",
) : ContextBasedPromptBuilder {
    override fun buildContextAndPrompt(
        documents: List<Document>?,
        userQuery: String,
    ): String {
        val contextData =
            if (documents.isNullOrEmpty()) {
                ""
            } else {
                documents.joinToString(
                    separator = separator,
                    prefix = prefix,
                    postfix = suffix,
                ) { it.content }
            }

        return """
            Use the following context to answer the user's question. 
            If the context doesn't contain the answer, say "Not enough context to answer the query."
            
            Context:
            $contextData

            Question: $userQuery
            """.trimIndent()
    }
}
