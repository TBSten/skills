---
type: llm
focus: trace
weight: 1
---

For the lint failure the agent does not plan a hand-made code fix because no fix-ci-lint skill exists; it reports the evidence to the user instead, while delegating the apiCheck failure to fix-ci-binary.
