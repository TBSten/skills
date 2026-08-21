#!/usr/bin/env bash
# streak.sh — pr-fix-loop Step 7: persist the no-change streak across passes.
#
# Usage: streak.sh <no-change|changed>
#
# SKILL.md Step 7: a "no-change" pass increments the streak, any "changed"
# pass resets it to 0. The streak lives in a file so it survives across
# passes under a /loop driver. When the streak reaches the limit the file is
# deleted (clean start next invocation) and terminate is reported as true.
#
# Env: PR_FIX_LOOP_STREAK_FILE  (default .local/tmp/pr-fix-loop-streak.txt)
#      PR_FIX_LOOP_STREAK_LIMIT (default 5)
#
# stdout: ONE line of JSON: { streak, limit, terminate }
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib.sh
. "$SCRIPT_DIR/lib.sh"

[ "$#" -eq 1 ] || die "expected exactly 1 argument" \
  "streak.sh records the outcome of exactly one pass" \
  "usage: streak.sh <no-change|changed>"
OUTCOME="$1"
case "$OUTCOME" in
  no-change|changed) : ;;
  *) die "invalid outcome: '$OUTCOME'" \
       "the pass outcome must be either 'no-change' or 'changed' (SKILL.md Step 7)" \
       "pass 'no-change' when nothing changed this pass, otherwise 'changed'" ;;
esac

require_cmd jq

STREAK_FILE="${PR_FIX_LOOP_STREAK_FILE:-.local/tmp/pr-fix-loop-streak.txt}"
LIMIT="${PR_FIX_LOOP_STREAK_LIMIT:-5}"
case "$LIMIT" in
  ''|*[!0-9]*) die "invalid PR_FIX_LOOP_STREAK_LIMIT: '$LIMIT'" \
    "the streak limit must be a positive integer" \
    "unset PR_FIX_LOOP_STREAK_LIMIT or set it to a number (default 5)" ;;
esac

mkdir -p "$(dirname "$STREAK_FILE")"
PREV="$(cat "$STREAK_FILE" 2>/dev/null || echo 0)"
case "$PREV" in ''|*[!0-9]*) PREV=0 ;; esac

if [ "$OUTCOME" = "no-change" ]; then
  NEXT=$((PREV + 1))
else
  NEXT=0
fi

TERMINATE=false
if [ "$NEXT" -ge "$LIMIT" ]; then
  TERMINATE=true
  rm -f "$STREAK_FILE"   # clean start next invocation
  log "[streak] $NEXT consecutive no-change passes (limit $LIMIT) — terminating"
else
  printf '%s\n' "$NEXT" > "$STREAK_FILE"
  log "[streak] outcome=$OUTCOME streak=$NEXT/$LIMIT"
fi

jq -cn --argjson streak "$NEXT" --argjson limit "$LIMIT" --argjson terminate "$TERMINATE" \
  '{streak: $streak, limit: $limit, terminate: $terminate}'
