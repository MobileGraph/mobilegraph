# ADR-005: kotlinx.serialization as the Exclusive Serialization Strategy

## Status
Accepted

## Context
The framework serializes: graph state, checkpoints, prompt templates, tool schemas, memory contents, traces, and metrics. Multiple serialization libraries exist (Gson, Moshi, Jackson, kotlinx.serialization). A choice must be made.

## Decision
kotlinx.serialization is the exclusive serialization library throughout MobileGraph. No Gson, Moshi, or Jackson dependencies are introduced. JSON, CBOR, and Protobuf formats are supported through kotlinx.serialization format modules.

## Alternatives Considered
- Gson: Reflection-based, not KMP compatible, poor performance
- Moshi: JVM-only, requires code generation or reflection
- Jackson: JVM-only, heavy, designed for server-side use
- Custom serialization: Maintenance burden without proportional benefit

## Tradeoffs
- Pros:
  - KMP native works on all targets
  - Compile-time code generation (no reflection)
  - Type-safe
  - Official JetBrains support and roadmap
- Cons:
  - Requires @Serializable annotation on all serializable types
  - Less runtime flexibility than reflection-based libraries

## Consequences
All framework data classes used in serialization paths are annotated with @Serializable. Third-party developers extending MobileGraph must annotate their state types. The framework provides validation that catches missing annotations at graph construction time.