# ADR-022: Modular Dependency Graph with Zero Circular Dependencies

## Status
Accepted

## Context
A multi-module framework creates significant risk of circular dependencies. Without explicit architectural enforcement, module boundaries erode over time as convenience dependencies are added.

## Decision
The module dependency graph is strictly acyclic (DAG). `mobilegraph-core` has no MobileGraph module dependencies. Higher-level modules depend on lower-level modules, never vice versa.

### Canonical Dependency Order (lowest to highest)

**Foundation Layer** (no upward dependencies):

1. `mobilegraph-core` — zero internal dependencies
2. `mobilegraph-state` — no internal dependencies
3. `mobilegraph-checkpoint` — no internal dependencies

**Intelligence Layer** (depends on Foundation):

4. `mobilegraph-models` (depends on core)
5. `mobilegraph-tools` (depends on core, models)
6. `mobilegraph-prompts` (depends on core, models, tools)
7. `mobilegraph-parsers` (depends on core, models, prompts)

**Knowledge Layer** (depends on Foundation + select Intelligence):

8. `mobilegraph-documents` (depends on core)
9. `mobilegraph-embeddings` (depends on core, models)
10. `mobilegraph-vectorstores` (depends on documents, embeddings)
11. `mobilegraph-retrieval` (depends on documents, vectorstores, parsers)

**Intelligence Layer — Higher-order** (bridges Intelligence + Knowledge):

12. `mobilegraph-rag` (depends on retrieval, models, prompts)
13. `mobilegraph-graph` (depends on core, state, checkpoint)
14. `mobilegraph-agents` (depends on core, state, checkpoint, graph, models, tools, prompts)

### Enforcement
The DAG constraint is currently enforced by Gradle's module dependency declarations. Introducing a circular dependency causes a Gradle build failure. Future CI enforcement via architectural tests is planned.

## Consequences
- Foundation modules have no knowledge of Intelligence or Knowledge types.
- Knowledge modules have no knowledge of agent or graph types.
- Cross-cutting concerns (events, middleware, execution context) are defined as interfaces in `mobilegraph-core` to avoid upward dependencies.
- The `mobilegraph-rag` module acts as the bridge between Intelligence and Knowledge layers.