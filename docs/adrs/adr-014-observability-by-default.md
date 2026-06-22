# ADR-014: Observability Enabled by Default with Zero Developer Opt-In

## Status
Accepted

## Context
Observability is typically an opt-in concern in libraries. However, AI applications without observability are impossible to operate in production—cost overruns, quality degradation, and agent misbehavior are invisible. The question is: should developers need to configure observability to get it?

## Decision
All framework operations are instrumented by default. Traces, metrics, and cost estimates are generated for every model call, tool execution, retrieval, and graph transition. In the absence of a configured exporter, data is buffered locally (debug builds) or discarded after TTL expiry (production builds with no exporter). No developer action is required to get instrumentation.

## Alternatives Considered
- Opt-in instrumentation: Most developers skip it; production systems are blind
- Compile-time instrumentation: Possible with KSP but adds complexity
- Manual SDK integration: Status quo for most frameworks; insufficient for MobileGraph's goals

## Tradeoffs
- Pros:
  - Production systems are observable without developer effort
  - Debugging AI workflows is immediately possible
  - Cost visibility by default prevents bill shock
- Cons:
  - Minimal performance overhead (<1ms per operation per benchmarks)
  - Privacy: trace data must be sanitized before export (PII filter is configurable)

## Consequences
Every module implements the Traceable interface. The MobileGraph Tracer is injected into every component via the ExecutionContext. The PII filter pipeline sanitizes all trace data before export when piiFilter == true in the security configuration.