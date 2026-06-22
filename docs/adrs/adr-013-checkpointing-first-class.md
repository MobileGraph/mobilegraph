# ADR-013: Checkpointing as a First-Class Concern, Not an Add-On

## Status
Accepted

## Context
Many frameworks treat persistence and recovery as optional features. For a mobile AI framework, checkpointing is existential - without it, a phone call terminates all AI workflow progress. The architectural question is whether checkpointing is default-on or opt-in.

## Decision
Checkpointing is enabled by default for all graph executions. The SqliteCheckpointer is the default implementation. Developers can disable checkpointing explicitly (for ephemeral/testing workflows) but enabling it requires no configuration. Checkpoint data is automatically cleaned up after successful execution completion based on configurable retention policy.

## Alternatives Considered
- Opt-in checkpointing: Most developers will not enable it; application instability
- In-memory only: Cannot survive process termination; not acceptable for mobile
- Remote only: Requires network; fails in offline scenarios

## Tradeoffs
- Pros:
  - Applications are resilient to interruption by default
  - Zero configuration for standard use cases
  - Local SQLite ensures checkpointing works offline
- Cons:
  - Adds SQLite write on every node transition (mitigated by write-ahead logging)
  - Storage consumption grows with execution history; managed by retention policy

## Consequences
The SQLite WAL (Write-Ahead Log) mode is enabled for the checkpoint database to minimize write latency. Checkpoint data is compressed using LZ4 for large state objects. The retention policy defaults to 7 days and is configurable.