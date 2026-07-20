# Core & Setup Guide

This guide covers the fundamental setup and architectural patterns of the MobileGraph SDK.

---

## 1. Prerequisites & Installation

### Environment Checklist
MobileGraph leverages the latest Kotlin Multiplatform features. Ensure your environment meets these requirements:
*   **Kotlin:** 2.0.0+ 
*   **JDK:** 17+
*   **Android SDK:** Min SDK 24, Target SDK 37
*   **Gradle:** 8.4+

### Dependencies
Add the SDK to your `build.gradle.kts`. MobileGraph is **engine-agnostic**, meaning you must provide a Ktor HTTP engine implementation (like OkHttp or Darwin) for networking to work.

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.mobilegraph:mobilegraph-sdk:0.5.0-alpha")
    
    // Choose the engine for your platform
    implementation("io.ktor:ktor-client-okhttp:3.5.1") // Android/JVM
}
```

### Manifest Permissions (Android)
Ensure your `AndroidManifest.xml` includes the Internet permission:
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

---

## 2. Initialization (The DSL)

**What**: The central entry point to configure the ADK's subsystems (Models, Tools, Memory, etc.) using a type-safe Kotlin DSL.

**Why**: Mobile apps require a unified configuration that handles lifecycle and dependency injection. 

**How**:
```kotlin
MobileGraph.initialize() {
    // Configure AI models and their specific behaviors
    withModels {
        chat("gpt-4o", OpenAIChatModel(apiKey = "...")) {
            isDefault = true // Set this as the primary model for the app
            
            // Define default settings for every call to this model
            defaultConfig {
                temperature = 0.7f // Set creativity level (0.0 to 1.0)
                maxTokens = 1024   // Limit response size to save battery
            }
            
            // Add automatic behaviors (Interceptors)
            middleware {
                +LoggingMiddleware()           // Enable performance & prompt logging
                +RetryMiddleware(maxRetries = 3) // Auto-recover from network drops
                +ChatMemoryMiddleware()        // Auto-manage conversation history
            }
        }
    }
    
    // Choose the strategy for conversation persistence
    withMemory { 
        useWindowChatMemory(k = 5) // Keep last 5 turns to balance context vs cost
    }
}
```

---

## 2. Choosing Your Interaction Pattern

MobileGraph provides two primary ways to interact with LLMs. Choosing the right one is essential for correct history management.

### Pattern A: Session-Scoped (`session.model()`)
**What**: State-aware interaction bound to a specific user conversation.
**Why**: Use this for **Conversational UIs** (Chat screens). It automatically manages `sessionId` and retrieves/saves history via `ChatMemoryMiddleware`.

**How**:
```kotlin
// 1. Create a session (optionally specify a model)
val session = mobileGraph.createSession(modelName = "gpt-4o-mini")

// 2. Interact - History and Session ID are handled automatically
val model = session.model()
model.invoke(prompt) 
```

### Pattern B: Global-Scoped (`MobileGraph.models.chat()`)
**What**: Stateless interaction using the global default model.
**Why**: Use this for **Background/Utility Tasks** (e.g., summarizing a notification, translating a single field, or classification) where conversation history is irrelevant.

**How**:
```kotlin
// Interact directly - No session ID or history will be attached
val model = MobileGraph.models.chat()
model.invoke(prompt)
```

---

## 3. Dynamic Model Selection (The Registry)

**What**: A mechanism to retrieve specific models from the ADK's global registry by their registered name or specific capabilities.

**Why**: Most apps have a default model, but complex AI workflows often require "Model Orchestration"—switching between different LLMs based on cost, speed, or capabilities (like Vision).

**How**:
```kotlin
// 1. Access the global registry
val registry = MobileGraph.models.registry()

// 2. Retrieve a model by its unique name
val fastModel = registry.chat("gpt-4o-mini")

// 3. Retrieve a model based on capability (e.g., Image processing)
val visionModel = registry.chatFor(Capability.Vision)

// 4. Use it directly
fastModel?.invoke(prompt)
```

---

## 4. Real-Time Streaming

**What**: The ability to receive AI responses token-by-token as they are generated, rather than waiting for the entire response.

**Why**: Essential for **Mobile UX**. Streaming reduces perceived latency and makes the app feel responsive.

**How (Session-Scoped)**:
Use the `.stream()` extension on a session to maintain history while streaming.
```kotlin
session.stream("Tell me a story").collect { chunk ->
    print(chunk.text) // Update your UI in real-time
}
```

**How (Global/Stateless)**:
Use the `.stream()` method on a model directly for background tasks.
```kotlin
val model = MobileGraph.models.chat()
model.stream(prompt, context = myContext).collect { chunk ->
    // Process chunk
}
```
---

## 5. Custom Model Endpoints

MobileGraph allows you to point to any custom server, whether it's a local Ollama instance or a proprietary corporate endpoint.

### Option A: OpenAI-Compatible (LocalAI, Ollama, vLLM)
If your server follows the OpenAI API standard, you can reuse the `OpenAIChatModel` by providing a custom `baseUrl`.

```kotlin
val env = MobileGraphEnvironment.Builder()
    .withModels {
        // Example: Connecting to a local Ollama instance
        val localModel = OpenAIChatModel(
            name = "llama3", 
            apiKey = "ollama", 
            baseUrl = "http://10.0.2.2:11434/v1"
        )

        chat(localModel) {
            isDefault = true
            defaultConfig {
                temperature = 0.7f
            }
        }
    }
    .build()

```

### Option B: Fully Custom Implementation
For proprietary protocols, implement the `ChatModel` interface directly. This is useful for internal APIs that don't follow the OpenAI spec.

```kotlin
class MyPrivateServerModel(override val name: String = "internal-llm") : ChatModel {
    
    override suspend fun invoke(
        prompt: ChatPromptValue,
        config: ModelConfig?,
        context: ExecutionContext
    ): ModelOutput {
        // 1. Convert MobileGraph prompt to your server's format
        val lastMessage = prompt.messages.last().content
        
        // 2. Perform your network call (using Ktor, Retrofit, etc.)
        val response = "Response from my private server to: $lastMessage"
        
        // 3. Return as a ChatOutput
        return ModelOutput.ChatOutput(AssistantMessage(response))
    }

    override fun stream(
        prompt: ChatPromptValue,
        config: ModelConfig?,
        context: ExecutionContext
    ): Flow<ChatChunk> = flow {
        // Implement streaming logic here if your server supports it
    }

    override fun supports(capability: Capability): Boolean = false
    override fun readModelConfig(): ModelConfig? = null
}

// Usage in registration
val env = MobileGraphEnvironment.Builder()
    .withModels {
        chat(MyPrivateServerModel())
    }
    .build()
```