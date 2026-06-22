# ADR-009: Memory Persistence by Default

## Status
Accepted

## Context
Conversation memory can be ephemeral (lost on app restart) or persistent (survives restarts). Defaulting to ephemeral memory is simpler to implement but creates terrible user experiences. Defaulting to persistent requires more framework infrastructure.

## Decision
All memory implementations in MobileGraph persist their state by default. Ephemeral (in-memory only) mode must be explicitly requested. The default BufferMemory writes to SQLDelight-backed storage automatically. This is a mobile-first decision: mobile apps restart constantly and users expect conversation continuity.

## Alternatives Considered
- Ephemeral by default: Simpler implementation; terrible UX for mobile users
- Explicit persistence: Developers must opt-in; most will forget; creates inconsistent behavior
- Platform-conditional: Different defaults per platform; creates inconsistent framework behavior

## Tradeoffs
- Pros:
  - Correct behavior by default for mobile
  - Users experience conversation continuity across app restarts
- Cons:
  - Slightly more storage I/O per message
  - Developers must explicitly clear memory to reset conversations

## Consequences
Memory implementations accept a MemoryConfig that includes persistence: PersistenceMode (Persistent, Ephemeral, SessionOnly). The default MobileGraphConfig uses Persistent. All memory data is encrypted using the platform credential store.