# ADR-024: Experimental API Annotation Strategy

## Status
Accepted

## Context
The framework must evolve while providing stability. New features should be released early for feedback but without full backward compatibility burden.

## Decision
@ExperimentalMobileGraph is applied to all APIs not yet promoted to stable. Using experimental APIs requires opt-in via @OptIn(ExperimentalMobileGraph::class) at the call site. This is the same mechanism used by Kotlin standard library experimental APIs. A feature-level opt-in system is also provided for more granular control.

## Consequences
Experimental APIs can be broken between any releases. Migration guides are provided when experimental APIs are promoted (and changed) to stable. The documentation clearly distinguishes stable and experimental APIs. IDE tooling warns on experimental API usage.