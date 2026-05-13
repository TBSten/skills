# Issue Markdown Format

Specification for the Markdown files written to
`.local/tmp/exploratory-nightly-<date>/issues/<NN>-<kebab-slug>.md`.

This format is the contract between the skill and any downstream notification step
(Slack / Discord / email / GitHub Issues sync). **Field names, heading levels, and ordering must
be preserved exactly**.

## Filename rules

- Path: `.local/tmp/exploratory-nightly-<date>/issues/<NN>-<kebab-slug>.md`
- `<date>`: `YYYYMMDD` (e.g. `20260513`)
- `<NN>`: zero-padded 2-digit sequential number, starting at `01`. No gaps, no duplicates.
- `<kebab-slug>`: lowercase letters + digits + hyphens, ≤ 40 chars. Should hint at the issue
  topic.

Examples:

- `.local/tmp/exploratory-nightly-20260513/issues/01-bcv-baseline-drift.md`
- `.local/tmp/exploratory-nightly-20260513/issues/02-kotlin-2-4-ga-not-tracked.md`

## Body template

```markdown
# <Short title>

**Category**: cat<N> (<name>)
**Severity**: P0 | P1 | P2 | P3
**Detected at commit**: <first 12 chars of git rev-parse HEAD>

## Reproduction
- ...
- ...

## Detail
<~200 chars. The first lines are the candidate excerpt for downstream notification.>

## Fix proposal (optional)
- ...
```

## Required fields

| Field | Example | Notes |
|-------|---------|-------|
| `# <title>` | `# BCV baseline diverges from compat-k*` | Line 1. ≤ 60 chars. Non-Latin OK. |
| `**Category**` | `**Category**: cat2 (CI logs / public-API baseline)` | Must match a name from `categories.md` |
| `**Severity**` | `**Severity**: P1` | One of P0 / P1 / P2 / P3 |
| `**Detected at commit**` | `**Detected at commit**: 89b45afbc1a3` | `git rev-parse --short=12 HEAD` |
| `## Reproduction` | 2–5 bullet points | Granular enough for someone other than the original Claude to reproduce |
| `## Detail` | ~200 chars | The first 200 chars are the notification excerpt. Pack "what's wrong" into the lead. |

## Optional fields

- `## Fix proposal (optional)`: candidate fix approaches
- `## Related links`: PR / issue / docs / external URLs
- `## Dynamic evidence`: command output excerpts if cat3 / cat4 produced any

## NG patterns

- Line 1 starts with `## ` instead of `# ` → downstream formatting sees an empty title
- `**Category**:` renamed to English / Japanese variant the script doesn't recognize → metadata
  is dropped
- The same phenomenon split across multiple files → deduplicate during the 50–58 min formatting
  window
- Sequential numbering with gaps (e.g. `01` then `03`) → notification numbering breaks
- The body contains a TODO list or chat log instead of a self-contained issue description → write
  the issue as if it would be read once, in isolation, by someone unfamiliar with the run

## Why the format is strict

A downstream CI step (often a shell script) parses these files by `sort`-ing the directory and
grepping for the fixed metadata lines. Any deviation breaks the contract silently — the
notification just drops the affected issue.

If a project wants a different format, it should adopt this skill's output as-is and run a
separate post-processing step. Don't modify the format inside this skill — the format **is** the
skill's external contract.
