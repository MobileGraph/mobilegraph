# ADR-015: Tool Schema Generation from Kotlin Data Classes

## Status
Accepted

## Context
AI models that support function calling require tool schemas in JSON Schema format (OpenAI) or Anthropic's tool schema format. Developers should not need to write JSON Schema manually—it should be generated from their Kotlin data class definitions.

## Decision
Tool input types are Kotlin data classes annotated with @Serializable. The framework generates JSON Schema from these data classes using a SchemaGenerator that processes kotlinx.serialization descriptors. Provider-specific schema formatters convert the canonical schema to the provider's required format.

## Alternatives Considered
- Manual JSON Schema: Error-prone; schema drifts from actual type definition; poor DX
- Annotation-based schema (custom annotations): More granular control but more annotation overhead
- Runtime reflection: Not KMP compatible

## Tradeoffs
- Pros:
  - Schema is always in sync with actual type definition
  - Type annotations (description, example) are added via @SerialInfo annotations
  - No reflection required—uses serialization descriptors
- Cons:
  - Schema generation from descriptors is less granular than hand-written schemas
  - Complex types (polymorphic, recursive) require careful handling

## Consequences
A @ToolDescription(value = "...") annotation is provided for documenting tool parameters. The SchemaGenerator walks the SerialDescriptor tree to produce JSON Schema. Provider adapters implement SchemaFormatter to produce provider-specific tool definitions.