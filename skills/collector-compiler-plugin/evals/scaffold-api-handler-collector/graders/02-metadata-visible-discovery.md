---
type: llm
focus: trace
weight: 1
---

The IR extension registers the generated hint functions with registerFunctionAsMetadataVisible(...) and, in the consuming module, looks them up via referenceFunctions(CallableId(...hints package..., collect_<tag>)) to recover the collected classes from the parameter type.
