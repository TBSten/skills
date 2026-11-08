---
status: Experimental
group: Git / GitHub
---

# visual-explanation Rule

[Japanese](./visual-explanation.ja.md)

A [Claude Code](https://docs.anthropic.com/en/docs/claude-code) rule that makes every human-facing
explanation (PR descriptions, ticket descriptions, reports) start with something visual: a diagram,
a screenshot, a video or real output.

## Quick Start

### 1. Install the rule:

```bash
curl -fsSL https://rules.tbsten.me/i | bash -s -- visual-explanation
```

### 2. (Recommended) Install the skill that does the mechanical work:

```bash
gh skill install tbsten/skills visual-explanation
```

The [`visual-explanation` skill](../skills/visual-explanation.md) provides the HTML template, the
screenshot script, the script that attaches images to a PR with SHA-pinned URLs, a body checker and
the PR template setup. The rule tells Claude to use it when it is installed.

## What it does

This rule is always loaded (no path trigger). It tells Claude:

| Topic | Rule |
|---|---|
| When | Writing a PR body, a ticket description or a report for humans |
| What to show | Chosen by the kind of change: Before / After for visual or output changes, a mechanism diagram for bugs, a one-page summary for new APIs, a GIF for things that move, a removed / kept diagram for deletions, a now / goal diagram for new tickets, a diff for tiny changes, `<details>` output when nothing is visual, nothing for CI-only changes |
| How to make it | Decide the one sentence the image says before drawing and use it as the heading (no sentence, no image), one message per image, only a heading and short labels inside the image, two or three meaningful colors, alt text says what the image shows, built from real runs (no mocks), honest about how it was made, HTML + headless Chrome at scale 2–3, readable at ~420px, project design tokens |
| Where images live (PR) | Committed to `.github/pr-assets/` on the PR branch and removed in the next commit; the body links the SHA-pinned URL (never a branch URL). Keep each image under ~500 KB: with merge commits the image stays in main's history, with squash merges the pinned commit is reachable only from the PR |
| Where images live (ticket) | Attached in the ticket tool, or SHA-pinned URL as a fallback |
| Position | First image at the top of "What I did" (or "Background" for tickets); prose supports it |

## Files

The rule ships only `RULE.md` (installed as `.claude/rules/visual-explanation.md`). It deliberately
has no reference files: the installer overwrites files in the current directory, and a bundled
`.github/pull_request_template.md` would clobber an existing template. The skill's
`setup-pr-template.sh` handles the template instead.

## Origin

Extracted from a project where PRs gained diagrams and screenshots after a single comment in a
thread, spread through Claude's project memory. Images went from 0 / 9 PRs to 19 / 27 (plus 6 with
textual evidence). Because the practice lived only in memory, it would stop if that memory were lost or the work
moved to another environment, so this rule puts it in the repository.
