package io.mobilegraph.documents

import kotlin.math.min

/**
 * Interface for splitting documents into smaller chunks.
 */
interface TextSplitter {
    suspend fun split(document: Document): List<Document>
}

/**
 * Simple character-based text splitter.
 * @property separator      Character that to separate the text block into multi parts. Defaults to \n\n
 * @property chunkSize      The maximum number of characters allowed in a single chunk.
 *                          Defaults to `500`.
 * @property chunkOverlap  The number of characters that consecutive chunks should share
 *                          at their boundaries to preserve context. Must be less than or
 *                          equal to [chunkSize]. Defaults to `200`.
 */
class CharacterTextSplitter(
    private val separator: String = "\n\n",
    private val chunkSize: Int = 500,
    private val chunkOverlap: Int = 200,
) : TextSplitter {
    override suspend fun split(document: Document): List<Document> {
        val chunks = splitText(document.content)
        return chunks.mapIndexed { index, text ->
            Document(
                id = "${document.id}_chunk_$index",
                content = text,
                metadata =
                    document.metadata +
                        mapOf(
                            "chunk_index" to index.toString(),
                            "total_chunks" to chunks.size.toString(),
                        ),
            )
        }
    }

    private fun splitText(text: String): List<String> {
        if (text.length <= chunkSize) return listOf(text)

        val parts = text.split(separator)
        val chunks = mutableListOf<String>()
        var currentChunk = StringBuilder()

        for (part in parts) {
            if (currentChunk.isNotEmpty() && currentChunk.length + part.length + separator.length > chunkSize) {
                chunks.add(currentChunk.toString())

                // Keep overlap
                val overlapStart = maxOf(0, currentChunk.length - chunkOverlap)
                currentChunk = StringBuilder(currentChunk.substring(overlapStart))
                if (currentChunk.isNotEmpty()) currentChunk.append(separator)
            }
            if (currentChunk.isNotEmpty() && currentChunk.last().toString() != separator) {
                currentChunk.append(separator)
            }
            currentChunk.append(part)
        }

        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString())
        }

        return chunks
    }
}

/**
 * A text splitter that recursively splits text into chunks using a hierarchy of separators.
 *
 * The splitter works by trying a list of separators in order (e.g. `"\n\n"`, `"\n"`, `" "`, `""`).
 * It starts with the most preferred separator and falls back to finer-grained ones if the
 * resulting chunks are still too large. This ensures that semantically related text
 * (e.g. paragraphs, sentences) is kept together as much as possible.
 *
 * Overlap between consecutive chunks is preserved so that context is not lost at boundaries.
 *
 * @property chunkSize      The maximum number of characters allowed in a single chunk.
 *                          Defaults to `500`.
 * @property chunkOverlap  The number of characters that consecutive chunks should share
 *                          at their boundaries to preserve context. Must be less than or
 *                          equal to [chunkSize]. Defaults to `200`.
 * @property keepSeparator  If `true`, the separator used to split the text is retained
 *                          at the beginning of each resulting chunk. If `false`, separators
 *                          are discarded. Defaults to `false`.
 * @property stripWhitespace If `true`, leading and trailing whitespace is stripped from
 *                          each chunk before it is emitted. If `false`, whitespace is
 *                          preserved as-is. Defaults to `true`.
 *
 */
class RecursiveTextSplitter(
    private val chunkSize: Int = 500,
    private val chunkOverlap: Int = 200,
    private val keepSeparator: Boolean = false,
    private val stripWhitespace: Boolean = true,
) : TextSplitter {
    init {
        require(chunkOverlap <= chunkSize) {
            "chunkOverlap ($chunkOverlap) must be <= chunkSize ($chunkSize)"
        }
    }

    override suspend fun split(document: Document): List<Document> {
        val contentType = document.metadata[DocumentMetadata.MIME_TYPE]
        val separators = getSeparators(contentType)
        val chunks = splitTextRecursive(document.content, separators)
        return chunks.mapIndexed { index, text ->
            Document(
                id = "${document.id}_chunk_$index",
                content = text,
                metadata =
                    document.metadata +
                        mapOf(
                            "chunk_index" to index.toString(),
                            "total_chunks" to chunks.size.toString(),
                        ),
            )
        }
    }

    private fun getSeparators(contentType: String?): List<String> =
        when (contentType) {
            "text/markdown" -> {
                listOf(
                    "\n# ",
                    "\n## ",
                    "\n### ",
                    "\n#### ",
                    "\n##### ",
                    "\n###### ",
                    "```\n",
                    "\n\n",
                    "\n",
                    " ",
                    "",
                )
            }

            "application/json" -> {
                listOf("}", "],\n", "\n", " ", "")
            }

            else -> {
                listOf("\n\n", "\n", " ", "")
            }
        }

    private fun splitTextRecursive(
        text: String,
        separators: List<String>,
    ): List<String> {
        val finalChunks = mutableListOf<String>()

        var goodSeparator = ""
        var newSeparators = listOf<String>()

        for (i in separators.indices) {
            val sep = separators[i]
            if (sep == "" || text.contains(sep)) {
                goodSeparator = sep
                newSeparators = separators.subList(i + 1, separators.size)
                break
            }
        }

        val splits = splitTextWithSeparator(text, goodSeparator, keepSeparator)

        val goodSplits = mutableListOf<String>()

        for (s in splits) {
            if (length(s) < chunkSize) {
                goodSplits.add(s)
            } else {
                if (goodSplits.isNotEmpty()) {
                    val merged = mergeSplits(goodSplits, if (keepSeparator) "" else goodSeparator)
                    finalChunks.addAll(merged)
                    goodSplits.clear()
                }
                if (newSeparators.isEmpty()) {
                    finalChunks.add(s)
                } else {
                    finalChunks.addAll(splitTextRecursive(s, newSeparators))
                }
            }
        }

        if (goodSplits.isNotEmpty()) {
            val merged = mergeSplits(goodSplits, if (keepSeparator) "" else goodSeparator)
            finalChunks.addAll(merged)
        }

        return finalChunks
    }
    // -------------------------------------------------------------------------
    // _split_text_with_regex()
    // -------------------------------------------------------------------------

    private fun splitTextWithSeparator(
        text: String,
        separator: String,
        keepSeparator: Boolean,
    ): List<String> {
        val splits: List<String> =
            when {
                separator.isEmpty() -> {
                    text.map { it.toString() }
                }

                keepSeparator -> {
                    val escaped = Regex.escape(separator)
                    val parts = text.split(Regex("($escaped)"))

                    val merged = mutableListOf<String>()
                    var i = 0
                    while (i < parts.size) {
                        if (parts[i] == separator && i + 1 < parts.size) {
                            merged.add(parts[i] + parts[i + 1])
                            i += 2
                        } else if (parts[i] != separator) {
                            merged.add(parts[i])
                            i++
                        } else {
                            i++
                        }
                    }
                    merged
                }

                else -> {
                    text.split(separator)
                }
            }
        return splits.filter { it.isNotEmpty() }
    }

    private fun length(text: String): Int = text.length

    private fun mergeSplits(
        splits: List<String>,
        separator: String,
    ): List<String> {
        val separatorLen = length(separator)
        val docs = mutableListOf<String>()
        val currentDoc = mutableListOf<String>()
        var total = 0

        for (d in splits) {
            val dLen = length(d)
            val projectedLen = total + dLen + (if (currentDoc.isNotEmpty()) separatorLen else 0)

            if (projectedLen > chunkSize) {
                if (currentDoc.isNotEmpty()) {
                    val doc = joinDocs(currentDoc, separator)
                    if (doc != null) docs.add(doc)
                    while (currentDoc.size > 1 && total > chunkOverlap) {
                        total -= length(currentDoc[0]) + (if (currentDoc.size > 1) separatorLen else 0)
                        currentDoc.removeAt(0)
                    }
                }
            }

            currentDoc.add(d)
            total += dLen + (if (currentDoc.size > 1) separatorLen else 0)
        }

        val doc = joinDocs(currentDoc, separator)
        if (doc != null) docs.add(doc)

        return docs
    }

    private fun joinDocs(
        docs: List<String>,
        separator: String,
    ): String? {
        val text = docs.joinToString(separator)
        val result = if (stripWhitespace) text.trim() else text
        return if (result.isEmpty()) null else result
    }
}

/**
 * Token-aware text splitter.
 * Since actual tokenization is platform/model specific, this implementation
 * uses a [Tokenizer] to count tokens.
 */
class TokenTextSplitter(
    private val chunkSize: Int = 512,
    private val chunkOverlap: Int = 50,
    private val tokenizer: Tokenizer = DefaultTokenizer(),
) : TextSplitter {
    override suspend fun split(document: Document): List<Document> {
        val text = document.content
        val chunks = mutableListOf<String>()

        var start = 0
        while (start < text.length) {
            var end = min(start + (chunkSize * 4), text.length)

            // Refine end based on token count
            while (end > start && tokenizer.countTokens(text.substring(start, end)) > chunkSize) {
                // Back off more intelligently
                val currentTokens = tokenizer.countTokens(text.substring(start, end))
                val ratio = chunkSize.toFloat() / currentTokens
                val newLength = ((end - start) * ratio).toInt()
                end = start + maxOf(1, newLength)
            }

            chunks.add(text.substring(start, end))

            // Move start forward, accounting for overlap
            val actualChunkText = text.substring(start, end)
            val actualTokens = tokenizer.countTokens(actualChunkText)

            // Estimate how many characters to move forward to maintain token overlap
            val moveForwardRatio = (actualTokens - chunkOverlap).toFloat() / actualTokens
            val moveForward = (actualChunkText.length * moveForwardRatio).toInt()

            start += maxOf(1, moveForward)

            if (end == text.length) break
        }

        return chunks.mapIndexed { index, content ->
            Document(
                id = "${document.id}_chunk_$index",
                content = content,
                metadata =
                    document.metadata +
                        mapOf(
                            "chunk_index" to index.toString(),
                            "total_chunks" to chunks.size.toString(),
                        ),
            )
        }
    }
}
