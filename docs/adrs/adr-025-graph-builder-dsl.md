# ADR-025: Graph Builder DSL Design

## Status
Accepted

## Context
Graph construction must be type-safe, readable, and validated at compile time where possible. Options include: fluent builder, annotation-based, JSON/YAML definition, or Kotlin DSL.

## Decision
A Kotlin DSL using @DslMarker and inline reified functions is used for graph construction. The DSL provides type-safe node and edge definitions, validates state type compatibility at construction, and catches common errors (duplicate node IDs, missing entry node) at build() time.

```kotlin
val graph = graph<CustomerSupportState> {
    node("classify") { state ->
        // node implementation
    }
    node("resolve_billing") { state ->
        // node implementation
    }
    node("resolve_technical") { state ->
        // node implementation
    }
    
    conditionalEdge("classify") { state ->
        when (state.intent) {
            Intent.BILLING -> "resolve_billing"
            Intent.TECHNICAL -> "resolve_technical"
        }
    }
    
    entry("classify")
    exit("resolve_billing", "resolve_technical")
}
```

## Consequences
The DSL is implemented as a builder class with @DslMarker annotation to prevent accidental receiver scope leakage. Node implementations are lambda functions, enabling natural Kotlin closure semantics. The built graph is an immutable GraphDefinition<S>.