#!/usr/bin/env bash
# resolve-thread.sh — pr-fix-loop Step 5-A: resolve one inline review thread.
#
# Usage: resolve-thread.sh <threadId> [commitHash]
#   threadId   — node id from fetch-pr-state.sh unresolvedThreads[].id
#   commitHash — optional: the commit that fixed the thread (echoed in the
#                output for the pass report; the association itself lives in
#                the commit message — see review-handling.md)
#
# Runs the resolveReviewThread GraphQL mutation and verifies the thread came
# back isResolved. Idempotent: resolving an already-resolved thread succeeds.
#
# stdout: ONE line of JSON: { threadId, resolved: true, commit }
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib.sh
. "$SCRIPT_DIR/lib.sh"

[ "$#" -ge 1 ] && [ "$#" -le 2 ] || die "expected 1 or 2 arguments" \
  "resolve-thread.sh resolves exactly one review thread per call" \
  "usage: resolve-thread.sh <threadId> [commitHash]"
THREAD_ID="$1"
COMMIT="${2:-}"

require_cmd jq
require_cmd "$GH_CMD"
preflight_gh_auth

MUTATION='mutation($threadId: ID!){
  resolveReviewThread(input: {threadId: $threadId}) { thread { id isResolved } }
}'

if ! RESP="$("$GH_CMD" api graphql -f query="$MUTATION" -f threadId="$THREAD_ID")"; then
  die "resolveReviewThread mutation failed for $THREAD_ID" \
    "the GraphQL call did not return — bad thread id, missing permission, or network" \
    "check the id against fetch-pr-state.sh unresolvedThreads[].id and 'gh auth status', then re-run"
fi

IS_RESOLVED="$(printf '%s' "$RESP" | jq -r '.data.resolveReviewThread.thread.isResolved // "false"')"
[ "$IS_RESOLVED" = "true" ] || die \
  "thread $THREAD_ID did not come back resolved" \
  "the mutation returned but isResolved is not true: $(printf '%s' "$RESP" | jq -c '.data // .errors // .')" \
  "verify the thread id and that your account may resolve threads on this PR"

log "[resolve-thread] resolved $THREAD_ID"
jq -cn --arg threadId "$THREAD_ID" --arg commit "$COMMIT" \
  '{threadId: $threadId, resolved: true,
    commit: (if $commit == "" then null else $commit end)}'
