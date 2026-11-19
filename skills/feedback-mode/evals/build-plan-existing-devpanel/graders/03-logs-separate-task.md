---
type: llm
focus: trace
weight: 1
---

The plan wires logs by reading the existing logger's recentLogs() (e.g. as getRecentLogs) and explicitly leaves any changes to the logging infrastructure to a separate task.
