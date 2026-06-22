# ADR-022: Modular Dependency Graph with Zero Circular Dependencies

## Status
Accepted

## Context
A 16-module framework creates significant risk of circular dependencies. Without explicit architectural enforcement, module boundaries erode over time as convenience dependencies are added.

## Decision
The module dependency graph is strictly acyclic (DAG). mobilegraph-core has no MobileGraph module dependencies. Higher-level modules depend on lower-level modules, never vice versa. ArchUnit tests enforce the DAG constraint in CI. Any PR introducing a circular dependency fails CI automatically.

### Canonical Dependency Order (lowest to highest)
1. text
2. core
3. security (depends on core)
4. storage (depends on core, security)
5. observability (depends on core)
6. models (depends on core, observability)
7. prompts (depends on core, models)
8. parsers (depends on core, models)
9. documents (depends on core)
10. knowledge (depends on core, storage, security, models)
11. retrieval (depends on core, knowledge, models, documents)
12. memory (depends on core, storage, models, prompts)
13. tools (depends on core, observability)
14. agents (depends on core, models, prompts, parsers, memory, tools, observability)
15. graph (depends on core, agents, storage, observability)
16. compose (depends on core, graph, agents)
17. swiftui (depends on core, graph, agents)

## Consequences
Modules below agents have no knowledge of agent-specific types. Modules below graph have no knowledge of graph-specific types. Cross-cutting concerns (observability, security) use interfaces defined in core to avoid upward dependencies.