---
name: Feature request
about: Suggest a new feature or enhancement for MobileGraph
title: ''
labels: ''
assignees: ''

---

# Feature Summary

Provide a short and clear description of the feature.

Example:

> Add support for Azure OpenAI Chat Models.

---

# Problem Statement

What problem are you trying to solve?

Describe the current limitation or pain point.

---

# Proposed Solution

Describe your proposed solution.

Include API examples if applicable.

Example:

```kotlin
MobileGraph.initialize {
    withModels {
        chat("azure-gpt4", AzureChatModel(...))
    }
}
```

---

# Alternative Solutions

Describe any alternative approaches you considered.

---

# Use Case

Explain how this feature would be used in a real application.

Example:

* Enterprise applications
* Android apps
* Kotlin Multiplatform
* Offline AI
* Agent workflows

---

# Affected Module(s)

Select all that apply.

* [ ] mobilegraph-core
* [ ] mobilegraph-openai
* [ ] mobilegraph-google
* [ ] mobilegraph-anthropic
* [ ] mobilegraph-memory
* [ ] mobilegraph-tools
* [ ] mobilegraph-parsers
* [ ] mobilegraph-documents
* [ ] mobilegraph-retrieval
* [ ] mobilegraph-agents
* [ ] Documentation
* [ ] Samples
* [ ] Other

---

# Is this a Breaking Change?

* [ ] Yes
* [ ] No

If yes, explain why.

---

# Additional Context

Add screenshots, links, references, research, or other relevant information.

---

# Contribution

Would you be interested in implementing this feature?

* [ ] Yes
* [ ] No
* [ ] I need guidance

---

# Checklist

Before submitting this request:

* [ ] I searched existing issues.
* [ ] This feature aligns with MobileGraph's vision.
* [ ] I have described the problem clearly.
* [ ] I included example usage where applicable.
