# ADR 0001: A2A (Agent-to-Agent) Protocol Integration

## Status

Accepted

## Context

MobileGraph is an AI agent runtime for mobile and edge environments. While MobileGraph already supports MCP (Model Context Protocol) for connecting agents to tools and resources, there is a need for **agent-to-agent interoperability** — the ability for a MobileGraph agent to delegate work to remote agents built on different frameworks.

### Problem

- MobileGraph agents cannot communicate with agents from other ecosystems
- Complex workflows that require specialized remote agents are not possible
- There is no standard way to discover, authenticate, and interact with remote agents

### Drivers

- Growing ecosystem of specialized AI agents
- Need for agent orchestration across frameworks
- Industry standardization via A2A protocol (Linux Foundation)

## Decision

Implement A2A protocol support as a **clean, modular protocol adapter** in a separate optional module (`mobilegraph-a2a`).

### Key Design Decisions

1. **A2A as a Protocol Adapter**: A2A is implemented as an integration layer, not embedded in agent reasoning. Remote agents are treated as external, untrusted systems.

2. **Client-Only Module**: The initial implementation focuses on A2A client capabilities (consuming remote agents). Server capabilities (exposing MobileGraph agents) are deferred — mobile apps typically don't run HTTP servers.

3. **Separate Module**: A2A is an optional Gradle module, not a core dependency. Existing apps work without it.

4. **Reuse Existing Abstractions**: 
   - `GraphNode` for graph integration (`A2ANode`)
   - `MobileGraphPlugin` for plugin architecture
   - `MobileGraphEvent` for observability
   - `MobileGraphException` for error hierarchy
   - `ExecutionResult.AwaitingReview` for input-required states

5. **Direct Protocol Implementation**: Rather than depending on a third-party A2A SDK (none is mature for KMP), the protocol is implemented directly using Ktor and kotlinx.serialization, keeping the protocol layer isolated and thoroughly tested.

## Architecture

```
MobileGraph Runtime
       │
       ├── MCP Layer (tools/resources)
       │
       └── A2A Layer (optional)
             │
             ├── A2AClient (JSON-RPC 2.0)
             ├── A2ANode (GraphNode)
             └── A2APlugin (MobileGraphPlugin)
```

## Alternatives Considered

### Custom REST API
- **Pros**: Simple, flexible
- **Cons**: No standard discovery, no interoperability, custom per-agent

### MCP-Only Delegation
- **Pros**: Reuses existing infrastructure
- **Cons**: MCP is designed for tool/resource access, not agent delegation. Different lifecycle model.

### Proprietary Agent Protocol
- **Pros**: Optimized for MobileGraph
- **Cons**: No interoperability, vendor lock-in, maintenance burden

### A2A Protocol (Chosen)
- **Pros**: Open standard, industry-backed, interoperable, well-defined lifecycle
- **Cons**: Protocol complexity, evolving spec

## Consequences

### Positive
- **Interoperability**: MobileGraph agents can work with any A2A-compatible agent
- **Ecosystem access**: Access to growing ecosystem of specialized agents
- **Modularity**: Optional module, no impact on existing functionality
- **Observability**: Full tracing via MobileGraphEvent

### Negative
- **Complexity**: Additional protocol layer to maintain
- **Networking**: Requires network connectivity (mobile constraint)
- **Security surface**: Remote agents are untrusted external systems
- **Protocol evolution**: A2A spec may change, requiring updates

### Mitigations
- Protocol types are isolated in `models/` package
- Trust boundary enforced by `A2ATaskAdapter`
- Network resilience via timeouts, retries, reconnection
- Comprehensive test suite for wire compatibility
