---
name: operations
description: >
  Operational tips for pr-fix-loop — gh CLI prerequisite, dirty-worktree abort, owner/repo
  derivation, log-fetch timing, rerun blocking conditions, non-interactive rebase, cross-platform
  stat, commit granularity, and API pagination. The reference for when pr-fix-loop hits an
  implementation snag in any of Steps 1–7.
---

# pr-fix-loop operational tips

## gh CLI is a hard prerequisite

This skill assumes `gh` (GitHub CLI) and `git` work locally. Verify auth up front with
`gh auth status`. On failure, report to the user immediately and abort this pass.

## Derive owner / repo from origin

When only a PR list is given, derive owner/repo:

```bash
gh repo view --json owner,name -q '.owner.login + "/" + .name'
```

Prefer `origin` on multi-remote repos. Capture it once per session into `$OWNER` / `$REPO` and
reuse (as GraphQL `-F` variables and REST path segments) — safer than re-deriving.

## When to abort branch checkout

Sequentially checking out several PRs churns the local worktree. To avoid touching the user's
in-progress work, at pass start run `git status -s` and **abort the skill if the worktree is
dirty**. (A `--force-on-dirty` flag could be added later if needed; unimplemented for now.)

Alternatively, stash small/short-lived work with `git stash push -u -m "pr-fix-loop temp save"`
before switching branches and `git stash pop` after — but `pop` can conflict, so only do this for
genuinely short-lived work.

## Log-fetch timing

`gh run view --log` may not return until the **whole workflow** finishes. Individual job logs come
back almost immediately via:

```bash
gh api "/repos/$OWNER/$REPO/actions/jobs/<jobId>/logs"
```

Completed jobs' logs are fetchable even while other jobs are still `in_progress`.

## Rerun timing

`gh run rerun --failed` is **blocked while the workflow is in-progress** (`This workflow is already
running`). In that case, decide "defer to next pass" and skip this PR's transient-failure handling.
Sleeping and polling burns the loop's token/time budget.

## Commit granularity

Split each PR's fix commits into **meaningful units**. "lint fix + apiDump" is two logical changes —
separate them when you can. But a single review-thread fix that spans multiple files is fine as one
commit (per-thread granularity).

## Non-interactive rebase

The shell Claude Code runs in cannot open an editor, so `git rebase -i` does not fit a
non-interactive flow. In Step 2 (commit tidy-up), replace it with a "reset → reconstruct" pattern:
`git reset --soft <block-parent> && git commit -m "..."`. This is a shell-capability constraint, not
a repo-policy ban.

## Cross-platform stat

`stat -f %m` (BSD/macOS) and `stat -c %Y` (GNU/Linux) are incompatible. For the 24h cooldown
marker's mtime check, use a portable method — python3 (`os.path.getmtime`) or perl
(`(stat shift)[9]`). Plain `stat` is environment-dependent and may not work.

## Pagination

GitHub API default page sizes are small:

- REST (`gh api repos/.../issues/<pr>/comments`): 30 by default → `gh api --paginate` for all pages
- GraphQL `reviewThreads(first: N)`: N per page → paginate with `pageInfo { hasNextPage endCursor }`

Single-call fetches silently drop data on PRs with 50+ threads or 30+ comments.
