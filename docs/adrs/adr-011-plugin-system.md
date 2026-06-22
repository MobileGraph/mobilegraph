# ADR-011: Plugin System Using Service Locator Pattern with Explicit Registration

## Status
Accepted

## Context
MobileGraph needs a plugin system for third-party extensions. Options include: Java ServiceLoader, reflection-based discovery, Kotlin Koin/Kodein DI, annotation processing, or explicit registration.

## Decision
Plugins are registered explicitly through the MobileGraph.initialize {} DSL. No automatic discovery via classpath scanning or reflection. The MobileGraphPlugin interface is the registration contract. This is the approach used by Ktor plugins and is well-understood by Kotlin developers.

## Alternatives Considered
- Java ServiceLoader: Works on JVM; does not work reliably on Kotlin/Native (iOS); requires classpath scanning
- Reflection-based discovery: Not compatible with KMP (no reflection on Kotlin/Native)
- Annotation processing (KSP): Compile-time; cannot handle runtime plugin installation
- Full DI framework (Koin/Kodein): Adds significant dependency; more complexity than required

## Tradeoffs
- Pros:
  - KMP compatible; no reflection required
  - Explicit > implicit - no "magic" discovery
  - Consistent with Ktor's plugin API which Kotlin developers already know
  - Fail-fast at initialization if required plugins are missing
- Cons:
  - Developers must explicitly register plugins; no auto-discovery convenience

## Consequences
The MobileGraphPlugin interface requires implementing install (config: MobileGraphConfig). Plugin ordering is defined by registration order. Plugin API compatibility is validated at installation time using semantic version comparison.