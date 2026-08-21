---
name: pr-fix-loop
description: >
  Manage multiple GitHub Pull Requests in parallel. In a single loop pass, for every PR:
  check CI status, auto-classify each failing check (transient infra / lint / binary-compat /
  build / test) and delegate to the matching fix-ci-* skill, then handle unresolved review
  comments end to end (fetch → fix → commit → push → resolve). Auto-chains rebases for stacked
  PRs (PR Y's base is PR X's branch). Combine with a `/loop` driver (e.g. every 10 min) to run
  unattended; exit after N consecutive no-change passes.
  Use when requested: "fix multiple PRs at once", "PR fix loop", "pr-fix-loop", "get stacked
  PRs green in order", "fix CI while my PRs sit", "auto-handle CI and review comments across
  PRs", "複数 PR を見ながら修正", "PR ループ", "stacked PR を順番に green に".
metadata:
  status: Active
  group: Git / GitHub
---

# pr-fix-loop

Operational skill for driving **multiple GitHub PRs to green in parallel**. One invocation is
**one loop pass**. For every PR in the list it:

1. Takes the latest base into the branch (rebase; parent PR branch for stacks, else the default branch)
2. Fetches CI status, classifies each failing check, and delegates to a `fix-ci-*` skill
3. Handles unresolved review comments (fetch → fix → commit → push → resolve)
4. Chains rebases when PRs are stacked (PR Y's base = PR X's branch)

This skill defines a **single pass**. To repeat on a schedule, drive it with a loop runner —
e.g. `/loop 10m /pr-fix-loop <PR-list>` (a cadence at which CI advances roughly one step per pass
and the prompt cache is not blown every time). When you observe **N consecutive no-change passes**
(default 5), emit the exit signal — see [Step 7](#step-7-termination-check).

> **Project assumption.** Examples use Kotlin/Gradle task names (`ktlintCheck`, `apiCheck`,
> `compileKotlin`, `jvmTest`) because that is where this skill was hardened, but nothing here is
> Kotlin-specific. Substitute your project's lint / API-check / build / test task names; the
> classification heuristics in [`references/failure-classification.md`](references/failure-classification.md)
> are keyed on generic job/log patterns.

## Scripts (the deterministic core — run as-is)

The deterministic parts of every pass are implemented as executable scripts in
[`scripts/`](scripts/). Run them via `${CLAUDE_SKILL_DIR}/scripts/<name>` (in Claude Code
`${CLAUDE_SKILL_DIR}` is the directory containing this SKILL.md; otherwise substitute the
skill's install path). **Do not read the scripts to re-derive their commands, do not modify
them, and do not re-implement their logic inline — execute them and read their JSON.**
They are the single source of truth for pagination, classification patterns, rebase mechanics,
and streak persistence; hand-transcribing any of it is how data gets silently dropped.

| Script | Step | One line |
|--------|------|----------|
| `fetch-pr-state.sh <pr...>` | 3 | Preflight + batch-fetch per-PR state (checks, threads, comments — fully paginated) |
| `classify-failure.sh <runId> <jobId>` | 4 | Fetch one failing job's log and classify it (transient → lint → binary → test → build → unknown) |
| `rebase-pass.sh <pr...>` | 1 & 6 | Rebase every PR onto its own latest base, stacks in topological order |
| `resolve-thread.sh <threadId> [commit]` | 5 | Resolve one inline review thread |
| `mark-comment-handled.sh <commentId> <commit...>` | 5 | Wrap one issue comment in the handled marker |
| `streak.sh <no-change\|changed>` | 7 | Persist the no-change streak; report `terminate` |

Shared contract: stdout is **one line of JSON** (multi-PR scripts emit a JSON array); progress
and errors go to stderr; on failure the script exits non-zero with a
"what / why / how to fix" message — relay that message to the user and stop the pass (or defer
the PR) as it says. All scripts are idempotent, so re-running after a partial pass is safe.

With the scripts in charge of the mechanics, **your job per pass collapses to**: run scripts →
read JSON → delegate to `fix-ci-*` / write the fix → commit & push → resolve/mark → report.

## Prerequisites

`git`, `gh` (GitHub CLI), and `jq` must work locally (the scripts check all three and verify
`gh auth status`; on failure they exit with the fix instruction — report it to the user and
abort the pass). See [`references/operations.md`](references/operations.md) for the operational
contract (dirty-worktree abort, log-fetch timing, rerun blocking, commit granularity).

## Input

- A **list of PR numbers** (one or more, space-separated). e.g. `179 180 181`
- If omitted, derive the single PR for the current branch via `gh pr view --json number`

Branch name, base branch, and stack relationships are read **every pass** by the scripts —
never held as a static mapping in the input, so the loop follows reorders / rebases / branch
renames automatically.

## Workflow (one pass)

Run the following **for every PR** in one pass. Do not skip PRs (if one is fully in-progress and
nothing can be done, still advance the others). Steps are a natural-number sequence.

### Step 1: Rebase the latest base into each PR branch

```bash
"${CLAUDE_SKILL_DIR}/scripts/rebase-pass.sh" $PR_LIST
```

The script fetches origin, reads each PR's branch/base, orders stacks topologically
(parent before child), and per PR: matches the local branch to remote, rebases onto the latest
of **its own base** only when behind, and pushes with `--force-with-lease`. On conflict it
aborts the rebase and defers — never auto-merges. Read the JSON array:

- `action: "clean"` — already up to date, nothing pushed
- `action: "rebased"` — rebased and pushed
- `action: "conflict-deferred"` — see `detail`; report "deferred to next pass" and move on

This prevents a PR branch from going stale when its base moves under it (a sibling PR merged
this session, a dependabot bump, another collaborator's merge).

### Step 2: Commit tidy-up (optional; fires at ≥ 10 commits ahead of base)

When a PR branch is **≥ 10 commits** ahead of base, tidy the commits for reviewability. The shell
Claude Code runs in cannot complete interactive rebase (`-i`), so do everything non-interactively.
Detect blocks with these heuristics (top wins; leave non-matching commits untouched):

1. **Same conventional-commit prefix in a run** (e.g. three consecutive `fix(ci): ...`) → squash into one
2. **`fixup!` / `squash!` prefix** → squash into the preceding commit (autosquash, done manually)
3. **A run of small commits touching only the same file/file-set** → squash into one

Implement via `git reset --soft <block-parent> && git commit -m "..."`, working from the bottom up,
then `git push --force-with-lease`. **Never lose a commit**: back up first
(`git update-ref refs/pr-fix-loop-backup/$pr/<ts> HEAD`), and on any conflict / empty result restore
the backup and report "tidy failed, deferred". Don't re-squash the same PR within 24h (chatter
guard). If 10+ commits are all logically distinct, don't force it — report "no tidy candidates".
Ordering vs. stacks (Step 6): **tidy parent → push parent → rebase child → tidy child**.

### Step 3: Fetch all PR state in one batch

```bash
"${CLAUDE_SKILL_DIR}/scripts/fetch-pr-state.sh" $PR_LIST
```

Preflight (gh auth, dirty-worktree abort, owner/repo derivation) is built in. The script pages
through **both** comment kinds — inline review threads (GraphQL) and conversation-level issue
comments (REST) — so nothing is silently dropped on busy PRs. Read the JSON array; per PR:

- `failingChecks: [{jobId, jobName, runId}]` — input for Step 4
- `inProgress` — number of checks still running (for the report table)
- `unresolvedThreads: [{id, path, line, firstComment}]` — input for Step 5-A
- `unhandledIssueComments: [{id, excerpt}]` — input for Step 5-B
- `stackParent` — the parent PR number when this PR stacks on another listed PR, else `null`

### Step 4: Classify failing checks and delegate

For **every** entry in a PR's `failingChecks`:

```bash
"${CLAUDE_SKILL_DIR}/scripts/classify-failure.sh" <runId> <jobId>
```

The script fetches the job log and applies the ordered heuristics of
[`references/failure-classification.md`](references/failure-classification.md)
(transient → lint → binary → test → build → unknown; the trailing `> Task :xxx: FAILED` task
name is the truth, the job name is display only). Read `{kind, taskName, evidence, delegate,
logTail}` and act:

| `kind` | Your action |
|--------|-------------|
| `transient` | `gh run rerun <runId> --failed` (rejected while the workflow is in-progress — defer to next pass) |
| `lint` | delegate to `fix-ci-lint` skill |
| `binary` | delegate to `fix-ci-binary` skill |
| `build` | delegate to `fix-ci-build` skill |
| `test` | delegate to `fix-ci-test` skill |
| `unknown` | **your judgment** — present `logTail` and the job URL to the user; do not attempt a blind fix |

The `fix-ci-*` names are a **naming scheme, not a hard dependency**. If the matching skill does
**not** exist in the current repo, summarize `evidence` + `logTail` and report to the user — do
**not** run off and hand-fix. Do each PR's fix on its own branch (`git checkout <headRef>`).

### Step 5: Handle unresolved review / issue comments

Conventions in [`references/review-handling.md`](references/review-handling.md). In brief, for
each item from Step 3's JSON — fix → commit → push, then:

- **inline review thread** (`unresolvedThreads`):
  `"${CLAUDE_SKILL_DIR}/scripts/resolve-thread.sh" <threadId> [commitHash]`
- **issue-level comment** (`unhandledIssueComments`; no native resolve on GitHub):
  `"${CLAUDE_SKILL_DIR}/scripts/mark-comment-handled.sh" <commentId> <commitHash...>` — wraps
  the body in the collapsible handled marker with the fixing commit(s) appended
- **Miss-prevention**: `fetch-pr-state.sh` re-scans both kinds every pass; anything still listed
  counts the same as `unresolved ≥ 1` toward Step 7's "changed" verdict

### Step 6: Stack rebase chain

`rebase-pass.sh` already processes stacks in **topological order** (Step 3's `stackParent`
shows the relationships). After pushing a fix commit to a **parent** PR during Steps 4–5,
run the script again to chain the children:

```bash
"${CLAUDE_SKILL_DIR}/scripts/rebase-pass.sh" $PR_LIST
```

It is idempotent — untouched PRs report `clean`, children of the pushed parent report
`rebased`, conflicts report `conflict-deferred` (give up for this pass and report — never
auto-merge).

### Step 7: Termination check

Classify the pass as **"no-change"** or **"changed"**:

- **no-change** = every PR has `failingChecks == [] && unresolvedThreads == [] &&
  unhandledIssueComments == []` **and** you pushed no new commit this pass
- otherwise = **changed**

Then persist the verdict:

```bash
"${CLAUDE_SKILL_DIR}/scripts/streak.sh" no-change   # or: changed
```

Read `{streak, limit, terminate}`. Any "changed" pass resets the streak; the streak survives
across passes (file under `.local/tmp/`), so a `/loop` driver stops cleanly. When `terminate`
is `true` (default limit 5), state "terminating" explicitly — the streak file is already
cleaned for the next invocation.

Always append **`(no-change streak: N/limit)`** to the end-of-pass report. Whether the user
stops `/loop` itself is their call, but the skill treats `terminate: true` as a
"nothing left to do" signal.

## References

- [`references/failure-classification.md`](references/failure-classification.md) — what
  `classify-failure.sh` implements: the ordered kinds, per-kind signals, and false-positive
  caveats (read to sanity-check verdicts and to judge `unknown`)
- [`references/review-handling.md`](references/review-handling.md) — Step 5 conventions:
  the handled-marker wrap, commit↔thread association, per-point commits, miss-prevention
- [`references/operations.md`](references/operations.md) — operational contract: script
  preflight (auth / dirty worktree / owner-repo), log-fetch timing, rerun blocking,
  non-interactive rebase, cross-platform `stat`, commit granularity, pagination background

## Output (end of each pass)

Close each pass with one short table (columns map straight onto `fetch-pr-state.sh` fields):

```
| PR  | failures | in-progress | unresolved | action |
|-----|----------|-------------|------------|--------|
| 179 | 0        | 0           | 0          | (untouched — already green) |
| 180 | apiCheck | 1           | 0          | apiDump pushed, waiting on CI rerun |
| 181 | 0        | 5           | 0          | rebased #180 fix in, pushed |
```

The final line carries the current streak, e.g.:

- `no-change (streak 1/5) — recheck in 10 min`
- `changed (CI in progress, streak reset) — recheck next pass`
- `no-change (streak 5/5) — terminating`

At `terminate: true`, state "terminating" on that line (the script has already removed the
streak file).
