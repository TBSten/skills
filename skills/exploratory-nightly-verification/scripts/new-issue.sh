#!/usr/bin/env bash
# new-issue.sh — atomically allocate the next issue number and generate the
# issue skeleton in the fixed format (references/issue-format.md).
#
# Usage:
#   new-issue.sh <cat1..cat5> <P0..P3> <kebab-slug> [title] [--date YYYYMMDD] [--dir <run-dir>]
#
# Prints the created file path on stdout. The caller (AI) fills in only the
# body sections (## Reproduction / ## Detail / ## Fix proposal); the H1,
# metadata lines, numbering and filename are owned by this script.
#
# Numbering is parallel-safe: the next NN is claimed via an atomic mkdir lock
# under <run-dir>/tmp/seq/, so concurrent invocations never produce duplicate
# or gapped numbers.
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
  sed -n '2,16p' "$0" | sed 's/^# \{0,1\}//'
}

CAT="" SEV="" SLUG="" TITLE="" DATE="" RUN_DIR="${NIGHTLY_RUN_DIR:-}"
pos=0
while [ $# -gt 0 ]; do
  case "$1" in
    --date)
      [ $# -ge 2 ] || die "--date requires a value" "" "pass --date YYYYMMDD"
      DATE=$2; shift 2 ;;
    --dir)
      [ $# -ge 2 ] || die "--dir requires a value" "" "pass --dir <run-dir>"
      RUN_DIR=$2; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *)
      pos=$((pos + 1))
      case $pos in
        1) CAT=$1 ;;
        2) SEV=$1 ;;
        3) SLUG=$1 ;;
        4) TITLE=$1 ;;
        *) die "too many positional arguments: $1" "" "run with --help" ;;
      esac
      shift ;;
  esac
done

case "$CAT" in
  cat1) CAT_NAME="Static analysis" ;;
  cat2) CAT_NAME="CI logs, public-API baseline, docs consistency" ;;
  cat3) CAT_NAME="Dynamic build / test" ;;
  cat4) CAT_NAME="Upstream release watching" ;;
  cat5) CAT_NAME="Comparison / leftover angles" ;;
  '') die "missing <catN> argument" "" "usage: new-issue.sh <cat1..cat5> <P0..P3> <slug> [title]" ;;
  *) die "invalid category: $CAT" "must be one of cat1..cat5" "pass e.g. cat2" ;;
esac

case "$SEV" in
  P0|P1|P2|P3) ;;
  '') die "missing <severity> argument" "" "usage: new-issue.sh <cat1..cat5> <P0..P3> <slug> [title]" ;;
  *) die "invalid severity: $SEV" "must be one of P0 / P1 / P2 / P3" "pass e.g. P2" ;;
esac

[ -n "$SLUG" ] || die "missing <slug> argument" "" "usage: new-issue.sh <cat1..cat5> <P0..P3> <slug> [title]"
case "$SLUG" in
  *[!a-z0-9-]*|-*|*-|*--*) die "invalid slug: $SLUG" \
    "slug must be kebab-case: lowercase letters / digits / single hyphens" \
    "e.g. bcv-baseline-drift" ;;
esac
if [ ${#SLUG} -gt 40 ]; then
  die "slug too long (${#SLUG} > 40 chars): $SLUG" \
    "issue-format.md caps slugs at 40 chars" "shorten the slug"
fi

if [ -z "$RUN_DIR" ]; then
  if [ -z "$DATE" ]; then DATE=$(date +%Y%m%d); fi
  RUN_DIR=".local/tmp/exploratory-nightly-$DATE"
fi
ISSUES_DIR="$RUN_DIR/issues"
SEQ_DIR="$RUN_DIR/tmp/seq"
if [ ! -d "$ISSUES_DIR" ]; then
  die "run directory not initialized: $ISSUES_DIR not found" \
    "issues must live in the scaffold created by init-run.sh" \
    "run scripts/init-run.sh first (pass the same --date/--dir to both scripts)"
fi
mkdir -p "$SEQ_DIR"

GIT=${GIT_CMD:-git}
if ! $GIT rev-parse --git-dir >/dev/null 2>&1; then
  die "not inside a git repository (cwd: $(pwd))" \
    "the **Detected at commit** line requires git rev-parse" \
    "cd to the project root before running new-issue.sh"
fi
COMMIT=$($GIT rev-parse --short=12 HEAD)

if [ -z "$TITLE" ]; then
  TITLE=$(printf '%s' "$SLUG" | tr '-' ' ')
  first=$(printf '%s' "$TITLE" | cut -c1 | tr '[:lower:]' '[:upper:]')
  rest=$(printf '%s' "$TITLE" | cut -c2-)
  TITLE="$first$rest"
fi

# --- Atomic numbering ------------------------------------------------------
# max = highest NN across existing issue files AND already-claimed seq slots
# (a claim exists the instant another process wins mkdir, even before its
# issue file lands on disk).
max=0
for f in "$ISSUES_DIR"/[0-9][0-9]-*.md "$SEQ_DIR"/[0-9][0-9]; do
  if [ ! -e "$f" ]; then continue; fi
  b=${f##*/}
  n=${b%%-*}
  case "$n" in [0-9][0-9]) ;; *) continue ;; esac
  n=$((10#$n))
  if [ "$n" -gt "$max" ]; then max=$n; fi
done

NN=""
i=$((max + 1))
while [ "$i" -le 99 ]; do
  cand=$(printf '%02d' "$i")
  if mkdir "$SEQ_DIR/$cand" 2>/dev/null; then
    NN=$cand
    break
  fi
  i=$((i + 1))
done
[ -n "$NN" ] || die "issue number space exhausted (>99)" \
  "the 2-digit NN scheme caps a run at 99 issues" \
  "this run is saturated — stop filing and consolidate"

FILE="$ISSUES_DIR/$NN-$SLUG.md"
cat > "$FILE" <<EOF
# $TITLE

**Category**: $CAT ($CAT_NAME)
**Severity**: $SEV
**Detected at commit**: $COMMIT

## Reproduction
- (fill in: 2-5 bullet steps, reproducible by someone other than you)

## Detail
(fill in: ~200 chars; the first lines are the downstream notification excerpt)

## Fix proposal (optional)
- (optional: candidate fixes; delete this section if none)
EOF

printf '%s\n' "$FILE"
