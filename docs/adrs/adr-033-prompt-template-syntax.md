# ADR-008: Prompt Template Variable Syntax

## Status
Accepted

## Context
Prompt templates need variable substitution. The syntax must be unambiguous, readable, and not conflict with common natural language patterns or code samples that appear in prompts.

## Decision
MobileGraph uses {variable_name} for required variables and {variable_name?} for optional variables. Double curly braces {{literal_brace}} escape to a literal brace. This syntax is consistent with Python's str.format(), familiar to most developers, and unambiguous in AI prompt contexts.

## Alternatives Considered
- Mustache {{variable}}: Conflicts with escaped literal brace representation; Mustache has more syntax complexity than needed
- Jinja2 {{ variable }}: Python-ecosystem convention; unfamiliar to Kotlin developers; complex tag system
- Dollar sign \$variable: Conflicts with Kotlin string interpolation; confusing in Kotlin codebases
- XML-style <variable name="x"/>: Verbose; poor readability in long prompts

## Tradeoffs
- Pros:
  - Familiar to developers from Python f-string and str.format() conventions
  - Unambiguous parsing
  - Optional variable syntax {name?} is a clear extension
- Cons:
  - JSON examples in prompts may need escaping if they contain single braces (mitigated by context-aware parser)

## Consequences
The template parser is implemented with a simple state machine. Validation runs at template construction time, reporting all missing variables clearly. The formatInstructions() contract includes the list of required and optional variables.