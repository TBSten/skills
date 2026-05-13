---
name: exploratory-nightly-verification
description: >
  Operational rules for a 60-minute single-shot exploratory verification of a Kotlin project's
  main branch (Android / JVM / KMP / Compose / server-side Kotlin). Runs sequentially through
  cat1–cat5 inside a single CI job (e.g. a nightly GitHub Actions workflow), writes findings
  Markdown-by-Markdown to .local/tmp/exploratory-nightly-<date>/issues/<NN>-<slug>.md as it
  goes, and produces a SUMMARY.md at the end. No PR side effects (no comments, no issue
  filing, no pushes). Tracks the latest Kotlin / Compose / Gradle / AGP releases alongside the
  project itself.
  Use when requested: "nightly exploration", "nightly 探索", "夜間探索的検証",
  "exploratory-nightly", "60 分 budget で main を探索", "夜間 main quality scan".
---

# Exploratory Nightly Verification

Run from a nightly CI job (e.g. `anthropics/claude-code-action` on a schedule) to explore the
**main branch** in a 60-minute budget. Targets the library / app itself, not a specific PR.

This skill is a sibling to [`exploratory-pr-verification`](../exploratory-pr-verification/SKILL.md),
but differs significantly:

| | exploratory-pr-verification | exploratory-nightly-verification |
|--|----------------------------|---------------------------------|
| Target | a specific PR diff | the latest main |
| Time budget | open-ended (loop until deadline) | 60 min, single shot |
| Parallelism | 5 parallel subagents | 1 agent, sequential cat1 → cat5 |
| Output channel | PR comments + tickets | issue Markdown files only |
| Side effects | posts PR comments | **none** (read-only) |
| Loop / reentry | yes (multiple iterations) | no |

## 0. Invariants (do not violate)

| # | Rule | Why |
|---|------|-----|
| A | **Write each finding immediately** to `.local/tmp/exploratory-nightly-<date>/issues/<NN>-<slug>.md`. Don't batch. | The 60-min budget can kill the job at any point; batched findings get lost. |
| B | **Stop new exploration at T=50min.** Spend the last 10 min on formatting / deduplication / SUMMARY.md. | If the job times out mid-exploration, you lose only formatting time, not findings. |
| C | **Issue filenames are zero-padded sequential** (`01-<slug>.md`, `02-<slug>.md`, ...). | Downstream notification scripts (if any) sort by filename — gaps and duplicates break parsing. |
| D | **No side effects on the project**: no PR comments, no GitHub issue filing, no pushes, no branch ops. | Nightly is read-only verification. Side effects from a scheduled job are accident-prone. |

## 1. Workflow (PDCA compressed into 60 minutes)

1. **Environment confirmation (~3 min)**
   - `pwd` / `git rev-parse HEAD` / `git status --short`
   - Note the current versions from `gradle/libs.versions.toml` or equivalent
2. **Plan (~5 min)**
   - Read [`references/categories.md`](references/categories.md), pick the cat1 → cat5 order
   - Note a per-cat time budget in TodoWrite or a scratch list
3. **Sequential exploration (~50 min)**
   - cat1 → cat2 → cat3 → cat4 → cat5, ~8–10 min each
   - For every finding, **write the issue file immediately** in the fixed format
   - One file written → next finding
4. **Final formatting (50–58 min)**
   - Re-read all issue files: deduplicate, polish titles, tighten the `## 詳細` body
   - Write `.local/tmp/exploratory-nightly-<date>/SUMMARY.md` with count + per-category /
     per-severity breakdown
5. **Termination report (~60 min)**
   - Final message: "wrote N issues, wrote SUMMARY.md". That's all.

## 2. Categories

Five categories run sequentially. Full details in [`references/categories.md`](references/categories.md).
Summary:

- **cat1 — Static analysis**: KDoc / SoT / silent failure / type design / TODO grep
- **cat2 — CI logs, public-API baseline, docs consistency**: nightly warning grep, baseline drift,
  README ↔ docs consistency
- **cat3 — Dynamic build / test**: run the project's primary `test` task (and integration / docs
  variants if present); capture failures and warnings
- **cat4 — Upstream release watching**: Kotlin / Compose Multiplatform / Gradle / AGP releases vs.
  the project's `libs.versions.toml`
- **cat5 — Comparison / leftover angles**: sibling-library comparison, residual TODO grep,
  semver / BCP review, sample-app launch sanity

Parallelism is **off** — single CI VM with a 60-minute budget cannot afford parallel build
contention. Sequential is the safe default.

## 3. Issue Markdown format

Full spec in [`references/issue-format.md`](references/issue-format.md). Required fields:

- Path: `.local/tmp/exploratory-nightly-<date>/issues/<NN>-<kebab-slug>.md`
  - `<NN>`: zero-padded 2-digit sequential number, starting at `01`
  - `<kebab-slug>`: lowercase + hyphens, ≤ 40 chars
  - `<date>`: `YYYYMMDD` (e.g. `20260513`)
- Line 1: `# <Short title>` (≤ 60 chars; non-Latin OK)
- Then these metadata lines + headings:
  - `**Category**: cat<N> (<name>)`
  - `**Severity**: P0 | P1 | P2 | P3`
  - `**Detected at commit**: <first 12 chars of git rev-parse HEAD>`
  - `## Reproduction`
  - `## Detail`
  - `## Fix proposal (optional)`

The title, the metadata lines, and the first few lines of `## Detail` are the candidate excerpt
for any downstream notification (Slack / Discord / email). Write them at a granularity that's
meaningful **without** the surrounding context.

## 4. Severity reference

| Severity | Definition |
|----------|------------|
| P0 | A library consumer will hit a guaranteed build / runtime failure on a default path |
| P1 | Public API behavior is broken / security concern / critical docs error |
| P2 | Internal inconsistency, refactor opportunity, future-brittleness signal |
| P3 | Noise level — typo, minor improvement |

P0 / P1: write the issue immediately when found. P2 / P3: write if time allows.

## 5. Tool selection by category

- `Read` / `Glob` / `Grep`: static analysis (cat1, cat2, cat5)
- `Bash`: Gradle / git / API-dump roundtrip (cat2, cat3)
  - Long tasks: `timeout 8m <cmd>` to protect the 60-min budget
  - Always redirect to `.local/tmp/exploratory-nightly-<date>/tmp/<ts>-<cmd>.log` —
    don't truncate with `grep` / `head` on the live stream
- `WebFetch`: Kotlin / Compose / Gradle / AGP release pages (cat4)
- `WebSearch`: cat5 leftover-angle exploration

The `.local/tmp/exploratory-nightly-<date>/tmp/` log files are **not** artifact-preserved by
default — only the issue Markdown files travel out of the job. Whatever matters must be transcribed
into the issue file before the job ends.

## 6. Unexpected-failure handling

- `WebFetch` rate-limit / failure → write that fact itself as a P3 issue, proceed to the next category
- Gradle task failure → write as P1 (or P2 if the failure is clearly CI-environment-specific)
- This SKILL.md fails to load → the wrapping CI step's `if: always()` should still surface the failure;
  no recovery in the skill itself

## 7. Termination

After the exploration loop ends, write `.local/tmp/exploratory-nightly-<date>/SUMMARY.md`:

```markdown
# Nightly Exploration Summary

- Run timestamp: <ISO 8601>
- Target commit: <git rev-parse HEAD>
- Issue count: N

## Per-category breakdown
- cat1: N
- cat2: N
- cat3: N
- cat4: N
- cat5: N

## Per-severity breakdown
- P0: N
- P1: N
- P2: N
- P3: N
```

Then **stop**. Do not re-kick. Do not call `/loop`. Do not call `ScheduleWakeup`. The final user
message says only "wrote N issues, wrote SUMMARY.md".

## 8. Notification integration (optional)

If the wrapping CI workflow uploads the issue Markdown files to Slack / Discord / email, the
filename ordering and the issue-format spec (`references/issue-format.md`) are the contract.

A typical pipeline:

1. This skill writes `.local/tmp/exploratory-nightly-<date>/issues/<NN>-<slug>.md`
2. A separate CI step (a shell script, not this skill) parses the directory in `sort` order and
   produces the notification payload

Keep the parsing contract simple — the H1 line for the title, the `**Category**` / `**Severity**`
/ `**Detected at commit**` lines for metadata, the first 200 chars of `## Detail` for the body
excerpt. See [`references/issue-format.md`](references/issue-format.md) for the canonical spec.

The notification integration is **outside this skill's scope**. The skill writes Markdown; the
notification step parses Markdown.
