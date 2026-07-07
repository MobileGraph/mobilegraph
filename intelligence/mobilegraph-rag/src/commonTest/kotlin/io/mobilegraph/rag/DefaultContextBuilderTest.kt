package io.mobilegraph.rag

import io.mobilegraph.documents.Document
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultContextBuilderTest {
    @Test
    fun testBuildContext() {
        val builder = DefaultContextBasedPromptBuilder(prefix = "Context:\n", separator = "\n")
        val docs =
            listOf(
                Document(id = "1", content = "first"),
                Document(id = "2", content = "second"),
            )

        val context = builder.buildContextAndPrompt(docs, "user query")
        val expected = """
Use the following context to answer the user's question. 
If the context doesn't contain the answer, say "Not enough context to answer the query."

Context:
Context:
first
second

Question: user query
        """.trimIndent()
        assertEquals(expected, context)
    }

    @Test
    fun testEmptyDocs() {
        val builder = DefaultContextBasedPromptBuilder()
        val context = builder.buildContextAndPrompt(emptyList(), "user query")
        val expected = """
Use the following context to answer the user's question. 
If the context doesn't contain the answer, say "Not enough context to answer the query."

Context:


Question: user query
        """.trimIndent()
        assertEquals(expected, context)
    }
}
