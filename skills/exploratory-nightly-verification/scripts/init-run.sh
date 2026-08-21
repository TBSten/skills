#!/usr/bin/env bash
# init-run.sh — scaffold the nightly exploration run directory (idempotent).
#
# Usage:
#   init-run.sh [--date YYYYMMDD] [--dir <run-dir>]
#
# Creates .local/tmp/exploratory-nightly-<date>/{issues,tmp}, records the start
# epoch once (re-runs keep the original start time), and prints a single JSON
# object: {"date","commit","dir","startedAt"} on stdout.
#
# Dependencies: bash (3.2+), git. macOS / Linux compatible.
set -euo pipefail

die() {
  printf 'ERROR: %s\n' "$1" >&2
  if [ -n "${2:-}" ]; then printf '  why: %s\n' "$2" >&2; fi
  if [ -n "${3:-}" ]; then printf '  fix: %s\n' "$3" >&2; fi
  exit 1
}

usage() {
  sed -n '2,10p' "$0" | sed 's/^# \{0,1\}//'
}

DATE=""
RUN_DIR="${NIGHTLY_RUN_DIR:-}"
while [ $# -gt 0 ]; do
  case "$1" in
    --date)
      [ $# -ge 2 ] || die "--date requires a value" "" "pass --date YYYYMMDD"
      DATE=$2; shift 2 ;;
    --dir)
      [ $# -ge 2 ] || die "--dir requires a value" "" "pass --dir <run-dir>"
      RUN_DIR=$2; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) die "unknown argument: $1" "init-run.sh takes only --date/--dir" "run with --help" ;;
  esac
done

if [ -z "$DATE" ]; then
  DATE=$(date +%Y%m%d)
fi
case "$DATE" in
  [0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]) ;;
  *) die "invalid --date: $DATE" "date must be YYYYMMDD (e.g. 20260513)" "pass --date YYYYMMDD" ;;
esac

GIT=${GIT_CMD:-git}
if ! $GIT rev-parse --git-dir >/dev/null 2>&1; then
  die "not inside a git repository (cwd: $(pwd))" \
    "the run records the commit under verification via git rev-parse" \
    "cd to the project root before running init-run.sh"
fi
COMMIT=$($GIT rev-parse --short=12 HEAD)

if [ -z "$RUN_DIR" ]; then
  RUN_DIR=".local/tmp/exploratory-nightly-$DATE"
fi

mkdir -p "$RUN_DIR/issues" "$RUN_DIR/tmp"

MARKER="$RUN_DIR/.started-at"
if [ ! -f "$MARKER" ]; then
  date +%s > "$MARKER"
fi
STARTED_AT=$(cat "$MARKER")
case "$STARTED_AT" in
  ''|*[!0-9]*) die "corrupt start marker: $MARKER" "expected a bare epoch integer" \
    "delete $MARKER and re-run init-run.sh" ;;
esac

# Values are shell-generated (digits / hex / path without quotes expected);
# escape backslash + double quote in the dir just in case.
esc_dir=$(printf '%s' "$RUN_DIR" | sed 's/\\/\\\\/g; s/"/\\"/g')
printf '{"date":"%s","commit":"%s","dir":"%s","startedAt":%s}\n' \
  "$DATE" "$COMMIT" "$esc_dir" "$STARTED_AT"
