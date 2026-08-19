---
paths:
  - <project-name>-ksp/src/main/kotlin/**/*.kt
---

# <project-name>-ksp Architecture (feature / core / options / util)

The processor module is **3 layers + a composition root + a leaf context**. This is an `internal`
implementation convention; it never changes the public API (runtime annotations and KSP options).

## Layers

| Layer | Location | Responsibility |
|---|---|---|
| top-level (root) | `ksp/*.kt` | KSP entry point and cross-cutting infra only (`<Name>SymbolProcessor` / `Provider` / `ProcessContext`). See `ksp-top-level.md` |
| feature | `ksp/feature/<name>/` | Per-annotation entry: discover → extract arguments → validate → call core. Holds no generation logic. See `ksp-feature-top-level.md` |
| core | `ksp/core/<sub>/` | Project-specific generation logic, plus the exception hierarchy in `core/error`. See `ksp-core-top-level.md` |
| options | `ksp/options/` | KSP option model and parsing. A cross-cutting model every layer may read |
| util | `ksp/util/` | Generic helpers only — nothing project-specific. KSP-flavoured helpers go in `util/ksp/` |

**Do not create a fifth top-level package.** If something fits none of these, it usually belongs in
`core/<new-sub>` or `util/`.

## Dependency direction (one-way)

```
<Name>SymbolProcessor (root)
   ├─▶ feature/<name> ─▶ core/<sub> ─▶ util
   └─▶ ProcessContext (leaf)
feature ─▶ ProcessContext   (the only upward dependency; ProcessContext is a leaf, so no cycle)
```

| Layer | May import | Must NOT import |
|---|---|---|
| `util/` (top level) | Kotlin stdlib only | core, feature, root, runtime module, **KSP API** (put those in `util/ksp/`), any project-specific type |
| `util/ksp/` | Kotlin stdlib, KSP API (generic use) | core, feature, root, runtime module, project-specific types |
| `core/` (except `error`) | util, options, `core/error`, runtime module, KSP API | **feature**, `<Name>SymbolProcessor` / `Provider`, **`ProcessContext`** |
| `core/error/` | util only | every other core sub-package, options, feature, root, KSP API |
| `options/` | util, `core/error` (to throw parse failures), runtime module | other core sub-packages, feature, root, KSP API |
| `feature/<name>/` | core, options, util, runtime module, KSP API, **`ProcessContext`** | **another `feature/<name>` (no feature-to-feature dependency)**, `<Name>SymbolProcessor` / `Provider` |
| root | feature, core, options, util, runtime module, KSP API, ProcessContext | generation logic, per-annotation handling |
| `ProcessContext` (leaf) | KSP API, options | feature, core, util, `<Name>SymbolProcessor` |

- **The only upward dependency is `feature → ProcessContext`.** `core` never sees `ProcessContext`;
  it declares the narrower capabilities it needs.
- **The root package holds exactly three files** (processor, provider, context). Nothing else.
- `options` throws from `core/error` while `core` / `feature` / root read `options`. This is the one
  mutual reference between packages, and it is safe because `core/error` is a leaf depending only on
  `util`.
- Boundaries are enforced automatically by [Konsist](https://github.com/LemonAppDev/konsist),
  import-based, in three specs: `AllKotlinFilesTest` (root allow-list, per-file line budget),
  `feature/ArchTest`, `core/ArchTest`. **Change this table → change those specs in the same commit.**

## ProcessContext & context parameters

- Requires `-Xcontext-parameters` (Kotlin 2.2.x).
- `ProcessContext = { resolver, options, codeGenerator, logger }`. **`logger` is non-null** — a
  `KSPLogger?` forces an unreachable fallback branch through every generator.
- Declare only the capabilities a layer needs:
  - feature: `context(ctx: ProcessContext) internal fun processXxx(): List<KSAnnotated>`
  - core: `context(options: <Name>Options, logger: KSPLogger) internal fun Appendable.appendXxx(...)`
- Per-call values (source/target declarations, templates, …) stay ordinary function parameters.

## Naming

- feature: file `Process<Name>.kt`, function `processXxx` (top-level, lowerCamel).
- core generation: `appendXxx` — an `Appendable` extension, string append, no KotlinPoet.
- Per-annotation differences live in `GenerateSourceAnnotation` implementations (`core/common/`),
  named `<Name>SourceAnnotation`.

## Cross-cutting rules

- **Diagnostics**: user misuse is never thrown. Use `logger.error(message, ksNode)` and `return`
  immediately, so the build reports a clean COMPILATION_ERROR with no partial output. Throwing
  surfaces as an opaque INTERNAL_ERROR. Only genuinely unexpected internal states throw.
- **Every diagnostic states a solution**, not only what is wrong.
- **`when` without `else`**, so a new enum/sealed entry becomes a compile error. Never write or
  generate an unsafe `as` cast. Prefer `firstOrNull()` over `first()`.
- **Single source of truth**: naming logic in `core/common`, tokens in the runtime module, options in
  `options/`.
- **File size**: 10–300 lines is the target, 500 the hard ceiling — split by responsibility.
- **Generation**: never write an empty file; keep `Dependencies(aggregating = true, ...)`; escape
  generated identifiers.

## Adding a new annotation

1. Declare the annotation in the runtime module.
2. Add `<Name>SourceAnnotation` (a `GenerateSourceAnnotation` implementation) in `core/common/`,
   overriding only the rules that differ. **Do not add a `when` over the implementations.**
3. Add `feature/<name>/Process<Name>.kt` with `context(ctx: ProcessContext) fun processXxx()`.
4. Reuse the generators in `core/`; extend `core/` if something is missing — never put generation
   logic in `feature`.
5. Register one dispatch line in `<Name>SymbolProcessor.process()`.
6. Add the five test kinds for the feature plus `test/` module data (see `ksp-test.md`).
