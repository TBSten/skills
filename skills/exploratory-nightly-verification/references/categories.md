# Exploration Categories

Each category runs for ~8–10 minutes, sequentially cat1 → cat5.

## cat1 — Static analysis

- **Tools**: `Read`, `Glob`, `Grep`
- **Angles**:
  - **KDoc / comment drift**: function signature ≠ documented behavior; "used by X" rot
  - **Single source of truth**: same constant / config defined in multiple places
  - **Silent failure**: `try { ... } catch (e: Throwable) {}` paths with no `warn` / `log`
  - **Type design**: over-nullable / widening type / `enum` vs `sealed` mismatch
  - **TODO grep**: `grep -rn "TODO\|FIXME" --include="*.kt"` for stale items
- **Notes**:
  - Public-API modules deserve the most rigor
  - For compiler-plugin / annotation-processor sub-projects, also check generated-declaration
    naming stability

## cat2 — CI logs, public-API baseline, docs consistency

- **Tools**: `Bash` (`gh`, `git`, gradle), `Read`, `Grep`
- **Angles**:
  - **Nightly run warning grep**: `gh run list --workflow nightly-checking.yml --limit 5` (or the
    project's nightly workflow filename), then `gh run view <run-id> --log | grep -ic warning`
  - **Public-API baseline drift** (only if the project uses a binary-compatibility validator):
    `./gradlew apiCheck --continue`. Failure → P1 (or P2 if the failure is intended).
  - **Docs link rot**: extract URLs from `docs/` and `*.md`, sample-fetch via `WebFetch` to check
    HTTP status
  - **README ↔ CLAUDE.md / docs consistency**: outdated descriptions, version-pinning drift
- **Notes**:
  - Always: `timeout 8m ./gradlew <task> > .local/tmp/exploratory-nightly-<date>/tmp/<ts>-apiCheck.log 2>&1`
  - More than ~80% of warnings are P2 / P3 — write the file but don't over-investigate

## cat3 — Dynamic build / test

- **Tools**: `Bash` (gradle)
- **Angles**:
  - **Primary test task**: `./gradlew jvmTest` / `./gradlew testDebugUnitTest` / `./gradlew test`
    — pick the project's primary task. Add the project's standard tag filter (e.g. exclude
    property-based-test tags if they run in a separate nightly job)
  - **Integration tests**: if the project has a separate `integrationTest/` Gradle build, run its
    primary test task too
  - **Docs / sample tests**: if the project has a docs or samples sub-build with tests, run those
  - **`publishToMavenLocal`**: confirm artifacts publish cleanly
- **Notes**:
  - If a property-based-test (PBT) tag is normally excluded by a parallel CI matrix, exclude it
    here too (e.g. `-Dkotest.tags='!PBT'`)
  - On test failure: spend at most one attempt narrowing the cause, then file as P1
  - Logs go to `.local/tmp/exploratory-nightly-<date>/tmp/<ts>-<task>.log`

## cat4 — Upstream release watching

- **Tools**: `Bash` (`scripts/check-upstream.sh`), `WebFetch` (release notes only), `Read`
- **Procedure**:
  1. Run `"$SKILL_ROOT/scripts/check-upstream.sh"` — it fetches the five upstream sources
     deterministically (Kotlin and Compose Multiplatform via the GitHub Releases API, Gradle via
     `services.gradle.org/versions/current`, AGP via Google Maven metadata for
     `com.android.tools.build:gradle`, AndroidX via Google Maven metadata for `androidx.core:core`),
     compares them against `gradle/libs.versions.toml` + `gradle/wrapper/gradle-wrapper.properties`,
     and prints `[{"tool","latest","project","drift"}]`
  2. Your judgment is only the last step: for entries with drift `minor` / `major`, `WebFetch` the
     release **notes** and decide whether a breaking change is announced
- **Severity mapping**:
  - ≥ 1 minor version drift → P2
  - Breaking change announced in the upstream release notes → P1
  - `"drift": "error"` entry (rate limit / fetch failure) → P3 issue recording the fact; continue
- **Notes**:
  - If the project has a multi-version test matrix (e.g. Kotlin 2.3.21 / 2.4.0-Beta2), flag any
    gap between the matrix and the latest upstream (`"drift": "ahead"` entries are the hint that
    the project is on a pre-release ahead of the stable channel)

## cat5 — Comparison / leftover angles

- **Tools**: `WebSearch`, `Read`, `Grep`, `mcp__deepwiki__ask_question` (optional)
- **Angles**:
  - **Sibling-library comparison**: pick a peer of the project's genre and audit one design facet
    (see [Category-roles.md in exploratory-pr-verification](../../exploratory-pr-verification/references/category-roles.md)
    for genre-specific candidate lists)
  - **Residual TODO grep**: `grep -rn "TODO\|FIXME" .claude/ docs/ README.md` and the project root
    for items that should have been tackled by now
  - **Sample-app launch sanity**: `./gradlew :sample:run --dry-run` (or the project's demo task)
    — confirm the demo at least configures correctly
  - **Semver / BCP review**: scan the last 3 months of public-API changes; any that look like
    breaking changes without a deprecation cycle → P1
  - **Locale / case folding**: try Japanese + Turkish (the `ı`/`I` case) inputs against any
    string-handling feature; common edge-case source
- **Notes**:
  - This is the "use the remaining time" zone. Writing 3–4 P2 / P3 issues is usually higher value
    than chasing one P1
  - Saturation indicator: cat5 returns to the same angle in successive runs without finding new
    material → the project is healthy in that dimension
