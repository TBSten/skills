---
paths:
  - <project-name>-ksp/src/main/kotlin/**/ksp/feature/*.kt
---

# <project-name>-ksp feature root (`ksp/feature/*.kt`)

❌ **Never put a `.kt` directly under `feature/`.** Every file belongs in a `feature/<name>/`
sub-directory — one annotation, one directory, no deeper nesting. (`feature/ArchTest` enforces this.)
See `ksp-architecture.md` for the full picture.

Each `feature/<name>/` contains:
- `Process<Name>.kt` — the single entry point
  `context(ctx: ProcessContext) internal fun processXxx(): List<KSAnnotated>`
- helpers specific to that annotation (argument parsing, annotation-specific validation)

✅ A feature does:
- discover with `resolver.getSymbolsWithAnnotation(...)` and `partition { it.validate() }`
- extract and validate annotation arguments
- call a `core` generator and write out through the shared `createNewKotlinFile`
- return the symbols that failed `validate()`, so KSP re-offers them next round

❌ A feature does NOT:
- assemble generated code (→ `core/`)
- import another `feature/<name>` — wanting shared code is the signal to move it down into `core`
- hold logic that is not annotation-specific (→ `core/common` or `util/`)
