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
        assertEquals("Context:\nfirst\nsecond", context)
    }

    @Test
    fun testEmptyDocs() {
        val builder = DefaultContextBasedPromptBuilder()
        assertEquals("", builder.buildContextAndPrompt(emptyList(), "user query"))
    }
}
