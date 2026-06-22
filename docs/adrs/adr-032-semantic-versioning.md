# ADR-007: Semantic Versioning and API Stability

## Status
Accepted

## Context
As a framework used by multiple applications across different platforms, MobileGraph must provide strong version stability and predictable upgrades. Breaking API changes force expensive migrations and can damage the ecosystem. A clear strategy for versioning and deprecation is essential.

## Decision
Adopt **Semantic Versioning (SemVer)** strictly across all MobileGraph modules.

- **MAJOR (X.0.0)**: For incompatible API changes. Includes a migration guide and examples.
- **MINOR (0.Y.0)**: For backward-compatible functionality additions. Guaranteed source and binary compatibility for existing stable APIs.
- **PATCH (0.0.Z)**: For backward-compatible bug fixes and performance improvements.

### API Stability Levels
- **Stable**: Backward compatibility guaranteed. Changes require a deprecation cycle.
- **Experimental**: Marked with `@ExperimentalMobileGraphApi`. Subject to change without notice. Requires `@OptIn`.
- **Internal**: Anything in `internal` packages or marked `internal` is not part of the public contract.

### Deprecation Policy
Breaking changes must follow a multi-release cycle:
1. Introduce the new API.
2. Mark the old API as `@Deprecated` with a replacement suggestion.
3. Maintain both for at least two minor releases.
4. Remove in the next major version.

## Alternatives Considered
- **Date-Based Versioning**: Rejected as it fails to communicate compatibility or the nature of changes.
- **Continuous Versioning (Build numbers)**: Rejected as it provides no stability guarantees to consumers.

## Tradeoffs
### Pros
- **Predictable Upgrades**: Developers can upgrade patch and minor versions with high confidence.
- **API Maturity**: Encourages deliberate design as public APIs are treated as long-term contracts.
- **Tooling Support**: Enables use of binary compatibility validators in CI.

### Cons
- **Maintenance Burden**: Maintaining deprecated APIs increases codebase size and complexity temporarily.
- **Slower Velocity**: Redesigning core components requires more planning and a longer rollout period.

## Consequences
- The `kotlinx-binary-compatibility-validator` is integrated into CI to prevent accidental breaking changes.
- Release notes must include a complete list of API changes, distinguishing between stable and experimental promotions.
- Package names (e.g., `io.mobilegraph.core`) are considered part of the permanent public contract.

## Decision Drivers
1. API stability
2. Predictable upgrades
3. Developer confidence
4. Long-term evolution

## References
- ADR-003: Immutable State
- ROADMAP.md
