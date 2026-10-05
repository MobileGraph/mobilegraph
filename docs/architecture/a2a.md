# A2A (Agent-to-Agent) Protocol Integration

## Overview

MobileGraph provides first-class support for the [A2A (Agent-to-Agent) protocol](https://a2aproject.github.io/A2A/), enabling MobileGraph agents to discover and communicate with remote A2A-compatible agents.

Fully compliant with **A2A Protocol v1.0** (with automatic fallback to v0.3 / v0.2.1).

### A2A vs MCP

| Aspect | MCP | A2A |
|--------|-----|-----|
| **Purpose** | Agent ↔ Tools/Resources | Agent ↔ Agent |
| **Scope** | Vertical (capabilities) | Horizontal (collaboration) |
| **Discovery** | Server configuration | Agent Card (`/.well-known/agent-card.json`) |
| **Task Model** | Request/Response | Stateful task lifecycle |
| **Streaming** | SSE (for server events) | SSE (for task updates) |

```
                 MobileGraph Agent
                        │
              ┌─────────┴─────────┐
              │                   │
             MCP                 A2A
              │                   │
              v                   v
        Local/Remote         Remote Agent
        Tools/Resources      (Coding, Analysis, etc.)
```

## Architecture

### A2A Client Architecture

The A2A client (`:intelligence:mobilegraph-a2a`) is modularized into dedicated components:

- **`A2AClient`**: High-level facade for discovery, messaging, task management, polling, and lifecycle control.
- **`A2AMethods`**: Dual-version JSON-RPC method name mapping (`SendMessage`, `SendStreamingMessage`, `GetTask`, `CancelTask` vs `message/send`, `message/stream`, `tasks/get`, `tasks/cancel`).
- **`A2AResponseParser`**: Polymorphic parser supporting direct tasks and A2A v1.0 wrapper patterns (`task`, `statusUpdate`, `artifactUpdate`).
- **`A2AStreamProcessor`**: SSE event stream processor with automatic payload unwrapping.

```
MobileGraph Agent
       │
       v
   A2AClient (JSON-RPC 2.0 / HTTPS)
       │
       ├── discoverAgent()   → AgentCard
       ├── sendMessage()     → SendMessage (A2A v1.0) / message/send (v0.3)
       ├── streamMessage()   → SendStreamingMessage (A2A v1.0) / message/stream (v0.3)
       ├── getTask()         → GetTask (A2A v1.0) / tasks/get (v0.3)
       ├── cancelTask()      → CancelTask (A2A v1.0) / tasks/cancel (v0.3)
       └── pollTask()        → Flow<A2ATask>
```

### Integration with MobileGraph Graph

The `A2ANode` is a `GraphNode` implementation that delegates work to remote agents:

```
Previous Node → A2ANode → Remote A2A Agent → A2ANode → Next Node
```

States are mapped as follows:

| A2A Task State | MobileGraph Mapping |
|---|---|
| `submitted` | Task created, execution queued |
| `working` | Execution in progress |
| `input-required` | `ExecutionResult.AwaitingReview` |
| `completed` | `ExecutionResult.Success` |
| `failed` | `ExecutionResult.Error` |
| `canceled` | Cancellation |

## Agent Card

Agent Cards are JSON documents hosted at `/.well-known/agent-card.json` that describe a remote agent's:

- **Identity**: name, description, version
- **Endpoint & Interfaces**: A2A JSON-RPC URL, `supportedInterfaces`, `preferredTransport`
- **Capabilities**: streaming, push notifications
- **Skills**: available functions/services
- **Security**: authentication requirements

## Task Lifecycle

```
submitted
   │
   v
working
   │
   ├──→ input-required ──→ working
   │
   ├──→ completed
   │
   ├──→ failed
   │
   └──→ canceled
```

## Streaming

When a remote agent supports streaming, the client uses SSE (Server-Sent Events) via the `SendStreamingMessage` (or `message/stream`) JSON-RPC method. Events include:

- `TaskStatusUpdate` — state changes
- `TaskArtifactUpdate` — incremental artifacts
- `TaskComplete` — final result
- `StreamError` — error event

## Artifacts

Artifacts are the output/deliverables of A2A tasks. They support multi-modal content:

- **TextPart**: Plain text
- **FilePart**: Files (inline base64 or URI reference)
- **DataPart**: Structured JSON data

## Authentication

A2A authentication is driven by the Agent Card's `securitySchemes`. Supported providers:

- `BearerTokenAuthProvider` — OAuth2 / JWT tokens
- `ApiKeyAuthProvider` — API key in header/query
- `NoAuthProvider` — No authentication

## Error Handling

All A2A errors extend `MobileGraphException`:

- `A2ADiscoveryException` — Agent Card fetch/parse failures
- `A2AProtocolException` — JSON-RPC protocol errors
- `A2ATaskNotFoundException` — Task not found
- `A2ATimeoutException` — Operation timeout
- `A2AStreamException` — SSE streaming errors
- `A2ANetworkException` — Transport-level failures
- `A2AValidationException` — Response validation failures
- `A2AAuthenticationException` — Authentication failures

## Observability

A2A operations publish `MobileGraphEvent` for correlation:

- `A2ATaskSubmitted` — task sent to remote agent
- `A2ATaskStatusChanged` — status update received
- `A2AArtifactReceived` — artifact received
- `A2ATaskCompleted` — task completed successfully
- `A2ATaskFailed` — task failed

Each event includes `traceId`, `requestId`, `taskId`, `remoteAgent`, and `nodeId` for full correlation.
