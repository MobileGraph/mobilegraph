# 🗺 Roadmap

MobileGraph is being developed in incremental phases, with each phase building on a stable, modular, and extensible architecture.

Our mission is to become the **Kotlin-first AI framework for the Android ecosystem and Kotlin Multiplatform including IOS**, enabling developers to build everything from simple AI assistants to enterprise-grade, autonomous agentic applications.

| Phase      |  Status   | Focus                                  |
| ---------- |:---------:| -------------------------------------- |
| ✅ Phase 1  | Completed | Core Runtime & Provider Abstraction    |
| ✅ Phase 2  | Completed | Knowledge Layer (RAG)                  |
| ✅ Phase 3  | Completed | Agent Runtime (Durable Workflows)      |
| ✅ Phase 4  | Completed | Multi-Model Cloud Ecosystem            |
| ✅ Phase 5  | Completed | Model Context Protocol (MCP)           |
| ✅ Phase 6  | Completed | Agent-to-Agent (A2A) Protocol          |
| 🔜 Phase 7  |  Planned  | Local AI & Edge Inference              |
| 🔮 Phase 8  |  Future   | Android & Automotive Ecosystem         |
| 🚀 Phase 9  |  Vision   | MobileGraph Studio (Visual Builder)    |

---

## ✅ Phase 1 — Core Runtime
Foundation for AI-powered applications. Abstraction layer for chat models, session management, middleware, and structured outputs.

## ✅ Phase 2 — Knowledge Layer (RAG)
Enable Retrieval-Augmented Generation. Includes document loaders, text splitters, vector store abstractions, and a retrieval DSL.

## ✅ Phase 3 — Agent Runtime
Durable, mobile-first agent execution engine. Support for stateful graphs, autonomous tool use, Human-in-the-loop (HITL), and automatic process death recovery.

## ✅ Phase 4 — Model Ecosystem
Expanded cloud provider suite. Native integration for OpenAI, Gemini, Claude, DeepSeek, Hugging Face, and OpenRouter. Includes multi-modal vision and intelligent model routing.

## ✅ Phase 5 — Model Context Protocol (MCP)
Enable seamless interoperability with external tools and services through MCP. Includes MCP client, Streamable HTTP and SSE transports, and dynamic tool discovery.

## ✅ Phase 6 — Agent-to-Agent (A2A) Protocol
Enable multi-agent collaboration across heterogeneous systems via A2A Protocol v1.0.

### Highlights
* A2A Client for Android & KMP (`:intelligence:mobilegraph-a2a`)
* Agent Card discovery (`/.well-known/agent-card.json`)
* Task delegation via JSON-RPC (`SendMessage`, `GetTask`, `CancelTask`)
* Real-time SSE streaming updates (`SendStreamingMessage`)
* Human-in-the-loop (HITL) task handling (`input-required`)
* Multi-modal artifacts and conversation history tracking
* Automatic protocol version fallback (v1.0 → v0.3)

---

## 🔜 Phase 7 — Local AI & Edge Inference

Bring private, offline AI directly to Android devices.

### Planned Features
* On-device Small Language Model (SLM) inference
* llama.cpp & GGUF integration
* ONNX Runtime & MediaPipe GenAI support
* Qualcomm AI Engine / NPU acceleration
* Hybrid local + cloud routing

---

## 🔮 Phase 8 — Android Ecosystem

Extend MobileGraph across the Android platform family.

### Highlights
* **Android**: Jetpack Compose integration, WorkManager AI tasks.
* **Automotive**: IVI AI framework, Vehicle Data integration, distraction-aware execution.
* **TV/XR**: Voice-first interactions and spatial AI agents.

---

## 🚀 Phase 9 — MobileGraph Studio

Create a visual development environment for designing AI workflows.

### Highlights
* Drag-and-drop workflow builder
* Visual State Graph editor & debugger
* Prompt Playground & RAG Inspector
* Token & cost optimization dashboard
