# ADR-027: Local Vector Store Using SQLite with VSS Extension

## Status
Accepted

## Context
Mobile applications need vector search capability for offline-first RAG. Commercial vector databases (Pinecone, Weaviate) require network. On-device options include: custom ANN index, SQLite vector extensions, custom B-tree implementation, or flat file.

## Decision
The local vector store uses sqlite-vss (SQLite Vector Similarity Search extension) via SQLDelight for vector operations and SQLite FTS5 for full-text search. This provides both vector and keyword search on-device with no additional processes or network.

## Alternatives Considered
- Faiss (Facebook AI Similarity Search): Excellent performance; requires native compilation; complex KMP integration
- Custom LSH implementation: Significant implementation complexity; maintenance burden
- Flat file with linear scan: Simple; O(n) search; not scalable beyond ~1000 vectors
- Milvus Lite: Not KMP compatible

## Tradeoffs
- Pros:
  - Pure SQL uses existing SQLite infrastructure
  - No additional native dependencies beyond SQLite (already on every mobile device)
  - ACID compliant - consistent with checkpoint storage
- Cons:
  - Performance limits at ~100k vectors (sufficient for most mobile use cases)
  - Less performant than dedicated ANN libraries (Faiss, HNSWlib) for large indexes

## Consequences
LocalVectorStore is the default knowledge provider for offline scenarios. For production scale (>100k vectors), remote providers (Pinecone, Weaviate) are recommended. The KnowledgeSource abstraction enables transparent switching.