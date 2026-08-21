#!/usr/bin/env bash
# pr-state.sh — one-shot PR state snapshot as JSON (SKILL.md §1/§14/§16 helper).
#
# Usage:
#   pr-state.sh <pr-number> [--repo <owner/name>]
#
# Prints one JSON object covering:
#   git               — force-push detection (local HEAD no longer on origin/<branch>
#                       + new remote commits) and the new-commit list
#   maintainerLatency — last non-self comment time + latency band with the
#                       SKILL.md §16 recommended action built in
#   ownComments       — own cumulative comment count vs the 8/12 saturation
#                       thresholds (pr-comment-policy.md)
#   reviewThreads     — total / unresolved review-thread counts (GraphQL, paginated)
#
# Env:
#   GH_CMD    — override the gh command (mock-friendly; default: gh)
#   GIT_CMD   — override the git command (default: git)
#   NOW_EPOCH — override "now" for latency computation (testing)
#
# Dependencies: bash (3.2+), gh (authenticated), git, jq. macOS / Linux compatible.
set -euo pipefail

die() {
  printf 'ERROR: %s\n' "$1" >&2
  if [ -n "${2:-}" ]; then printf '  why: %s\n' "$2" >&2; fi
  if [ -n "${3:-}" ]; then printf '  fix: %s\n' "$3" >&2; fi
  exit 1
}

usage() { sed -n '2,20p' "$0" | sed 's/^# \{0,1\}//'; }

PR="" REPO=""
while [ $# -gt 0 ]; do
  case "$1" in
    --repo)
      [ $# -ge 2 ] || die "--repo requires a value" "" "pass --repo owner/name"
      REPO=$2; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    -*) die "unknown option: $1" "" "run with --help" ;;
    *)
      [ -z "$PR" ] || die "too many positional arguments: $1" "" "usage: pr-state.sh <pr-number>"
      PR=$1; shift ;;
  esac
done

[ -n "$PR" ] || die "missing <pr-number> argument" "" "usage: pr-state.sh <pr-number> [--repo owner/name]"
case "$PR" in
  ''|*[!0-9]*) die "invalid <pr-number>: $PR" "must be a positive integer" "pass e.g. 186" ;;
esac

GH=${GH_CMD:-gh}
GIT=${GIT_CMD:-git}
command -v jq >/dev/null 2>&1 || die "jq not found" \
  "all gh output is post-processed with jq" "install jq (brew install jq / apt-get install jq)"
if [ -z "${GH_CMD:-}" ]; then
  command -v gh >/dev/null 2>&1 || die "gh not found" \
    "PR state is read via the GitHub CLI" "install gh and run gh auth login (or set GH_CMD to a mock)"
fi

iso_to_epoch() { # ISO-8601 Z timestamp → epoch seconds
  if date -u -j >/dev/null 2>&1; then
    date -j -u -f "%Y-%m-%dT%H:%M:%SZ" "$1" +%s   # BSD / macOS
  else
    date -u -d "$1" +%s                            # GNU / Linux
  fi
}

# --- repo & PR head --------------------------------------------------------
if [ -z "$REPO" ]; then
  REPO=$($GH repo view --json nameWithOwner 2>/dev/null | jq -r '.nameWithOwner // empty') || true
  [ -n "$REPO" ] || die "could not resolve the repository" \
    "no --repo given and 'gh repo view' failed (not in a repo clone, or gh unauthenticated)" \
    "pass --repo owner/name, or cd into the project clone and check 'gh auth status'"
fi
OWNER=${REPO%%/*}
NAME=${REPO##*/}

if ! pr_json=$($GH pr view "$PR" --repo "$REPO" --json number,headRefName,headRefOid,url 2>/dev/null); then
  die "gh pr view $PR failed (repo: $REPO)" \
    "the PR may not exist, or gh is unauthenticated" \
    "check the PR number and 'gh auth status'"
fi
BRANCH=$(printf '%s' "$pr_json" | jq -r '.headRefName')
HEAD_OID=$(printf '%s' "$pr_json" | jq -r '.headRefOid')
PR_URL=$(printf '%s' "$pr_json" | jq -r '.url')

SELF=$($GH api user 2>/dev/null | jq -r '.login // empty') || true
[ -n "$SELF" ] || die "could not resolve own login via 'gh api user'" \
  "own-comment counting needs to know who \"self\" is" "check 'gh auth status'"

# --- comments (REST, paginated) -------------------------------------------
raw_comments=$($GH api --paginate "repos/$REPO/issues/$PR/comments" 2>/dev/null) || raw_comments='[]'
# --paginate emits one JSON array per page, concatenated → slurp + flatten.
comments=$(printf '%s' "$raw_comments" | jq -s '[.[][] | {author: .user.login, createdAt: .created_at}]')

own_count=$(printf '%s' "$comments" | jq --arg self "$SELF" '[.[] | select(.author == $self)] | length')
LAST_OTHER_AT=$(printf '%s' "$comments" | jq -r --arg self "$SELF" \
  '[.[] | select(.author != $self)] | if length == 0 then "" else .[length-1].createdAt end')
LAST_OTHER_BY=$(printf '%s' "$comments" | jq -r --arg self "$SELF" \
  '[.[] | select(.author != $self)] | if length == 0 then "" else .[length-1].author end')

# --- latency band (SKILL.md §16 table built in) ----------------------------
NOW=${NOW_EPOCH:-$(date +%s)}
BAND="none" BAND_RANGE="" ACTION="no non-self comment yet — nothing to react to" AGE=null
if [ -n "$LAST_OTHER_AT" ]; then
  then_epoch=$(iso_to_epoch "$LAST_OTHER_AT")
  AGE=$((NOW - then_epoch))
  if   [ "$AGE" -lt 3600 ];  then BAND="active"; BAND_RANGE="< 1h"
    ACTION="Active fix phase — P1 cluster follow-up has high ROI (but noise risk)"
  elif [ "$AGE" -lt 10800 ]; then BAND="normal"; BAND_RANGE="1-3h"
    ACTION="Normal review — hold 1-comment-per-iteration cadence"
  elif [ "$AGE" -lt 32400 ]; then BAND="busy"; BAND_RANGE="3-9h"
    ACTION="Pending / busy elsewhere — continue exploring, fill time with ticket bookkeeping"
  else BAND="quiet"; BAND_RANGE="> 9h"
    ACTION="Low priority / quiet — reduce posting cadence; even P1 should wait for cluster"
  fi
fi

# --- own-comment saturation state (pr-comment-policy.md: >8 warn, >12 stop) -
if   [ "$own_count" -gt 12 ]; then COMMENT_STATE="stop"
elif [ "$own_count" -gt 8 ];  then COMMENT_STATE="saturation-warning"
else COMMENT_STATE="ok"; fi

# --- git force-push detection ----------------------------------------------
GIT_AVAILABLE=false FETCH_OK=false KNOWN_MISSING=false FORCE_PUSH=false
NEW_COMMITS='[]' NEW_COUNT=0
if $GIT rev-parse --git-dir >/dev/null 2>&1; then
  GIT_AVAILABLE=true
  if $GIT fetch origin "$BRANCH" >/dev/null 2>&1; then
    FETCH_OK=true
  fi
  if $GIT rev-parse --verify --quiet "origin/$BRANCH" >/dev/null 2>&1; then
    NEW_COUNT=$($GIT rev-list --count "HEAD..origin/$BRANCH")
    local_only=$($GIT rev-list --count "origin/$BRANCH..HEAD")
    if [ "$local_only" -gt 0 ]; then KNOWN_MISSING=true; fi
    # Known local commits vanished from the remote branch AND the remote moved
    # → the branch history was rewritten (force-push / rebase-squash).
    if [ "$local_only" -gt 0 ] && [ "$NEW_COUNT" -gt 0 ]; then FORCE_PUSH=true; fi
    NEW_COMMITS=$($GIT log --format='%H%x09%s' "HEAD..origin/$BRANCH" \
      | jq -R -s '[split("\n")[] | select(length > 0) | split("\t") | {sha: .[0], subject: (.[1] // "")}]')
  fi
fi

# --- unresolved review threads (GraphQL, paginated) ------------------------
QUERY='query($owner:String!,$name:String!,$number:Int!,$cursor:String){
  repository(owner:$owner,name:$name){
    pullRequest(number:$number){
      reviewThreads(first:100,after:$cursor){
        pageInfo{hasNextPage endCursor}
        nodes{isResolved}
      }
    }
  }
}'
threads_total=0 threads_unresolved=0
cursor=""
while :; do
  if [ -n "$cursor" ]; then
    page=$($GH api graphql -f query="$QUERY" -F owner="$OWNER" -F name="$NAME" -F number="$PR" -f cursor="$cursor" 2>/dev/null) || break
  else
    page=$($GH api graphql -f query="$QUERY" -F owner="$OWNER" -F name="$NAME" -F number="$PR" 2>/dev/null) || break
  fi
  rt=$(printf '%s' "$page" | jq '.data.repository.pullRequest.reviewThreads // empty')
  if [ -z "$rt" ]; then break; fi
  threads_total=$((threads_total + $(printf '%s' "$rt" | jq '.nodes | length')))
  threads_unresolved=$((threads_unresolved + $(printf '%s' "$rt" | jq '[.nodes[] | select(.isResolved | not)] | length')))
  if [ "$(printf '%s' "$rt" | jq -r '.pageInfo.hasNextPage')" != "true" ]; then break; fi
  cursor=$(printf '%s' "$rt" | jq -r '.pageInfo.endCursor')
done

# --- assemble --------------------------------------------------------------
jq -n \
  --argjson pr "$PR" \
  --arg repo "$REPO" \
  --arg branch "$BRANCH" \
  --arg headOid "$HEAD_OID" \
  --arg url "$PR_URL" \
  --arg self "$SELF" \
  --arg generatedAt "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  --argjson gitAvailable "$GIT_AVAILABLE" \
  --argjson fetchOk "$FETCH_OK" \
  --argjson newCommitCount "$NEW_COUNT" \
  --argjson newCommits "$NEW_COMMITS" \
  --argjson knownHashMissing "$KNOWN_MISSING" \
  --argjson forcePushSuspected "$FORCE_PUSH" \
  --arg lastAt "$LAST_OTHER_AT" \
  --arg lastBy "$LAST_OTHER_BY" \
  --argjson ageSeconds "$AGE" \
  --arg band "$BAND" \
  --arg bandRange "$BAND_RANGE" \
  --arg action "$ACTION" \
  --argjson ownCount "$own_count" \
  --arg commentState "$COMMENT_STATE" \
  --argjson threadsTotal "$threads_total" \
  --argjson threadsUnresolved "$threads_unresolved" \
  '{pr: $pr, repo: $repo, branch: $branch, headOid: $headOid, url: $url,
    self: $self, generatedAt: $generatedAt,
    git: {available: $gitAvailable, fetchOk: $fetchOk,
          newCommitCount: $newCommitCount, newCommits: $newCommits,
          knownHashMissing: $knownHashMissing, forcePushSuspected: $forcePushSuspected},
    maintainerLatency: {lastNonSelfCommentAt: (if $lastAt == "" then null else $lastAt end),
                        lastNonSelfCommentAuthor: (if $lastBy == "" then null else $lastBy end),
                        ageSeconds: $ageSeconds, band: $band,
                        bandRange: (if $bandRange == "" then null else $bandRange end),
                        recommendedAction: $action},
    ownComments: {count: $ownCount, warnAbove: 8, stopAbove: 12, state: $commentState},
    reviewThreads: {total: $threadsTotal, unresolved: $threadsUnresolved}}'
