# MobileGraph 📱🤖

> **The Kotlin Multiplatform AI Framework for Building Intelligent Applications**

[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/docs/multiplatform.html)
[![Android](https://img.shields.io/badge/Android-Supported-3DDC84?logo=android&logoColor=white)]()
[![iOS](https://img.shields.io/badge/iOS-Supported-000000?logo=apple&logoColor=white)]()
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-v0.4.0--alpha-orange.svg)]()

MobileGraph is a **Kotlin Multiplatform AI Framework** that enables developers to build intelligent applications for **Android, iOS, Android Automotive, Android TV, and the broader Kotlin ecosystem**.

It provides a unified, provider-agnostic programming model for integrating **Large Language Models (LLMs)**, **Retrieval-Augmented Generation (RAG)**, **Prompt Engineering**, **Structured Outputs**, **Tool Calling**, **Memory**, **Agentic Workflows**, and **Graph-based Orchestration** into modern Kotlin applications.

Designed with a **mobile-first philosophy**, MobileGraph embraces the realities of mobile computing—application lifecycle changes, process death, intermittent connectivity, constrained resources, and offline-first experiences—while maintaining a clean, strongly typed, and idiomatic Kotlin developer experience.

Rather than being a simple wrapper around AI APIs, MobileGraph provides the runtime infrastructure required to build resilient, scalable, and production-ready AI applications across multiple platforms.

---

# ✨ Inspiration

MobileGraph is inspired by the architectural ideas pioneered by **LangChain** and **LangGraph**, which introduced modular LLM orchestration, retrieval pipelines, tool integration, and stateful workflow execution.

Instead of replicating those frameworks, MobileGraph reimagines these concepts for the Kotlin ecosystem by combining:

- Kotlin Multiplatform
- Strongly typed APIs
- Mobile-first runtime architecture
- Lifecycle-aware execution
- Immutable state management
- Provider-agnostic abstractions
- Native coroutine-based concurrency

Our goal is to provide Kotlin developers with a modern AI framework that feels as natural as **Ktor** for networking and **Jetpack Compose** for UI development while embracing the best architectural ideas from today's AI ecosystem.

MobileGraph is built **with respect for the open-source community**, and we are grateful to the projects whose ideas continue to inspire modern AI development.

# 🚀 Why MobileGraph?

The rapid evolution of Large Language Models has transformed how developers build intelligent applications. Frameworks such as LangChain and LangGraph have demonstrated the power of modular prompts, retrieval pipelines, tools, memory, and agent orchestration.

However, most AI frameworks have been designed primarily for **server-side applications**.

Mobile applications operate in a fundamentally different environment.

Applications can be:

- Suspended or terminated by the operating system
- Sent to the background at any time
- Affected by unstable or intermittent network connectivity
- Constrained by battery, memory, and storage limitations
- Expected to continue providing responsive user experiences even under resource pressure

These challenges require more than simply wrapping an LLM API.

MobileGraph was created to address these challenges by providing a **mobile-first AI runtime** built specifically for the Kotlin ecosystem.

Instead of treating mobile devices as thin clients, MobileGraph provides the infrastructure needed to build resilient, stateful, and production-ready AI applications that feel native to Android, iOS, and Kotlin Multiplatform.

---

## ✨ Core Principles

MobileGraph is built around a small set of architectural principles that guide every module in the framework.

### 📱 Mobile-First Runtime

Unlike traditional AI frameworks, MobileGraph embraces the realities of mobile computing.

The runtime is designed to support:

- Lifecycle-aware execution
- Background processing
- Process recovery
- Checkpoint-ready workflows
- Offline-first architecture
- Resource-conscious execution

---

### 🔄 Provider Agnostic

Applications should not depend on a specific AI provider.

MobileGraph provides a unified abstraction layer that allows developers to integrate multiple AI providers through a consistent API.

Supported providers include:

- OpenAI
- Google Gemini
- Anthropic Claude
- OpenRouter

with additional providers planned for future releases.

Changing providers should require little or no application code changes.

---

### 🧩 Modular by Design

Every capability within MobileGraph is implemented as an independent module.

Developers only include the functionality they need.

Examples include:

- Prompt Engineering
- Structured Output Parsing
- Tool Calling
- Retrieval-Augmented Generation (RAG)
- Memory
- Agent Framework
- Graph Runtime

This modular architecture keeps applications lightweight while remaining highly extensible.

---

### 🔒 Strongly Typed APIs

MobileGraph embraces Kotlin's type system.

Instead of relying on loosely typed maps or dynamic JSON structures, the framework encourages:

- Immutable models
- Type-safe builders
- Explicit contracts
- Compile-time validation
- Clear domain abstractions

This leads to safer, more maintainable applications.

---

### ⚡ Kotlin Multiplatform Native

MobileGraph is built using **Kotlin Multiplatform** from the ground up.

Business logic is shared across platforms while preserving native user experiences.

Supported platforms include:

- Android
- iOS
- Android Automotive
- Android TV
- Desktop (planned)
- JVM applications

---

### 🌊 Coroutine-First Architecture

The framework is built around Kotlin Coroutines and Flow.

This provides:

- Structured concurrency
- Reactive streaming
- Cooperative cancellation
- Efficient asynchronous execution

Developers work with familiar Kotlin APIs without introducing additional reactive frameworks.

---

### 🧠 Agentic by Design

Modern AI applications require more than simple prompt-response interactions.

MobileGraph provides the building blocks for intelligent systems through:

- Tool execution
- Memory
- Retrieval
- Multi-agent collaboration
- Stateful graph execution
- Human-in-the-loop workflows
- Durable execution

These capabilities enable developers to build applications that can reason, plan, and execute complex workflows.

---

## 🎯 Design Goals

MobileGraph aims to provide a developer experience that is:

- Simple enough for building a chatbot in minutes
- Powerful enough for enterprise-scale AI workflows
- Flexible enough to support multiple AI providers
- Efficient enough for resource-constrained mobile devices
- Stable enough for long-term production use
- Idiomatic to Kotlin and Kotlin Multiplatform

The framework is designed to grow with your application—from a single prompt to sophisticated multi-agent systems—without requiring fundamental architectural changes.

---
# 🗺 Roadmap

MobileGraph is being developed in incremental phases, with each phase building on a stable, modular, and extensible architecture.

Our mission is to become the **Kotlin-first AI framework for the Android ecosystem and Kotlin Multiplatform**, enabling developers to build everything from simple AI assistants to enterprise-grade, autonomous agentic applications. [See Roadmap Guide](./docs/ROADMAP.md)    

| Phase      |    Status   | Focus                        |
| ---------- | :---------: | ---------------------------- |
| ✅ Phase 1  |  Completed  | Core Runtime & Lifecycle     |
| ✅ Phase 2  |  Completed  | Knowledge Layer (RAG)        |
| ✅ Phase 3  |  Completed  | Multi-Agent Orchestration    |
| ✅ Phase 4  |  Completed  | Multi-Model Cloud Ecosystem  |
| 🚧 Phase 5 | In Progress | Model Context Protocol (MCP) |
| 🔜 Phase 6 |   Planned   | Local AI & Edge Inference    |
| 🔮 Phase 7 |    Future   | Android Ecosystem            |
| 🚀 Phase 8 |    Vision   | MobileGraph Studio           |

---

We welcome community contributions, ideas, and feedback as we continue building the future of AI development for the Android ecosystem.


## 🚀 Quick Start in 30 Seconds

### 1. Installation

To be published soon into maven. The repo is under alpha testing and active development.

Add the SDK to your project via JitPack:

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // A single dependency for the entire SDK
    implementation("com.github.MobileGraph.mobilegraph:mobilegraph-sdk:0.1.0-alpha01")
}
```

### 2. Initialize the SDK
Configure multiple AI providers and intelligent routing in one type-safe DSL:

```kotlin
val mobileGraph = MobileGraph.initialize(context) {
    withModels {
        // Register multiple brains
        openai(apiKey = "sk-...") { isDefault = true }
        gemini(apiKey = "...")
        claude(apiKey = "...")
        
        // Setup intelligent routing
        router("smart-assistant") {
            policy {
                condition { it.hasImages }
                use("gpt-4o")
            }
            default("claude-3-5-sonnet-20241022")
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
| **Core & Setup**                      | AI Framework Initialization, Interaction Patterns, Model Registry      | [Read Guide](./docs/usage/core-setup.md)                  |
| **Intelligence**                      | Prompt Composer DSL, Structured Parsers, Vision (Vision)               | [Read Guide](./docs/usage/models-intelligence.md)         |
| **Multi-Model**                       | OpenAI, Gemini, Claude, OpenRouter Integration                         | [Read Guide](./docs/usage/multi-model-orchestration.md)   |
| **Agent Framework**                   | Multi-Agent Orchestration, Parallel Execution, Hierarchical            | [Read Guide](./docs/usage/agent-framework.md)             |
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
