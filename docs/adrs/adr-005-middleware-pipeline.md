# ADR-005-MIDDLEWARE.md

# ADR-005: Middleware Pipeline Architecture

## Status

Accepted

---

## Context

Many framework concerns are cross-cutting.

Examples include:

* retry
* tracing
* metrics
* caching
* rate limiting
* logging
* circuit breakers
* guardrails

Embedding these concerns directly inside:

* models
* tools
* retrievers
* agents

would create:

* duplicated logic
* tighter coupling
* reduced extensibility
* difficult testing

A composable mechanism is required.

---

## Problem Statement

How should MobileGraph introduce cross-cutting behavior while preserving:

* composability
* deterministic execution
* testability
* modularity
* separation of concerns

---

## Decision

MobileGraph adopts a middleware pipeline architecture.

Cross-cutting concerns must be implemented through middleware rather than embedded directly into business components.

---

## Middleware Contract

```kotlin
interface Middleware<I, O> {

    suspend fun intercept(
        input: I,
        context: ExecutionContext,
        next: suspend (I, ExecutionContext) -> O
    ): O

}
```

---

## Execution Flow

```text
Request

↓

TracingMiddleware

↓

MetricsMiddleware

↓

RetryMiddleware

↓

Provider

↓

Response
```

Each middleware decides whether to:

* continue execution
* modify the request
* modify the response
* terminate execution
* raise an exception

---

## Architectural Principles

Prefer:

```text
Middleware Composition
```

Avoid:

```text
Business Logic + Retry Logic

Business Logic + Metrics Logic

Business Logic + Tracing Logic
```

Cross-cutting concerns belong outside core business components.

---

## Middleware Responsibilities

Suitable concerns include:

### Retry

Automatic retries for transient failures.

---

### Metrics

Latency measurement.

Cost tracking.

Counters.

---

### Tracing

Span creation.

Correlation IDs.

Execution events.

---

### Logging

Debug logs.

Audit logs.

---

### Rate Limiting

Request throttling.

---

### Caching

Response caching.

Embedding caching.

---

### Circuit Breakers

Failure protection.

---

### Guardrails

Input validation.

Output validation.

---

## Unsuitable Middleware

Middleware should not contain:

* business workflows
* domain logic
* agent reasoning
* graph execution

These responsibilities belong elsewhere.

---

## Deterministic Order

Middleware execution order must be deterministic.

Example:

```text
Tracing

↓

Metrics

↓

Retry

↓

Provider
```

Changing middleware order may change behavior.

Therefore order must always be explicit.

---

## Middleware Nesting

Execution resembles:

```text
Tracing {

    Metrics {

        Retry {

            Provider

        }

    }

}
```

Responses unwind in reverse order.

---

## Examples

### Retry Middleware

```kotlin
RetryMiddleware(
    policy = ExponentialBackoff()
)
```

---

### Metrics Middleware

```kotlin
MetricsMiddleware(
    collector
)
```

---

### Cache Middleware

```kotlin
CacheMiddleware(
    cache
)
```

---

## Middleware Scope

Middleware may exist at:

### Model Level

```text
OpenAIChatModel
```

---

### Tool Level

```text
HttpTool
```

---

### Retriever Level

```text
VectorRetriever
```

---

### Agent Level

```text
ReActAgent
```

---

### Graph Level

```text
WorkflowGraph
```

---

## Consequences

### Positive

#### Separation of Concerns

Business logic remains clean.

---

#### Reusability

Middleware can be reused across components.

---

#### Testability

Each middleware can be tested independently.

---

#### Extensibility

New behaviors can be added without modifying existing components.

---

#### Consistency

Common behaviors remain uniform across the framework.

---

### Negative

#### Additional Abstraction

Pipelines add complexity.

---

#### Ordering Sensitivity

Incorrect ordering may change behavior.

---

#### Debugging Depth

Call chains become deeper.

---

## Alternatives Considered

---

### Inheritance

Example:

```kotlin
class RetryOpenAIModel :
    OpenAIChatModel()
```

Rejected.

Reason:

Creates class explosion.

---

### Decorators

Example:

```kotlin
LoggingModel(
    RetryModel(
        OpenAIModel()
    )
)
```

Rejected.

Reason:

Verbose and difficult to compose.

---

### Embedded Logic

Example:

```kotlin
OpenAIChatModel {

    retry()

    metrics()

}
```

Rejected.

Reason:

Violates separation of concerns.

---

### Reflection-Based Plugins

Rejected.

Reason:

Hidden behavior.

Poor predictability.

---

## Error Handling

Middleware should propagate exceptions.

Avoid swallowing failures.

Use:

```kotlin
MobileGraphException
```

subtypes.

---

## Context Propagation

Middleware receives:

```kotlin
ExecutionContext
```

and should enrich existing context rather than create new contexts.

---

## Future Middleware Examples

Possible future middleware:

```text
RetryMiddleware

MetricsMiddleware

TracingMiddleware

LoggingMiddleware

RateLimitMiddleware

CacheMiddleware

CircuitBreakerMiddleware

GuardrailMiddleware

PIIFilterMiddleware

CostTrackingMiddleware

DeadlineMiddleware

TimeoutMiddleware
```

These should remain independent and composable.

---

## Dependency Implications

This ADR affects:

```text
mobilegraph-models

mobilegraph-tools

mobilegraph-retrieval

mobilegraph-memory

mobilegraph-agents

mobilegraph-graph

mobilegraph-observability
```

Therefore this ADR is considered foundational.

---

## Decision Drivers

Priority order:

1. Separation of concerns
2. Reusability
3. Extensibility
4. Deterministic execution
5. Testability

---

## Architectural Law

Cross-cutting concerns belong in middleware.

Business components should remain focused on their primary responsibilities.

Middleware order must be deterministic.

Hidden behavior is prohibited.

---

## References

* ARCHITECTURE.md
* DESIGN_PRINCIPLES.md
* ADR-001-KMP.md
* ADR-002-FLOW.md
* ADR-004-CAPABILITIES.md

---

Accepted Date:

2026

Author:

MobileGraph Architecture Board
