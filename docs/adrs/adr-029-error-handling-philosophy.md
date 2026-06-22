# ADR-029: Error Handling Philosophy - Typed Errors, Not Exception-First

## Status
Accepted

## Context
AI applications fail in many ways: model errors, tool timeouts, parse failures, retrieval errors, network failures. The error handling model determines how resilient and debuggable applications are.

## Decision
MobileGraph uses a typed error model:
- Programmer errors (invalid configuration, missing required fields): Throw exceptions at initialization/construction time
- Domain errors (model API failure, tool timeout, parse failure): Returned as sealed class error variants
- Framework errors (internal inconsistency): Wrapped in MobileGraphException with full context

Exceptions are not used for control flow. Every operation that can fail in a domain-expected way returns a result type. The framework never swallows exceptions silently.

## Consequences
when expressions on result types are the primary error handling pattern. The framework provides extension functions on Success {}, onError {}, getOrElse {} for ergonomic result handling. Stack traces are preserved in error variants via the cause: Throwable? field.