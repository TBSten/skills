# exploratory-pr-verification

Operational rules for exploratory PR verification driven by multiple parallel subagents
in a Kotlin project.

## Install

```sh
gh skill install tbsten/skills exploratory-pr-verification
```

## Overview

This skill operationalizes the "deep-audit a PR alongside its maintainer" workflow that emerged
across multiple OSS contribution phases on Kotlin projects (Android / JVM / KMP / Compose /
server-side Kotlin). It's the orchestrator's playbook: read it at the start of every iteration,
inject the relevant sections into each subagent's prompt, and run the termination checklist after
every cat completes.

## When to use

Trigger this skill when you need to:

- Deep-audit a non-trivial PR over multiple iterations (typically 5–20)
- Coordinate **N parallel subagents** (default 5) each owning a distinct verification domain
- Track findings in a numbered ticket system without going noisy on the PR thread
- Decide when to **post a PR comment** vs. keep a finding in the ticket pool
- Run a maintainer-aware loop that adapts to maintainer response latency
- Stop cleanly at a deadline, with a retrospective and cluster analysis

If you're doing a single 60-min spike on `main` (no PR diff, no loop), use the sibling
[`exploratory-nightly-verification`](./exploratory-nightly-verification.md) instead.

## Five categories

| cat | Domain | Build needed |
|-----|--------|--------------|
| cat1 | Source code (static) | No |
| cat2 | PR / env / docs / CI | No |
| cat3 | Build / test (dynamic, isolated cache) | Yes |
| cat4 | e2e / happy path (MCP-driven) | Yes |
| cat5 | Comparison with sibling libraries | Maybe |

See [`category-roles.md`](./exploratory-pr-verification/references/category-roles.md) for the
detailed per-cat angle list and sibling-library comparison targets per Kotlin project genre
(Compose / KMP utility / DI / annotation-processor / compiler plugin / server framework / test
library / build tooling).

## Core constraints (override everything else)

The two rules that took the most user repetitions to internalize, promoted to override-level:

- **A. Never stop the loop** — after a single iteration completes, mechanically walk the §17
  termination checklist; if unmet, kick the next iteration immediately. Don't ask "what next?"
- **B. Reflect user feedback into the skill immediately** — any new operational rule the user
  mentions even once must land in this skill's files in the same turn, not as ad-hoc prompt
  tweaks or session memory

## Temporary directory convention

All temporary files / logs / sandboxes / tickets live under:

```
.local/tmp/exploratory-pr-<id>/
├── FINAL-SUMMARY.md
├── problems/<NNNN>-<slug>.md
├── log/iter<N>-cat<X>-*.log
├── gradle-isolation/cat<N>/
├── iter<N>-poc/           # pre-PR-comment PoC sandboxes
└── iter<N>-cat<X>/sandbox/ # cat-owned source-mutation sandboxes
```

`<id>` is typically the PR number, or a range like `186-187` when auditing stacked PRs.

## References

The detailed operational rules are split across:

- [`category-roles.md`](./exploratory-pr-verification/references/category-roles.md) — per-cat
  domain, angle, MCP driver mapping; sibling-library comparison targets by project genre
- [`pdca-workflow.md`](./exploratory-pr-verification/references/pdca-workflow.md) — 5-step PDCA
  cycle for dynamic verification, config-file mutation surface, output-confirmation tactics
- [`ticket-format.md`](./exploratory-pr-verification/references/ticket-format.md) — numbering
  scheme, per-cat reservation, directory layout, file format, severity guidelines, dedup
- [`pr-comment-policy.md`](./exploratory-pr-verification/references/pr-comment-policy.md) —
  posting threshold, cluster policy, saturation limits, mandatory pre-post PoC, self-correction
  format, latency-aware cadence
- [`cluster-families.md`](./exploratory-pr-verification/references/cluster-families.md) — C-1 …
  C-11 family classification for follow-up PR scope design
- [`retrospective-meta.md`](./exploratory-pr-verification/references/retrospective-meta.md) —
  past-feedback table, lag interpretation, how to maintain the table

## Prerequisites

- Git-managed Kotlin / Gradle project
- `gh` CLI authenticated
- `.local/` in `.gitignore`
- Access to MCP drivers matching the project's target surface (Playwright for web, Maestro for
  Android, IntelliJ-MCP for IDE plugins, etc.) — only required for cat4
- The orchestrator runs in an environment that allows N parallel subagents and background tasks
