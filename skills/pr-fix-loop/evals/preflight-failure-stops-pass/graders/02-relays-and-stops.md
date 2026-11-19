---
type: llm
focus: trace
weight: 1
---

After the script failed its preflight, the agent relayed the script's error and fix instruction (e.g. run 'gh auth login' or commit/stash the changes) to the user and stopped the pass rather than working around it.
