---
name: review-handling
description: >
  Conventions for pr-fix-loop Step 5: fixing and closing out review threads and issue-level
  comments. Fetching is owned by scripts/fetch-pr-state.sh; closing out is owned by
  scripts/resolve-thread.sh (native GitHub resolve) and scripts/mark-comment-handled.sh
  (collapsible <details> handled-marker wrap with the fixing commit hash — GitHub has no native
  resolve for conversation comments). This document keeps the judgment rules: commit↔thread
  association, per-point commits for multi-point comments, and miss-prevention.
---

# Review handling — pr-fix-loop Step 5

The conventions behind `pr-fix-loop` Step 5. The parent `SKILL.md` only sketches this; the
mechanics live in `scripts/` — run them as-is, don't re-derive their API calls.

A PR has **two kinds** of comment, and pr-fix-loop must check **both**. `fetch-pr-state.sh`
returns both per PR, fully paginated:

1. **inline review thread** → `unresolvedThreads` (GitHub has a native resolve mechanism;
   "unhandled" ⇔ `isResolved == false`)
2. **issue-level / conversation comment** → `unhandledIssueComments` (no resolve mechanism;
   "handled" is marked by the wrap convention below)

## 5-A. Inline review threads

A PR with ≥ 1 unresolved thread is delegated to a **fix-ci-pr-comments** skill if one exists.
Otherwise, work from `unresolvedThreads[]` (`{id, path, line, firstComment}`) and handle one at
a time: understand → fix → commit → push → then

```bash
"${CLAUDE_SKILL_DIR}/scripts/resolve-thread.sh" <threadId> [commitHash]
```

The script runs the `resolveReviewThread` mutation and verifies the thread came back resolved.

When you handle several threads in one commit, **associate** them with the fix (don't
bulk-resolve everything blindly). Ideally write "handled in this commit" per thread; including
the thread id in the commit message is enough — the optional `commitHash` argument only echoes
the association into the pass report.

## 5-B. Issue-level comments

GitHub has no native resolve mechanism here, so "handled" is marked by **wrapping the body in a
collapsible `<details><summary>…</summary>` and appending `--> <commit-hash>`**:

```
<details><summary>Resolved</summary>

(original comment body)

</details>

--> <commit-hash>
```

This is a convention this skill establishes; the summary text is a project choice (e.g.
`Resolved`, `done`, or a localized label) — set it once via the `PR_FIX_LOOP_RESOLVED_SUMMARY`
environment variable (default `Resolved`) and it is used consistently by both the unhandled
filter in `fetch-pr-state.sh` and the writer.

Work from `unhandledIssueComments[]` (`{id, excerpt}`; the excerpt is the first 200 chars —
fetch the full body from GitHub if the excerpt is not enough to act on). Understand → implement
the fix → commit → push → then

```bash
"${CLAUDE_SKILL_DIR}/scripts/mark-comment-handled.sh" <commentId> <commitHash...>
```

The script fetches the original body, wraps it, PATCHes it back, and verifies the marker took.
It is idempotent: an already-wrapped comment is left untouched.

For a comment containing several independent points, split the fix into per-point commits and
pass every hash (`mark-comment-handled.sh <id> <commit-A> <commit-B>`); note which commit
covers which point in your pass report.

## Miss-prevention

Every pass, `fetch-pr-state.sh` re-fetches **all** issue comments and review threads with full
pagination (mandatory — a single page returns 30 comments / `first: N` threads, and anything
beyond it would be silently lost). If ≥ 1 comment lacks the handled-marker wrap, count it the
**same as `unresolved ≥ 1`** toward Step 7's "changed" verdict. Watching only inline review
threads and missing issue comments lets a maintainer's follow-up sit unhandled.
