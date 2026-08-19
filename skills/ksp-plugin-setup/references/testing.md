# Test infrastructure: what to copy and how to extend it

The test **conventions** ship into the generated project as `assets/rules/ksp-test.md` (layout, the
five test kinds, golden format, naming rules) — that is the single source of truth. This file
explains the infrastructure files themselves, so you can stand them up and grow them.

## What each file does

| File (under `testing/`) | Role |
|---|---|
| `compile/Compilation.kt` | `compileWithProcessor` — a real Kotlin + KSP compilation via kctfork — plus the result wrapper, `generatedSourceText()` and `normalizedCompilerOutput()` |
| `compile/SnapshotCompile.kt` | `runCompileSnapshotTest` — compile and pin the fixed facet set in one golden. Also options → `ksp { arg(...) }` rendering |
| `snapshot/SnapshotAssertion.kt` | Facet-based Markdown golden comparison, path derivation, the update flag, fence expansion |
| `generator/Generator.kt` | The `Generator` abstraction (`representativeValues()` + `arb()`), its DSL, and `union` / `cartesian` / `map` / `withRepresentativeValues` |
| `generator/<Name>OptionsGenerator.kt` | The option axis: full product for PBT, narrowed representatives for goldens |
| `poet/Scenario.kt` | KotlinPoet input builders (`SnapshotScenario`, `Prop`, `dataClass`, `snapshotScenarios`) |
| `konsist/KonsistSupport.kt` | Shared Konsist scope and layer predicates for the three architecture specs |

Copy them in that order — each depends only on the ones above it.

## Two decisions worth understanding before you extend this

**Why `Generator` has two sides.** `representativeValues()` is deterministic and small; `arb()` is
the full space. A snapshot suite and a property suite then share one definition of "the interesting
inputs for this axis" instead of drifting apart. `withRepresentativeValues` is the lever that keeps
the golden matrix affordable while leaving `arb()` untouched.

**Why inputs are built with KotlinPoet, not string literals.** Families compose: a "property shape"
axis and an "exclude" axis can be crossed mechanically, and every case renders through one formatter,
so goldens never churn on whitespace. Generation itself still uses plain string append — KotlinPoet
is a test-only dependency.

## Standing it up

1. Add the test dependencies: `kctforkCore`, `kctforkKsp`, `konsist`, `kotlinPoet`, `kotest`,
   `kotestRunnerJunit5`, `kotestProperty`.
2. Configure the `test` task with the four required settings (see `build-and-ci.md`).
3. Copy the `testing/` files and rename the package + the `ksppluginsetup.*` option prefix and
   snapshot-update property.
4. Copy the three Konsist specs (`AllKotlinFilesTest`, `feature/ArchTest`, `core/ArchTest`) and
   update `KonsistSupport.kt`'s constants: root package, module name, allowed root files, allowed
   `core` sub-packages, composition-root type names.
5. Run once with the update flag to record the first goldens, then **read them** before committing —
   the first recording is the moment to catch wrong output, and after that a diff is all you see.

## Adding a feature's tests

Create `feature/<name>/` with all five files, even if some start as stubs — a missing file is
invisible, a stub is a visible TODO. Then:

- Put curated cases in `scenario/`, one file per **family** (one axis of variation), each exposing a
  `Generator<SnapshotScenario>`.
- In `<Name>SnapshotTest`, `union` the families and `cartesian` them with the option generator; each
  point becomes one test case and one golden.
- Record in the spec's KDoc what you deliberately did **not** cover and why. A reader should never
  have to guess whether an absent case is an oversight or a decision.

## Konsist notes

- Scope with `Konsist.scopeFromProduction(moduleName = "<project-name>-ksp", sourceSetName = "main")`
  so the test source set is excluded.
- Konsist does **not** model context parameters. Check them by inspecting the declaration text, and
  cut at `text.substringBefore('{')` so a `context(` inside a string literal or comment in the body
  cannot make the assertion pass spuriously.
- The checks are import-based. That is why the project convention is to import referenced symbols
  rather than use fully-qualified inline references — an inline FQN slips past every rule.

## Property-based tests

The `Generator` abstraction gives every axis an `arb()`, but wiring the PBT files is a separate step:
generators existing is not the same as property tests running. If `<Name>PropertyTest` is still a
stub, say so — do not describe the project as using PBT.

## Related skill

For snapshot testing in a KMP/Compose **application** (image and semantics snapshots, PBT base
classes, diff scripts) use the **`kmp-snapshot-testing-setup`** skill instead — it solves a different
problem from the compiler-output goldens described here.
