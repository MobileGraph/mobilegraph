# RAG Retrieval & Pipeline Guide

Retrieval is the act of finding relevant context from your knowledge base to answer a user's question. This guide covers retrieval strategies and how to configure the RAG pipeline.

## Retrieval Strategies

MobileGraph supports several built-in retrievers to balance speed and accuracy.

### 1. VectorStoreRetriever (Semantic Search)
The most common retriever. It uses vector embeddings to find documents with similar meanings, even if keywords don't match.

```kotlin
val retriever = VectorStoreRetriever(
    vectorStore = myStore,
    topK = 5,
    similarityThreshold = 0.75f // Only return highly relevant chunks
)
```

### 2. BM25Retriever (Keyword Search)
A traditional search engine algorithm. It is excellent for finding exact matches, technical terms, or specific product IDs that vector search might miss.

```kotlin
val retriever = BM25Retriever(allDocuments)
```

### 3. HybridRetriever (Balanced Search)
Combines Vector and BM25 search results using Reciprocal Rank Fusion (RRF). This is often the best choice for production apps.

```kotlin
val hybrid = HybridRetriever(listOf(vectorRetriever, bm25Retriever))
```

### 4. MultiQueryRetriever (Improved Recall)
Uses an LLM to generate variations of the user's question before searching. This helps when the initial query is vague.

```kotlin
val multiQuery = MultiQueryRetriever(vectorRetriever, chatModel)
```

---

## Targeted Retrieval (Filtering)

You can restrict your search to a specific document or subset of data using `RetrievalFilter`.

```kotlin
// Only search within the specific user manual
val filter = RetrievalFilter.DocumentId("manual-001")

val results = retriever.retrieve("how to reset?", filter = filter)
```

---

## The RAG Pipeline

The `RagPipeline` orchestrates the entire process: **Search -> Augment -> Generate**.

### Basic Setup

```kotlin
MobileGraph.initialize() {
    // ... model and vector store config ...

    withRetrievers {
        register("default", vectorRetriever, isDefault = true)
    }

    withRag {
        retriever(vectorRetriever)
        chatModel(myChatModel)
        // contextBuilder(customBuilder) // Optional: for custom prompts/guardrails
    }
}
```

### Executing a Query

```kotlin
// 1. Standard execution (returns model response)
val result = MobileGraph.rag.pipeline().execute("What is the battery life?")

// 2. Detailed execution (includes debug metadata)
val detailed = MobileGraph.rag.pipeline().executeDetailed("...")
println("Prompt used: ${detailed.finalPrompt}")
println("Scores: ${detailed.retrievedDocs.map { it.score }}")
```

---

## Creating a Custom Retriever

Implement the `Retriever` interface to integrate any external search engine or custom logic.

```kotlin
class MyCustomRetriever : Retriever {
    override suspend fun retrieve(
        query: String,
        filter: RetrievalFilter?,
        context: ExecutionContext
    ): List<Document> {
        // Your custom logic: call a REST API, query a DB, etc.
        return listOf(Document(id = "ext-1", content = "External content..."))
    }
}
```

---

## Customizing Prompts & Guardrails

You can control how context is presented to the LLM and add strict instructions by providing a custom `ContextBasedPromptBuilder`.

```kotlin
class GuardrailedBuilder : ContextBasedPromptBuilder {
    override fun buildContextAndPrompt(documents: List<Document>?, userQuery: String): String {
        val contextText = documents?.joinToString("\n") { it.content } ?: ""
        return """
            Answer using ONLY the context below. 
            If you don't know, say "Not enough info".
            
            Context: $contextText
            Question: $userQuery
        """.trimIndent()
    }
}
```
