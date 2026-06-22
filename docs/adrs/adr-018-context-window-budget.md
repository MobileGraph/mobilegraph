# ADR-018: Context Window Budget Management in Prompt Composer

## Status
Accepted

## Context
Complex prompts composed from system instructions, memory, retrieved context, tool schemas, and conversation history can easily exceed model context window limits. Without budget management, prompts silently truncate or cause API errors.

## Decision
The PromptComposer implements a token budget manager that estimates token counts for each PromptComponent and enforces a configurable maximum. Components have priorities; lower-priority components are reduced or removed when the budget is exceeded. System prompts have highest priority; retrieved context has lower priority than recent conversation history.

### Default Priority Order (highest to lowest)
1. System instructions
2. Tool schemas
3. Recent conversation history (last N turns)
4. Retrieved context
5. Few-shot examples
6. Older conversation history

## Alternatives Considered
- No budget management: Causes API errors or silent truncation at the model level
- Hard truncation: Removes content without semantic consideration
- Error on overflow: Forces developers to manage context manually; poor DX

## Tradeoffs
- Pros:
  - Prompts always fit within context window
  - Priority-based reduction preserves most important context
  - Transparent - budget decisions are logged in traces
- Cons:
  - Token counting is approximate (character-based estimation or tiktoken)
  - Priority system may not fit all use cases (customizable via configuration)

## Consequences
Token estimation uses a TokenCounter interface with a fast character-ratio estimator for default use and optional tiktoken integration for accuracy. Budget decisions are recorded in the prompt trace span.