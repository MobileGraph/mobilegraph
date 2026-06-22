# ADR-001: Kotlin Multiplatform as the Foundation Technology

## Status
Accepted

## Context
MobileGraph aims to be a long-lived AI application framework for Kotlin developers, targeting Android, iOS, Desktop, Backend, and future edge environments. Maintaining separate implementations for each platform would introduce duplicated logic, inconsistent APIs, higher maintenance costs, and slower evolution. A common runtime is required to maximize code reuse while supporting native platform integration.

## Decision
Adopt Kotlin Multiplatform (KMP) as the primary development model. MobileGraph will follow a "Common First" strategy. 

- All core business logic, AI abstractions, graph execution, memory, and observability live in `commonMain`.
- Platform-specific integrations (lifecycle, credential storage, background execution) live in `androidMain` and `iosMain` using `expect`/`actual` declarations.
- Platform-specific code should be introduced only when absolutely necessary; business logic must remain platform-independent.

## Alternatives Considered
- **Separate Android and iOS SDKs**: Rejected due to maintenance burden and inevitable API divergence.
- **JVM-only Framework**: Rejected as it does not satisfy the vision of targeting mobile devices (especially iOS).
- **React Native or Flutter**: Rejected as they use the wrong language for Kotlin developers and often have poorer native integration depth than KMP.
- **Shared Business Logic with Native Wrappers**: Rejected as it creates unnecessary complexity and fragmentation.

## Tradeoffs
### Pros
- **Single Codebase**: 90%+ code shared between platforms; core abstractions are implemented once.
- **Consistent APIs**: Developers experience the same API across all platforms.
- **iOS/Native Support**: iOS developers get native Swift APIs via generated Kotlin/Native frameworks.
- **Reduced Maintenance**: Features evolve together and bug fixes propagate automatically.
- **Future Expansion**: The same runtime can later support Desktop, Backend, WASM, etc., without major redesign.

### Cons
- **KMP Complexity**: Gradle configuration and build toolchains are more sophisticated.
- **Kotlin/Native Performance**: Compilation is slower than pure JVM.
- **Library Limitations**: Must choose KMP-compatible dependencies; some JVM libraries are unavailable.

## Consequences
- All framework data classes used in state or communication are implemented in `commonMain`.
- All third-party dependencies must be KMP-compatible (e.g., kotlinx.coroutines, kotlinx.serialization, Ktor, SQLDelight).
- Most tests reside in `commonTest` using `kotlin.test`, `Kotest`, or `Turbine`.
- All `expect`/`actual` declarations must be documented and tested on both primary platforms.

## Decision Drivers
1. Maintainability
2. API consistency
3. Cross-platform capability
4. Performance
5. Platform specialization

## References
- ARCHITECTURE.md
- DESIGN_PRINCIPLES.md
- ROADMAP.md
