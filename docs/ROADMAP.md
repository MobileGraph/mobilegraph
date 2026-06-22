# ROADMAP.md

# MobileGraph Roadmap

This document defines the implementation roadmap for MobileGraph.

The roadmap intentionally prioritizes foundation and stability over feature count.

Architecture documents describe the long-term vision.

This roadmap describes what should be built now.

---

# Guiding Principles

MobileGraph follows these rules:

* Build from the bottom up.
* Validate architecture before expanding scope.
* Favor stability over feature count.
* Avoid premature abstractions.
* Build modules incrementally.
* Keep APIs small and composable.

---

# Current Milestone

Current focus:

```text
Phase 2 — Knowledge Layer
```

Target modules:

```text
mobilegraph-documents
mobilegraph-stores
mobilegraph-retrieval
mobilegraph-memory
```

Everything else is out of scope.

---

# Phase 1 — Foundation

Status:

```text
COMPLETED ✅
```

Goal:

Establish the runtime foundation and validate the public APIs.

Modules:

```text
mobilegraph-core
mobilegraph-models
mobilegraph-prompts
mobilegraph-parsers
mobilegraph-tools
```

---

## mobilegraph-core

Responsibilities:

* ExecutionContext
* Typed identifiers
* Middleware
* Capabilities
* Exceptions
* Lifecycle state
* Cancellation

---

## mobilegraph-models

Responsibilities:

* ChatModel
* StreamingChatModel
* EmbeddingModel

Initial providers:

```text
OpenAI
```

---

## mobilegraph-prompts

Responsibilities:

* PromptTemplate
* ChatPrompt
* Prompt composition
* Variable substitution

---

## mobilegraph-parsers

Responsibilities:

* OutputParser
* JsonParser
* Structured outputs

---

## mobilegraph-tools

Responsibilities:

* Tool abstraction
* Tool metadata
* Tool execution

---

# Phase 1 Success Criteria

The following example should work:

```kotlin
val prompt = PromptTemplate(...)

val rendered = prompt.render(...)

val response = model.invoke(...)

val result = parser.parse(response)
```

---

# Phase 2 — Knowledge Layer

Status:

```text
IN PROGRESS 🚀
```

Modules:

```text
mobilegraph-documents
mobilegraph-stores
mobilegraph-retrieval
mobilegraph-memory
```

Goals:

Introduce document and retrieval capabilities.

---

## Documents

Responsibilities:

* Document model
* Chunk model
* Metadata
* Splitters

---

## Stores

Responsibilities:

* Vector store abstractions
* Persistence interfaces

---

## Retrieval

Responsibilities:

* Retriever interface
* Vector retrieval
* Hybrid retrieval

---

## Memory

Responsibilities:

* Conversation memory
* Summary memory
* Vector memory

---

# Phase 2 Success Criteria

Support:

```text
Prompt
+
Retriever
+
Model
```

without introducing agents.

---

# Phase 3 — Agent Runtime

Status:

```text
PLANNED
```

... [Rest of the file remains same]
