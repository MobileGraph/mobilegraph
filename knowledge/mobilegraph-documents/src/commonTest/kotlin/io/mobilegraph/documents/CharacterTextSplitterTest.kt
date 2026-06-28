package io.mobilegraph.documents

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CharacterTextSplitterTest {
    @Test
    fun testSplit() =
        runTest {
            // CharacterTextSplitter tries to group parts separated by `separator` into chunks of `chunkSize`.
            // If a part is too big, it still adds it.
            val splitter = CharacterTextSplitter(separator = "\n", chunkSize = 10, chunkOverlap = 0)
            val doc = Document(id = "doc1", content = "line1\nline2\nline3")

            val chunks = splitter.split(doc)

            // "line1" (5) + "\n" + "line2" (5) = 11 > 10.
            // So it should emit "line1" then "line2" then "line3"
            assertEquals(3, chunks.size)
            assertEquals("line1", chunks[0].content)
            assertEquals("line2", chunks[1].content)
            assertEquals("line3", chunks[2].content)
        }

    @Test
    fun testOverlap() =
        runTest {
            // CharacterTextSplitter overlap implementation in current code:
            // val overlapStart = maxOf(0, currentChunk.length - chunkOverlap)
            // currentChunk = StringBuilder(currentChunk.substring(overlapStart))

            val splitter = CharacterTextSplitter(separator = "\n", chunkSize = 10, chunkOverlap = 2)
            val doc = Document(id = "doc1", content = "line1\nline2\nline3")
            val chunks = splitter.split(doc)

            // 1. currentChunk adds "line1" (5). Length 5.
            // 2. Next part "line2" (5). 5 + 1 + 5 = 11 > 10.
            // 3. Emit "line1".
            // 4. Overlap: last 2 chars of "line1" are "e1". currentChunk becomes "e1\n".
            // 5. Add "line2". currentChunk is "e1\nline2" (8).
            // 6. Next part "line3" (5). 8 + 1 + 5 = 14 > 10.
            // 7. Emit "e1\nline2".
            // 8. Overlap: last 2 chars of "e1\nline2" are "e2". currentChunk becomes "e2\n".
            // 9. Add "line3". currentChunk is "e2\nline3" (8).
            // 10. End: Emit "e2\nline3".

            assertEquals(3, chunks.size)
            assertEquals("line1", chunks[0].content)
            assertEquals("e1\nline2", chunks[1].content)
            assertEquals("e2\nline3", chunks[2].content)
        }
}
