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

## Prerequisites

`git` and `gh` (GitHub CLI) must work locally. Verify with `gh auth status`; if it fails, report
to the user and abort the pass. See [`references/operations.md`](references/operations.md) for the
full operational contract (owner/repo derivation, dirty-worktree abort, log-fetch timing, rerun
blocking, commit granularity).

## Input

- A **list of PR numbers** (one or more, space-separated). e.g. `179 180 181`
- If omitted, derive the single PR for the current branch via `gh pr view --json number`

Branch name, base branch, and stack relationships are read **every pass** via
`gh pr view <num> --json headRefName,baseRefName` — never held as a static mapping in the input,
so the loop follows reorders / rebases / branch renames automatically.

## Workflow (one pass)

Run the following **for every PR** in one pass. Do not skip PRs (if one is fully in-progress and
nothing can be done, still advance the others). Steps are a natural-number sequence.

### Step 1: Rebase the latest base into each PR branch

At the top of the pass, `git fetch origin`, then rebase each PR branch **onto the latest of its own
`baseRefName`** — which is the **parent PR branch** for a stack, or the **default branch**
otherwise, so it differs per PR. This prevents a PR branch from going stale when its base moves
under it (a sibling PR merged this session, a dependabot bump, another collaborator's merge).

```bash
git fetch origin
for pr in $PR_LIST; do
  read -r branch base <<<"$(gh pr view "$pr" --json headRefName,baseRefName -q '.headRefName + " " + .baseRefName')"
  git checkout "$branch"
  git fetch origin "$branch" "$base"
  git reset --hard "origin/$branch"   # match remote first
  if ! git merge-base --is-ancestor "origin/$base" HEAD; then
    git rebase "origin/$base" || { git rebase --abort; echo "[PR #$pr] rebase conflict, defer to next pass"; continue; }
    git push --force-with-lease origin "$branch"
  fi
done
```

On conflict, `git rebase --abort` and report "deferred to next pass" — never auto-merge. For a
stack, the `baseRefName` **is** the parent PR's `headRefName`; if the parent is not yet merged,
rebase the parent to latest first, then the child. Process in topological order.

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

`gh pr view` is cheap; collect everything at once:

```bash
for pr in $PR_LIST; do
  gh pr view "$pr" --json statusCheckRollup,state,headRefName,baseRefName,mergeable
done
```

Derive owner/repo once (`gh repo view --json owner,name -q '.owner.login + "/" + .name'`, prefer
`origin`) and reuse. Also fetch **both** comment kinds — a PR has **inline review threads** and
**conversation-level issue comments**, and both must be checked. Both APIs paginate; page through
them. See [`references/review-handling.md`](references/review-handling.md) for the exact GraphQL /
REST queries and the "unhandled" predicate.

### Step 4: Classify failing checks and delegate

Walk each PR's `statusCheckRollup`; for every job with `conclusion == "FAILURE"`, determine the
failure kind using the ordered heuristics in
[`references/failure-classification.md`](references/failure-classification.md)
(transient → lint → binary → test → build). Then delegate:

| Kind | Delegate to |
|------|-------------|
| transient infra (toolchain redirect / 5xx / runner image / network timeout) | `gh run rerun <runId> --failed` (rejected while the workflow is in-progress — defer to next pass) |
| lint (`ktlintCheck`, `eslint`, …) | `fix-ci-lint` skill |
| binary compat (`apiCheck` / BCV) | `fix-ci-binary` skill |
| build (`compileKotlin` / `assembleDebug` / …) | `fix-ci-build` skill |
| test (`jvmTest` / `jsBrowserTest` / `iosTest` / …) | `fix-ci-test` skill |

The `fix-ci-*` names are a **naming scheme, not a hard dependency**. If the matching skill does
**not** exist in the current repo, fetch the failing job's log tail
(`gh api /repos/$OWNER/$REPO/actions/jobs/<jobId>/logs`), summarize the cause, and report to the
user — do **not** run off and hand-fix. Do each PR's fix on its own branch (`git checkout <headRefName>`).

### Step 5: Handle unresolved review / issue comments

Full procedure in [`references/review-handling.md`](references/review-handling.md). In brief:

- **inline review thread** → fix per thread, push, then `resolveReviewThread` mutation
- **issue-level comment** (no resolve mechanism on GitHub) → mark handled by wrapping the body in a
  collapsible `<details><summary>…</summary>` marker with the fixing commit hash appended (a
  convention this skill establishes; the marker text is a project choice)
- **Miss-prevention**: scan issue comments every pass; any comment lacking the handled-marker counts
  the same as `unresolved ≥ 1` toward Step 7's "changed" verdict

### Step 6: Stack rebase chain

If a PR's `baseRefName` points at **another PR's branch** (= that PR's `headRefName`), it is a
stack (e.g. `#181.baseRefName == #180.headRefName` → #181 stacks on #180). After pushing a commit
to the parent, rebase and force-push the child:

```bash
git checkout <child.headRefName>
git rebase <parent.headRefName>
git push --force-with-lease origin <child.headRefName>   # only if the rebase was clean
```

On conflict, give up for this pass and report — never auto-merge. Process stacks in **topological
order**: finish the parent's fix + push before rebasing the child.

### Step 7: Termination check

Re-derive (or reuse Step 3) state and classify the pass as **"no-change"** or **"changed"**:

- **no-change** = every PR has `failures == [] && unresolved_review_threads == 0 &&
  unhandled_issue_comments == 0` **and** you pushed no new commit this pass
- otherwise = **changed**

To avoid chatter under a `/loop` driver, **exit after the no-change streak reaches N (default 5)**;
any "changed" pass resets the streak. Persist the streak so it survives across passes:

```bash
STREAK_FILE=.local/tmp/pr-fix-loop-streak.txt
mkdir -p "$(dirname "$STREAK_FILE")"
prev=$(cat "$STREAK_FILE" 2>/dev/null || echo 0)
[[ "$LOOP_OUTCOME" == "no-change" ]] && next=$((prev + 1)) || next=0
echo "$next" > "$STREAK_FILE"
if (( next >= 5 )); then
  echo "[pr-fix-loop] 5 consecutive no-change passes — terminating"
  rm -f "$STREAK_FILE"   # clean start next invocation
fi
```

Always append **`(no-change streak: N/5)`** to the end-of-pass report; at `5/5`, state "terminating"
explicitly. Whether the user stops `/loop` itself is their call, but the skill treats `5/5` as a
"nothing left to do" signal.

## References

- [`references/failure-classification.md`](references/failure-classification.md) — Step 4 CI
  failure-kind heuristics (ordered transient → lint → binary → test → build, with false-positive notes)
- [`references/review-handling.md`](references/review-handling.md) — Step 5 review-thread /
  issue-comment fetch, fix, resolve, and the handled-marker wrap convention; pagination
- [`references/operations.md`](references/operations.md) — `gh` prerequisites, owner/repo
  derivation, dirty-worktree abort, log-fetch timing, rerun blocking, non-interactive rebase,
  cross-platform `stat`, commit granularity

## Output (end of each pass)

Close each pass with one short table:

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

At `5/5`, state "terminating" on that line and delete the streak file (see Step 7 snippet).
