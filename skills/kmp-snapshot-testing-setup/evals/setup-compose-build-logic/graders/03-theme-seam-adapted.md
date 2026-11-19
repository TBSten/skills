---
type: llm
focus: trace
weight: 1
---

The generated ComposeSnapshot.kt no longer references the placeholder AppTheme.Provider / WithTestGraph and instead wraps content with AcmeTheme (or removes the wrapper), and ui/core/testing/build.gradle.kts depends on :core:designsystem.
