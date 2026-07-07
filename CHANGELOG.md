# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

# Changelog
## v0.3.0-alpha - 2026-07-08
### Added

#### Agent Runtime

Introduced the MobileGraph Agent Runtime.

New capabilities include:

Agent abstraction
Stateful execution
Execution context propagation
Agent lifecycle management
Coroutine-first execution model
#### Graph Runtime

Introduced graph-based workflow orchestration inspired by state machine execution.

Features include:

Directed workflow execution
Node abstraction
Edge routing
State propagation
Deterministic execution
#### State Management

Added immutable state management throughout graph execution.

Features include:

Immutable execution state
State transitions
Checkpoint-ready architecture
Replay-friendly execution model
#### Tool Integration

Integrated the Tool API into the agent runtime.

Agents can now invoke registered tools while maintaining execution context and middleware support.

#### Prompt Integration

Integrated Prompt Templates with agent execution.

- 
## v0.2.0-alpha - 2026-06-30

### Added

- Core Runtime
- Providers
- Middleware
- Memory
- Documents
- Splitters
- Embeddings
- Retrieval
- RAG

### Changed

- Improved DSL
- Improved event architecture

### Fixed

- Various bug fixes
- 
## [0.1.0-alpha] - 2026-06-13

### Added
- **ADK Foundation**: Initial release of the MobileGraph Agent Development Kit.
- **Resilient Core**: Automatic checkpointing and lifecycle-aware graph execution engine.
- **Model Orchestration**: Support for OpenAI, Gemini, and capability-based provider abstraction.
- **Tool Infrastructure**: Support for function calling with built-in semantic tool selection.
- **Prompt DSL**: Type-safe DSL for structured, token-aware prompt composition.
- **Parsers**: Structured data extraction using `kotlinx.serialization`.
- **Memory Management**: Local-first conversation persistence with sliding-window and summarization strategies.
- **Observability**: Passive observability via a global event stream and customizable logging middleware.
- **KMP Support**: First-class support for Android and iOS targets.
