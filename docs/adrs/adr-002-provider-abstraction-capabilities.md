# ADR-002: Provider Abstraction and Capability-Based Discovery

## Status
Accepted

## Context
AI model providers (OpenAI, Gemini, Anthropic, etc.) have incompatible APIs, request/response formats, and features. MobileGraph must provide a consistent interface for these providers without forcing a rigid class hierarchy or creating tight coupling to concrete implementations. Applications should be able to switch providers easily while leveraging specific strengths (like vision or tool calling) when available.

## Decision
Adopt a **Capability-Based Interface Abstraction** model.

- **Interface-Based**: Model providers are defined as Kotlin interfaces (e.g., `ChatModel`) with no abstract base classes. This allows for composition over inheritance and makes third-party implementations lightweight.
- **Capability Checks**: Models must implement a `supports(Capability): Boolean` method. Application code depends on behaviors (e.g., `Capability.FunctionCalling`) rather than concrete types.
- **Extension-Driven**: Provider-specific features are exposed through Kotlin extension functions rather than polluting the core interface.
- **Middleware**: Cross-cutting behaviors like retries and logging are added via decorators/middleware rather than base class logic.

### Example
```kotlin
if (model.supports(Capability.FunctionCalling)) {
    // Leverage tool calling safely
}
```

## Alternatives Considered
- **Abstract Base Class**: Rejected as it creates tight coupling and makes external provider implementation cumbersome.
- **Provider-Specific Branching (`is OpenAIChatModel`)**: Rejected as it leads to vendor lock-in and fragile application logic.
- **Lowest Common Denominator API**: Rejected as it hides the unique strengths of advanced models.

## Tradeoffs
### Pros
- **Provider Independence**: Application logic remains portable and reusable.
- **Extensibility**: New capabilities can be defined and supported by providers without breaking changes.
- **Testability**: Interfaces and capabilities are trivial to mock.
- **Future-Proof**: New providers can be integrated into existing graphs if they support the required capabilities.

### Cons
- **Abstraction Overhead**: Requires a thin layer of mapping between framework types and provider-specific SDKs.

## Consequences
- The `ModelRegistry` handles runtime selection based on required capabilities.
- Providers are lightweight adapters that map `ChatRequest` to their native formats.
- Developers use a uniform `ChatModel` API for 90% of use cases, falling back to capability checks only for specialized features.

## Decision Drivers
1. Provider independence
2. API consistency
3. Extensibility
4. Maintainability

## References
- ADR-001: Kotlin Multiplatform
- ADR-004: Kotlin Flow
- ARCHITECTURE.md
