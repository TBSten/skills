---
type: llm
focus: trace
weight: 1
---

The new expenses page component does not import the supabase client (@/lib/supabase) directly; all expenses data access lives in hooks under apps/web/src/data/expenses/.
