# ADR-012: Graph State Schema Validation at Build Time

## Status
Accepted

## Context
Graph execution depends on state being serializable for checkpointing. If developers forget to annotate their state class with @Serializable, the failure happens at runtime during the first checkpoint save after potentially significant computation. This is a poor developer experience.

## Decision
Graph construction validates that the state type s has a registered KSerializer<S> at GraphBuilder.build() time. If validation fails, a GraphDefinitionException is thrown immediately, before any execution begins. Developers see the error at startup or at the point of graph construction.

## Alternatives Considered
- Runtime validation at first checkpoint: Fails late; poor DX; may lose computation
- KSP annotation processor: Could catch at compile time but adds build complexity
- No validation (trust developers): Silent failures in production; unacceptable

## Tradeoffs
- Pros:
  - Fail-fast with clear error messages
  - No computation wasted before discovering misconfiguration
- Cons:
  - Slightly increases graph construction time (negligible - microseconds)

## Consequences
The graph<S> {} DSL builder function is an inline reified function to capture the KSerializer<S> at the call site. The builder validates serializer availability and throws GraphDefinitionException with actionable error messages.