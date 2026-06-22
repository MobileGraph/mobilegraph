# ADR-017: Hybrid Retrieval as the Recommended Default

## Status
Accepted

## Context
RAG applications must choose a retrieval strategy: pure vector (semantic) search, pure keyword (BM25) search, or hybrid combining both. Each has different precision/recall tradeoffs for different query types.

## Decision
MobileGraph recommends HybridRetriever as the default retrieval strategy. The default fusion algorithm is Reciprocal Rank Fusion (RRF). Pure vector and keyword retrievers are available but the documentation and quick-start guides use hybrid retrieval as the starting point.

## Alternatives Considered
- Vector-only default: Better for semantic queries; poor for named entity and keyword queries (e.g., "find document MG-2024-001")
- Keyword-only default: Good for exact matching; poor for semantic queries
- Leave decision to developer: Common framework approach; leads to suboptimal defaults

## Tradeoffs
- Pros:
  - Consistently better recall across diverse query types
  - RRF is parameter-free (no weight tuning required for default behavior)
  - Degrades gracefully if one index is unavailable
- Cons:
  - Requires both vector and keyword indexes (more storage)
  - Slightly higher latency than single-strategy retrieval

## Consequences
The local vector store (LocalVectorStore) implements both vector search (sqlite-vss extension) and full-text search (SQLite FTS5). The HybridRetriever can operate entirely on-device without external dependencies.