---
name: failure-classification
description: >
  CI failure-kind heuristics used in pr-fix-loop Step 4. Try in order —
  transient infra (toolchain redirect / 5xx / network) → lint (ktlintCheck / eslint) →
  binary compat (apiCheck / BCV) → test (jvmTest / jsBrowserTest / iosTest) →
  build (compileKotlin / assembleDebug) → unknown — and take the first match. Transient is first
  so a "rerun-only, don't touch code" failure is never misrouted to a code-fixing skill. Lists
  job-name / log-signature / delegate-skill per kind, plus false-positive notes (warning vs fail,
  job name vs task name).
---

# Failure classification heuristics

When a CI job is `conclusion == "FAILURE"`, decide the kind. Try top-to-bottom and take the first
match. Transient infra is **first** because that kind is fixed by a rerun alone, without touching
code — so it must never be misrouted to a `fix-ci-*` code-fixing skill.

## 1. Transient infra failure

Any of these phrases in the job log ⇒ transient infra:

- Toolchain / dependency redirect returning HTTP 4xx / 5xx (e.g. a JDK-toolchain download host)
- `Could not GET 'https://...'` / `Could not HEAD 'https://...'`
- `Received status code 5\d\d` (502 / 503 / 504)
- CI action failures like `Failed to download` / `connection reset`
- Cache restore failure (`Failed to restore`) — **unless** the build proceeded and failed later for
  another reason, in which case classify by that reason
- Runner startup failure (`The runner has received a shutdown signal`)
- `npm ERR! network` / `EAI_AGAIN` / `ECONNRESET`
- Registry/daemon pull failure (network flavor, not an auth `pull access denied`)

Action: `gh run rerun <runId> --failed`. If it fails because the workflow is still in-progress,
**defer to the next pass**.

## 2. Lint

Job-name / log signatures:

- Job name contains `Lint` / `ktlint` / `eslint` / `Format`
- Log has `ktlintCheck FAILED` / `Lint task FAILED`
- Log has a lint-rule violation line (e.g. ktlint `standard:` prefixed)
- An `eslint --fix`-fixable error pattern

Delegate to: **fix-ci-lint** skill (if present in the project).

## 3. Binary compatibility

Job-name / log signatures:

- Job name contains `Validate Binary Compatibility` / `apiCheck` / `BCV`
- Log has `Task :*:apiCheck FAILED`
- Log has `API check failed for project` followed by a `--- / +++ / @@` diff

Delegate to: **fix-ci-binary** skill (assumed to know how to regenerate via `apiDump`).

Note: when the repo contains an **independent build** (a nested project with its own `gradlew`,
e.g. an `integrationTest` project — not a composite build), the parent `apiDump` and the nested
`apiDump` may both be needed (`(cd integrationTest && ./gradlew apiDump)`). Either fix-ci-binary
handles that, or pr-fix-loop instructs it explicitly.

## 4. Test

Job-name / log signatures:

- Job name contains `Test` / `jvmTest` / `jsBrowserTest` / `wasmJsBrowserTest` /
  `iosSimulatorArm64Test` / `testDebugUnitTest`
- Log has `> Task :*:test FAILED`
- Log has `org.opentest4j.AssertionFailedError` / `java.lang.AssertionError` / a test-framework
  multi-assertion error

Delegate to: **fix-ci-test** skill.

Note: `Test JS` / `Test iOS` can be environment-dependent — the same code may pass locally but fail
in CI. Have fix-ci-test attempt a **local repro**; if it does not reproduce, consider treating it as
transient (a rerun candidate).

## 5. Build

Job-name / log signatures:

- Job name contains `Publish to Maven Local` / `Build` / `Compile` / `assembleDebug` / `assembleRelease`
- Log has `> Task :*:compileKotlin* FAILED` / `Could not resolve all dependencies`
- Log has `Unresolved reference` / `Type mismatch` (compile error)

Delegate to: **fix-ci-build** skill. Build errors border lint / test / binary-compat; depending on
root cause another skill may need to take over. Let fix-ci-build re-classify as needed.

## 6. Unknown

No match ⇒ unknown: **report to the user only**, do not attempt a fix. Present the last ~50 log
lines and the job URL.

## Heuristic caveats

This is coarse pattern-matching and has false positives:

- A `Test JS` job that emits a `warning` line is **warn, not fail** — find the job's true failure
  line separately
- A test whose name merely contains the phrase "binary compatibility" is not `apiCheck` — combine
  the **task name** (`*:apiCheck` vs `*:test`) with the job name; the job name is for display, the
  task name is the truth

When unsure, always locate the trailing `> Task :*: FAILED` line and re-classify by task name.
