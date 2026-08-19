---
paths:
  - <project-name>-ksp/src/main/kotlin/**/ksp/core/*.kt
---

# <project-name>-ksp core root (`ksp/core/*.kt`)

❌ **Never put a `.kt` directly under `core/`.** Every file belongs in one of the sub-packages below
(`core/ArchTest` enforces the list). See `ksp-architecture.md` for the full picture.

| Sub-package | Contents |
|---|---|
| `core/common/` | Parts shared by every generator: naming, property matching, KDoc rendering, visibility, target validation, diagnostics helpers, the write-out helper, `GenerateSourceAnnotation` and its `<Name>SourceAnnotation` implementations, plus per-property function types passed to generators as parameters |
| `core/<featureFun>/` | One sub-package per generation family (e.g. `copyFun`, `combineFun`). Add a new one when a family genuinely differs; do not over-generalise |
| `core/error/` | The exception hierarchy (`<Name>Exception` base + usage / option branches + `Unknown` with a report link). A **leaf**: imports `util` and nothing else, which is what lets `options` throw from it |

✅ core does: project-specific generation. Factor shared parts into `common` and let the family
sub-packages compose them.

❌ core does NOT:
- import `feature`
- import `ProcessContext` — take `context(options, logger)` instead
- import anything but `util` from `core/error/`
- absorb helpers that are actually generic (those belong in `util/`)
- exceed 500 lines in a file

## The `GenerateSourceAnnotation` pattern

Per-annotation differences are expressed as **overridable members with safe defaults**, never as a
`when` over the implementations. The interface is deliberately not `sealed`, so adding an annotation
means adding one implementation file and editing no existing branch.

Split the rules by scope:
- **Once per generated function** (skip an `object` target, warn on an ineffective marker, …) → a
  member of `GenerateSourceAnnotation`.
- **Per property / per parameter** (is this excluded, which source property maps here) → a standalone
  function type that the generator takes as an ordinary parameter, so a caller not driving generation
  from an annotation can pass a plain lambda.
