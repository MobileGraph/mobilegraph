# ADR-016: Reactive State Management for UI Integration

## Status
Accepted

## Context
The Compose and SwiftUI modules need to expose graph and agent execution state to UI components. The state management model must be compatible with both platforms' reactive UI paradigms.

## Decision
MobileGraphViewModel (for Compose) exposes execution state as StateFlow<UiState>. Graph events are collected in viewModelScope and mapped to UI states. For SwiftUI, the equivalent @Observable class exposes Swift AsyncSequence streams. The framework provides base classes and extension functions but does not mandate a specific architecture pattern.

## Alternatives Considered
- MVI (Model-View-Intent): Opinionated; conflicts with applications using their own MVI framework
- Direct Flow collection in Composables: Lifecycle-unsafe without proper collectAsStateWithLifecycle
- Event bus: Global state contamination; hard to test

## Tradeoffs
- Pros:
  - Works with any ViewModel-based architecture
  - Lifecycle-safe collection with collectAsStateWithLifecycle
  - Testable—state is deterministic from graph events
- Cons:
  - Developers need to understand Flow collection; not entirely hidden

## Consequences
MobileGraphViewModel provides executeGraph() and executeAgent() coroutine helpers that are automatically scoped to viewModelScope. Graph cancellation occurs automatically when the ViewModel is cleared.