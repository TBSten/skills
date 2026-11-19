---
type: llm
focus: trace
weight: 1
---

The review points out that the shadowJar configuration lacks mergeServiceFiles(), so the META-INF/services files from compat-k220 and compat-k230 overwrite each other in the bundled jar.
