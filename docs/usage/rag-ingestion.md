# RAG Ingestion Guide

Ingestion is the process of converting raw data from various sources into searchable document chunks within the MobileGraph knowledge base. This guide covers how to use the built-in loaders, splitters, and how to create your own.

## The Ingestion DSL

MobileGraph provides a clean DSL for ingesting data. It handles the full lifecycle: loading, splitting, embedding, and storing.

```kotlin
MobileGraph.knowledge.ingest {
    docId("manual-001")            // Recommended for targeted retrieval
    docName("Product User Guide") // Human readable name
    loader(TextDocumentLoader(myText))
    splitter(RecursiveTextSplitter(chunkSize = 500, chunkOverlap = 100))
    // vectorStore(customStore)   // Optional: defaults to environment default
}
```

---

## Document Loaders

Loaders are responsible for retrieving and parsing raw content.

### 1. Built-in Loaders

| Loader | Description |
| :--- | :--- |
| `TextDocumentLoader` | Ingests a raw plain-text string. |
| `MarkdownDocumentLoader` | Ingests Markdown text; sets MIME type to `text/markdown`. |
| `HtmlDocumentLoader` | Ingests HTML strings and performs basic tag stripping. |
| `JsonDocumentLoader` | Ingests JSON objects or arrays. Can extract specific fields via `contentPath`. |
| `CsvDocumentLoader` | Converts CSV rows into key-value context chunks. |
| `PdfDocumentLoader` | Orchestrates PDF ingestion via a provided `PdfExtractor`. |
| `WebsiteLoader` | Orchestrates web scraping via a provided `WebScraper`. |

### 2. Custom Loader Example

You can implement `DocumentLoader` to support any proprietary or unsupported format.

```kotlin
class MyCustomXmlLoader(private val xml: String) : DocumentLoader {
    override suspend fun load(): List<Document> {
        // Your custom parsing logic here
        val parsedText = xml.substringAfter("<content>").substringBefore("</content>")
        return listOf(
            Document(
                id = "xml-${xml.hashCode()}",
                content = parsedText,
                metadata = mapOf("format" to "xml")
            )
        )
    }
}
```

---

## Text Splitters

Splitters break large documents into smaller chunks, which is critical for staying within LLM context limits and improving search relevance.

### 1. Available Splitters

- **`RecursiveTextSplitter`**: The recommended default. It tries to split on a hierarchy of separators (paragraphs, then sentences, then words) to keep semantically related text together.
- **`CharacterTextSplitter`**: Splits text based on a fixed character count and a specific separator.
- **`TokenTextSplitter`**: Splits text based on an estimated token count, ensuring chunks fit perfectly into LLM windows.

### 2. Custom Splitter Example

Implement `TextSplitter` to control exactly how your data is chunked.

```kotlin
class SentenceSplitter : TextSplitter {
    override suspend fun split(document: Document): List<Document> {
        // Simple split by period
        return document.content.split(". ").mapIndexed { index, sentence ->
            document.copy(
                id = "${document.id}_s$index",
                content = sentence,
                metadata = document.metadata + ("type" to "sentence")
            )
        }
    }
}
```

---

## Best Practices
1. **Always use `docId`**: Providing a unique ID during ingestion allows you to perform **Targeted Retrieval** (searching only within that specific document) later.
2. **Tune Overlap**: A `chunkOverlap` of 10-20% of your `chunkSize` is usually optimal for maintaining context between fragments.
3. **Metadata**: Use metadata to store attributes like `category`, `author`, or `priority` to enable advanced filtering.
