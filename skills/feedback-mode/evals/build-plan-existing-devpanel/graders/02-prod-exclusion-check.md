---
type: llm
focus: trace
weight: 1
---

The plan keeps the feature out of production at build time (import.meta.env.DEV with dynamic import / dead-code elimination) and includes checking the production build output with the skill's check-no-feedback-in-prod.sh script.
