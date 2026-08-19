---
name: exploratory-pr-verification
description: >
  Operational rules for exploratory PR verification driven by multiple parallel subagents
  in a Kotlin project (Android / JVM / KMP / Compose / server-side Kotlin).
  Covers PDCA / MCP-driven dynamic checks / ticket bookkeeping / PR comment etiquette /
  build isolation / time budgeting / loop termination / maintainer-response latency tracking.
  Designed for OSS contributors who want to deeply audit a PR alongside its maintainer
  without going noisy on the PR thread.
  Use when requested: "PR exploratory verification", "PR 探索検証", "並列 subagent kick",
  "PR loop", "ticket 駆動で PR review", "stacked PR を多角的に audit",
  "complex PR を 5 並列で深掘り".
metadata:
  status: Active
  group: Git / GitHub
---

# Exploratory PR Verification — Operational Rules

Standard rules for kicking N parallel subagents to exploratory-verify a PR.
Read this file at the start of each iteration before kicking subagents, and inject the relevant
sections into each subagent prompt.

> **Project assumption.** Examples are written for Kotlin projects on Gradle (Android, JVM, KMP,
> Compose Multiplatform, server-side Kotlin). Adjust the task names to match the project at hand:
> - `./gradlew jvmTest` / `./gradlew testDebugUnitTest` / `./gradlew test` — pick the project's primary test task
> - Public-API snapshot: only relevant if the project uses something like `binary-compatibility-validator`
>   (`apiCheck` / `apiDump`). Skip §5 otherwise.
> - MCP drivers (Playwright / Maestro / IntelliJ-MCP) — depend on the target surface

## 0. Core constraints (override everything below)

The two rules that took the most user repetitions to internalize. They win against any other section.

### Constraint A: Do not stop the loop

After a single iteration's completion notification, **never** ask the user "what should I do next?".
Mechanically run the termination checklist in §17. If unmet, kick the next iteration immediately.
If maintainer response is awaited and dynamic verification is temporarily thin, fill the time with
ticket bookkeeping / FINAL-SUMMARY updates / cluster analysis until the deadline.

### Constraint B: User feedback → immediate skill update

If the user points out a new operational rule (new perspective, process improvement) **even once**,
reflect it into this SKILL.md in the same turn. Do not let it stay as ad-hoc prompt tweaks or memory;
it will drift on the next iteration.

Reflection flow:

1. Edit SKILL.md (or a referenced file) within the same turn as the feedback
2. Commit immediately (do not batch at PR-creation time)
3. Briefly acknowledge to the user that the rule has been recorded

## 1. Overall workflow

1. **Re-read SKILL.md** (every loop start — the user may have appended new rules)
2. **State check**: `git fetch origin <branch>` + `git rev-list --count HEAD..origin/<branch>` (force-push detection)
3. **Hard reset if needed** (`git reset --hard origin/<branch>`); revert working tree first if dirty
4. **Kick N (default 5) explore subagents in parallel** (cat1–cat5; all `run_in_background: true`)
5. **Receive completion notifications one by one**; aggregate findings
6. **Post a PR comment only for P0/P1 findings** that are not duplicates of previous comments
7. **Loop termination handling (§17)**: walk the checklist; if unmet → kick next iteration; if met → close

⚠️ **Do not stop.** Constraint A applies. The check is mechanical — see §17.

## 2. Five category roles (see [references/category-roles.md](references/category-roles.md))

Each iteration kicks the categories in parallel, each with **autonomous angle-selection authority**:

| cat | Domain | Build needed | Primary activity |
|-----|--------|--------------|------------------|
| **cat1** | Source code (static) | ✗ | Doc drift / SoT violation / type design / silent failure / brittleness / semver |
| **cat2** | PR / env / docs / CI | ✗ | PR description / README / docs site / CI log warning grep / static analysis / ChangeLog |
| **cat3** | Build / test (dynamic) | ✓ (isolated cache) | Re-run primary build / test tasks, publish-and-inspect artifact, cross-target sanity |
| **cat4** | e2e / happy path | ✓ (isolated cache) | **MCP-driven** end-to-end via a sandbox sample app (Compose UI, Android app, server endpoint, etc.) |
| **cat5** | Comparison / leftover angles | ✗ or ✓ | **Compare with sibling libraries in the same niche**, stress, locale, interop, semver review |

cat5 can rotate to other angles once saturated. [references/category-roles.md](references/category-roles.md)
lists comparison-target ideas per project genre (Kotlin DSL / Android library / Compose library /
KMP library / server framework / build tooling / etc.).

## 3. PDCA cycle for dynamic verification

cat3 / cat4 / cat5 dynamic-verification work runs this 5-step PDCA. See
[references/pdca-workflow.md](references/pdca-workflow.md) for which config files to mutate, MCP
tool selection, and output-confirmation tactics.

1. **Environment setup**: isolated build cache, publish-to-`mavenLocal`, dependency confirmation
2. **Config mutation**: build scripts / version catalog / settings.gradle / source fixtures
   (sandbox-only — main project tree is read-only; see §9)
3. **Actually run** ← MCP required for cat4 when a UI / device / browser is involved
4. **Confirm output** ← MCP snapshot / view-hierarchy / unpacked artifact
5. **(Optional) Loop steps 1–3** with varied options

## 4. CI log inspection (cat2)

```sh
gh run list --branch <branch> --limit 10 --repo <owner>/<repo>
gh run view <run-id> --log --repo <owner>/<repo> \
  > .local/tmp/exploratory-pr-<id>/log/iter<N>-cat2-ci-<run-id>.log
grep -i -E "warning|deprecated|unsafe|unchecked" <log-file>
```

CI may be green while warnings pile up — track the baseline count and how it shifts across
maintainer commits. Surfacing baseline drift is a frequent P2 source.

## 5. Public-API snapshot roundtrip (cat3, optional)

Only relevant if the project ships a binary-compatibility baseline. Typical example:

```sh
./gradlew apiDump --continue \
  --project-cache-dir=.local/tmp/exploratory-pr-<id>/gradle-isolation/cat3 \
  -Dorg.gradle.daemon=false
git diff --stat -- '*.api' '*.klib.api'
```

A zero-line diff is the expected state. Any diff = baseline-and-implementation drift = ticket
candidate. After inspection, `git checkout -- '*.api' '*.klib.api'` to revert.

Skip this section if the project does not use a binary-compatibility validator. (For server-side
or app projects, public-API drift may instead manifest as REST schema / Proto / GraphQL diff —
adapt the check accordingly.)

## 6. Ticket bookkeeping

See [references/ticket-format.md](references/ticket-format.md) for: number prefix scheme,
subdirectory layout (`active` / `resolved/` / `methodology/` / `non-pr/`), file format,
deduplication policy, and the parallel-subagent number-reservation scheme.

## 7. PR comments

See [references/pr-comment-policy.md](references/pr-comment-policy.md) for: posting threshold
(P0/P1 only), de-duplication against previous comments, clustering policy, saturation limits,
the **mandatory pre-post dynamic PoC** for any comment that proposes a fix, and self-correction
flow when a previously proposed fix turns out to be wrong.

## 8. Build isolation

Each cat gets its own Gradle project-cache directory:

```
--project-cache-dir=.local/tmp/exploratory-pr-<id>/gradle-isolation/cat<N>
-Dorg.gradle.daemon=false
```

⚠️ `--project-cache-dir` only isolates `.gradle/`. Each `<module>/build/` directory is still
physically shared at the project root. Parallel builds can race on the same output file — split
the `touch` domains between cats (see §9).

## 9. Working-tree cleanliness

- Revert all mutations after verification:
  - `git checkout -- <files>`
  - `rm -rf <untracked-dirs>`
  - Confine all temporary files to `.local/tmp/exploratory-pr-<id>/`
- At each cat's completion, log `git status --porcelain`
- Split the `touch` domain between cats to avoid working-tree conflicts:
  - cat1, cat2: read-only
  - cat3: API-baseline files (revert after any dump)
  - cat4: sandbox app / integration-test dir (revert)
  - cat5: cat5-owned stress directory only

### cat3 / cat4 source mutation rule (= incident-driven)

If a parallel subagent mutates a source file under the main project tree, another cat's build may
break (real incident: cat3 left a probe file → cat4's sample-app build failed). Therefore:

- Source mutations **must be sandboxed**: `.local/tmp/exploratory-pr-<id>/iter<N>-cat<X>/sandbox/`
- The main project tree (each module's `src/`) is **read-only** — no leftover temporary files
- On completion, delete the sandbox + confirm `git status --porcelain` reports clean

## 10. Time management

- Each cat: **≤ 60 min**
- **At T=50min, start reverting** (timeout safety; cat4 has hit this before)
- Long tasks: `timeout 30m <cmd>`
- Record results to a log file before reporting completion

### Dynamic deadline change protocol

If the user changes the deadline mid-run (e.g. "let's cut at 17:00 instead of midnight"):

1. **Update wake-ups immediately**: delete any existing deadline cron / scheduled wakeup; create
   a new one ~3 min before the new deadline (one-shot)
2. **Recompute ticket-number reservation**: fewer remaining iterations may justify dropping to
   sequential mode (see below)
3. **Re-walk §17 termination checklist** with the new deadline — early termination may now apply
4. **In-flight subagents**: decide whether to wait or kill based on remaining budget
5. **Update FINAL-SUMMARY** deadline notation

### Sequential-mode auto-trigger when < 1.5h remains

When deadline-remaining drops under 1.5h, **disable parallel kicks; switch to sequential mode**:

- Parallel-mode build-race + working-tree conflict risk dominates the small remaining window
- Sequential mode = 1–2 cats focused, completable in 25–30 min
- The final iteration's value is **cleanup + 1–2 tickets**, not breadth — use it as a saturation signal

The orchestrator should `date`-check remaining time before kicking and pick the appropriate prompt template.

## 11. Required reading (every cat)

Each subagent prompt must direct the cat to read, in order:

- This SKILL.md (`<repo-root>/.claude/skills/exploratory-pr-verification/SKILL.md`) — first
- `.local/tmp/exploratory-pr-<id>/FINAL-SUMMARY.md` (the latest snapshot)
- Recent iteration log files (the cat's previous output)
- Salient ticket files (the important ones — skim only)

**The orchestrator** must re-read this SKILL.md at iteration start (to catch user-added rules).

### Default sweep on iteration 1

In **iter 1**, run these static sweeps to surface release-blockers and docs gaps early:

1. **docs-site grep**: `grep -rn "<main-feature-keyword>" docs/ README.md CHANGELOG.md` — zero
   hits is a release-blocker candidate
2. **PR description grep**: `gh pr view <NNN> --json body --jq .body | grep "BREAKING"` —
   confirm breaking-change disclosure
3. **Public-API baseline integrity** (when applicable): immediate drift check
4. **CI warning baseline count**: `gh run view <run-id> --log | grep -ic warning` — record the
   baseline, observe shifts in later iterations
5. **CHANGELOG.md / RELEASE-NOTES.md presence**: missing-changelog is a publish-flow risk

All five are static, < 5 min, and must run in iter 1's cat1 / cat2.

### Log preservation rule

For all gradle / MCP / gh command output in cat3 / cat4 / cat5:

```sh
./gradlew <task> --continue \
  > .local/tmp/exploratory-pr-<id>/iter<N>-cat<X>-<ts>-<task>.log 2>&1
gh run view <run-id> --log \
  > .local/tmp/exploratory-pr-<id>/iter<N>-cat<X>-<ts>-gh-run.log
```

**Forbidden** (silent truncation):

- `| grep` / `| head` / `| tail` on the live stream (warnings / deprecation notices vanish)
- Letting output flow only to stdout without persisting to file

**OK**: `> file 2>&1` (merge stderr, persist all), `tee` (display + persist). If you need to
filter, **save the raw log first**, then `grep` / `awk` against the file.

## 12. Comparison with sibling libraries (cat5)

Pick 3–4 actively-maintained competitors in the same niche and compare on:

- CLI / config-option validation (does it `fail` early with a clear message?)
- Public surface (what's `internal` vs exported)
- Error message style (does it cite source location? does it hint at config keys?)
- Generated / synthetic artifact naming and stability
- API breakage policy (semver, deprecation cycle)

See [references/category-roles.md](references/category-roles.md) for comparison-target ideas
per Kotlin project genre.

### Rotate cat5 into comparison from iter 1, not as an afterthought

Past lesson: cat5 was rotated to "compare with peers" only at iter 14. Result: a P1 ticket from
iter 1–13 was downgraded at iter 17 once a comparable library was confirmed to share the same
limitation. → **early peer-norm verification protects P-level assignment**.

iter 1 cat5 budget:

- 3–4 peer comparisons (the must-compare library + 2–3 same-genre PRs)
- iter 2–3: fill gaps
- iter 4+: rotate to other angles once saturated

## 13. Autonomous angle selection

Each cat picks its own angle — orchestrator does **not** prescribe. Cats consult existing tickets
and prior iteration logs to avoid duplicates. The orchestrator does not ask the user
"what should we look at next?" — that violates the autonomy principle of exploratory verification.

## 14. Maintainer-response watching

- At iteration start: `git fetch && git rev-list --count HEAD..origin/<branch>`
- Non-zero → hard reset, run `git show --stat` on the new commit(s), reconcile against existing tickets
- Tickets resolved by maintainer fixes move to `_resolved-` → `resolved/`

### Rebase-squash / force-push handling

If history shows a squash (multiple known commits collapsed into one):

1. `git log --oneline origin/<branch> -10`
2. Verify whether known commit hashes have **disappeared** (e.g. `feat! + chore(bcv)!: ...` collapsed)
3. Update tickets that referenced the old hash to the new hash (preserve traceability)
4. Rerun the key dynamic checks (primary test task, sample-app launch) to confirm behavior unchanged
5. `git reset --hard origin/<branch>`

## 15. PR-comment posting trail

- Record every comment URL in FINAL-SUMMARY.md
- Before posting, read all existing comments (`gh pr view <NNN> --json comments`) and avoid content duplication

## 16. Maintainer-response latency as a phase-strength signal

The latency from **comment posted → maintainer reaction** reveals their engagement state.

| Latency | State | Recommended action |
|---|---|---|
| < 1h | Active fix phase | P1 cluster follow-up has high ROI (but noise risk) |
| 1–3h | Normal review | Hold 1-comment-per-iteration cadence |
| 3–9h | Pending / busy elsewhere | Continue exploring, fill time with ticket bookkeeping |
| > 9h | Low priority / quiet | Reduce posting cadence; even P1 should wait for cluster |

A latency drop (e.g. 9h → 40min) signals the maintainer entering an active phase — typically
deadline-driven, which means it's time to consider closing the exploration.

Track via `gh pr view <NNN> --json comments --jq '.comments[-1].createdAt'` at each iteration end.

## 17. Loop-termination check (run after every 5-cat completion)

Run this **mechanically** the moment all parallel cats complete:

1. **Re-read this SKILL.md** (user may have appended rules)
2. **Termination checklist**:
   - [ ] Deadline reached
   - [ ] Active ticket count exceeded the user-specified ceiling (e.g. "≥ 20 tickets")
   - [ ] User issued a new explicit termination instruction
   - [ ] 5 consecutive iterations with zero new P0/P1 + ROI saturation across all cats
3. **None hit → kick next iteration immediately** (Constraint A)
4. **Maintainer-response wait dampens dynamic verification?** Don't stop. Fill the time with:
   - Ticket cluster reorganization / merge candidate clustering
   - FINAL-SUMMARY.md updates
   - resolved/ migration reassessment
   - P-level re-evaluation of accumulated tickets
   - Polishing ticket prose for future PR-comment candidates
5. **Any hit → run close sequence**:
   - Write the closing snapshot to FINAL-SUMMARY.md
   - Report to user (deadline met / total tickets / remaining P0/P1 / maintainer summary / PR-comments-posted count)
   - **Clean up all crons / scheduled wakeups** (`CronList` → `CronDelete` everything except the
     deadline-notification cron; this prevents `<<autonomous-loop>>` sentinels from firing post-close)
   - Final `git status --porcelain` to confirm working-tree clean
   - **KPT retrospective**: write up Keep / Problem / Try and reflect improvements into this SKILL.md
     before marking the skill-update done (past lesson: retrospective requests came post-PR and forced rework)

### autonomous-loop sentinel handling

If a cron / `ScheduleWakeup` fires `<<autonomous-loop-dynamic>>` after close:

1. Walk §17 checklist mechanically
2. If 4/4 already-closed → **do not schedule the next wakeup** (don't call `ScheduleWakeup`)
3. Briefly tell the user "the loop is terminated" — do not silently ignore the fire

### Final iteration in sequential mode

The last iteration before deadline runs in **sequential mode (1–2 cats focused)**, not parallel:

- Parallel-mode build/working-tree conflicts dominate the small budget
- Final-iter angle = cleanup + 1–2 tickets; use it as a ROI-saturation indicator
- Wrap in 25–30 min; use the remaining budget to finalize FINAL-SUMMARY

## 18. Post-close handshake

After §17 close until deadline:

- **Maintainer-fix watching**: periodic `git fetch`; update affected tickets if new commits land
- **No new exploration kicks** (close-decision loses meaning otherwise)
- **User Q&A responses are still expected** (Q&A is independent of the explore loop)
- **Cron / autonomous-loop sentinels fire**: handle as above (silent termination)
- **Deadline hit**: final report + cron cleanup confirmation + working-tree clean confirmation

## 19. Retrospective meta-analysis

See [references/retrospective-meta.md](references/retrospective-meta.md) for the past-feedback table
(theme × repetition count × where-it-landed × skill-update lag) and the lag-pattern interpretation.

## 20. Cumulative ticket family clusters

See [references/cluster-families.md](references/cluster-families.md) for the C-1 … C-11 family
classification used in FINAL-SUMMARY to plan follow-up PR scopes.

---

This SKILL.md is a snapshot of accumulated exploration knowledge. Update it whenever the user adds
a new rule (Constraint B), and prefer adding to the existing sections over creating siblings.
