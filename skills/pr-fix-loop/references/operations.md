---
name: operations
description: >
  Operational contract for pr-fix-loop — what the scripts' preflight enforces (gh auth,
  dirty-worktree abort, owner/repo derivation), plus the judgment-side tips that stay with the
  AI: log-fetch timing, rerun blocking conditions, non-interactive rebase, cross-platform stat,
  commit granularity, and why pagination is non-negotiable. The reference for when pr-fix-loop
  hits an implementation snag in any of Steps 1–7.
---

# pr-fix-loop operational tips

The mechanics themselves live in `scripts/` — run them as-is (see SKILL.md "Scripts"). This
document explains the contract they enforce and the operational judgment that remains with you.

## Script contract

- **stdout**: exactly one line of JSON per invocation (multi-PR scripts emit a JSON array).
  Everything human-readable goes to **stderr**.
- **Failure**: non-zero exit with a `what / why / fix` message on stderr. Relay it to the user;
  it tells you whether to abort the pass or defer one PR.
- **Idempotent**: safe to re-run after a partial pass (e.g. `rebase-pass.sh` reports `clean`
  for PRs it already settled; `mark-comment-handled.sh` leaves an already-wrapped comment alone).
- **`GH_CMD`**: environment variable that substitutes the `gh` binary — used by tests to inject
  a stub; leave it unset in normal operation.
- **`PR_FIX_LOOP_RESOLVED_SUMMARY`**: the handled-marker summary text (default `Resolved`);
  see review-handling.md. Reader (`fetch-pr-state.sh`) and writer (`mark-comment-handled.sh`)
  share the value via this variable, so overriding it keeps them consistent.

## Hard prerequisites: gh, git, jq

Every script's preflight verifies `git` / `gh` / `jq` exist and `gh auth status` passes. On
failure it exits with the fix instruction (e.g. `gh auth login`) — report to the user and abort
this pass.

## Owner / repo derivation

Scripts derive owner/repo themselves from the current clone (`gh repo view`); prefer running
them from a clone whose `origin` points at the GitHub repo. Nothing to pass in — a wrong-repo
result means the working directory is the wrong clone.

## Dirty-worktree abort

Sequentially checking out several PRs churns the local worktree. To avoid touching the user's
in-progress work, `fetch-pr-state.sh` and `rebase-pass.sh` **abort while the worktree is dirty**.
Resolve it before the pass: commit, or stash small/short-lived work with
`git stash push -u -m "pr-fix-loop temp save"` and `git stash pop` after — but `pop` can
conflict, so only do this for genuinely short-lived work.

## Log-fetch timing

`gh run view --log` may not return until the **whole workflow** finishes, so
`classify-failure.sh` uses the per-job log endpoint instead: completed jobs' logs are fetchable
even while other jobs are still `in_progress`. If the script fails with "log not available",
that job hasn't finished writing its log — defer to the next pass.

## Rerun timing

`gh run rerun --failed` (your action on `kind: "transient"`) is **blocked while the workflow is
in-progress** (`This workflow is already running`). In that case, decide "defer to next pass"
and skip this PR's transient-failure handling. Sleeping and polling burns the loop's token/time
budget.

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

`stat -f %m` (BSD/macOS) and `stat -c %Y` (GNU/Linux) are incompatible. For the Step 2 24h
cooldown marker's mtime check, use a portable method — python3 (`os.path.getmtime`) or perl
(`(stat shift)[9]`). Plain `stat` is environment-dependent and may not work.

## Pagination (why fetch-pr-state.sh owns the fetch)

GitHub API default page sizes are small: REST issue comments return 30 per page, GraphQL
`reviewThreads(first: N)` returns N per page. A single-call fetch **silently drops data** on
PRs with 50+ threads or 30+ comments — a dropped review comment is a maintainer follow-up that
never gets handled. `fetch-pr-state.sh` pages through both APIs to completion
(`--paginate` / `pageInfo { hasNextPage endCursor }`); never hand-roll a one-shot fetch in its
place.
