# MobileGraph: The Agent Development Kit (ADK) 📱🤖

[![KMP](https://img.shields.io/badge/Kotlin-Multiplatform-blue?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)
[![v0.1.0-alpha](https://img.shields.io/badge/version-v0.1.0--alpha-orange.svg)]()

**Kotlin-first AI framework for Android, iOS, Android Automotive, Android TV, and Kotlin Multiplatform—bringing LLMs, RAG, agents, MCP, and on-device AI into a unified developer experience.**

MobileGraph provides a unified programming model for integrating LLMs, memory, tools, structured outputs, and future agentic workflows into mobile and multiplatform applications.

Designed with a mobile-first mindset, MobileGraph helps developers build resilient AI experiences while remaining provider-agnostic and fully native.

MobileGraph is more than just an SDK for LLMs—it is a comprehensive **Agent Development Kit (ADK)** for Kotlin Multiplatform. It provides the industrial-grade infrastructure needed to run AI agents in the real world: handling process death, managing local memory, and executing complex reasoning graphs natively on mobile devices.

---

## ✨ Why an ADK instead of just an SDK?

Most SDKs are simple wrappers around an API. MobileGraph is an **ADK** because it provides the "brain" and "nervous system" for your mobile AI:

*   **Resilient Execution**: Automatic checkpointing. If the OS kills your app during an AI task, MobileGraph resumes exactly where it left off.
*   **Stateful Graphs**: Define complex AI workflows as deterministic state machines.
*   **Native Multiplatform**: 100% Kotlin. No performance-draining bridges or non-native UI overhead.
*   **Provider Agnostic**: Seamlessly switch between OpenAI, Gemini, Anthropic, or local models with a unified capability-based API.
*   **Mobile-First Memory**: Smart sliding-window and summarization strategies that respect mobile battery and context limits.

---
# 🗺 Roadmap

MobileGraph is being developed in incremental phases, with each phase building on a stable, modular, and extensible architecture.

Our mission is to become the **Kotlin-first AI framework for the Android ecosystem and Kotlin Multiplatform**, enabling developers to build everything from simple AI assistants to enterprise-grade, autonomous agentic applications. [See Roadmap Guide](./docs/ROADMAP.md)    

| Phase      |    Status   | Focus                        |
| ---------- | :---------: | ---------------------------- |
| ✅ Phase 1  |  Completed  | Core Runtime                 |
| ✅ Phase 2  |  Completed  | Knowledge Layer (RAG)        |
| ✅ Phase 3  |  Completed  | Agent Runtime                |
| 🚧 Phase 4 | In Progress | Model Ecosystem              |
| 🔜 Phase 5 |   Planned   | Model Context Protocol (MCP) |
| 🔜 Phase 6 |   Planned   | Local AI & Edge Inference    |
| 🔮 Phase 7 |    Future   | Android Ecosystem            |
| 🚀 Phase 8 |    Vision   | MobileGraph Studio           |

---

We welcome community contributions, ideas, and feedback as we continue building the future of AI development for the Android ecosystem.


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
### 4. Create and Run a Resilient Agent
Define and execute complex workflows with just a few lines:
```kotlin
    val agent = MyResearchAgent(mobileGraph.models.chat())
    val workflow = stateGraph {
        start("research")
        node(AgentNode("research", agent, runtime))
        node(EndNode("finish"))
        edge("research", "finish")
    }
    
// Run the agentic workflow with automatic checkpointing
    val result = runtime.run(workflow, initialState)
```

## 🛠 Project Ecosystem

### Foundation
*   **`mobilegraph-core`**: The backbone. Handles execution context, lifecycle, events, and component registry.
*   **`mobilegraph-checkpoint`**: Infrastructure for state persistence and durable execution.
*   **`mobilegraph-state`**: Core interfaces for immutable graph state and variable management.

### Intelligence & Agents
*   **`mobilegraph-agents`**: Orchestration logic for multi-agent workflows, parallel execution, and hierarchical delegation.
*   **`mobilegraph-graph`**: Graph-based state machine engine with support for Fan-out/in and breakpoints.
*   **`mobilegraph-models`**: Adapters for major LLM providers (OpenAI, Gemini, etc.) and capability orchestration.
*   **`mobilegraph-tools`**: Infrastructure for function calling, tool registration, and semantic tool selection.
*   **`mobilegraph-parsers`**: Type-safe structured data extraction (JSON-to-Kotlin objects) with prose extraction.
*   **`mobilegraph-prompts`**: A type-safe DSL for building token-aware, structured prompts.
*   **`mobilegraph-rag`**: High-level orchestration for retrieval-augmented generation pipelines.

### Knowledge & RAG
*   **`mobilegraph-documents`**: Document ingestion and processing (PDF, Text, Markdown).
*   **`mobilegraph-embeddings`**: Interfaces and adapters for vector embedding models.
*   **`mobilegraph-vectorstores`**: On-device and remote vector storage (e.g., SQLite-based vector search).
*   **`mobilegraph-retrieval`**: Semantic search logic and context retrieval strategies.

---

## 📱 Sample Applications

The project includes a comprehensive reference implementation for Android that demonstrates the following core patterns:

*   **Resilient Chat**: Basic state-aware interaction that survives backgrounding and process death.
*   **RAG (Retrieval-Augmented Generation)**: On-device PDF ingestion, vector embedding, and semantic retrieval.
*   **Autonomous Tool Agents**: LLMs that can independently decide when and how to call local Kotlin functions (e.g., Weather, Calculator).
*   **Human-in-the-Loop (HITL)**: Workflows that pause for manual approval or feedback before proceeding.
*   **Parallel Execution (Fan-out/in)**: Running multiple agents simultaneously (e.g., a Researcher and a Poet) and merging their results.
*   **Hierarchical Sub-Agents**: Complex "Manager-Worker" orchestrations where agents manage their own internal sub-graphs.

### Running the Samples
1.  **Configure API Key**: Add your OpenAI API key to `local.properties` in the root folder:
    ```properties
    open_ai_api=sk-your-key-here
    ```
2.  **Android**: Open the project in Android Studio and run the `:androidApp` configuration. The **Master Screen** provides a dashboard to launch each of these specific demos.
3.  **iOS**: To be added soon.

---

## 📖 Documentation

Detailed guides for building agentic workflows with MobileGraph:

| Group                                 | Features                                                               | Guide                                                     |
|:--------------------------------------|:-----------------------------------------------------------------------|:----------------------------------------------------------|
| **Core & Setup**                      | ADK Initialization, Interaction Patterns, Model Registry               | [Read Guide](./docs/usage/core-setup.md)                  |
| **Intelligence**                      | Prompt Composer DSL, Structured Parsers, Custom Models                 | [Read Guide](./docs/usage/models-intelligence.md)         |
| **Agent Framework**                   | Multi-Agent Orchestration, Parallel Execution, Hierarchical, HITL      | [Read Guide](./docs/usage/agent-framework.md)             |
| **Persistence & State**               | Graph State, Checkpointing, Durable Execution, Resumption              | [Read Guide](./docs/usage/agent-framework.md)             |
| **Tools & Agents**                    | Function Calling, Semantic Tool Selection, Vector Caching              | [Read Guide](./docs/usage/tools-agents.md)                |
| **Resilience & Memory**               | Chat Memory, Sliding Windows, Local-First Sync                         | [Read Guide](./docs/usage/memory-state.md)                |
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
