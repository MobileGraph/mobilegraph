# MobileGraph Architecture

## Purpose

This document defines the architecture of MobileGraph.

It serves as the long-term architectural reference for the framework and provides guidance for contributors, maintainers, and AI coding agents.

Implementation details may evolve.

Architectural principles should remain stable.

---

# High-Level Architecture

MobileGraph is a Kotlin Multiplatform AI Application Framework inspired by the architectural principles of LangChain and LangGraph.

The framework is composed of independent modules organized into layers.

Dependencies flow downward only. Reverse dependencies are forbidden.

```mermaid
graph TB
    subgraph APP["📱 Application Layer"]
        AndroidApp["androidApp<br/><i>Jetpack Compose</i>"]
        iOSApp["iosApp<br/><i>SwiftUI</i>"]
        Shared["shared<br/><i>KMP shared code</i>"]
    end

    subgraph SDK["🔷 MobileGraph SDK"]
        Facade["MobileGraph Facade<br/><i>Entry point & DSL</i>"]
    end

    subgraph INTELLIGENCE["🧠 Intelligence Layer"]
        Models["mobilegraph-models"]
        Prompts["mobilegraph-prompts"]
        Parsers["mobilegraph-parsers"]
        Tools["mobilegraph-tools"]
        RAG["mobilegraph-rag"]
        Graph["mobilegraph-graph"]
        Agents["mobilegraph-agents"]
    end

    subgraph KNOWLEDGE["📚 Knowledge Layer"]
        Documents["mobilegraph-documents"]
        Embeddings["mobilegraph-embeddings"]
        Retrieval["mobilegraph-retrieval"]
        VectorStores["mobilegraph-vectorstores"]
    end

    subgraph FOUNDATION["⚙️ Foundation Layer"]
        Core["mobilegraph-core"]
        State["mobilegraph-state"]
        Checkpoint["mobilegraph-checkpoint"]
    end

    AndroidApp --> SDK
    iOSApp --> Shared
    Shared --> SDK

    SDK --> INTELLIGENCE
    SDK --> KNOWLEDGE
    SDK --> FOUNDATION

    INTELLIGENCE --> FOUNDATION
    KNOWLEDGE --> FOUNDATION
    INTELLIGENCE -.->|"RAG uses Retrieval"| KNOWLEDGE

    style APP fill:#1a1a2e,stroke:#e94560,color:#fff
    style SDK fill:#16213e,stroke:#0f3460,color:#fff
    style INTELLIGENCE fill:#0f3460,stroke:#533483,color:#fff
    style KNOWLEDGE fill:#533483,stroke:#e94560,color:#fff
    style FOUNDATION fill:#2a2a4a,stroke:#0f3460,color:#fff
```

![High-Level Architecture](High-Level%20Architecture.png)

---

# Architectural Principles

The architecture is guided by the following principles:

* **Strong typing first** — Leverage Kotlin's type system to catch errors at compile time.
* **Provider agnostic design** — Application code should never depend on specific model providers.
* **Composition over inheritance** — Assemble behavior from small interfaces and middleware.
* **Explicit over magic** — Prefer explicit DSL configuration over convention-based auto-discovery.
* **Observability by default** — Every operation emits events through `EventPublisher`.
* **Mobile-first execution** — Respect lifecycle, memory, and battery constraints.
* **Lifecycle awareness** — Execution must adapt to foreground, background, suspended, and restored states.
* **Progressive complexity** — Simple use-cases should require minimal code; advanced patterns are opt-in.
* **Backward compatibility** — Public API surfaces must remain stable across minor versions.

---

# Layered Architecture

## Layer 1 — Foundation

Provides runtime infrastructure. No module in this layer may depend on higher layers.

### Modules

| Module | Responsibility | Key Types |
|---|---|---|
| `mobilegraph-core` | Runtime kernel, sessions, events, middleware, execution context, DI | `MobileGraph`, `MobileGraphSession`, `ExecutionContext`, `Middleware<I,O>`, `EventPublisher`, `MobileGraphEvent` |
| `mobilegraph-state` | Graph state container | `GraphState` (mutable key-value map) |
| `mobilegraph-checkpoint` | Snapshot and restore graph state | `CheckpointStore`, `InMemoryCheckpointStore` |

### Core Internal Architecture

```mermaid
graph LR
    subgraph mobilegraph-core
        direction TB
        MG["MobileGraph<br/><i>Singleton facade</i>"]
        ENV["MobileGraphEnvironment<br/><i>Immutable DI container</i>"]
        RT["MobileGraphRuntime<br/><i>Internal kernel</i>"]
        SESS["MobileGraphSession<br/><i>Isolated interaction unit</i>"]

        MG --> ENV
        MG --> RT
        RT --> SESS

        subgraph Cross-Cutting
            MW["Middleware&lt;I,O&gt;"]
            EVT["EventPublisher /<br/>MobileGraphEvent"]
            CTX["ExecutionContext<br/><i>TraceId, SessionId, RequestId</i>"]
            MEM["ChatMemory"]
            TOOL_I["Tool / ToolRegistry /<br/>ToolSelector"]
            REG["ComponentProvider<br/><i>Registry pattern</i>"]
        end

        RT --> EVT
        SESS --> CTX
        ENV --> REG
    end

    subgraph mobilegraph-state
        GS["GraphState<br/><i>Mutable key-value map</i>"]
    end

    subgraph mobilegraph-checkpoint
        CKP["CheckpointStore<br/><i>Snapshot & restore</i>"]
        IMCK["InMemoryCheckpointStore"]
        CKP --> IMCK
    end

    style MG fill:#e94560,stroke:#fff,color:#fff
    style ENV fill:#c73659,stroke:#fff,color:#fff
    style RT fill:#c73659,stroke:#fff,color:#fff
    style SESS fill:#c73659,stroke:#fff,color:#fff
```

### Responsibilities

* Execution context and propagation
* Cancellation (via `CancellationToken`)
* Middleware pipeline
* Typed exceptions (`MobileGraphException`)
* Lifecycle state management
* Capability system
* Tracing and observability
* Session isolation

---

## Layer 2 — Knowledge

Provides data ingestion, storage, and retrieval capabilities. Knowledge modules may depend only on Foundation modules.

### Modules

| Module | Responsibility | Key Types |
|---|---|---|
| `mobilegraph-documents` | Document loading and text splitting | `Document`, `DocumentLoader`, `TextSplitter` |
| `mobilegraph-embeddings` | Model-agnostic embedding facade | Embedding access layer |
| `mobilegraph-vectorstores` | Vector persistence and similarity search | `VectorStore`, `SQLiteVectorStore`, `InMemoryVectorStore`, `VectorStoreRegistry` |
| `mobilegraph-retrieval` | Multi-strategy retrieval | `Retriever`, `BM25Retriever`, `HybridRetriever`, `MultiQueryRetriever`, `VectorStoreRetriever` |

### Knowledge Layer Architecture

```mermaid
graph TD
    subgraph mobilegraph-documents
        DOC["Document<br/><i>text + metadata + docId</i>"]
        DL["DocumentLoader"]
        TS["TextSplitter<br/><i>Recursive / Character</i>"]
        subgraph Loaders
            TXT_L["TextDocumentLoader"]
            WEB_L["WebDocumentLoader<br/><i>JSoup-based</i>"]
            PDF_L["PdfDocumentLoader<br/><i>PDFBox</i>"]
            STRUCT_L["StructuredDocumentLoader<br/><i>CSV, JSON</i>"]
        end
        DL --> TXT_L & WEB_L & PDF_L & STRUCT_L
    end

    subgraph mobilegraph-embeddings
        EMB_F["Embedding Facade<br/><i>Model-agnostic</i>"]
    end

    subgraph mobilegraph-vectorstores
        VS["VectorStore<br/><i>Interface</i>"]
        IMVS["InMemoryVectorStore"]
        SQVS["SQLiteVectorStore<br/><i>SQLDelight + cosine sim</i>"]
        VSR["VectorStoreRegistry"]
        VS --> IMVS & SQVS
    end

    subgraph mobilegraph-retrieval
        RET["Retriever<br/><i>Interface</i>"]
        VSR2["VectorStoreRetriever"]
        BM25["BM25Retriever<br/><i>TF-IDF keyword</i>"]
        HYB["HybridRetriever<br/><i>Fusion of multiple</i>"]
        MQR["MultiQueryRetriever<br/><i>LLM-expanded queries</i>"]
        RR["RetrieverRegistry"]
        RET --> VSR2 & BM25 & HYB & MQR
    end

    DL -->|"load"| DOC
    DOC -->|"split"| TS
    TS -->|"chunks"| VS
    VS -->|"embed & store"| EMB_F
    VSR2 --> VS
    HYB --> VSR2 & BM25

    style DOC fill:#533483,stroke:#fff,color:#fff
    style VS fill:#533483,stroke:#fff,color:#fff
    style RET fill:#533483,stroke:#fff,color:#fff
```

![Retrieval Architecture](retrieval_architecture.png)

---

## Layer 3 — Intelligence

Provides model interaction, prompt engineering, output parsing, tool calling, and RAG orchestration. Intelligence modules may depend on Foundation and Knowledge layers.

### Modules

| Module | Responsibility | Key Types |
|---|---|---|
| `mobilegraph-models` | LLM abstraction + 7 provider integrations + middleware implementations | `ChatModel`, `StreamingChatModel`, `EmbeddingModel`, `ModelRegistry`, `ModelRouter` |
| `mobilegraph-prompts` | Prompt templates and composition DSL | `PromptTemplate`, `PromptComposer`, `PromptRegistry` |
| `mobilegraph-parsers` | Structured output parsing | `Parser<T>`, `StructuredOutputParser`, `ParseResult` (Success / Failure / Partial) |
| `mobilegraph-tools` | Tool registry and smart selection | `SemanticToolSelector`, `AllToolSelector`, `JsonEmbeddingStore` |
| `mobilegraph-rag` | RAG pipeline orchestration | `RagPipeline`, `SimpleRagPipeline`, `ContextBasedPromptBuilder` |

### Models Module Architecture

```mermaid
graph TB
    subgraph Interfaces
        Model["Model"]
        CM["ChatModel"]
        SCM["StreamingChatModel"]
        EM["EmbeddingModel"]
        Model --> CM
        Model --> EM
        CM --> SCM
    end

    subgraph Providers
        OAI["OpenAI<br/><i>gpt-4o, gpt-4o-mini</i>"]
        ANT["Anthropic<br/><i>Claude</i>"]
        GEM["Google<br/><i>Gemini</i>"]
        DS["DeepSeek"]
        HF["HuggingFace"]
        OR["OpenRouter"]
        MP["MediaPipe<br/><i>On-device TFLite</i>"]
    end

    subgraph Middleware
        LOG["LoggingMiddleware"]
        RETRY["RetryMiddleware"]
        TOOL_MW["ToolSelectionMiddleware"]
        CHAT_MEM["ChatMemoryMiddleware"]
        MWC["MiddlewareChatModel<br/><i>Pipeline executor</i>"]
    end

    subgraph Infrastructure
        REG_M["ModelRegistry<br/><i>Named model lookup</i>"]
        ROUTE["ModelRouter<br/><i>Dynamic model selection</i>"]
        HIST["ChatMessageHistory<br/><i>Buffer / Summary buffer</i>"]
    end

    CM --> OAI & ANT & GEM & DS & HF & OR
    EM --> MP
    MWC --> LOG & RETRY & TOOL_MW & CHAT_MEM

    style OAI fill:#0f3460,stroke:#fff,color:#fff
    style ANT fill:#0f3460,stroke:#fff,color:#fff
    style GEM fill:#0f3460,stroke:#fff,color:#fff
    style MP fill:#533483,stroke:#fff,color:#fff
```

### Prompts, Parsers & Tools

```mermaid
graph LR
    subgraph mobilegraph-prompts
        PT["PromptTemplate<br/><i>Variable substitution</i>"]
        PC["PromptComposer<br/><i>DSL for system/human/ai</i>"]
        PR["PromptRegistry"]
    end

    subgraph mobilegraph-parsers
        PARSE["Parser&lt;T&gt;"]
        JSON_P["JsonOutputParser"]
        STRUCT["StructuredOutputParser&lt;T&gt;<br/><i>Kotlinx Serialization</i>"]
        PRESULT["ParseResult<br/><i>Success / Failure / Partial</i>"]
    end

    subgraph mobilegraph-tools
        SEL["ToolSelector"]
        ALL_S["AllToolSelector"]
        SEM_S["SemanticToolSelector<br/><i>Embedding-based</i>"]
        ES["JsonEmbeddingStore<br/><i>Persistent cache</i>"]
    end

    SEM_S --> ES

    style PC fill:#0f3460,stroke:#fff,color:#fff
    style STRUCT fill:#0f3460,stroke:#fff,color:#fff
    style SEM_S fill:#0f3460,stroke:#fff,color:#fff
```

---

## Layer 4 — Agent Runtime

Provides orchestration capabilities. Agent modules may depend on all lower layers.

### Modules

| Module | Responsibility | Key Types |
|---|---|---|
| `mobilegraph-graph` | Directed state graph execution engine | `StateGraph`, `GraphNode`, `GraphEdge`, `ExecutionEngine`, `HumanReviewNode`, `JoinNode`, `EndNode` |
| `mobilegraph-agents` | Agentic loop combining model + tools + graph | `Agent`, `AgentNode`, `AgentRuntime`, `DefaultAgentRuntime` |

### Graph Engine & Agents Architecture

```mermaid
graph TD
    subgraph mobilegraph-graph
        SG["StateGraph<br/><i>Directed graph definition</i>"]
        GN["GraphNode"]
        GE["GraphEdge"]
        EE["ExecutionEngine<br/><i>Walks the graph</i>"]
        END_N["EndNode"]
        JOIN["JoinNode<br/><i>Parallel branches</i>"]
        HRN["HumanReviewNode<br/><i>Pause for approval</i>"]
        ER["ExecutionResult<br/><i>Success / AwaitingReview / Error</i>"]
        SGB["StateGraphBuilder<br/><i>DSL</i>"]
    end

    subgraph mobilegraph-agents
        AGT["Agent<br/><i>Interface</i>"]
        AGTN["AgentNode<br/><i>GraphNode wrapper</i>"]
        AGTR["AgentRuntime<br/><i>Orchestrator</i>"]
    end

    AGT --> AGTN
    AGTN --> EE
    AGTR --> SG
    SG --> GN & GE
    EE --> ER

    style SG fill:#0f3460,stroke:#fff,color:#fff
    style AGT fill:#0f3460,stroke:#fff,color:#fff
    style AGTR fill:#0f3460,stroke:#fff,color:#fff
```

![Graph Architecture](graph_architecture.png)

![Agent Architecture](agent_architecture.png)

---

## Layer 5 — Application / UI Layer

Platform-specific applications that consume the SDK.

| Module | Platform | Technology |
|---|---|---|
| `androidApp` | Android | Jetpack Compose |
| `iosApp` | iOS | SwiftUI |
| `shared` | Cross-platform | KMP shared module |

UI modules may depend on all lower layers.

---

# Module Dependency Graph

This diagram shows the actual Gradle dependency edges between all modules (derived from `build.gradle.kts` files).

```mermaid
graph TD
    subgraph Foundation
        core["mobilegraph-core"]
        state["mobilegraph-state"]
        checkpoint["mobilegraph-checkpoint"]
    end

    subgraph Intelligence
        models["mobilegraph-models"]
        prompts["mobilegraph-prompts"]
        parsers["mobilegraph-parsers"]
        tools["mobilegraph-tools"]
        rag["mobilegraph-rag"]
        mgraph["mobilegraph-graph"]
        agents["mobilegraph-agents"]
    end

    subgraph Knowledge
        docs["mobilegraph-documents"]
        embed["mobilegraph-embeddings"]
        retrieval["mobilegraph-retrieval"]
        vstore["mobilegraph-vectorstores"]
    end

    %% Intelligence → Foundation
    models --> core
    tools --> core
    tools --> models
    prompts --> core
    prompts --> models
    prompts --> tools
    parsers --> core
    parsers --> models
    parsers --> prompts
    mgraph --> core
    mgraph --> state
    mgraph --> checkpoint
    agents --> core
    agents --> state
    agents --> checkpoint
    agents --> mgraph
    agents --> models
    agents --> tools
    agents --> prompts

    %% Knowledge → Foundation + Intelligence
    docs --> core
    embed --> core
    embed --> models
    vstore --> docs
    vstore --> embed
    retrieval --> docs
    retrieval --> vstore
    retrieval --> parsers

    %% RAG bridges Intelligence + Knowledge
    rag --> retrieval
    rag --> models
    rag --> prompts

    style core fill:#e94560,stroke:#fff,color:#fff
    style state fill:#e94560,stroke:#fff,color:#fff
    style checkpoint fill:#e94560,stroke:#fff,color:#fff
    style models fill:#0f3460,stroke:#fff,color:#fff
    style prompts fill:#0f3460,stroke:#fff,color:#fff
    style parsers fill:#0f3460,stroke:#fff,color:#fff
    style tools fill:#0f3460,stroke:#fff,color:#fff
    style rag fill:#0f3460,stroke:#fff,color:#fff
    style mgraph fill:#0f3460,stroke:#fff,color:#fff
    style agents fill:#0f3460,stroke:#fff,color:#fff
    style docs fill:#533483,stroke:#fff,color:#fff
    style embed fill:#533483,stroke:#fff,color:#fff
    style retrieval fill:#533483,stroke:#fff,color:#fff
    style vstore fill:#533483,stroke:#fff,color:#fff
```

### Dependency Rules

**Allowed:**

* Agent Runtime → Intelligence, Knowledge, Foundation
* Intelligence → Knowledge, Foundation
* Knowledge → Foundation
* RAG → Retrieval (bridge between Intelligence and Knowledge)

**Forbidden:**

* Foundation → any higher layer
* Knowledge → Intelligence (except via embeddings → models)
* Circular dependencies of any kind

---

# Runtime Architecture

Execution is based on:

* **Kotlinx Coroutines** — structured concurrency for all async operations
* **Flow<T>** — the only streaming primitive (no callbacks or observer APIs)
* **SupervisorJob** — fault isolation between sessions

```mermaid
graph LR
    REQ["ChatRequest"]
    LOG["1. LoggingMiddleware<br/><i>Traces requests</i>"]
    RETRY["2. RetryMiddleware<br/><i>Exponential backoff</i>"]
    TOOL["3. ToolSelectionMiddleware<br/><i>Selects relevant tools</i>"]
    MEM["4. ChatMemoryMiddleware<br/><i>Injects history</i>"]
    MODEL["ChatModel.invoke()"]
    RESP["ModelOutput"]

    REQ --> LOG --> RETRY --> TOOL --> MEM --> MODEL --> RESP

    style REQ fill:#1a1a2e,stroke:#e94560,color:#fff
    style RESP fill:#1a1a2e,stroke:#e94560,color:#fff
    style MODEL fill:#0f3460,stroke:#fff,color:#fff
```

![Runtime Architecture](run_time_architecture.png)

---

# ExecutionContext

Every operation receives an `ExecutionContext`.

ExecutionContext carries:

| Field | Type | Purpose |
|---|---|---|
| `traceId` | `TraceId` | End-to-end trace correlation |
| `sessionId` | `SessionId` | Session isolation |
| `requestId` | `RequestId` | Per-request identification |
| `metadata` | `Metadata` | Extensible key-value metadata |
| `locale` | `String?` | User locale for localized responses |
| `deadline` | `Long?` | Timeout deadline |
| `cancellationToken` | `CancellationToken` | Cooperative cancellation |

ExecutionContext propagates automatically across layers. Components must enrich existing context rather than create new contexts.

---

# RAG Pipeline — End-to-End Flow

```mermaid
sequenceDiagram
    participant User
    participant Session as MobileGraphSession
    participant RAG as RagPipeline
    participant Ret as Retriever
    participant VS as VectorStore
    participant EM as EmbeddingModel
    participant PB as ContextPromptBuilder
    participant LLM as ChatModel
    participant Evt as EventPublisher

    User->>Session: chat("What is X?")
    Session->>RAG: execute(query)
    RAG->>Ret: retrieve(query)
    Ret->>EM: embed(query)
    EM-->>Ret: queryVector
    Ret->>VS: similaritySearch(queryVector)
    VS-->>Ret: relevant Documents
    Ret-->>RAG: RetrievalResult
    RAG->>Evt: RetrievalCompleted
    RAG->>PB: buildContextAndPrompt(docs, query)
    PB-->>RAG: augmented prompt
    RAG->>LLM: invoke(prompt)
    LLM-->>RAG: ModelOutput
    RAG->>Evt: RagResponseGenerated
    RAG-->>Session: ModelOutput
    Session-->>User: AI response grounded in context
```

---

# Capability System

Provider-specific behavior is abstracted using capabilities.

Examples:

```kotlin
Capability.Streaming
Capability.FunctionCalling
Capability.StructuredOutput
Capability.Vision
```

Application code should never rely on provider implementations.

Avoid:

```kotlin
if (model is OpenAIChatModel) // ❌
```

Prefer:

```kotlin
model.supports(Capability.FunctionCalling) // ✅
```

---

# Middleware Architecture

Cross-cutting concerns are implemented using the `Middleware<I, O>` interface:

```kotlin
interface Middleware<I, O> {
    suspend fun intercept(
        input: I,
        context: ExecutionContext,
        next: suspend (I, ExecutionContext) -> O,
    ): O
}
```

**Built-in middleware:**

| Middleware | Purpose |
|---|---|
| `LoggingMiddleware` | Traces requests and responses |
| `RetryMiddleware` | Exponential backoff with configurable retries |
| `ToolSelectionMiddleware` | Selects relevant tools for the request |
| `ChatMemoryMiddleware` | Injects conversation history into prompts |

Middleware order must be deterministic. Middleware must not change business semantics.

---

# State Architecture

State is:

* Immutable (nodes produce new state, never mutate)
* Serializable
* Replayable
* Versioned

```text
State S  +  Node N  →  State S'
```

State transitions are observable. Checkpointing and recovery are first-class capabilities.

---

# Error Model

Errors are typed. Avoid generic exceptions.

```text
MobileGraphException
├── ConfigurationException
├── ExecutionException
├── TimeoutException
└── CancellationException
```

Recoverable operations should prefer `Result<T>`.

---

# Lifecycle Awareness

MobileGraph is mobile-first. Execution must adapt to:

| State | Behavior |
|---|---|
| Foreground | Full execution |
| Background | Reduced priority, respect battery |
| Offline | On-device models only (e.g., MediaPipe) |
| Suspended | Checkpoint and pause |
| Killed | Clean resource release |
| Restored | Resume from checkpoint |

Lifecycle events are runtime events, not UI events.

---

# Platform Architecture

```mermaid
graph TB
    subgraph KMP["Kotlin Multiplatform"]
        COMMON["commonMain<br/><i>All SDK logic</i>"]
        ANDROID["androidMain<br/><i>MediaPipe, SQLDelight Android</i>"]
        IOS["iosMain<br/><i>SQLDelight Native driver</i>"]
        JVM["jvmMain<br/><i>SQLDelight JDBC, tests</i>"]
    end

    subgraph External
        KTOR["Ktor Client<br/><i>HTTP for model APIs</i>"]
        SERIAL["Kotlinx Serialization<br/><i>JSON parsing</i>"]
        COROUTINES["Kotlinx Coroutines<br/><i>Async runtime</i>"]
        SQLDELIGHT["SQLDelight<br/><i>Vector persistence</i>"]
        MEDIAPIPE["MediaPipe Tasks<br/><i>On-device embeddings</i>"]
    end

    COMMON --> KTOR & SERIAL & COROUTINES
    ANDROID --> MEDIAPIPE & SQLDELIGHT
    IOS --> SQLDELIGHT
    JVM --> SQLDELIGHT

    style KMP fill:#16213e,stroke:#0f3460,color:#fff
    style COMMON fill:#0f3460,stroke:#fff,color:#fff
```

---

# Extension Model

MobileGraph supports extension through interfaces:

* `ChatModel` / `StreamingChatModel` — custom model providers
* `EmbeddingModel` — custom embedding providers
* `Tool<I, O>` — custom tool implementations
* `Retriever` — custom retrieval strategies
* `VectorStore` — custom vector storage backends
* `Middleware<I, O>` — custom cross-cutting concerns
* `DocumentLoader` — custom document ingestion
* `Agent` — custom agentic workflows

Reflection-based plugins are discouraged. Prefer explicit registration via DSL.

---

# Key Design Patterns

| Pattern | Where | Purpose |
|---|---|---|
| **Singleton Facade** | `MobileGraph` | Single entry point, DSL initialization |
| **Builder / DSL** | `MobileGraphEnvironment.Builder`, `withModels {}`, `withTools {}` | Fluent configuration |
| **Session Isolation** | `MobileGraphSession` | Each session has its own history, model binding, and event stream |
| **Middleware Chain** | `Middleware<I,O>`, `MiddlewareChatModel` | Cross-cutting concerns (logging, retry, memory, tool selection) |
| **Registry** | `ModelRegistry`, `ToolRegistry`, `RetrieverRegistry`, `VectorStoreRegistry` | Named component lookup with defaults |
| **Strategy** | `Retriever`, `ToolSelector`, `ChatModel`, `EmbeddingModel` | Swappable implementations behind interfaces |
| **Event Bus** | `EventPublisher` → `SharedFlow<MobileGraphEvent>` | Decoupled observability |
| **State Graph** | `StateGraph`, `GraphNode`, `GraphEdge` | LangGraph-style directed workflow execution |
| **Component Provider** | `MobileGraphEnvironment.getComponent<T>()` | Type-safe service locator |

---

# Module Independence

Modules should:

* Have a single responsibility
* Expose stable APIs
* Hide implementation details

Modules should evolve independently.

---

# Architectural Laws

1. Dependencies flow downward.
2. `Flow<T>` is the only streaming primitive.
3. `ExecutionContext` is universal.
4. Capabilities replace provider checks.
5. State is immutable.
6. Middleware order is deterministic.
7. Errors are typed.
8. Execution must be resumable.
9. Observability must be passive.
10. Core must remain dependency-free.

---

# Module Summary Table

| Layer | Module | Responsibility | Key Types |
|---|---|---|---|
| **Foundation** | `mobilegraph-core` | Runtime, sessions, events, middleware, context, DI | `MobileGraph`, `MobileGraphSession`, `ExecutionContext`, `Middleware` |
| | `mobilegraph-state` | Graph state container | `GraphState` |
| | `mobilegraph-checkpoint` | Snapshot & restore graph state | `CheckpointStore`, `InMemoryCheckpointStore` |
| **Intelligence** | `mobilegraph-models` | Model abstraction + 7 providers + middleware | `ChatModel`, `EmbeddingModel`, `ModelRegistry`, `RetryMiddleware` |
| | `mobilegraph-prompts` | Prompt templates, composition DSL | `PromptTemplate`, `PromptComposer`, `PromptRegistry` |
| | `mobilegraph-parsers` | Structured output parsing | `Parser<T>`, `StructuredOutputParser`, `ParseResult` |
| | `mobilegraph-tools` | Tool registry & smart selection | `SemanticToolSelector`, `AllToolSelector`, `JsonEmbeddingStore` |
| | `mobilegraph-rag` | RAG pipeline orchestration | `RagPipeline`, `SimpleRagPipeline`, `ContextBasedPromptBuilder` |
| | `mobilegraph-graph` | Directed state graph engine | `StateGraph`, `GraphNode`, `ExecutionEngine`, `HumanReviewNode` |
| | `mobilegraph-agents` | Agentic loop (model + tools + graph) | `Agent`, `AgentNode`, `AgentRuntime` |
| **Knowledge** | `mobilegraph-documents` | Document loading & text splitting | `Document`, `TextSplitter`, `WebDocumentLoader`, `PdfDocumentLoader` |
| | `mobilegraph-embeddings` | Embedding facade | Model-agnostic embedding access |
| | `mobilegraph-vectorstores` | Vector persistence & similarity search | `VectorStore`, `SQLiteVectorStore`, `InMemoryVectorStore` |
| | `mobilegraph-retrieval` | Multi-strategy retrieval | `Retriever`, `BM25Retriever`, `HybridRetriever`, `MultiQueryRetriever` |

---

# Long-Term Vision

MobileGraph aims to provide a unified runtime across:

* Android
* iOS
* Edge devices

allowing workflows and agents to be reused across environments.

Implementation may change. Architectural principles should endure.

---

# Appendix — Static Architecture Diagrams

![Dataflow Architecture](dataflow_architecture.png)

![Security Architecture](security_architecture.png)