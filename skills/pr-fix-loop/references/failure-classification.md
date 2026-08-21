---
name: failure-classification
description: >
  Explanation of the CI failure-kind heuristics implemented by scripts/classify-failure.sh
  (pr-fix-loop Step 4). Ordered transient infra (toolchain redirect / 5xx / network) →
  lint (ktlintCheck / eslint) → binary compat (apiCheck / BCV) → test (jvmTest / jsBrowserTest /
  iosTest) → build (compileKotlin / assembleDebug) → unknown; first match wins. Transient is
  first so a "rerun-only, don't touch code" failure is never misrouted to a code-fixing skill.
  Read this to sanity-check the script's verdict and to judge kind "unknown" — do not re-apply
  the patterns by hand.
---

# Failure classification heuristics

**These heuristics are implemented by `scripts/classify-failure.sh` — run the script, don't
pattern-match logs yourself.** This document explains the decision order and its caveats so you
can sanity-check a verdict, read the `evidence` array, and exercise judgment on
`kind: "unknown"` (the one case the script deliberately leaves to you).

The script tries the kinds top-to-bottom and takes the first match. Transient infra is **first**
because that kind is fixed by a rerun alone, without touching code — so it must never be
misrouted to a `fix-ci-*` code-fixing skill. For kinds 2–5 the script trusts the trailing
`> Task :xxx: FAILED` line above all: **the task name is the truth, the job name is display
only** (see caveats).

## 1. Transient infra failure

Any of these phrases in the job log ⇒ transient infra:

- Toolchain / dependency redirect returning HTTP 4xx / 5xx (e.g. a JDK-toolchain download host)
- `Could not GET 'https://...'` / `Could not HEAD 'https://...'`
- `Received status code 5\d\d` (502 / 503 / 504)
- CI action failures like `Failed to download` / `connection reset`
- Cache restore failure (`Failed to restore`) — **unless** the build proceeded and failed later
  for another reason (a `> Task :*: FAILED` line exists), in which case the script classifies by
  that task instead
- Runner startup failure (`The runner has received a shutdown signal`)
- `npm ERR! network` / `EAI_AGAIN` / `ECONNRESET`
- Registry/daemon pull failure (network flavor, not an auth `pull access denied`)

Your action on `kind: "transient"`: `gh run rerun <runId> --failed`. If it fails because the
workflow is still in-progress, **defer to the next pass**.

## 2. Lint

Failed-task names like `ktlintCheck` / `lintDebug` / `spotlessCheck`; job names containing
`Lint` / `Format`; log signatures like `ktlintCheck FAILED`, a ktlint rule violation line
(`(standard:...)`), or an `eslint --fix`-fixable error pattern.

Delegate to: **fix-ci-lint** skill (if present in the project).

## 3. Binary compatibility

Failed task `apiCheck` (BCV); job names containing `Validate Binary Compatibility` / `BCV`;
log signature `API check failed for project` followed by a `--- / +++ / @@` diff.

Delegate to: **fix-ci-binary** skill (assumed to know how to regenerate via `apiDump`).

Note: when the repo contains an **independent build** (a nested project with its own `gradlew`,
e.g. an `integrationTest` project — not a composite build), the parent `apiDump` and the nested
`apiDump` may both be needed (`(cd integrationTest && ./gradlew apiDump)`). Either fix-ci-binary
handles that, or pr-fix-loop instructs it explicitly.

## 4. Test

Failed-task names ending in `Test` / `Tests` (`jvmTest`, `jsBrowserTest`, `wasmJsBrowserTest`,
`iosSimulatorArm64Test`, `testDebugUnitTest`, `allTests`); job names containing `Test`; log
signatures `org.opentest4j.AssertionFailedError` / `java.lang.AssertionError` / a test-framework
multi-assertion error.

Delegate to: **fix-ci-test** skill.

Note: `Test JS` / `Test iOS` can be environment-dependent — the same code may pass locally but fail
in CI. Have fix-ci-test attempt a **local repro**; if it does not reproduce, consider treating it as
transient (a rerun candidate).

## 5. Build

Failed-task names starting with `compile` / `assemble` / `build` / `publish`
(`compileKotlin*`, `assembleDebug`, `assembleRelease`); job names containing `Build` /
`Compile` / `Publish to Maven Local`; log signatures `Could not resolve all dependencies`,
`Unresolved reference`, `Type mismatch` (compile error).

Delegate to: **fix-ci-build** skill. Build errors border lint / test / binary-compat; depending on
root cause another skill may need to take over. Let fix-ci-build re-classify as needed.

## 6. Unknown

No match ⇒ the script returns `kind: "unknown"` with `delegate: null` and does **not** guess.
This is where your judgment comes in: present the `logTail` (last ~50 lines) and the job URL to
the user — do not attempt a blind fix.

## Heuristic caveats

This is coarse pattern-matching and has false positives — reasons to glance at `evidence`
before delegating:

- A `Test JS` job that emits a `warning` line is **warn, not fail** — the true failure line may
  be elsewhere in `logTail`
- A test whose name merely contains the phrase "binary compatibility" is not `apiCheck`. The
  script therefore keys on the trailing `> Task :*: FAILED` **task name** first and only falls
  back to job-name/log signatures when no failed task line exists — the job name is for
  display, the task name is the truth
