# Core & Setup Guide

This guide covers the fundamental setup and architectural patterns of the MobileGraph ADK.

---

## 1. Initialization (The DSL)

**What**: The central entry point to configure the ADK's subsystems (Models, Tools, Memory, etc.) using a type-safe Kotlin DSL.

**Why**: Mobile apps require a unified configuration that handles lifecycle and dependency injection. 

**How**:
```kotlin
MobileGraph.initialize(context) {
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
