# ADR-010: Sealed Classes for All Sum Types

## Status
Accepted

## Context
The framework has many result types that can be one of several variants: ParseResult, ToolResult, ModelOutput, AgentEvent, NodeResult. The choice between sealed classes, algebraic data types, exceptions, and nullable returns affects API ergonomics and safety.

## Decision
All sum types in MobileGraph public APIs are modeled as Kotlin sealed classes. Exceptions are reserved for programmer errors (misuse of the API). Domain errors (tool failure, parse failure, model error) are modeled as sealed class variants and must be explicitly handled by callers. Null is not used to represent the absence of a value where a sealed class variant communicates semantics better.

## Alternatives Considered
- Exceptions for all errors: Poor for async contexts; cannot model partial results; forces try/catch everywhere
- Nullable returns: Does not communicate error cause; null is ambiguous
- Result<T, E> (Arrow): Arrow is an excellent library but adds a significant dependency; MobileGraph's sealed class approach achieves the same goals
- Kotlin Result<T>: Only captures Throwable as error type, cannot model domain-specific error variants

## Tradeoffs
- Pros:
  - Exhaustive when expressions enforce complete error handling
  - Error variants carry rich diagnostic information
  - No external dependency
  - Idiomatic Kotlin
- Cons:
  - More verbose than nullable returns for simple cases

## Consequences
All public APIs that can fail return sealed class result types. The Kotlin when expression with is checks is the expected consumption pattern. IDE tooling automatically suggests exhaustive when expressions.