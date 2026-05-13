# Five Category Roles (cat1 – cat5)

The orchestrator kicks five subagents in parallel each iteration. Each cat owns one of the five
domains below. Within its domain, the cat picks the angle autonomously (do **not** prescribe
specific files in the kick prompt; let it choose against the existing ticket pool).

## cat1 — Source-code static analysis

- **Build needed**: no
- **Tools**: `Read`, `Glob`, `Grep`, `mcp__jetbrains__*` (optional)
- **Angles**:
  - **KDoc / comment drift** — function signature ≠ documented behavior, "used by X" rot
  - **Single source of truth** — same constant / config defined in multiple places (extract candidate)
  - **Silent failure** — `try { ... } catch (e: Throwable) { /* swallowed */ }` paths that surface
    no warning, return empty list / null, or fall back without telemetry
  - **Type design** — over-nullable / widening type / `enum` vs `sealed` mismatch / public surface
    smell
  - **TODO grep** — `grep -rn "TODO\|FIXME" --include="*.kt"` to find rot
  - **Semver / API stability** — has a public surface changed shape (rename, default-value flip,
    return type widening) without a deprecation cycle?
- **Notes**:
  - Public API of the library (the modules exposed to consumers) deserves the most rigor
  - For Kotlin Compiler Plugin / annotation-processor projects, add a "generated declaration name
    stability" angle (synthetic name collisions across modules)

## cat2 — PR / environment / docs / CI

- **Build needed**: no
- **Tools**: `Bash` (`gh`, `git`), `Read`, `Grep`, `WebFetch` (optional)
- **Angles**:
  - **PR description quality** — `gh pr view <NNN> --json body --jq .body`. Does it disclose
    breaking changes? Does it link the issue? Does it match the diff?
  - **README / docs site / CHANGELOG drift** — has a new public feature been documented?
  - **CI log warnings** — `gh run view <run-id> --log > .local/tmp/.../ci-<run-id>.log` then
    grep for `warning|deprecated|unsafe|unchecked`. Track baseline count across iterations.
  - **Static analysis CI step** — lint / detekt / ktlint warnings introduced by this PR
  - **Public-API baseline static check** — `git diff --stat -- '*.api' '*.klib.api'` (only if
    the project uses a binary-compatibility validator)
  - **License / dependency surface** — new dependencies, version bumps with unannounced behavior
- **Notes**: green CI does not imply zero warnings. The warning baseline is itself a P2 source.

## cat3 — Build / test (dynamic)

- **Build needed**: yes (isolated cache)
- **Tools**: `Bash` (Gradle), unzip / `javap` for artifact inspection
- **Angles**:
  - **Re-run primary build / test tasks** for the most-impacted modules
  - **publishToMavenLocal** + unpack the resulting jar / aar / klib; inspect what's actually
    exposed (deprecation annotations, internal-leak via bytecode `public` modifier, etc.)
  - **Cross-target sanity** — for KMP, run a representative test on jvm + android + a native /
    js target; for Android-only libraries, run debug + release variants
  - **Public-API baseline roundtrip** — `./gradlew apiDump` then `git diff --stat -- '*.api'`
    (only when applicable)
  - **Build-script smoke** — change a single config option (`kotlin.experimental.X`, an
    Android `buildFeatures` flag, a `compilerOptions` switch) in a **sandbox copy** and see if
    the project survives
- **Notes**:
  - Use `--project-cache-dir=.local/tmp/exploratory-pr-<id>/gradle-isolation/cat3` and
    `-Dorg.gradle.daemon=false` to keep cat3 from racing other cats
  - `<module>/build/` is still shared at the project root — see SKILL.md §8

## cat4 — End-to-end / happy path (MCP-driven)

- **Build needed**: yes (isolated cache)
- **Tools**: MCP drivers — choose by target surface:
  - **Compose Web / JS**: `mcp__playwright__*` against a `:dev:jsBrowserDevelopmentRun`
  - **Android app**: `mcp__maestro__*` (or `mcp__mobile-mcp__*`) against `:dev:installDebug`
  - **IDE plugin / IntelliJ**: `mcp__jetbrains__execute_run_configuration`
  - **Desktop app**: launch the binary, then `mcp__claude-in-chrome__*` or screenshot tooling
  - **Server-side**: `curl` / `http` + `mcp__playwright__*` for hosted UIs
- **Angles**:
  - Walk the **happy path** of the feature added by the PR end-to-end
  - Walk **boundary conditions** (zero items, max items, empty input, very long input, locale =
    `ja-JP` and `tr-TR` for case folding edge cases)
  - **Regression check** on a feature *not* touched by the PR — confirm the diff hasn't broken
    something orthogonal
- **Notes**:
  - cat4 owns a sandbox copy of the demo / dev module — main project tree is read-only
  - At T=50min, start reverting; cat4 is the most prone to timeout

## cat5 — Comparison / leftover angles

- **Build needed**: maybe
- **Tools**: `Read`, `Grep`, `WebFetch`, `WebSearch`, `mcp__deepwiki__ask_question`
- **Angles** (rotate as saturation hits):

### Sibling-library comparison (recommended starting angle)

Pick 3–4 actively-maintained competitors in the same niche and compare on:

- CLI / config-option validation
- Public surface (what's `internal` vs exported)
- Error message style (does it cite source location? does it hint at config keys?)
- Generated / synthetic artifact naming and stability
- API breakage policy (semver, deprecation cycle)

**Comparison-target ideas by Kotlin project genre** (pick the genre that fits the PR's project):

| Genre | Sibling libraries to consider |
|-------|-------------------------------|
| Compose / UI library | `androidx.compose.*`, `compose-multiplatform`, `Voyager`, `Decompose`, `Navigation3` |
| KMP utility library | `kotlinx-datetime`, `kotlinx-coroutines`, `kotlinx-serialization`, `arrow-kt` |
| Dependency injection | `Koin`, `Hilt`, `Metro` (ZacSweers/metro), `Kodein-DI` |
| Annotation-processor / codegen | `kotlinx.serialization` (compiler-plugin), `Mokkery`, `Poko`, `Showkase` |
| Compiler plugin | Compose Compiler, Power-Assert, `kotlinx-rpc`, SKIE, Koin Compiler |
| Server framework | `Ktor`, `Spring Boot Kotlin`, `Quarkus Kotlin`, `Micronaut Kotlin` |
| Test library | `Kotest`, `MockK`, `Turbine`, `assertk` |
| Build tooling | `vanniktech/gradle-maven-publish-plugin`, `Spotless`, `detekt`, `dokka` |

> `mcp__deepwiki__ask_question` is useful here — point it at a sibling's GitHub repo and ask
> "how does <repo> handle <feature>?" to get a quick comparative read.

### Stress / boundary

Stress the feature with adversarial inputs: 10× the expected scale, locale extremes, surrogate-pair
unicode, deeply nested structures.

### Locale / a11y / interop

Switch system language, run with screen-reader if applicable, test interop boundaries (Kotlin ↔
Java, Kotlin ↔ Swift on iOS, JVM ↔ JS on KMP).

### Leftover-angle sweep

Pick any angle the other four cats haven't touched in recent iterations. Saturation indicator =
cat5 ROI drops to one P3 ticket per iteration.

### Rotate from iter 1, not as an afterthought

cat5 must include sibling-library comparison from iter 1. Past lesson in SKILL.md §12 — late
rotation caused a P-level mis-assignment that persisted 12+ iterations.

## Number-reservation scheme across cats

To avoid ticket-number collisions across the five parallel cats, the orchestrator pre-allocates
a per-cat range each iteration (see [ticket-format.md](ticket-format.md)):

- cat1: `NNNN+0` … `NNNN+2`
- cat2: `NNNN+3` … `NNNN+5`
- cat3: `NNNN+6` … `NNNN+8`
- cat4: `NNNN+9` … `NNNN+11`
- cat5: `NNNN+12` … `NNNN+14`

Each cat typically uses 1–2 of its reserved range. Unused numbers roll over to the next iteration's
reservation.
