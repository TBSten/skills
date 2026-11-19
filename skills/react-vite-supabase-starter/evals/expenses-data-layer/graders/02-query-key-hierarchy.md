---
type: llm
focus: trace
weight: 1
---

The list hook uses useQuery with a hierarchical query key whose first element is "expenses" (e.g. ["expenses", user?.id]) and is only enabled when a user is logged in.
