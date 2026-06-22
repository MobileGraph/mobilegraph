# ADR-028: Background Execution Strategy for Android and iOS

## Status
Accepted

## Context
Long-running AI operations (document indexing, agent workflows, background knowledge updates) must continue when the app is in the background. Android and iOS have different background execution models and strict limits.

## Decision
- Android: Long-running AI operations use WorkManager with a CoroutineWorker. Graph state is checkpointed before the OS kills the process. Work is tagged with execution IDs for recovery.
- iOS: Background tasks use BGTaskScheduler for deferred work and BGProcessingTask for longer operations. State is checkpointed before the background time limit.

Both platforms hook the MobileGraphRuntime's lifecycle listeners to trigger checkpoint saves before process suspension.

## Consequences
The BackgroundExecutor expect/actual interface abstracts platform differences. Android implementation uses WorkManager constraints (network required for provider calls, battery not low). iOS implementation registers task identifiers in Info.plist. Background work limits are enforced: max 15 minutes on Android, OS-defined on iOS.