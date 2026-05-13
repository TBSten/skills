# exploratory-nightly-verification

A 60-minute single-shot exploratory verification of a Kotlin project's `main` branch.

## Install

```sh
gh skill install tbsten/skills exploratory-nightly-verification
```

## Overview

Designed to run from a nightly CI job (e.g. `anthropics/claude-code-action` on a cron schedule).
The skill walks cat1 → cat5 sequentially within a 60-minute budget, writing each finding to a
Markdown issue file as it goes. At the end it produces a SUMMARY.md. **No PR side effects** —
nightly runs are read-only.

This is the sibling skill to [`exploratory-pr-verification`](./exploratory-pr-verification.md),
but the constraints are very different:

| | exploratory-pr-verification | exploratory-nightly-verification |
|--|----------------------------|---------------------------------|
| Target | a specific PR diff | the latest `main` |
| Time budget | open-ended (loop until deadline) | 60 min, single shot |
| Parallelism | 5 parallel subagents | 1 agent, sequential cat1 → cat5 |
| Output channel | PR comments + tickets | issue Markdown files only |
| Side effects | posts PR comments | **none** (read-only) |
| Loop / reentry | yes | no |

## When to use

- A nightly job needs to do a 60-min quality sweep of `main` without touching any PR
- You want sequential cat coverage (parallelism is unsafe at this budget)
- You want findings written to disk as they're discovered (job timeout doesn't lose them)
- You may want a downstream CI step to ship the findings to Slack / Discord / a GitHub issue —
  this skill's output format is the contract

## Invariants

| # | Rule |
|---|------|
| A | **Write each finding immediately** to `.local/tmp/exploratory-nightly-<date>/issues/<NN>-<slug>.md`. Don't batch. |
| B | **Stop new exploration at T=50min.** Spend the last 10 minutes on formatting / dedup / SUMMARY.md. |
| C | **Issue filenames are zero-padded sequential** (`01-<slug>.md`, `02-<slug>.md`, ...). |
| D | **No side effects on the project**: no PR comments, no GitHub issue filing, no pushes, no branch ops. |

## Categories (run sequentially)

- **cat1 — Static analysis**: KDoc / SoT / silent failure / type design / TODO grep
- **cat2 — CI logs, public-API baseline, docs consistency**: nightly warning grep, baseline drift,
  README / docs / CHANGELOG consistency
- **cat3 — Dynamic build / test**: project's primary `test` task (and integration / docs variants
  if present)
- **cat4 — Upstream release watching**: Kotlin / Compose Multiplatform / Gradle / AGP releases
  vs. the project's `libs.versions.toml`
- **cat5 — Comparison / leftover angles**: sibling-library audit, residual TODO grep,
  sample-app launch sanity, semver / BCP review, locale / case-folding edge cases

Full details in [`categories.md`](./exploratory-nightly-verification/references/categories.md).

## Issue format

Each finding is written to `.local/tmp/exploratory-nightly-<date>/issues/<NN>-<slug>.md`:

```markdown
# <Short title>

**Category**: cat<N> (<name>)
**Severity**: P0 | P1 | P2 | P3
**Detected at commit**: <first 12 chars of git rev-parse HEAD>

## Reproduction
- ...

## Detail
<~200 chars; the lead is the notification excerpt>

## Fix proposal (optional)
- ...
```

Full spec in [`issue-format.md`](./exploratory-nightly-verification/references/issue-format.md).

## Notification integration (optional)

The skill writes Markdown; **the notification step is outside the skill's scope**. If you want
Slack / Discord / email / GitHub Issues sync, a separate CI step parses the directory in `sort`
order and produces the notification payload. The strict filename / metadata-line / heading
contract in `issue-format.md` is what makes this parsing reliable.

## Prerequisites

- Git-managed Kotlin / Gradle project (Android / JVM / KMP / Compose / server-side)
- `gh` CLI available in the CI environment
- `.local/` in `.gitignore`
- The CI workflow file (e.g. `.github/workflows/nightly-checking.yml`) drives this skill on a
  schedule
