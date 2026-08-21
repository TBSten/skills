#!/usr/bin/env bash
# rebase-pass.sh — pr-fix-loop Steps 1 & 6: rebase every PR branch onto the
# latest of its own base, stacked PRs in topological order (parent first).
#
# Usage: rebase-pass.sh <pr-number> [<pr-number>...]
#
# Per PR (SKILL.md Step 1): fetch branch+base, reset --hard to origin/<branch>
# (match remote first), and only if origin/<base> is not already an ancestor:
# rebase onto origin/<base> and push --force-with-lease. On conflict: abort the
# rebase and defer to the next pass — never auto-merge. Because each PR's base
# is fetched right before it is processed, a child picks up the parent's fresh
# push within the same invocation (Step 6 chain). Idempotent: a second run on
# an already-clean set reports "clean" everywhere and pushes nothing.
#
# stdout: ONE line — a JSON array with one object per PR (topological order):
#   { pr, branch, base, action: "rebased"|"clean"|"conflict-deferred", detail }
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib.sh
. "$SCRIPT_DIR/lib.sh"

[ "$#" -ge 1 ] || die "no PR numbers given" \
  "rebase-pass.sh needs at least one PR number" \
  "usage: rebase-pass.sh <pr-number> [<pr-number>...]"

require_cmd git jq
require_cmd "$GH_CMD"
preflight_gh_auth
require_clean_worktree

# --- read branch / base for every PR (never a static mapping) -----------------
PRS=()
BRANCHES=()
BASES=()
i=0
for pr in "$@"; do
  require_pr_number "$pr"
  if ! pr_json="$("$GH_CMD" pr view "$pr" --json headRefName,baseRefName)"; then
    die "gh pr view $pr failed" \
      "PR #$pr could not be fetched" \
      "check the PR number and that it belongs to this repository"
  fi
  PRS[$i]="$pr"
  BRANCHES[$i]="$(printf '%s' "$pr_json" | jq -r '.headRefName')"
  BASES[$i]="$(printf '%s' "$pr_json" | jq -r '.baseRefName')"
  i=$((i + 1))
done
COUNT=$i

# --- topological order: a PR whose base is another listed PR's branch waits ---
remaining=""
idx=0
while [ "$idx" -lt "$COUNT" ]; do remaining="$remaining $idx"; idx=$((idx + 1)); done
ordered=""
while [ -n "$(printf '%s' "$remaining" | tr -d ' ')" ]; do
  progressed=0
  next_remaining=""
  for idx in $remaining; do
    blocked=0
    for j in $remaining; do
      [ "$j" = "$idx" ] && continue
      [ "${BRANCHES[$j]}" = "${BASES[$idx]}" ] && blocked=1
    done
    if [ "$blocked" = 1 ]; then
      next_remaining="$next_remaining $idx"
    else
      ordered="$ordered $idx"
      progressed=1
    fi
  done
  remaining="$next_remaining"
  [ "$progressed" = 1 ] || die "stack cycle detected among the given PRs" \
    "the base branches of the remaining PRs point at each other in a loop" \
    "check 'gh pr view <n> --json headRefName,baseRefName' for each PR and fix the bases"
done

WORK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/pr-fix-loop-rebase.XXXXXX")"
trap 'rm -rf "$WORK_DIR"' EXIT
RESULTS="$WORK_DIR/results.jsonl"
: > "$RESULTS"

emit() { # emit <pr> <branch> <base> <action> <detail>
  jq -cn --argjson pr "$1" --arg branch "$2" --arg base "$3" \
    --arg action "$4" --arg detail "$5" \
    '{pr: $pr, branch: $branch, base: $base, action: $action,
      detail: (if $detail == "" then null else $detail end)}' >> "$RESULTS"
}

git fetch origin 1>&2 || die "git fetch origin failed" \
  "the pass needs the latest remote state before rebasing" \
  "check network / remote configuration and re-run"

for idx in $ordered; do
  pr="${PRS[$idx]}"
  branch="${BRANCHES[$idx]}"
  base="${BASES[$idx]}"
  log "[rebase-pass] PR #$pr: $branch onto $base"

  git fetch origin "$branch" "$base" 1>&2 || die \
    "git fetch origin $branch $base failed (PR #$pr)" \
    "the PR branch or its base does not exist on origin (deleted or renamed?)" \
    "check 'gh pr view $pr' and the remote branches, then re-run"

  if ! git checkout "$branch" 1>&2 2>/dev/null; then
    git checkout -b "$branch" "origin/$branch" 1>&2 || die \
      "could not check out $branch (PR #$pr)" \
      "neither a local branch nor origin/$branch was checkoutable" \
      "inspect 'git branch -a' and the remote, then re-run"
  fi
  git reset --hard "origin/$branch" 1>&2   # match remote first

  if git merge-base --is-ancestor "origin/$base" HEAD; then
    emit "$pr" "$branch" "$base" "clean" ""
    continue
  fi

  if ! git rebase "origin/$base" 1>&2; then
    git rebase --abort 1>&2 || true
    emit "$pr" "$branch" "$base" "conflict-deferred" \
      "rebase conflict — aborted, defer to next pass (never auto-merge)"
    continue
  fi

  if git push --force-with-lease origin "$branch" 1>&2; then
    emit "$pr" "$branch" "$base" "rebased" ""
  else
    emit "$pr" "$branch" "$base" "conflict-deferred" \
      "push --force-with-lease rejected (remote moved underneath) — defer to next pass"
  fi
done

jq -cs '.' "$RESULTS"
