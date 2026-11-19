---
type: llm
focus: trace
weight: 1
---

shared/build.gradle.kts is updated to apply the kotlinx serialization plugin and add a kotlinx-serialization-json dependency to commonMain, without adding a duplicate kotlinx-coroutines-core dependency.
