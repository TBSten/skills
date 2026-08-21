#!/usr/bin/env bash
# new-ticket.sh — atomically allocate the next ticket number and generate the
# ticket skeleton in the fixed format (references/ticket-format.md).
#
# Usage:
#   new-ticket.sh --cat <1..5> --severity <P0..P3> --slug <kebab-slug> \
#                 (--id <pr-id> | --dir <exploration-dir>) \
#                 [--iter <N>] [--title <title>]
#
# Prints the created file path on stdout. The caller (AI) fills in only
# ## Location / ## Detail / ## Fix proposal; the H1, numbering, filename and
# metadata sections are owned by this script.
#
# Numbering is parallel-safe: the next NNNN is claimed via an atomic mkdir
# lock under <dir>/problems/.seq/, scanning problems/ AND its resolved/ /
# methodology/ / non-pr/ subdirectories for the current max. Five cats can
# call this concurrently — no reservation ranges, no renumbering.
#
# Dependencies: bash (3.2+). macOS / Linux compatible.
set -euo pipefail

die() {
  printf 'ERROR: %s\n' "$1" >&2
  if [ -n "${2:-}" ]; then printf '  why: %s\n' "$2" >&2; fi
  if [ -n "${3:-}" ]; then printf '  fix: %s\n' "$3" >&2; fi
  exit 1
}

usage() { sed -n '2,18p' "$0" | sed 's/^# \{0,1\}//'; }

CAT="" SEV="" SLUG="" TITLE="" ITER="" ID="" DIR="${EXPLORATORY_PR_DIR:-}"
while [ $# -gt 0 ]; do
  case "$1" in
    --cat)      [ $# -ge 2 ] || die "--cat requires a value" "" "pass --cat 1..5";      CAT=$2; shift 2 ;;
    --severity) [ $# -ge 2 ] || die "--severity requires a value" "" "pass --severity P0..P3"; SEV=$2; shift 2 ;;
    --slug)     [ $# -ge 2 ] || die "--slug requires a value" "" "pass --slug <kebab-slug>"; SLUG=$2; shift 2 ;;
    --title)    [ $# -ge 2 ] || die "--title requires a value" "" "pass --title <text>";  TITLE=$2; shift 2 ;;
    --iter)     [ $# -ge 2 ] || die "--iter requires a value" "" "pass --iter <N>";       ITER=$2; shift 2 ;;
    --id)       [ $# -ge 2 ] || die "--id requires a value" "" "pass --id <pr-id>";       ID=$2; shift 2 ;;
    --dir)      [ $# -ge 2 ] || die "--dir requires a value" "" "pass --dir <dir>";       DIR=$2; shift 2 ;;
    -h|--help)  usage; exit 0 ;;
    *) die "unknown argument: $1" "" "run with --help" ;;
  esac
done

case "$CAT" in
  1) CAT_LABEL="source-code static analysis" ;;
  2) CAT_LABEL="PR / environment / docs / CI" ;;
  3) CAT_LABEL="build / test (dynamic)" ;;
  4) CAT_LABEL="e2e / happy path (MCP-driven)" ;;
  5) CAT_LABEL="comparison / leftover angles" ;;
  '') die "missing --cat" "" "pass --cat 1..5" ;;
  *) die "invalid --cat: $CAT" "must be 1..5" "pass --cat 1..5" ;;
esac

case "$SEV" in
  P0|P1|P2|P3) ;;
  '') die "missing --severity" "" "pass --severity P0..P3" ;;
  *) die "invalid --severity: $SEV" "must be P0 / P1 / P2 / P3" "pass e.g. --severity P2" ;;
esac

[ -n "$SLUG" ] || die "missing --slug" "" "pass --slug <kebab-slug>"
case "$SLUG" in
  *[!a-z0-9-]*|-*|*-|*--*) die "invalid --slug: $SLUG" \
    "slug must be kebab-case: lowercase letters / digits / single hyphens" \
    "e.g. --slug gradle-cache-race" ;;
esac
if [ ${#SLUG} -gt 40 ]; then
  die "slug too long (${#SLUG} > 40 chars): $SLUG" "keep filenames scannable" "shorten the slug"
fi

if [ -n "$ITER" ]; then
  case "$ITER" in
    ''|*[!0-9]*) die "invalid --iter: $ITER" "must be a positive integer" "pass e.g. --iter 7" ;;
  esac
fi

if [ -z "$DIR" ]; then
  [ -n "$ID" ] || die "no exploration directory given" \
    "the ticket lives under .local/tmp/exploratory-pr-<id>/problems/" \
    "pass --id <pr-id> (or --dir <dir> / EXPLORATORY_PR_DIR)"
  DIR=".local/tmp/exploratory-pr-$ID"
fi
PROBLEMS="$DIR/problems"
if [ ! -d "$PROBLEMS" ]; then
  die "exploration directory not initialized: $PROBLEMS not found" \
    "tickets must live in the scaffold created by init-exploration.sh" \
    "run scripts/init-exploration.sh <pr-id> first"
fi
SEQ_DIR="$PROBLEMS/.seq"
mkdir -p "$SEQ_DIR"

if [ -z "$TITLE" ]; then
  TITLE=$(printf '%s' "$SLUG" | tr '-' ' ')
  first=$(printf '%s' "$TITLE" | cut -c1 | tr '[:lower:]' '[:upper:]')
  rest=$(printf '%s' "$TITLE" | cut -c2-)
  TITLE="$first$rest"
fi

# --- Atomic numbering ------------------------------------------------------
# max = highest NNNN across active tickets, moved tickets (resolved/ etc.) and
# already-claimed seq slots.
max=0
for f in "$PROBLEMS"/[0-9][0-9][0-9][0-9]-*.md \
         "$PROBLEMS"/resolved/[0-9][0-9][0-9][0-9]-*.md \
         "$PROBLEMS"/methodology/[0-9][0-9][0-9][0-9]-*.md \
         "$PROBLEMS"/non-pr/[0-9][0-9][0-9][0-9]-*.md \
         "$SEQ_DIR"/[0-9][0-9][0-9][0-9]; do
  if [ ! -e "$f" ]; then continue; fi
  b=${f##*/}
  n=${b%%-*}
  n=${n%.md}
  case "$n" in [0-9][0-9][0-9][0-9]) ;; *) continue ;; esac
  n=$((10#$n))
  if [ "$n" -gt "$max" ]; then max=$n; fi
done

NNNN=""
i=$((max + 1))
while [ "$i" -le 9999 ]; do
  cand=$(printf '%04d' "$i")
  if mkdir "$SEQ_DIR/$cand" 2>/dev/null; then
    NNNN=$cand
    break
  fi
  i=$((i + 1))
done
[ -n "$NNNN" ] || die "ticket number space exhausted (>9999)" \
  "the 4-digit scheme caps an exploration at 9999 tickets" \
  "this exploration is beyond saturated — close it"

OWNER="cat$CAT"
if [ -n "$ITER" ]; then OWNER="cat$CAT (iter $ITER)"; fi

FILE="$PROBLEMS/$NNNN-$SLUG.md"
cat > "$FILE" <<EOF
# $NNNN. $TITLE

## Severity

$SEV

## Owner

$OWNER — $CAT_LABEL

## Location

- (fill in: \`path/to/file.kt:line\`)

## Detail

(fill in: reproduction steps / evidence / why this matters)

## Fix proposal

Option (a): …
Option (b): …
EOF

printf '%s\n' "$FILE"
