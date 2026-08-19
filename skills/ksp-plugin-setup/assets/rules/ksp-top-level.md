---
paths:
  - <project-name>-ksp/src/main/kotlin/**/ksp/*.kt
---

# <project-name>-ksp top level (`ksp/*.kt`)

The root package is **orchestration and cross-cutting types only**. See `ksp-architecture.md` for the
full picture.

Allowed here, and nothing else:

- `<Name>SymbolProcessor.kt` — `process()` dispatches to every feature in order; parses options and
  aggregates deferred symbols.
- `<Name>SymbolProcessorProvider.kt` — the KSP provider (pulls `ProcessContext`'s ingredients out of
  the environment).
- `ProcessContext.kt` — `{ resolver, options, codeGenerator, logger }`. Leaf infra: imports neither
  `feature` nor `core`.

❌ Do not:
- write generation logic here (→ `core/`)
- write per-annotation handling here (→ `feature/<name>/`)
- write generic helpers here (→ `util/`)
- write exceptions here (→ `core/error/`)
- write the option model or its parsing here (→ `options/`)
- import `feature` or `core` from `ProcessContext.kt`

This is enforced by the root allow-list in `AllKotlinFilesTest`.
