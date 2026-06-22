# CONTRIBUTING_AI.md

# AI Contributor Guidelines

This document provides instructions for AI coding agents contributing to MobileGraph.

Examples include:

* Codex
* Claude Code
* Cursor
* Gemini CLI
* Future AI pair programmers

Human contributors may also use these guidelines.

---

# Purpose

MobileGraph is a long-lived framework.

Code quality, maintainability, and API stability are more important than feature count.

Contributors should optimize for long-term architecture rather than short-term convenience.

---

# Architectural Source of Truth

When making decisions, consult documents in this order:

1. ARCHITECTURE.md
2. DESIGN_PRINCIPLES.md
3. ROADMAP.md
4. ADR documents
5. Module handbooks

If implementation conflicts with architecture documents, architecture wins.

---

# General Principles

Always prefer:

* Strong typing
* Immutable data structures
* Composition over inheritance
* Explicit APIs
* Deterministic behavior

Avoid:

* Reflection
* Global mutable state
* Hidden side effects
* Magic behavior
* Provider-specific assumptions

---

# Current Scope

Current milestone:

```text
mobilegraph-core
mobilegraph-models
mobilegraph-prompts
mobilegraph-parsers
mobilegraph-tools
```

Do not implement:

```text
mobilegraph-memory

mobilegraph-retrieval

mobilegraph-agents

mobilegraph-graph

mobilegraph-compose

mobilegraph-swiftui
```

unless explicitly instructed.

---

# Dependency Rules

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

---

# Forbidden Dependencies

Never introduce:

```text
core → graph

core → agents

models → agents

prompts → models

tools → agents

stores → models
```

Circular dependencies are prohibited.

---

# Streaming

Flow<T> is the only streaming primitive.

Use:

```kotlin
Flow<T>
```

Avoid:

```kotlin
Callbacks

Listeners

Observers

RxJava
```

---

# Coroutines

Prefer:

```kotlin
suspend fun
```

and structured concurrency.

Avoid:

```kotlin
GlobalScope

runBlocking
```

inside framework code.

---

# ExecutionContext

Every operation receives an ExecutionContext.

Do not create new contexts unless absolutely necessary.

Prefer enriching existing context.

---

# Capability System

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

Application code should not depend on provider implementations.

---

# Exceptions

Use typed exceptions.

Base class:

```kotlin
MobileGraphException
```

Avoid:

```kotlin
RuntimeException

Exception
```

Recoverable operations should prefer Result<T>.

---

# State

State must be:

* immutable
* serializable
* replayable
* versioned

Nodes must not mutate shared state.

---

# Middleware

Cross-cutting concerns belong in middleware.

Examples:

* Retry
* Metrics
* Tracing
* Caching
* Rate limiting

Middleware order must remain deterministic.

---

# Reflection

Avoid reflection.

Prefer:

* interfaces
* sealed interfaces
* generics

Reflection-based registries are discouraged.

---

# Dependency Injection

MobileGraph does not own a DI framework.

Do not introduce:

```text
Koin

Hilt

Dagger
```

inside the framework.

Applications may choose their own DI solution.

---

# Package Naming

Use:

```text
io.mobilegraph.core.*

io.mobilegraph.models.*

io.mobilegraph.prompts.*

io.mobilegraph.tools.*
```

Avoid:

```text
com.mobilegraph.*
```

---

# Public APIs

Public APIs should:

* be small
* be stable
* be composable
* minimize breaking changes

Avoid deep inheritance trees.

Prefer interfaces.

---

# Testing

Every public API requires tests.

Use:

```text
Kotest
```

For Flow:

```text
Turbine
```

Framework code should prioritize:

* unit tests
* contract tests

over integration tests.

---

# Module Responsibilities

Modules should have a single responsibility.

Do not move responsibilities across layers.

Examples:

Prompts:

```text
Prompt templates

Variable substitution

Composition
```

Prompts should not:

```text
Call models

Perform retrieval

Execute tools
```

---

# Provider Independence

MobileGraph is provider agnostic.

Avoid introducing assumptions tied to:

* OpenAI
* Gemini
* Anthropic

Providers should remain interchangeable.

---

# Observability

Tracing failures must never fail application execution.

Observability is passive.

It must not change business behavior.

---

# Build Philosophy

Build from the bottom up.

Order:

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
```

Do not skip ahead.

---

# When Unsure

Prefer:

* simplicity
* explicitness
* maintainability

Do not optimize prematurely.

Avoid over-engineering.

Small APIs are better than powerful APIs.

Working code is better than clever code.

---

# Rule of Least Surprise

Framework users should be able to understand behavior without reading source code.

---

# Long-Term Goal

MobileGraph aims to become the primary Kotlin Multiplatform AI application framework.

Contributors should optimize for:

* readability
* maintainability
* extensibility
* API stability

rather than speed of implementation.

Implementation may evolve.

Architectural principles should endure.
