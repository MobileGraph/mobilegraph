# ADR-004: Kotlin Flow as the Exclusive Streaming Primitive

## Status
Accepted

## Context
MobileGraph is a highly asynchronous framework where streaming behavior is required across multiple subsystems: model responses (token-by-token), tool execution, agent loops (thought/action/observation), and observability events. A consistent streaming abstraction is necessary to avoid API fragmentation and reduce cognitive overhead for developers.

## Decision
Kotlin **`Flow<T>`** is adopted as the exclusive streaming primitive throughout the MobileGraph SDK. 

- All asynchronous sequences (e.g., `model.stream()`, `agent.execute()`) must return a `Flow`.
- Callbacks, listeners, and custom observer interfaces are strictly avoided in public APIs.
- Streaming APIs should be **cold** by default; execution starts only upon collection.
- Coroutine suspension and structured concurrency are used for terminal operations and backpressure management.

## Alternatives Considered
- **Callbacks**: Rejected due to poor composability, difficulty in managing cancellation, and "callback hell."
- **RxJava/RxKotlin**: Rejected as a heavy third-party dependency that is less idiomatic in modern Kotlin and less KMP-friendly.
- **LiveData**: Rejected as it is Android-specific and unsuitable for a cross-platform library.
- **Channels**: Considered a lower-level primitive; `Flow` provides a higher-level, safer abstraction and is preferred for public APIs.

## Tradeoffs
### Pros
- **Idiomatic Modern Kotlin**: Standardized part of the Kotlin ecosystem.
- **KMP Native**: Works seamlessly across all target platforms.
- **Structured Concurrency**: Cancellation propagates automatically when the collecting scope is cancelled.
- **Rich Operator Library**: Built-in support for `map`, `filter`, `combine`, `retry`, `debounce`, etc.

### Cons
- **iOS Interoperability**: iOS developers require a bridge to `AsyncSequence` (handled in `mobilegraph-swiftui` via SKIE or manual wrappers).

## Consequences
- The framework ensures uniform API surfaces across all modules (models, tools, graph, etc.).
- Errors terminate the `Flow` with typed `MobileGraphException` subtypes.
- Testing is standardized using tools like `Turbine` and `kotlin.test`.
- Hot flows (e.g., global observability streams) use `SharedFlow` or `StateFlow` explicitly.

## Decision Drivers
1. Consistency
2. Structured concurrency
3. Composability
4. Cancellation support
5. Testability

## References
- ARCHITECTURE.md
- DESIGN_PRINCIPLES.md
- ADR-001: Kotlin Multiplatform
