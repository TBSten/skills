---
type: llm
focus: trace
weight: 1
---

The review points out that CompatContext.load() picks the first factory with minVersion <= current and should instead pick the factory with the largest minVersion that is <= the current Kotlin version.
