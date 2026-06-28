package io.mobilegraph.documents.loaders

import io.mobilegraph.documents.DocumentMetadata
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TextDocumentLoaderTest {
    @Test
    fun testLoad() =
        runTest {
            val loader = TextDocumentLoader("hello", mapOf("key" to "value"))
            val docs = loader.load()

            assertEquals(1, docs.size)
            assertEquals("hello", docs[0].content)
            assertEquals("value", docs[0].metadata["key"])
            assertEquals("text/plain", docs[0].metadata[DocumentMetadata.MIME_TYPE])
        }
}
