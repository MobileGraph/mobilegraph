package io.mobilegraph.documents

/**
 * Interface for counting tokens in a text.
 * Different models use different tokenization strategies.
 */
interface Tokenizer {
    /**
     * Counts the number of tokens in the given text.
     */
    fun countTokens(text: String): Int
}

/**
 * A default tokenizer that provides a reasonable estimation for most LLMs.
 * For English, 1 token is roughly 4 characters or 0.75 words.
 */
class DefaultTokenizer : Tokenizer {
    override fun countTokens(text: String): Int {
        if (text.isEmpty()) return 0

        // A slightly more accurate estimation than just length / 4
        // Counts words and assigns them roughly 1.3 tokens each,
        // plus handles spaces and punctuation.
        val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val wordBasedCount = (words.size * 1.3).toInt()
        val charBasedCount = text.length / 4

        // Take the max to be conservative (better to over-split than under-split)
        return maxOf(wordBasedCount, charBasedCount, 1)
    }
}
