# MobileGraph: The Agent Development Kit (ADK) 📱🤖

[![KMP](https://img.shields.io/badge/Kotlin-Multiplatform-blue?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)
[![v0.1.0-alpha](https://img.shields.io/badge/version-v0.1.0--alpha-orange.svg)]()

**Kotlin-first framework for building AI applications, RAG systems, and agentic workflows on Android and Kotlin Multiplatform(KMP).**

MobileGraph provides a unified programming model for integrating LLMs, memory, tools, structured outputs, and future agentic workflows into mobile and multiplatform applications.

Designed with a mobile-first mindset, MobileGraph helps developers build resilient AI experiences while remaining provider-agnostic and fully native.

MobileGraph is more than just an SDK for LLMs—it is a comprehensive **Agent Development Kit (ADK)** for Kotlin Multiplatform. It provides the industrial-grade infrastructure needed to run AI agents in the real world: handling process death, managing local memory, and executing complex reasoning graphs natively on mobile devices. **Note: Agent framework under active devlopemnt, code not pushed yet**

---

## ✨ Why an ADK instead of just an SDK?

Most SDKs are simple wrappers around an API. MobileGraph is an **ADK** because it provides the "brain" and "nervous system" for your mobile AI:

*   **Resilient Execution**: Automatic checkpointing. If the OS kills your app during an AI task, MobileGraph resumes exactly where it left off.
*   **Stateful Graphs**: Define complex AI workflows as deterministic state machines.
*   **Native Multiplatform**: 100% Kotlin. No performance-draining bridges or non-native UI overhead.
*   **Provider Agnostic**: Seamlessly switch between OpenAI, Gemini, Anthropic, or local models with a unified capability-based API.
*   **Mobile-First Memory**: Smart sliding-window and summarization strategies that respect mobile battery and context limits.

---

## 🚀 Quick Start in 30 Seconds

### 1. Installation

To be published soon into maven. The repo is under alpha testing and active development. 

<!-- 
Add the core and models dependencies to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.mobilegraph:mobilegraph-core:0.1.0-alpha")
    implementation("io.mobilegraph:mobilegraph-models:0.1.0-alpha")
}
```
-->


### 2. Initialize the ADK
Configure your agent's models and capabilities in one type-safe DSL:

```kotlin
val mobileGraph = MobileGraph.initialize(context) {
    withModels {
        chat("gpt-4o", OpenAIChatModel(apiKey = "sk-...")) {
            isDefault = true
            middleware {
                +LoggingMiddleware()           // Real-time observability
                +RetryMiddleware(maxRetries = 3) // Mobile network resilience
                +ChatMemoryMiddleware()        // Automatic history management
            }
        }
    }
}
```

### 3. Create and Run a Session
Start a resilient, stateful interaction with just a few lines:

```kotlin
val session = mobileGraph.createSession()

// Resilient, streaming interaction
session.model().stream("Plan a 3-day trip to Tokyo").collect { chunk ->
    print(chunk.text)
}
```

---

## 🛠 Project Ecosystem

*   **`mobilegraph-core`**: The foundation. Handles graph execution, lifecycle, and state persistence.
*   **`mobilegraph-models`**: Adapters for major LLM providers and capability-based model orchestration.
*   **`mobilegraph-tools`**: Infrastructure for function calling and semantic tool selection.
*   **`mobilegraph-parsers`**: Type-safe structured data extraction (JSON to Kotlin objects).
*   **`mobilegraph-prompts`**: A type-safe DSL for building token-aware, structured prompts.

---

## 📱 Sample Applications

The project includes a reference implementation for both Android and iOS that demonstrates:
*   **Resilient Chat**: State-aware interaction that survives backgrounding.
*   **Tool Calling**: Real-time weather integration via Kotlin functions.
*   **Multi-Model Orchestration**: Seamless switching between high-power (GPT-4o) and fast (GPT-4o-mini) models.
*   **Local Persistence**: Automated saving and loading of chat history.

### Running the Samples
1.  **Configure API Key**: Add your OpenAI API key to `local.properties` in the root folder:
    ```properties
    open_ai_api=sk-your-key-here
    ```
2.  **Android**: Open the project in Android Studio and run the `:androidApp` configuration.
3.  **iOS**: Open `iosApp/iosApp.xcodeproj` in Xcode (macOS required) and run the app.

---

## 📖 Documentation

Detailed guides for building agentic workflows with MobileGraph:

| Group                                 | Features                                                               | Guide                                                     |
|:--------------------------------------|:-----------------------------------------------------------------------|:----------------------------------------------------------|
| **Core & Setup**                      | ADK Initialization, Interaction Patterns, Model Registry               | [Read Guide](./docs/usage/core-setup.md)                  |
| **Intelligence**                      | Prompt Composer DSL, Structured Parsers, Custom Models                 | [Read Guide](./docs/usage/models-intelligence.md)         |
| **Tools & Agents**                    | Function Calling, Semantic Tool Selection, Vector Caching              | [Read Guide](./docs/usage/tools-agents.md)                |
| **Resilience & State**                | Chat Memory, Sliding Windows, Local-First Sync                         | [Read Guide](./docs/usage/memory-state.md)                |
| **Observability**                     | Middleware Pipeline, Event Streams, Custom Logging                     | [Read Guide](./docs/usage/observability-extensibility.md) |
| **RAG: Document Ingestion**           | Document Ingestion, Vector Embedding, Similarity Search, Event Streams | [Read Guide](./docs/usage/rag-ingestion.md)               |
| **RAG: Retrieval and LLM Generation** | Document Retrieval, RAG Pipeline, Event Streams                        | [Read Guide](./docs/usage/rag-retrieval.md)               |

### Advanced
*   **[Architecture Overview](./docs/ARCHITECTURE.md)**: How MobileGraph handles state and lifecycle.
*   **[ADR (Architectural Decisions)](./docs/adrs)**: The "Why" behind our technical choices.

---

## 🤝 Contributing
Contributions are welcome.

If you'd like to contribute:

1. Fork the repository.
2. Create a feature branch.
3. Add tests for new functionality.
4. Submit a pull request.

Check out our **[CONTRIBUTING.md](CONTRIBUTING.md)** to join us.

## 📄 License
MobileGraph is licensed under the **Apache License 2.0**. See the [LICENSE](LICENSE) file for more info.
