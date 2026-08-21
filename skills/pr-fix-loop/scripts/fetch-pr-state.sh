#!/usr/bin/env bash
# fetch-pr-state.sh — pr-fix-loop Step 3: preflight + batch-fetch all PR state.
#
# Usage: fetch-pr-state.sh <pr-number> [<pr-number>...]
#
# Preflight: gh auth status, dirty-worktree abort, owner/repo derivation.
# Per PR it fetches CI checks, inline review threads (GraphQL, paginated with
# pageInfo { hasNextPage endCursor }) and issue-level comments (REST,
# --paginate) — pagination is mandatory; single-call fetches silently drop
# data on PRs with 50+ threads or 30+ comments (operations.md).
#
# stdout: ONE line — a JSON array with one object per PR:
#   { pr, headRef, baseRef, stackParent,          # stackParent: PR number of the
#     failingChecks: [{jobId, jobName, runId}],   #   parent when baseRef is another
#     inProgress,                                 #   listed PR's headRef, else null
#     unresolvedThreads: [{id, path, line, firstComment}],
#     unhandledIssueComments: [{id, excerpt}] }   # excerpt = first 200 chars
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib.sh
. "$SCRIPT_DIR/lib.sh"

[ "$#" -ge 1 ] || die "no PR numbers given" \
  "fetch-pr-state.sh needs at least one PR number" \
  "usage: fetch-pr-state.sh <pr-number> [<pr-number>...]"

require_cmd git jq
require_cmd "$GH_CMD"
preflight_gh_auth
require_clean_worktree
derive_owner_repo
log "[fetch-pr-state] repo: $OWNER/$REPO"

WORK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/pr-fix-loop-state.XXXXXX")"
trap 'rm -rf "$WORK_DIR"' EXIT

THREADS_QUERY='query($owner: String!, $repo: String!, $pr: Int!, $cursor: String){
  repository(owner: $owner, name: $repo){
    pullRequest(number: $pr){
      reviewThreads(first: 100, after: $cursor){
        pageInfo { hasNextPage endCursor }
        nodes { id isResolved path line comments(first: 1){ nodes { body } } }
      }
    }
  }
}'

fetch_one_pr() {
  local pr="$1" pr_json failing in_progress threads_file page cursor has_next
  local threads comments_raw comments

  if ! pr_json="$("$GH_CMD" pr view "$pr" --json number,headRefName,baseRefName,statusCheckRollup)"; then
    die "gh pr view $pr failed" \
      "PR #$pr could not be fetched from $OWNER/$REPO" \
      "check the PR number and that it belongs to this repository"
  fi

  # Failing checks: conclusion == FAILURE; jobId/runId parsed from detailsUrl.
  failing="$(printf '%s' "$pr_json" | jq -c '
    [ .statusCheckRollup[]?
      | select(((.conclusion // .state // "") | ascii_upcase) == "FAILURE")
      | { jobName: (.name // .context // "unknown") }
        + ((((.detailsUrl // .targetUrl // "")
             | capture("/actions/runs/(?<runId>[0-9]+)/job/(?<jobId>[0-9]+)"))?
           // {runId: null, jobId: null}))
      | {jobId, jobName, runId} ]')"

  in_progress="$(printf '%s' "$pr_json" | jq -c '
    [ .statusCheckRollup[]?
      | ((.status // .state // "") | ascii_upcase) as $s
      | select($s == "IN_PROGRESS" or $s == "QUEUED" or $s == "PENDING"
               or $s == "REQUESTED" or $s == "WAITING")
    ] | length')"

  # Inline review threads — GraphQL, paginate while hasNextPage (review-handling.md).
  threads_file="$WORK_DIR/threads-$pr.jsonl"
  : > "$threads_file"
  cursor=""
  while :; do
    if [ -z "$cursor" ]; then
      page="$("$GH_CMD" api graphql -f query="$THREADS_QUERY" \
        -F owner="$OWNER" -F repo="$REPO" -F pr="$pr")" || die \
        "reviewThreads query failed for PR #$pr" \
        "the GraphQL reviewThreads fetch did not return" \
        "check network / gh auth scopes and re-run"
    else
      page="$("$GH_CMD" api graphql -f query="$THREADS_QUERY" \
        -F owner="$OWNER" -F repo="$REPO" -F pr="$pr" -f cursor="$cursor")" || die \
        "reviewThreads pagination failed for PR #$pr (cursor $cursor)" \
        "a follow-up page of the reviewThreads fetch did not return" \
        "check network / gh auth scopes and re-run"
    fi
    printf '%s' "$page" \
      | jq -c '.data.repository.pullRequest.reviewThreads.nodes // []' >> "$threads_file"
    has_next="$(printf '%s' "$page" \
      | jq -r '.data.repository.pullRequest.reviewThreads.pageInfo.hasNextPage')"
    cursor="$(printf '%s' "$page" \
      | jq -r '.data.repository.pullRequest.reviewThreads.pageInfo.endCursor // empty')"
    [ "$has_next" = "true" ] || break
    [ -n "$cursor" ] || die "reviewThreads pagination stuck for PR #$pr" \
      "hasNextPage is true but endCursor is empty" \
      "re-run; if it persists, inspect the GraphQL response manually"
  done
  threads="$(jq -cs '
    [ .[] | .[] | select(.isResolved == false)
      | {id, path, line, firstComment: (.comments.nodes[0].body // "")} ]' "$threads_file")"

  # Issue-level comments — REST, --paginate (default page size 30).
  if ! comments_raw="$("$GH_CMD" api --paginate "repos/$OWNER/$REPO/issues/$pr/comments")"; then
    die "issue comments fetch failed for PR #$pr" \
      "gh api --paginate repos/$OWNER/$REPO/issues/$pr/comments did not return" \
      "check network / gh auth scopes and re-run"
  fi
  # --paginate emits one JSON array per page, concatenated — slurp and flatten.
  comments="$(printf '%s' "$comments_raw" | jq -cs --arg prefix "$HANDLED_PREFIX" '
    [ .[] | if type == "array" then .[] else . end ]
    | [ .[] | select((.body // "") | startswith($prefix) | not)
        | {id, excerpt: ((.body // "") | .[0:200])} ]')"

  printf '%s' "$pr_json" | jq -c \
    --argjson failing "$failing" \
    --argjson inProgress "$in_progress" \
    --argjson threads "$threads" \
    --argjson comments "$comments" \
    '{ pr: .number, headRef: .headRefName, baseRef: .baseRefName,
       stackParent: null,
       failingChecks: $failing, inProgress: $inProgress,
       unresolvedThreads: $threads, unhandledIssueComments: $comments }'
}

RESULTS="$WORK_DIR/results.jsonl"
: > "$RESULTS"
for pr in "$@"; do
  require_pr_number "$pr"
  log "[fetch-pr-state] fetching PR #$pr ..."
  fetch_one_pr "$pr" >> "$RESULTS"
done

# stackParent: baseRef equals another listed PR's headRef ⇒ that PR is the parent.
jq -cs '
  . as $prs
  | ([ $prs[] | {key: .headRef, value: .pr} ] | from_entries) as $byHead
  | [ $prs[] | .stackParent = ($byHead[.baseRef] // null) ]' "$RESULTS"
