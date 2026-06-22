# ADR-006: SQLDelight for Cross-Platform Database Access

## Status
Accepted

## Context
MobileGraph requires persistent storage for: checkpoints, memory, embeddings, audit logs, and offline queues. Android has Room/SQLite, iOS has Core Data/SQLite, JVM has H2/SQLite. A unified database abstraction is needed.

## Decision
SQLDelight is the database library for all structured data storage in MobileGraph. It generates type-safe Kotlin code from SQL schema files and supports Android (SQLite), iOS (SQLite), JVM (JDBC/SQLite), and JavaScript (SQL.js) targets.

## Alternatives Considered
- Room (Android-only): Excludes iOS; not KMP
- Core Data (iOS-only): Excludes Android; not KMP
- Realm: KMP support exists but complex dependency; license considerations
- Raw JDBC: JVM-only; not suitable for KMP

## Tradeoffs
- Pros:
  - Genuine KMP support - single schema definition, platform-specific drivers
  - Type-safe query generation from SQL
  - No reflection
  - Supports SQLite vector extensions for local vector search
- Cons:
  - SQL schema must be written manually
  - Less ORM-style convenience than Room

## Consequences
All database schemas are defined in .sq files and versioned with the framework. Database migrations are defined as named SQL migration files. The mobilegraph-storage module owns all SQLDelight definitions and provides typed repository interfaces.