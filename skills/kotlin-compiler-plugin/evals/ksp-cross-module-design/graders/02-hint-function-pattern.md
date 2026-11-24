---
type: llm
focus: trace
weight: 1
---

The response proposes a compiler-plugin hint-function design: each upstream module generates hint functions in a fixed package whose parameter type is the annotated class, and the downstream module finds those functions (e.g. via referenceFunctions / CallableId) and recovers the classes from the parameter types.
