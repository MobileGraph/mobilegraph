# ADR-006-LIFECYCLE.md

# ADR-006: Lifecycle-Aware Execution Model

## Status

Accepted

---

## Context

MobileGraph is designed primarily for mobile environments.

Applications may experience:

* background transitions
* process death
* memory pressure
* connectivity loss
* application suspension
* device reboot

Traditional server-side frameworks assume continuous execution.

This assumption is invalid on mobile devices.

Long-running operations such as:

* streaming models
* retrieval
* agents
* workflows
* graph execution

must tolerate lifecycle interruptions.

---

## Problem Statement

How should MobileGraph support:

* interruptions
* recovery
* cancellation
* resumability

while maintaining:

* deterministic execution
* portability
* type safety
* framework consistency

---

## Decision

MobileGraph adopts a lifecycle-aware execution model.

Lifecycle events are considered runtime events rather than UI events.

Execution should adapt automatically to lifecycle changes.

---

## Supported Lifecycle States

```kotlin id="mzyjlwm"
enum class LifecycleState {

    Foreground,

    Background,

    Offline,

    Suspended,

    Killed,

    Restored

}
```

---

## State Definitions

### Foreground

Normal execution.

No restrictions.

---

### Background

Application remains alive.

Execution may continue with reduced activity.

---

### Offline

Network unavailable.

Remote operations may:

* fail
* queue
* retry

depending on policy.

---

### Suspended

Execution is temporarily paused.

Resources should be released when possible.

---

### Killed

Process no longer exists.

Execution state should be recovered from checkpoints.

---

### Restored

Application has resumed after interruption.

Execution may continue from the latest checkpoint.

---

## Execution Flow

Example:

```text
Foreground

↓

Background

↓

Suspended

↓

Killed

↓

Restored

↓

Resume Execution
```

---

## Architectural Principles

Prefer:

```text
Checkpoint + Resume
```

Avoid:

```text
Restart Everything
```

Lifecycle interruptions should be survivable.

---

## Lifecycle Events

Example:

```kotlin id="eiboqxl"
sealed interface LifecycleEvent
```

---

### EnterForeground

```kotlin id="oqxdcmk"
data object EnterForeground
```

---

### EnterBackground

```kotlin id="hmcpylg"
data object EnterBackground
```

---

### NetworkLost

```kotlin id="fplqedl"
data object NetworkLost
```

---

### Suspended

```kotlin id="lgzmjra"
data object Suspended
```

---

### Restored

```kotlin id="bifxgqu"
data object Restored
```

---

## Consequences

### Positive

#### Mobile Resilience

Applications survive interruptions.

---

#### Improved UX

Long-running operations can continue.

---

#### Deterministic Recovery

Execution resumes from checkpoints.

---

#### Foundation for Agents

Future workflows become resumable.

---

#### Cross-Platform Consistency

Android and iOS share execution semantics.

---

### Negative

#### Increased Complexity

Execution state becomes more sophisticated.

---

#### Checkpoint Storage Cost

State persistence introduces overhead.

---

#### Additional Testing Requirements

Lifecycle transitions require extensive validation.

---

## Alternatives Considered

---

### Ignore Lifecycle Events

Rejected.

Reason:

Execution becomes fragile.

---

### UI-Layer Lifecycle Management

Rejected.

Reason:

Lifecycle becomes application-specific.

Framework behavior becomes inconsistent.

---

### Android-Specific Lifecycle Handling

Rejected.

Reason:

Violates KMP philosophy.

---

### Restart Execution After Interruptions

Rejected.

Reason:

Poor user experience.

Loses execution progress.

---

## Recovery Strategy

Execution should recover from:

```text
Checkpoint

↓

Restore State

↓

Resume Execution
```

Recovery should be transparent to framework users.

---

## Execution Policies

Future versions may support:

### Continue

Continue execution.

---

### Pause

Suspend execution.

---

### Queue

Queue operations until restoration.

---

### Cancel

Terminate execution.

---

### Retry

Attempt recovery.

---

These policies should remain configurable.

---

## Dependency Implications

This ADR affects:

```text
mobilegraph-core

mobilegraph-memory

mobilegraph-agents

mobilegraph-graph

mobilegraph-observability
```

Therefore it is considered foundational.

---

## Future Extensions

Possible future additions:

```text
LowMemory

BatterySaver

Charging

ThermalThrottling

ConnectivityRestored
```

These additions should remain additive.

---

## Integration Points

Platform-specific implementations may map:

### Android

```text
LifecycleOwner

ProcessLifecycleOwner
```

to:

```kotlin id="nsjfhjz"
LifecycleState
```

---

### iOS

```text
UIApplicationState
```

to:

```kotlin id="kgihpbt"
LifecycleState
```

Business logic should remain platform-independent.

---

## Relationship With ADR-003

Lifecycle recovery depends on:

```text
Immutable State

+

State Snapshots

+

Checkpointing
```

This ADR builds upon ADR-003.

---

## Decision Drivers

Priority order:

1. Mobile resilience
2. Recoverability
3. Cross-platform consistency
4. Determinism
5. User experience

---

## Architectural Law

Lifecycle events are runtime events.

Execution should adapt to interruptions rather than assume continuous availability.

Applications should recover rather than restart.

---

## References

* ARCHITECTURE.md
* DESIGN_PRINCIPLES.md
* ADR-001-KMP.md
* ADR-002-FLOW.md
* ADR-003-STATE.md
* ADR-005-MIDDLEWARE.md

---

Accepted Date:

2026-06-13

Author:

MobileGraph Architecture Board
