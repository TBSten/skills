#!/usr/bin/env bash
# classify-failure.sh — pr-fix-loop Step 4: classify one failing CI job.
#
# Usage: classify-failure.sh <runId> <jobId>
#   (both come from fetch-pr-state.sh failingChecks[])
#
# Fetches the job log (job logs are available even while the workflow is
# still in progress — operations.md) and applies the ordered heuristics of
# references/failure-classification.md:
#   transient → lint → binary → test → build → unknown  (first match wins;
#   transient first so a rerun-only failure is never routed to a code fixer).
# The trailing "> Task :xxx: FAILED" task name is the truth; the job name is
# display only. kind "unknown" is returned for the AI to judge — the script
# never guesses.
#
# stdout: ONE line of JSON:
#   { kind, taskName, evidence: [...], delegate, runId, jobId, jobName, logTail }
#   delegate: "rerun" (transient) | "fix-ci-lint" | "fix-ci-binary"
#           | "fix-ci-test" | "fix-ci-build" | null (unknown)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib.sh
. "$SCRIPT_DIR/lib.sh"

[ "$#" -eq 2 ] || die "expected exactly 2 arguments" \
  "classify-failure.sh needs the run id and the job id of one failing check" \
  "usage: classify-failure.sh <runId> <jobId> (see fetch-pr-state.sh failingChecks[])"
RUN_ID="$1"
JOB_ID="$2"

require_cmd jq
require_cmd "$GH_CMD"
preflight_gh_auth
derive_owner_repo

WORK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/pr-fix-loop-classify.XXXXXX")"
trap 'rm -rf "$WORK_DIR"' EXIT
LOG_FILE="$WORK_DIR/job.log"

JOB_NAME="$("$GH_CMD" api "/repos/$OWNER/$REPO/actions/jobs/$JOB_ID" | jq -r '.name // ""')" || die \
  "could not fetch job metadata for job $JOB_ID" \
  "gh api /repos/$OWNER/$REPO/actions/jobs/$JOB_ID failed" \
  "check the jobId (from fetch-pr-state.sh) and gh auth scopes"

if ! "$GH_CMD" api "/repos/$OWNER/$REPO/actions/jobs/$JOB_ID/logs" > "$LOG_FILE"; then
  die "could not fetch the log of job $JOB_ID" \
    "the job log endpoint failed — the job may not have finished writing its log yet" \
    "defer this check to the next pass (completed jobs' logs are fetchable even while the workflow is in progress)"
fi
log "[classify-failure] job $JOB_ID ($JOB_NAME): $(wc -l < "$LOG_FILE" | tr -d ' ') log lines"

lower() { printf '%s' "$1" | tr '[:upper:]' '[:lower:]'; }
grep_log() { grep -aE -e "$1" "$LOG_FILE" | head -n "${2:-2}" || true; }

# --- task-name truth: the trailing "> Task :xxx: FAILED" line -----------------
TASK_LINE="$(grep -aE -e '> Task :[^ ]+ FAILED' "$LOG_FILE" | tail -n 1 || true)"
TASK_NAME=""
if [ -n "$TASK_LINE" ]; then
  TASK_NAME="$(printf '%s\n' "$TASK_LINE" | sed -E 's/.*> Task (:[^ ]+) FAILED.*/\1/')"
fi
LEAF_LC="$(lower "${TASK_NAME##*:}")"
JOB_LC="$(lower "$JOB_NAME")"

KIND=""
EVIDENCE=""

# --- 1. transient infra (checked first: rerun-only, never a code fixer) -------
TRANSIENT_PATTERNS="Could not GET 'https?://
Could not HEAD 'https?://
Received status code 5[0-9][0-9]
Failed to download
connection reset
The runner has received a shutdown signal
npm ERR! network
EAI_AGAIN
ECONNRESET"
while IFS= read -r re; do
  [ -n "$re" ] || continue
  m="$(grep_log "$re")"
  [ -n "$m" ] && EVIDENCE="${EVIDENCE}${m}
"
done <<EOF_PATTERNS
$TRANSIENT_PATTERNS
EOF_PATTERNS
if [ -z "$EVIDENCE" ]; then
  # Cache restore failure is transient — unless the build proceeded and failed
  # later for another reason (a failed task exists), then classify by that.
  m="$(grep_log 'Failed to restore')"
  if [ -n "$m" ] && [ -z "$TASK_LINE" ]; then
    EVIDENCE="$m
"
  fi
fi
[ -n "$EVIDENCE" ] && KIND="transient"

# --- 2..5 by failed task name (the truth) -------------------------------------
if [ -z "$KIND" ] && [ -n "$TASK_NAME" ]; then
  case "$LEAF_LC" in
    *ktlint*|*lint*|*format*|*spotless*)   KIND="lint" ;;   # ktlintCheck, lintDebug, spotlessCheck
    apicheck)                              KIND="binary" ;; # apiCheck (BCV)
    *test|*tests)                          KIND="test" ;;   # jvmTest, jsBrowserTest, allTests, testDebugUnitTest
    compile*|assemble*|build*|publish*)    KIND="build" ;;  # compileKotlin*, assembleDebug, publish*
  esac
  [ -n "$KIND" ] && EVIDENCE="$TASK_LINE
"
fi

# --- 2..5 by job-name / log signatures (only when the task name decides nothing)
append_sig() { [ -n "$2" ] && SIG_EV="${SIG_EV}${2}
"; return 0; }
if [ -z "$KIND" ]; then
  # lint
  SIG_EV=""
  case "$JOB_LC" in *lint*|*format*) append_sig _ "job name: $JOB_NAME";; esac
  append_sig _ "$(grep_log 'ktlintCheck FAILED|Lint task FAILED')"
  append_sig _ "$(grep_log '\(standard:')"
  append_sig _ "$(grep_log 'potentially fixable with the .--fix. option')"
  if [ -n "$SIG_EV" ]; then KIND="lint"; EVIDENCE="$SIG_EV"; fi
fi
if [ -z "$KIND" ]; then
  # binary compatibility
  SIG_EV=""
  case "$JOB_LC" in *"binary compatibility"*|*apicheck*|*bcv*) append_sig _ "job name: $JOB_NAME";; esac
  append_sig _ "$(grep_log 'API check failed for project')"
  if [ -n "$SIG_EV" ]; then KIND="binary"; EVIDENCE="$SIG_EV"; fi
fi
if [ -z "$KIND" ]; then
  # test
  SIG_EV=""
  case "$JOB_LC" in *test*) append_sig _ "job name: $JOB_NAME";; esac
  append_sig _ "$(grep_log 'org\.opentest4j\.AssertionFailedError|java\.lang\.AssertionError')"
  if [ -n "$SIG_EV" ]; then KIND="test"; EVIDENCE="$SIG_EV"; fi
fi
if [ -z "$KIND" ]; then
  # build
  SIG_EV=""
  case "$JOB_LC" in *build*|*compile*|*assemble*|*"publish to maven local"*) append_sig _ "job name: $JOB_NAME";; esac
  append_sig _ "$(grep_log 'Could not resolve all dependencies|Unresolved reference|Type mismatch')"
  if [ -n "$SIG_EV" ]; then KIND="build"; EVIDENCE="$SIG_EV"; fi
fi

# --- 6. unknown: leave the judgment to the AI ---------------------------------
DELEGATE=""
case "${KIND:-unknown}" in
  transient) DELEGATE="rerun" ;;
  lint)      DELEGATE="fix-ci-lint" ;;
  binary)    DELEGATE="fix-ci-binary" ;;
  test)      DELEGATE="fix-ci-test" ;;
  build)     DELEGATE="fix-ci-build" ;;
  *)         KIND="unknown"; DELEGATE="" ;;
esac

EVIDENCE_JSON="$(printf '%s' "$EVIDENCE" | sed '/^[[:space:]]*$/d' | head -n 5 | jq -R . | jq -cs .)"
LOG_TAIL="$(tail -n 50 "$LOG_FILE" || true)"

jq -cn \
  --arg kind "$KIND" \
  --arg taskName "$TASK_NAME" \
  --argjson evidence "$EVIDENCE_JSON" \
  --arg delegate "$DELEGATE" \
  --arg runId "$RUN_ID" \
  --arg jobId "$JOB_ID" \
  --arg jobName "$JOB_NAME" \
  --arg logTail "$LOG_TAIL" \
  '{ kind: $kind,
     taskName: (if $taskName == "" then null else $taskName end),
     evidence: $evidence,
     delegate: (if $delegate == "" then null else $delegate end),
     runId: $runId, jobId: $jobId, jobName: $jobName,
     logTail: $logTail }'
