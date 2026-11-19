---
type: llm
focus: trace
weight: 1
---

tearDown() is wrapped with a LoggedErrorProcessor (or equivalent helper) that suppresses only the known unrelated logged errors such as Vue / Lsp ones, delegating all other errors so they still fail the test.
