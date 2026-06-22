# DESIGN_PRINCIPLES.md

# MobileGraph Design Principles

This document defines the fundamental principles that guide the design and evolution of MobileGraph.

These principles are intended to outlive individual implementations and releases.

Implementation details may change.

Design principles should endure.

---

# Principle 1 — Strong Typing First

Prefer compile-time safety over runtime flexibility.

Strong typing improves:

* correctness
* discoverability
* maintainability
* refactoring safety

Prefer:

```kotlin
ChatRequest

ChatResponse

ToolResult

PromptTemplate
```

Avoid:

```kotlin
Map<String, Any>

JsonObject

Dynamic values
```

Framework APIs should expose meaningful domain types.

---

# Principle 2 — Provider Agnostic Design

Application code should not depend on provider implementations.

Changing:

```kotlin
OpenAIChatModel
```

to:

```kotlin
GeminiChatModel
```

should not require rewriting:

* prompts
* parsers
* tools
* agents
* workflows

Provider-specific capabilities must be abstracted.

---

# Principle 3 — Composition Over Inheritance

Small composable building blocks are preferred over large inheritance hierarchies.

Prefer:

```text
Prompt
+
Model
+
Parser
```

Avoid:

```text
AbstractSuperAgentFrameworkBaseClass
```

Inheritance should be shallow.

Composition should be the default.

---

# Principle 4 — Explicit Over Magic

Behavior should be visible and predictable.

Avoid:

* hidden side effects
* reflection-based discovery
* global mutable state

Prefer:

```kotlin
RetryMiddleware()

TracingMiddleware()
```

Framework behavior should be understandable without reading internal source code.

---

# Principle 5 — Progressive Complexity

Simple use cases should remain simple.

Advanced capabilities should be opt-in.

Examples:

Simple:

```kotlin
model.invoke(request)
```

Moderate:

```kotlin
prompt
+
model
+
parser
```

Advanced:

```kotlin
graph {
}
```

Developers should only pay for what they use.

---

# Principle 6 — Mobile First

MobileGraph is designed for mobile environments first.

Applications may:

* lose connectivity
* move to background
* experience process death
* encounter memory pressure

Therefore the framework must support:

* checkpointing
* cancellation
* recovery
* resumability

as first-class capabilities.

---

# Principle 7 — Observability By Default

Every operation should be observable.

Tracing should support:

* debugging
* metrics
* cost tracking
* auditing

Observability must be passive.

Failures in observability must never affect application execution.

---

# Principle 8 — Deterministic Runtime

AI outputs may be probabilistic.

Framework execution must not be.

Given:

```text
State S
+
Node N
```

execution should always produce predictable behavior.

Framework components should avoid hidden nondeterminism.

---

# Principle 9 — Lifecycle Awareness

Lifecycle events are runtime events.

Supported states:

```text
Foreground

Background

Offline

Suspended

Killed

Restored
```

Execution should adapt automatically.

Lifecycle awareness must be built into the runtime.

---

# Principle 10 — Immutable State

State should be:

* immutable
* serializable
* replayable
* versioned

Nodes should not mutate shared state.

Instead:

```text
State S

↓

Node

↓

State S'
```

Immutable state enables:

* checkpointing
* replay
* debugging
* recovery

---

# Principle 11 — Capability-Based Abstraction

Capabilities should replace provider checks.

Avoid:

```kotlin
if (model is OpenAIChatModel)
```

Prefer:

```kotlin
model.supports(
    Capability.FunctionCalling
)
```

Framework users should depend on capabilities rather than implementations.

---

# Principle 12 — Middleware For Cross-Cutting Concerns

Cross-cutting concerns belong in middleware.

Examples:

* retry
* metrics
* tracing
* caching
* rate limiting

Middleware order must remain deterministic.

Middleware must not change business semantics.

---

# Principle 13 — Single Responsibility

Each module should have one primary responsibility.

Examples:

Prompts:

Responsible for:

* templates
* variables
* composition

Not responsible for:

* model invocation
* retrieval
* tool execution

Modules should evolve independently.

---

# Principle 14 — Backward Compatibility

Breaking changes are expensive.

Minor releases should be additive.

Deprecations should remain for at least two release cycles.

Major releases must provide migration guidance.

Public API stability is a priority.

---

# Principle 15 — Dependency Direction Matters

Dependencies flow downward.

```text
UI

↓

Agent Runtime

↓

Intelligence

↓

Knowledge

↓

Foundation
```

Reverse dependencies are forbidden.

Circular dependencies are prohibited.

---

# Principle 16 — Flow<T> Is The Streaming Primitive

MobileGraph standardizes on:

```kotlin
Flow<T>
```

Avoid:

* callbacks
* listeners
* observers
* RxJava

Structured concurrency and Flow provide a consistent execution model across the framework.

---

# Principle 17 — No Framework-Owned Dependency Injection

MobileGraph does not own a DI framework.

Do not introduce:

* Koin
* Hilt
* Dagger

Applications are free to choose their own DI solution.

---

# Principle 18 — Reflection Is A Last Resort

Prefer:

* interfaces
* sealed interfaces
* generics

Avoid:

* reflection
* runtime scanning
* annotation processors

Reflection should only be used when no practical alternative exists.

---

# Principle 19 — Build From The Bottom Up

Implementation order matters.

Build sequence:

```text
mobilegraph-core

↓

mobilegraph-models

↓

mobilegraph-prompts

↓

mobilegraph-parsers

↓

mobilegraph-tools

↓

mobilegraph-documents

↓

mobilegraph-retrieval

↓

mobilegraph-memory

↓

mobilegraph-agents

↓

mobilegraph-graph
```

Do not skip foundational layers.

---

# Principle 20 — Simplicity Wins

Prefer:

* readability
* maintainability
* explicit APIs

Avoid:

* clever abstractions
* premature optimization
* unnecessary complexity

Working code is better than elegant theory.

Small APIs are better than powerful APIs.

Simple systems are easier to evolve.

---

# Final Principle

Implementation may evolve.

Architectural principles should endure.

When faced with a tradeoff between convenience and maintainability, choose maintainability.

When faced with a tradeoff between magic and explicitness, choose explicitness.

When faced with a tradeoff between short-term gains and long-term architecture, choose long-term architecture.
