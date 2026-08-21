#!/usr/bin/env bash
# summary.sh — aggregate issues/*.md into SUMMARY.md (SKILL.md §7 format) and
# lint the issue files against references/issue-format.md.
#
# Usage:
#   summary.sh [--date YYYYMMDD] [--dir <run-dir>]
#
# Writes <run-dir>/SUMMARY.md and prints one JSON object on stdout:
#   {"ok",bool, "runDir","summaryPath","issueCount",
#    "perCategory":{...},"perSeverity":{...},"lint":[{code,file,message}],"lintCount"}
# "ok" is false when lint findings exist; the exit code is still 0 (lint
# findings are data for the caller to fix, not a script failure).
#
# Dependencies: bash (3.2+), git, jq. macOS / Linux compatible.
set -euo pipefail

die() {
  printf 'ERROR: %s\n' "$1" >&2
  if [ -n "${2:-}" ]; then printf '  why: %s\n' "$2" >&2; fi
  if [ -n "${3:-}" ]; then printf '  fix: %s\n' "$3" >&2; fi
  exit 1
}

usage() { sed -n '2,14p' "$0" | sed 's/^# \{0,1\}//'; }

DATE="" RUN_DIR="${NIGHTLY_RUN_DIR:-}"
while [ $# -gt 0 ]; do
  case "$1" in
    --date)
      [ $# -ge 2 ] || die "--date requires a value" "" "pass --date YYYYMMDD"
      DATE=$2; shift 2 ;;
    --dir)
      [ $# -ge 2 ] || die "--dir requires a value" "" "pass --dir <run-dir>"
      RUN_DIR=$2; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) die "unknown argument: $1" "" "run with --help" ;;
  esac
done

command -v jq >/dev/null 2>&1 || die "jq not found" \
  "summary.sh emits its report as JSON via jq" "install jq (brew install jq / apt-get install jq)"

if [ -z "$RUN_DIR" ]; then
  if [ -z "$DATE" ]; then DATE=$(date +%Y%m%d); fi
  RUN_DIR=".local/tmp/exploratory-nightly-$DATE"
fi
ISSUES_DIR="$RUN_DIR/issues"
[ -d "$ISSUES_DIR" ] || die "run directory not initialized: $ISSUES_DIR not found" \
  "nothing to summarize without the init-run.sh scaffold" \
  "run scripts/init-run.sh first (pass the same --date/--dir)"

GIT=${GIT_CMD:-git}
if $GIT rev-parse --git-dir >/dev/null 2>&1; then
  TARGET_COMMIT=$($GIT rev-parse HEAD)
else
  TARGET_COMMIT="(not a git repository)"
fi

LINT=""   # NDJSON lines, one lint finding per line
add_lint() { # code file message
  LINT="$LINT
$(jq -cn --arg code "$1" --arg file "$2" --arg message "$3" '{code:$code,file:$file,message:$message}')"
}

c1=0 c2=0 c3=0 c4=0 c5=0
p0=0 p1=0 p2=0 p3=0
count=0
nums=""      # space-separated claimed numbers (for gap/duplicate check)

for f in "$ISSUES_DIR"/*.md; do
  if [ ! -e "$f" ]; then continue; fi
  b=${f##*/}
  case "$b" in
    [0-9][0-9]-*.md) ;;
    *) add_lint bad-filename "$b" "filename must match NN-<kebab-slug>.md"; continue ;;
  esac
  count=$((count + 1))
  n=$((10#${b%%-*}))
  case " $nums " in
    *" $n "*) add_lint duplicate-number "$b" "number $n is used by more than one file" ;;
    *) nums="$nums $n" ;;
  esac

  first=$(head -n 1 "$f")
  case "$first" in
    "# "?*) ;;
    *) add_lint missing-h1 "$b" "line 1 must be '# <Short title>' (got: ${first:-<empty>})" ;;
  esac

  catline=$(awk '/^\*\*Category\*\*:/ { sub(/^\*\*Category\*\*:[ \t]*/, ""); print; exit }' "$f")
  case "$catline" in
    cat1*) c1=$((c1 + 1)) ;;
    cat2*) c2=$((c2 + 1)) ;;
    cat3*) c3=$((c3 + 1)) ;;
    cat4*) c4=$((c4 + 1)) ;;
    cat5*) c5=$((c5 + 1)) ;;
    '') add_lint missing-category "$b" "no '**Category**: cat<N> (...)' line found" ;;
    *) add_lint unknown-category "$b" "unrecognized category: $catline" ;;
  esac

  sev=$(awk '/^\*\*Severity\*\*:/ { sub(/^\*\*Severity\*\*:[ \t]*/, ""); sub(/[ \t]+$/, ""); print; exit }' "$f")
  case "$sev" in
    P0) p0=$((p0 + 1)) ;;
    P1) p1=$((p1 + 1)) ;;
    P2) p2=$((p2 + 1)) ;;
    P3) p3=$((p3 + 1)) ;;
    '') add_lint missing-severity "$b" "no '**Severity**: P0..P3' line found" ;;
    *) add_lint unknown-severity "$b" "unrecognized severity: $sev (must be P0/P1/P2/P3)" ;;
  esac

  if ! grep -q '^\*\*Detected at commit\*\*:' "$f"; then
    add_lint missing-commit "$b" "no '**Detected at commit**: <12-char sha>' line found"
  fi
done

# Gap check: claimed numbers must be exactly 1..count (order-independent).
i=1
while [ "$i" -le "$count" ]; do
  case " $nums " in
    *" $i "*) ;;
    *) add_lint numbering-gap "issues/" "expected number $(printf '%02d' "$i") is missing (files must be gapless from 01)" ;;
  esac
  i=$((i + 1))
done

TS=$(date -u +%Y-%m-%dT%H:%M:%SZ)
SUMMARY="$RUN_DIR/SUMMARY.md"
cat > "$SUMMARY" <<EOF
# Nightly Exploration Summary

- Run timestamp: $TS
- Target commit: $TARGET_COMMIT
- Issue count: $count

## Per-category breakdown
- cat1: $c1
- cat2: $c2
- cat3: $c3
- cat4: $c4
- cat5: $c5

## Per-severity breakdown
- P0: $p0
- P1: $p1
- P2: $p2
- P3: $p3
EOF

lint_json=$(printf '%s\n' "$LINT" | jq -s .)
jq -n \
  --arg runDir "$RUN_DIR" \
  --arg summaryPath "$SUMMARY" \
  --argjson issueCount "$count" \
  --argjson perCategory "{\"cat1\":$c1,\"cat2\":$c2,\"cat3\":$c3,\"cat4\":$c4,\"cat5\":$c5}" \
  --argjson perSeverity "{\"P0\":$p0,\"P1\":$p1,\"P2\":$p2,\"P3\":$p3}" \
  --argjson lint "$lint_json" \
  '{ok: ($lint | length == 0), runDir: $runDir, summaryPath: $summaryPath,
    issueCount: $issueCount, perCategory: $perCategory, perSeverity: $perSeverity,
    lint: $lint, lintCount: ($lint | length)}'
