#!/usr/bin/env bash
# local-ticket-system new-ticket script.
# Computes the next sequence number (scanning .local/ticket/ and its
# done/ closed/ deferred/ archived/ subdirectories) and copies the matching
# template into .local/ticket/. Prints the created ticket path on stdout.
# AI agents: run this script as-is. Do not read, modify, or reimplement it.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SKILL_DIR="${CLAUDE_SKILL_DIR:-$(cd "${SCRIPT_DIR}/.." && pwd)}"
ASSETS_DIR="${SKILL_DIR}/assets"
TICKET_DIR=".local/ticket"
STATUS_DIRS=("done" "closed" "archived" "deferred")

die() { # die <what> <why> <fix>
  printf 'ERROR: %s\nWHY: %s\nFIX: %s\n' "$1" "$2" "$3" >&2
  exit 1
}

usage() {
  cat >&2 <<'EOF'
Usage: new-ticket.sh <task|bug|chapter> <slug>

  <slug>  lowercase letters, digits, and hyphens (e.g. add-login)

Creates (and prints the path of):
  task     .local/ticket/task-{NNN}-<slug>.md   (NNN = per-type max + 1)
  bug      .local/ticket/bug-{NNN}-<slug>.md    (NNN = per-type max + 1)
  chapter  .local/ticket/chapter-<slug>.md      (no sequence number)

Run from the project root after setup.sh.
EOF
}

# --- Preflight ---------------------------------------------------------------
if [ $# -ne 2 ]; then
  usage
  die "expected 2 arguments, got $#" \
    "both a ticket type and a slug are required" \
    "run: new-ticket.sh <task|bug|chapter> <slug>"
fi

TYPE="$1"
SLUG="$2"

case "$TYPE" in
  task | bug | chapter) ;;
  *)
    usage
    die "unknown ticket type: $TYPE" \
      "type must be one of: task, bug, chapter" \
      "run: new-ticket.sh <task|bug|chapter> <slug>"
    ;;
esac

printf '%s' "$SLUG" | grep -qE '^[a-z0-9][a-z0-9-]*[a-z0-9]$|^[a-z0-9]$' ||
  die "invalid slug: $SLUG" \
    "slug must consist of lowercase letters, digits, and hyphens (no leading/trailing hyphen)" \
    "use a slug like add-login or fix-null-pointer"

[ -d "$TICKET_DIR" ] || die ".local/ticket/ not found" \
  "the ticket system has not been set up in this project" \
  "run scripts/setup.sh from the project root first"

case "$TYPE" in
  task) TEMPLATE_NAME="task-0xx-template.md" ;;
  bug) TEMPLATE_NAME="bug-0xx-template.md" ;;
  chapter) TEMPLATE_NAME="chapter-template.md" ;;
esac

# Prefer the project's local copy (may be customized); fall back to skill assets.
TEMPLATE=""
for cand in "$TICKET_DIR/$TEMPLATE_NAME" "$ASSETS_DIR/$TEMPLATE_NAME"; do
  if [ -f "$cand" ]; then
    TEMPLATE="$cand"
    break
  fi
done
[ -n "$TEMPLATE" ] || die "template not found: $TEMPLATE_NAME" \
  "neither $TICKET_DIR/$TEMPLATE_NAME nor $ASSETS_DIR/$TEMPLATE_NAME exists" \
  "run scripts/setup.sh again to restore the templates"

# --- Determine destination ---------------------------------------------------
if [ "$TYPE" = "chapter" ]; then
  DEST="$TICKET_DIR/chapter-$SLUG.md"
else
  # Per-type max sequence number across active + all status subdirectories.
  max=0
  for d in "$TICKET_DIR" "${STATUS_DIRS[@]/#/$TICKET_DIR/}"; do
    [ -d "$d" ] || continue
    for f in "$d/$TYPE"-*.md; do
      [ -e "$f" ] || continue # unmatched glob
      base="$(basename "$f")"
      n="${base#"$TYPE"-}"
      n="${n%%-*}"
      case "$n" in
        '' | *[!0-9]*) continue ;; # not a numbered ticket (e.g. template)
      esac
      n=$((10#$n))
      if [ "$n" -gt "$max" ]; then
        max=$n
      fi
    done
  done
  next=$((max + 1))
  seq="$(printf '%03d' "$next")"
  DEST="$TICKET_DIR/$TYPE-$seq-$SLUG.md"
fi

[ ! -e "$DEST" ] || die "ticket already exists: $DEST" \
  "a ticket with the same name is already present" \
  "pick a different slug, or edit the existing ticket instead"

# --- Create ------------------------------------------------------------------
cp "$TEMPLATE" "$DEST"
printf '%s\n' "$DEST"
