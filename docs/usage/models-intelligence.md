# Models & Intelligence Guide

This guide covers prompt engineering, structured data extraction, and custom model integrations.

---

## 1. Prompts API (Composer)

**What**: A type-safe DSL for building complex, multi-turn prompts with support for system messages, human input, and tool definitions.

**Why**: Manual string concatenation for prompts is error-prone and hard to maintain. The `promptComposer` ensures structure and provides mobile-specific features like token budgeting.

**How**:
```kotlin
val prompt = promptComposer {
    system("You are a specialized expert.")
    human("How do I use this SDK?")
    tools(listOf(weatherTool))
    tokenBudget = TokenBudget(total = 4000)
}.compose()
```

---

## 2. Parsers API (Structured Data)

**What**: A module that extracts type-safe Kotlin objects from raw AI text responses.

**Why**: LLMs return strings, but apps need data. Parsers handle the "messy" parts of AI output (like extra prose or markdown) and turn them into reliable objects.

**How**:
```kotlin
@Serializable
data class Review(val rating: Int, val summary: String)

val parser = structuredOutputParser<Review>()
val output = model.invoke(prompt)
val result = parser.parse(output)

if (result is ParseResult.Success) {
    println(result.value.summary)
}
```

---

## 3. Custom Model Integration (Own Server)

**What**: The ability to plug in any private server or local model into the ADK.

**Why**: Privacy and control. Many enterprises cannot use public LLM APIs. MobileGraph allows you to use your own infrastructure while keeping all the ADK's high-level features.

**How**:
```kotlin
class MyServerModel(val url: String) : ChatModel {
    override suspend fun invoke(...) : ModelOutput {
        // Your Ktor/Network logic here
    }
}

// Use it exactly like a built-in model
withModels { chat("my-server", MyServerModel("...")) }
```
