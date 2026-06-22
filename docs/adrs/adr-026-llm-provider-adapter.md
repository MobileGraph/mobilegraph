# ADR-026: LLM Provider Adapter Architecture

## Status
Accepted

## Context
Each LLM provider has unique request/response formats, authentication mechanisms, streaming protocols, and error codes. Provider adapters must handle these differences while exposing a unified ChatModel interface.

## Decision
Each provider adapter is a separate artifact (e.g., mobilegraph-openai, mobilegraph-anthropic). Adapters implement ChatModel, EmbeddingModel, or both. Each adapter contains: API client, request mapper, response mapper, stream handler, error mapper, and cost calculator. Provider adapters are not part of the core framework distribution.

## Consequences
Applications include only the provider adapters they need. The adapter interface contract is defined in mobilegraph-models which is the only core dependency. Provider adapter artifacts have provider SDK dependencies that are transitive but not imposed on the core framework.