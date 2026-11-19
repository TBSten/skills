---
type: llm
focus: trace
weight: 1
---

The add and delete hooks use the project's useMutate helper with invalidateKeys containing ["expenses"] and pass the Supabase result through handleSupabaseResult.
