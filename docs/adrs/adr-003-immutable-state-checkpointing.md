# ADR-003: Immutable State and Checkpointing

## Status
Accepted

## Context
MobileGraph supports long-running workflows, agents, and graph execution that must tolerate mobile lifecycle interruptions (process death, backgrounding). Traditional mutable shared state introduces race conditions, hidden side effects, and makes recovery or "time-travel" debugging extremely difficult. A deterministic and recoverable execution model is required.

## Decision
MobileGraph adopts an **Immutable State** model combined with **Automatic Checkpointing**.

- **Immutability**: Graph state must be an immutable data structure (typically a `@Serializable` Kotlin data class). Nodes do not mutate shared state; instead, they receive the current state and return a new state instance.
- **State Transitions**: Execution progresses through explicit transitions (`State S + Node N -> State S'`).
- **Checkpointing**: The framework automatically captures `StateSnapshot` objects after node transitions. Execution can resume from the latest checkpoint after a crash or process death.
- **Snapshots**: Each snapshot contains the execution ID, node ID, timestamp, and the full state object.

## Alternatives Considered
- **Mutable State with Locks**: Rejected due to high risk of concurrency bugs and difficulty in reliably capturing consistent checkpoints.
- **Event Sourcing**: Considered powerful but rejected for v1 due to excessive complexity for application developers.
- **Database-Centric State**: Rejected as execution logic should remain independent of specific storage technologies.

## Tradeoffs
### Pros
- **Deterministic Execution**: Predictable state transitions make debugging and testing (including replayability) much easier.
- **Mobile Resilience**: Execution can survive process death and resume transparently for the user.
- **Concurrency Safety**: Parallel branches of a graph can execute safely with independent state copies.
- **Auditability**: State history is naturally available, enabling time-travel debugging.

### Cons
- **Memory Overhead**: Frequent creation of large state objects can increase allocations (mitigated by data class structural sharing).
- **Storage/Serialization Cost**: Frequent checkpointing requires efficient serialization and consumes storage space.

## Consequences
- All framework data classes used in state paths must be annotated with `@Serializable` (kotlinx.serialization).
- The framework provides validation to catch missing annotations at graph construction time.
- Persistence occurs through a `StateStore` abstraction, keeping execution logic decoupled from storage implementations.
- Schema evolution requires explicit migration support (`StateMigrator`).

## Decision Drivers
1. Determinism
2. Recoverability
3. Mobile Resilience
4. Parallel Safety
5. Maintainability
