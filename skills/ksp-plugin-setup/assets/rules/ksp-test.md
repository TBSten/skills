---
paths:
  - <project-name>-ksp/src/test/**
  - test/src/**
---

# Tests

Three suites with distinct jobs — do not blur them:

1. **`<project-name>-ksp/src/test/`** — JVM-only end-to-end via
   [kctfork](https://github.com/zacsweers/kotlin-compile-testing): runs the real compiler + KSP and
   pins inputs, diagnostics and generated output as goldens.
2. **`test/`** — a KMP module that applies the processor for real and verifies the generated code's
   **runtime behaviour** on every target. `commonMain` holds annotated input, `commonTest` the
   verification, one file to one file.
3. **Konsist architecture specs** — layering enforcement (in suite 1, but not kctfork-based).

All of them use kotest `FreeSpec` (`internal class XxxTest : FreeSpec({ "..." { ... } })`, nested via
`"group" - { ... }`), kotest matchers with **`actual shouldBe expected`** word order (the reverse of
`kotlin.test`), `withClue` to keep context on failure, and `.config(enabled = false)` to disable.

## Layout

```
<project-name>-ksp/src/test/kotlin/.../ksp/
├── AllKotlinFilesTest.kt        # Konsist: root allow-list, per-file line budget
├── feature/
│   ├── ArchTest.kt              # Konsist: feature layering
│   └── <name>/
│       ├── <Name>BasicUsageTest.kt    # 1. happy path, example-based
│       ├── <Name>InvalidUsageTest.kt  # 2. misuse → diagnostic (golden + exit code)
│       ├── <Name>EdgeUsageTest.kt     # 3. rare shapes and marker semantics
│       ├── <Name>PropertyTest.kt      # 4. PBT over the generator's arb()
│       ├── <Name>SnapshotTest.kt      # 5. generator-driven goldens
│       └── scenario/                  # one file per family of curated cases
├── core/
│   ├── ArchTest.kt              # Konsist: core / options / util layering
│   └── <sub>/                   # pure logic tests (no compilation needed)
└── testing/                     # feature-independent infrastructure
    ├── compile/                 # compileWithProcessor / runCompileSnapshotTest / result wrappers
    ├── snapshot/                # facet golden comparison
    ├── poet/                    # KotlinPoet input builders
    ├── generator/               # Generator / union / cartesian / valid<Name>Options
    └── konsist/                 # shared Konsist scope + predicates
```

**Every feature gets all five test kinds.** Keeping the same five names across features makes a gap
visible at a glance.

## Generator-driven snapshots

Build inputs with **KotlinPoet** (never string literals), group them into families under `scenario/`,
`union` the families, then `cartesian` them with the option generator. Each resulting point is one
test case and one golden file — so adding a scenario extends coverage across every representative
option set at once.

The option generator has two deliberately different sides: `arb()` samples the full product for PBT,
while `representativeValues()` is narrowed by `withRepresentativeValues` to a handful of points. The
full product multiplies by every scenario in every family and will otherwise run away.

## Golden file format

Markdown, at `src/test/resources/snapshots/<TestClass>/<testCase>.md`. Every captured value is a
**facet** — there is no privileged "main" content — and each becomes a `## <name>` section with one
fenced block, in declaration order. The standard set is `Input:*` / `KSP options` /
`Output:ExitCode` / `Output:Console` / `Output:Generated sources`.

- The first `.` in the snapshot name is the class/case boundary and becomes a directory separator;
  later dots stay in the file name.
- Fences auto-expand to one backtick longer than the longest run inside the body, so generated KDoc
  containing its own ```` ```kt ```` example does not break the wrapping. Comparison is whole-file,
  so hand-editing a golden's fence fails the test rather than silently passing.
- **Test names become file names**: never use `:` (invalid on Windows), and never embed a sequence
  number in a test name, snapshot name, test package or golden file name — renumbering would rewrite
  unrelated goldens.
- Keep the input in ONE place: bind it to a `val` in the test and pass the same value to both the
  compile call and the `Input` facet.

## Diagnostics are golden-tested too

Pin the **error message text**, not merely the fact that compilation failed — a message that loses
its "Solution:" line should show up as a reviewable diff. Normalize the compiler output first
(temp dir → `<TMPDIR>`, per-run compilation dir, collapse stack frames), and assert the exit code
alongside the golden.

## Operations

```sh
# regenerate goldens
./gradlew :<project-name>-ksp:test -D<project-name>.snapshot.update=true
```

Four settings the `test` task must have, each for a concrete reason:
- `useJUnitPlatform()` — kotest runs on the JUnit Platform for JVM modules.
- `maxHeapSize = "2g"` — each kctfork test is a real in-process compilation.
- `forkEvery = 25` — kctfork classloaders accumulate; without recycling the worker eventually OOMs
  and cascades into unrelated failures.
- explicit `systemProperty` forwarding — `-D` flags do **not** propagate to the test worker JVM.

## kotest wiring differs by module type

- **JVM-only module**: `kotest-runner-junit5` + `useJUnitPlatform()`. No KSP, no `io.kotest` plugin.
- **KMP module**: the `io.kotest` plugin (applied **after** `ksp`, since its wiring is KSP-based),
  `kotest-framework-engine` in `commonTest`, and `kotest-runner-junit5` in **both** `jvmTest` and
  `androidUnitTest` (the latter does not inherit the former).
