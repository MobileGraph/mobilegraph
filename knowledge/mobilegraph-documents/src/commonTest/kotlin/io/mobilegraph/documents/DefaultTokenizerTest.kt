package io.mobilegraph.documents

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultTokenizerTest {
    @Test
    fun testCountTokens() {
        val tokenizer = DefaultTokenizer()

        assertEquals(0, tokenizer.countTokens(""))

        val text = "the quick brown fox"
        val tokens = tokenizer.countTokens(text)

        // 4 words * 1.3 = 5.2 -> 5
        // 19 chars / 4 = 4.75 -> 4
        // max(5, 4, 1) = 5
        assertEquals(5, tokens)
    }
}
