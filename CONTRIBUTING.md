# Contributing to MobileGraph ADK

Thank you for your interest in contributing to MobileGraph! We are building the first professional Agent Development Kit for the mobile ecosystem.

## Development Requirements
- **JDK**: 17+
- **Android SDK**: API 34+
- **IDE**: Android Studio or IntelliJ IDEA with the KMP plugin.

## Quality Standards
We maintain high standards for code quality and testing:
1.  **Tests**: All logic changes should include tests in `commonTest`. We aim for >95% coverage.
2.  **Formatting**: We use `Spotless` with `ktlint`. Run `./gradlew spotlessApply` before committing.
3.  **Static Analysis**: We use `Detekt`. Run `./gradlew detekt` to check for issues.

## Workflow
1.  **Fork** and create a feature branch (`git checkout -b feature/cool-new-thing`).
2.  **Discuss** major architectural changes via a GitHub Issue or by proposing a new ADR in `docs/adrs`.
3.  **Submit a PR** once tests pass and linting is clean.

Please read our [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) before participating.
