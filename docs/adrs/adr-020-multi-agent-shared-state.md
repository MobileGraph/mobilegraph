# ADR-020: Multi-Agent Communication via Shared Graph State

## Status
Accepted

## Context
Multi-agent systems require agents to communicate results to each other. Options include: shared mutable state, message passing, blackboard systems, and graph state propagation.

## Decision
In MobileGraph, agents in a multi-agent system communicate through the shared graph state S. Each agent node receives state, executes, and returns updated state. The SupervisorAgent coordinates by reading agent outputs from state and delegating tasks by updating state fields that sub-agents read. No direct agent-to-agent communication channels.

## Alternatives Considered
- Direct message passing: Requires agent discovery and message routing infrastructure; complex
- Shared mutable blackboard: Concurrency hazards; difficult to checkpoint; race conditions
- Event-driven messaging: Powerful but complex; overkill for most mobile multi-agent scenarios

## Tradeoffs
- Pros:
  - State-based communication is naturally checkpointable
  - No additional communication infrastructure
  - Deterministic communication order is graph-topology defined
  - Observable—all inter-agent communication visible in state transitions
- Cons:
  - State can grow large in complex multi-agent systems
  - Less flexible than direct messaging for highly dynamic agent topologies

## Consequences
Multi-agent state classes include typed fields for each agent's output and the supervisor's task assignments. The state schema validates that all required fields are present. Sub-agent nodes are responsible for reading their task assignment from state and writing their results to state.