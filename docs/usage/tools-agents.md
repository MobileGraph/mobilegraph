# Tools & Agents Guide

This guide covers how to give your AI "hands" via tools and how to manage them semantically.

---

## 1. Tools & Function Calling

**What**: A mechanism that allows the AI to perform real-world actions (e.g., getting weather, querying a database, or sending an email) by calling custom Kotlin functions.

**Why**: LLMs are limited to their training data. Tools give them "hands" to interact with your app's unique data and APIs in real-time.

**How to Create a Tool**:
Implement the `Tool<Input, Output>` interface.
```kotlin
val weatherTool = object : Tool<String, String> {
    override val metadata = ToolMetadata(
        name = "get_weather",
        description = "Gets the current weather for a specific city."
    )
    
    override suspend fun invoke(input: String, context: ExecutionContext): String {
        return "The weather in $input is 25°C and Sunny."
    }
}
```

**How to Register Tools**:
Register your tools during ADK initialization so the AI can discover them.
```kotlin
MobileGraph.initialize(context) {
    withTools {
        register(weatherTool)
    }
}
```

---

## 2. Tool Selection & Semantic Search

**What**: The logic used to decide which tools are relevant for a user's query and should be sent to the LLM.

**Why**: Mobile devices have limited context windows. Sending 100 tool definitions to the LLM for every query is expensive, slow, and can confuse the model. Tool Selection ensures only relevant tools are sent.

### Semantic Tool Selection
**What**: Uses vector embeddings to find tools whose descriptions are mathematically "close" (similar) to the user's query.

**How**:
```kotlin
withToolSelector(
    selector = SemanticToolSelector(
        embeddingModel = myEmbeddingModel,
        embeddingStore = myJsonStore,
        threshold = 0.85f // Only select tools with high relevance (85% similarity)
    )
)
```

---

## 3. Embedding Store (Vector Caching)

**What**: A local cache for vector representations of tools and documents.

**Why**: Generating embeddings costs money and time. Caching them locally allows for instant semantic search and offline tool selection without repeated LLM calls.

**How**:
```kotlin
val myStore = JsonEmbeddingStore(
    initialData = loadFile(),
    onSave = { json -> saveFile(json) }
)

withToolSelector(
    selector = SemanticToolSelector(embeddingModel = model, embeddingStore = myStore)
)
```
