package io.mobilegraph.documents

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecursiveTextSplitterTest {
    @Test
    fun testRecursiveSplit() =
        runTest {
            val splitter = RecursiveTextSplitter(chunkSize = 10, chunkOverlap = 0)
            val doc = Document(id = "doc1", content = "para1\n\npara2\nline2")

            val chunks = splitter.split(doc)

            assertEquals(3, chunks.size)
            assertEquals("para1", chunks[0].content)
            assertEquals("para2", chunks[1].content)
            assertEquals("line2", chunks[2].content)
        }

    @Test
    fun testRecursiveSplitWithOverlap() =
        runTest {
            // Force a specific set of separators by setting MIME type to something standard
            val splitter = RecursiveTextSplitter(chunkSize = 15, chunkOverlap = 10)
            val doc = Document(id = "doc1", content = "word1 word2 word3 word4")

            val chunks = splitter.split(doc)

            assertTrue(chunks.size >= 2)
            assertTrue(chunks[1].content.contains("word2")) // Should contain word from previous chunk
        }

    @Test
    fun testWithRagSampleText() =
        runTest {
            val sampleText =
                "MobileGraph SDK is a local-first AI framework. Kotlin-first " +
                    "framework for building AI applications, RAG systems, and agentic " +
                    "workflows on Android and Kotlin Multiplatform (KMP).\n" +
                    "\n" +
                    "MobileGraph provides a unified programming model for " +
                    "integrating LLMs, memory, tools, structured outputs, and future agentic " +
                    "workflows into mobile and multiplatform applications.\n" +
                    "\n" +
                    "Designed with a mobile-first mindset, " +
                    "MobileGraph helps developers build resilient AI experiences while remaining provider-agnostic and fully native.\n"

            // Using default chunkSize=200, chunkOverlap=50
            val splitter = RecursiveTextSplitter(chunkSize = 200, chunkOverlap = 50)
            val doc = Document(id = "rag-sample", content = sampleText)

            val chunks = splitter.split(doc)

            chunks.forEachIndexed { index, chunk ->
                if (index > 0) {
                    val prevContent = chunks[index - 1].content
                    val currentContent = chunk.content

                    val overlapLen = findOverlap(prevContent, currentContent)
                    assertTrue(
                        overlapLen > 0,
                        "No overlap found between chunk ${index - 1} and $index.\n" +
                            "Prev end: ${prevContent.takeLast(50)}\n" +
                            "Curr start: ${currentContent.take(50)}",
                    )
                }
            }
        }

    private fun findOverlap(
        s1: String,
        s2: String,
    ): Int {
        val maxOverlap = minOf(s1.length, s2.length)
        for (len in maxOverlap downTo 1) {
            if (s1.endsWith(s2.substring(0, len))) {
                return len
            }
        }
        return 0
    }
}
