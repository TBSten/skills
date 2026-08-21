# pr-fix-loop

One loop pass that drives **multiple GitHub PRs to green in parallel** — CI-failure triage plus
review-comment handling, in a single mechanical sweep.

## Install

```sh
gh skill install tbsten/skills pr-fix-loop
```

## Overview

This skill packages the "keep several PRs moving while they sit" workflow: rather than babysitting
one PR at a time, one invocation sweeps a whole list of PRs, and a `/loop` driver repeats the sweep
on a cadence until nothing changes. Each pass fetches CI status, classifies every failing check,
delegates the fix to a matching `fix-ci-*` skill, and takes review/issue comments from fetch all the
way through resolve — then rebases any stacked PRs in topological order.

## When to use

Trigger this skill when you want to:

- Drive **multiple PRs** (stacked or independent) to green without hand-holding each one
- Auto-classify failing checks (transient infra / lint / binary-compat / build / test) and route
  each to the right fixer
- Handle review comments end to end: fetch → fix → commit → push → resolve
- Keep **stacked PRs** consistent by auto-chaining rebases (child rebased onto parent after a parent push)
- Run **unattended** via a loop driver (e.g. `/loop 10m /pr-fix-loop 179 180 181`) that exits after
  N consecutive no-change passes

Even for a **single PR**, it's a fit when you want CI-fix and review-comment handling automated as one set.

## One pass, seven steps

| Step | What it does |
|------|--------------|
| 1 | `scripts/rebase-pass.sh` — rebase each PR branch onto the latest of its own base (parent branch for stacks, else default branch) |
| 2 | (Optional) Tidy commits when a branch is ≥ 10 commits ahead — non-interactively, never losing a commit |
| 3 | `scripts/fetch-pr-state.sh` — batch-fetch CI status + review threads + issue comments (both fully paginated) |
| 4 | `scripts/classify-failure.sh` per failing check, then delegate to the matching `fix-ci-*` skill |
| 5 | Fix, then `scripts/resolve-thread.sh` (native resolve) / `scripts/mark-comment-handled.sh` (handled-marker wrap) |
| 6 | Re-run `scripts/rebase-pass.sh` to chain stacked PRs in topological order |
| 7 | `scripts/streak.sh` — exit after N consecutive no-change passes (default 5) |

## Scripts (the deterministic core)

Everything mechanical — pagination, failure-pattern matching, rebase mechanics, streak
persistence — lives in executable scripts under `pr-fix-loop/scripts/`, invoked as
`${CLAUDE_SKILL_DIR}/scripts/<name>`. The AI executes them **as-is** and reads their one-line
JSON output (stderr carries progress and "what / why / fix" error messages); it never
transcribes or re-implements their logic, which is how paginated data used to get silently
dropped. All scripts are idempotent, mock-testable via the `GH_CMD` env var, and depend on
`git` + `gh` + `jq`.

| Script | One line |
|--------|----------|
| `fetch-pr-state.sh <pr...>` | Preflight + per-PR state (failing checks, threads, comments) as a JSON array |
| `classify-failure.sh <runId> <jobId>` | Job log → `{kind, taskName, evidence, delegate, logTail}` |
| `rebase-pass.sh <pr...>` | Topological rebase sweep → `{pr, action: rebased\|clean\|conflict-deferred}` |
| `resolve-thread.sh <threadId> [commit]` | Resolve one review thread |
| `mark-comment-handled.sh <id> <commit...>` | Wrap one issue comment in the handled marker |
| `streak.sh <no-change\|changed>` | Persist the streak → `{streak, limit, terminate}` |

## Design notes

- **Scripts are the SSoT for the mechanics.** Classification patterns, pagination, and rebase
  ordering are code, not prose — the AI's job collapses to reading JSON, delegating to
  `fix-ci-*`, writing fixes, and reporting.
- **No static stack map.** Branch / base / stack relationships are read every pass (inside the
  scripts) from `gh pr view --json headRefName,baseRefName`, so the loop follows reorders,
  rebases, and renames automatically.
- **`fix-ci-*` is a naming scheme, not a hard dependency.** If the matching skill is absent, the
  loop summarizes the failing job log and reports — it never runs off to hand-fix.
- **Safety first.** Rebase conflicts are aborted and deferred (never auto-merged); commit tidy-up
  always backs up first and restores on any failure.
- **Chatter guard.** The no-change streak is persisted so a `/loop` driver stops cleanly instead of
  churning forever.

## References

Detailed rules are split for progressive disclosure:

- [`failure-classification.md`](./pr-fix-loop/references/failure-classification.md) —
  explanation of `classify-failure.sh`'s ordered heuristics (transient → lint → binary → test →
  build → unknown; task name over job name; false-positive notes)
- [`review-handling.md`](./pr-fix-loop/references/review-handling.md) — Step 5 conventions:
  handled-marker wrap, commit↔thread association, per-point commits, miss-prevention
- [`operations.md`](./pr-fix-loop/references/operations.md) — script contract (preflight,
  JSON-on-stdout, idempotency), log-fetch timing, rerun blocking, non-interactive rebase,
  cross-platform `stat`, commit granularity, pagination background

## Project assumption

Examples use Kotlin/Gradle task names (`ktlintCheck`, `apiCheck`, `compileKotlin`, `jvmTest`)
because that is where the skill was hardened, but nothing is Kotlin-specific. Substitute your
project's lint / API-check / build / test task names; the classification heuristics key on generic
job/log patterns.

## Prerequisites

- A Git repository with PRs on GitHub
- `gh` (GitHub CLI) authenticated (`gh auth status`) and `jq` (the scripts verify both)
- `.local/` in `.gitignore` (the streak/backup markers live there)
- Best paired with a loop driver skill (e.g. `/loop`) for unattended cadence
- Optional: project-local `fix-ci-*` skills to delegate to (lint / binary / build / test / pr-comments)
