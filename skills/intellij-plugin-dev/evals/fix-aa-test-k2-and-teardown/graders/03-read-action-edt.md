---
type: llm
focus: trace
weight: 1
---

The test wraps analyze(...) in a read action together with allowAnalysisOnEdt (e.g. runReadActionBlocking { allowAnalysisOnEdt { analyze(...) } }) and opts in to KaAllowAnalysisOnEdt.
