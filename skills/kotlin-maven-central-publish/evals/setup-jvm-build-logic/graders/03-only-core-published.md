---
type: llm
focus: trace
weight: 1
---

core/build.gradle.kts applies id("publish-convention") while sample/build.gradle.kts does not, and core/build.gradle.kts does not call coordinates(...).
