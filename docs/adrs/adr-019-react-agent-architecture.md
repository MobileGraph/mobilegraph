# ADR-019: ReAct as the Default Agent Architecture

## Status
Accepted

## Context
Multiple agent architectures exist: ReAct (Reason + Act), Plan-and-Execute, LATS, Chain-of-Thought with Tools, Reflexion. The framework must provide a default that works well for the majority of use cases.

## Decision
ReAct (Yao et al., 2022) is the default agent architecture. It interleaves reasoning and acting in a loop: $Think \rightarrow Act \rightarrow Observe \rightarrow Think...$ until a final answer is reached. ReAct is well-understood, robust across model families, and performs well for tool-using agents.

## Alternatives Considered
- Plan-and-Execute: Better for multi-step tasks requiring upfront planning; more complex; worse for reactive tasks
- Chain-of-Thought only: No tool use capability
- LATS: Requires tree search; significantly more expensive in token cost
- Custom: Punt the decision to developers; inconsistent ecosystem behavior

## Tradeoffs
- Pros:
  - Widely understood architecture with extensive research validation
  - Simple enough to explain and reason about
  - Works with all major model families
  - Easily observable - each thought/act/observe step is an AgentEvent
- Cons:
  - Can get stuck in reasoning loops (mitigated by max iteration limit)
  - Less efficient than Plan-and-Execute for clearly defined multi-step tasks

## Consequences
PlannerAgent and SupervisorAgent are provided as alternatives. Custom agent architectures implement the Agent interface. The Agent Loop controller is reusable across all agent types for common functionality (step counting, interruption, checkpointing).