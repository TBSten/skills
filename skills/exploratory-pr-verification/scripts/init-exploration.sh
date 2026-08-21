#!/usr/bin/env bash
# init-exploration.sh — scaffold the exploration directory for a PR (idempotent).
#
# Usage:
#   init-exploration.sh <pr-id> [--dir <base-dir>]
#
# <pr-id> is typically the PR number, or a range like "186-187" for stacked PRs.
# Creates:
#   .local/tmp/exploratory-pr-<id>/
#   ├── FINAL-SUMMARY.md            (skeleton incl. cluster-family table; kept if it exists)
#   ├── problems/{resolved,methodology,non-pr}/
#   ├── log/
#   └── gradle-isolation/
# and prints one JSON object: {"id","dir","createdAt"}.
#
# Dependencies: bash (3.2+). macOS / Linux compatible.
set -euo pipefail

die() {
  printf 'ERROR: %s\n' "$1" >&2
  if [ -n "${2:-}" ]; then printf '  why: %s\n' "$2" >&2; fi
  if [ -n "${3:-}" ]; then printf '  fix: %s\n' "$3" >&2; fi
  exit 1
}

usage() { sed -n '2,16p' "$0" | sed 's/^# \{0,1\}//'; }

ID="" DIR="${EXPLORATORY_PR_DIR:-}"
while [ $# -gt 0 ]; do
  case "$1" in
    --dir)
      [ $# -ge 2 ] || die "--dir requires a value" "" "pass --dir <base-dir>"
      DIR=$2; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    -*) die "unknown option: $1" "" "run with --help" ;;
    *)
      [ -z "$ID" ] || die "too many positional arguments: $1" "" "usage: init-exploration.sh <pr-id>"
      ID=$1; shift ;;
  esac
done

[ -n "$ID" ] || die "missing <pr-id> argument" \
  "the exploration directory is keyed by PR id (number or range like 186-187)" \
  "usage: init-exploration.sh <pr-id>"
case "$ID" in
  *[!A-Za-z0-9._-]*|.*) die "invalid <pr-id>: $ID" \
    "id may contain only letters / digits / . _ - (and must not start with a dot)" \
    "use the PR number (e.g. 186) or a range (e.g. 186-187)" ;;
esac

[ -n "$DIR" ] || DIR=".local/tmp/exploratory-pr-$ID"

mkdir -p \
  "$DIR/problems/resolved" \
  "$DIR/problems/methodology" \
  "$DIR/problems/non-pr" \
  "$DIR/log" \
  "$DIR/gradle-isolation"

NOW=$(date -u +%Y-%m-%dT%H:%M:%SZ)
SUMMARY="$DIR/FINAL-SUMMARY.md"
if [ ! -f "$SUMMARY" ]; then
  cat > "$SUMMARY" <<EOF
# FINAL SUMMARY — exploratory-pr-$ID

- Target PR: $ID
- Started: $NOW
- Deadline: (TBD — set when the user names one)
- Status: exploring

## Iteration log

| iter | kicked at | cats | new tickets | PR comments |
|------|-----------|------|-------------|-------------|

## PR comments posted

(record every posted comment URL here — SKILL.md §15)

## Ticket clusters (close-time snapshot)

Family definitions: references/cluster-families.md

| Family | Active count | Tickets | Follow-up PR scope |
|--------|--------------|---------|--------------------|
| C-1 docs gap | 0 | | |
| C-2 SoT violation | 0 | | |
| C-3 silent failure | 0 | | |
| C-4 public-API / ABI break | 0 | | |
| C-5 name discovery / cross-module | 0 | | |
| C-6 test brittleness | 0 | | |
| C-7 publish / supply chain | 0 | | |
| C-8 sample-app / dogfood gap | 0 | | |
| C-9 process / methodology | 0 | | |
| C-10 upstream-library limitation | 0 | | |
| C-11 IDE-vs-CLI asymmetry | 0 | | |
EOF
fi

esc_dir=$(printf '%s' "$DIR" | sed 's/\\/\\\\/g; s/"/\\"/g')
printf '{"id":"%s","dir":"%s","createdAt":"%s"}\n' "$ID" "$esc_dir" "$NOW"
