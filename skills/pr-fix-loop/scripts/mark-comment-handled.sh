#!/usr/bin/env bash
# mark-comment-handled.sh — pr-fix-loop Step 5-B: mark one issue-level comment
# as handled (GitHub has no native resolve for conversation comments).
#
# Usage: mark-comment-handled.sh <commentId> <commitHash> [<commitHash>...]
#   commentId — from fetch-pr-state.sh unhandledIssueComments[].id
#   commitHash — the fixing commit(s); several hashes for a multi-point comment
#
# Wraps the original body in the handled-marker convention
# (review-handling.md):
#   <details><summary>Resolved</summary>\n\n<original>\n\n</details>\n\n--> <hash>
# The summary text is a project choice — override with
# PR_FIX_LOOP_RESOLVED_SUMMARY (must match the filter in fetch-pr-state.sh).
# Idempotent: an already-wrapped comment is left untouched.
#
# stdout: ONE line of JSON: { commentId, handled: true, alreadyHandled, commits }
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib.sh
. "$SCRIPT_DIR/lib.sh"

[ "$#" -ge 2 ] || die "expected a comment id and at least one commit hash" \
  "the handled-marker records which commit(s) fixed the comment" \
  "usage: mark-comment-handled.sh <commentId> <commitHash> [<commitHash>...]"
COMMENT_ID="$1"
shift

require_cmd jq
require_cmd "$GH_CMD"
preflight_gh_auth
derive_owner_repo

COMMITS_JSON="$(printf '%s\n' "$@" | jq -R . | jq -cs .)"
COMMITS_JOINED="$(printf '%s' "$COMMITS_JSON" | jq -r 'join(", ")')"

if ! ORIG="$("$GH_CMD" api "repos/$OWNER/$REPO/issues/comments/$COMMENT_ID" | jq -r '.body // ""')"; then
  die "could not fetch comment $COMMENT_ID" \
    "gh api repos/$OWNER/$REPO/issues/comments/$COMMENT_ID failed" \
    "check the id against fetch-pr-state.sh unhandledIssueComments[].id and re-run"
fi

case "$ORIG" in
  "$HANDLED_PREFIX"*)
    log "[mark-comment-handled] comment $COMMENT_ID already carries the handled marker — leaving as is"
    jq -cn --arg id "$COMMENT_ID" \
      '{commentId: $id, handled: true, alreadyHandled: true, commits: []}'
    exit 0
    ;;
esac

NEW_BODY="$(jq -nr --arg s "$RESOLVED_SUMMARY" --arg orig "$ORIG" --arg c "$COMMITS_JOINED" \
  '"<details><summary>\($s)</summary>\n\n\($orig)\n\n</details>\n\n--> \($c)"')"

if ! RESP="$(jq -cn --arg body "$NEW_BODY" '{body: $body}' \
  | "$GH_CMD" api -X PATCH "repos/$OWNER/$REPO/issues/comments/$COMMENT_ID" --input -)"; then
  die "PATCH of comment $COMMENT_ID failed" \
    "the handled-marker wrap could not be written back" \
    "check write permission on $OWNER/$REPO and re-run (the comment was not modified)"
fi

RESP_BODY="$(printf '%s' "$RESP" | jq -r '.body // ""')"
case "$RESP_BODY" in
  "$HANDLED_PREFIX"*) : ;;
  *) die "comment $COMMENT_ID does not carry the marker after PATCH" \
       "the API response body does not start with '$HANDLED_PREFIX'" \
       "inspect the comment on GitHub; re-run once the cause is clear" ;;
esac

log "[mark-comment-handled] wrapped comment $COMMENT_ID (--> $COMMITS_JOINED)"
jq -cn --arg id "$COMMENT_ID" --argjson commits "$COMMITS_JSON" \
  '{commentId: $id, handled: true, alreadyHandled: false, commits: $commits}'
