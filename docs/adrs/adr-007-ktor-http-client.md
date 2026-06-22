# ADR-007: Ktor as the HTTP Client for All Network Communication

## Status
Accepted

## Context
MobileGraph calls LLM provider APIs, vector database APIs, and observability endpoints over HTTP. A network client library must be selected.

## Decision
Ktor Client is used for all HTTP communication in MobileGraph. It is KMP-native, uses Kotlin coroutines natively, and supports all target platforms with pluggable engines (OkHttp on Android, Darwin on iOS, CIO on JVM).

## Alternatives Considered
- OkHttp: Android/JVM only; not KMP
- NSURLSession (direct): iOS only; not KMP
- Retrofit: Annotation-based, not KMP, requires OkHttp
- Platform-native http clients: Cannot be shared via commonMain

## Tradeoffs
- Pros:
  - KMP native
  - Coroutine native
  - Pluggable engines (OkHttp on Android for superior connection pooling)
  - Built-in JSON, logging, retry plugins
- Cons:
  - Less mature than OkHttp for Android; some edge cases require platform-specific tuning

## Consequences
LLM provider adapter implementations use Ktor's HttpClient through a MobileGraphHttpClient wrapper that applies security middleware, observability instrumentation, and retry policies. Certificate pinning is configured through Ktor's TLS configuration.