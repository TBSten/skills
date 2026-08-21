#!/usr/bin/env bash
# local-ticket-system move-ticket script.
# Moves a ticket file into a status subdirectory (done/ closed/ deferred/
# archived/). For deferred, appends the reason block skeleton (with today's
# date) to the ticket before moving.
# AI agents: run this script as-is. Do not read, modify, or reimplement it.
set -euo pipefail

die() { # die <what> <why> <fix>
  printf 'ERROR: %s\nWHY: %s\nFIX: %s\n' "$1" "$2" "$3" >&2
  exit 1
}

usage() {
  cat >&2 <<'EOF'
Usage: move-ticket.sh <ticket-file> <done|closed|deferred|archived>

Moves the ticket into the given status subdirectory next to it.
For deferred, a reason block skeleton is appended to the ticket first;
fill in its TODO placeholders after the move.

Example: move-ticket.sh .local/ticket/task-001-add-login.md done
EOF
}

# --- Preflight ---------------------------------------------------------------
if [ $# -ne 2 ]; then
  usage
  die "expected 2 arguments, got $#" \
    "both a ticket file and a status are required" \
    "run: move-ticket.sh <ticket-file> <done|closed|deferred|archived>"
fi

FILE="$1"
STATUS="$2"

case "$STATUS" in
  done | closed | deferred | archived) ;;
  *)
    usage
    die "unknown status: $STATUS" \
      "status must be one of: done, closed, deferred, archived" \
      "run: move-ticket.sh <ticket-file> <done|closed|deferred|archived>"
    ;;
esac

[ -f "$FILE" ] || die "ticket file not found: $FILE" \
  "the given path does not exist or is not a regular file" \
  "check the path (e.g. .local/ticket/task-001-add-login.md)"

BASE="$(basename "$FILE")"

case "$BASE" in
  chapter-*)
    if [ "$STATUS" = "done" ] || [ "$STATUS" = "closed" ]; then
      die "chapters cannot be moved to $STATUS/" \
        "chapters are completed by archiving, not by done/closed (see about.md)" \
        "move it to archived/ (all child tickets completed) or deferred/ instead"
    fi
    ;;
esac

# --- Resolve ticket root -----------------------------------------------------
PARENT="$(cd "$(dirname "$FILE")" && pwd)"
case "$(basename "$PARENT")" in
  done | closed | deferred | archived) TICKET_ROOT="$(dirname "$PARENT")" ;;
  *) TICKET_ROOT="$PARENT" ;;
esac

SRC="$PARENT/$BASE"
DEST_DIR="$TICKET_ROOT/$STATUS"
DEST="$DEST_DIR/$BASE"

[ "$SRC" != "$DEST" ] || die "ticket is already in $STATUS/: $FILE" \
  "source and destination are the same file" \
  "no move is needed; pick a different status if you meant another transition"

[ ! -e "$DEST" ] || die "destination already exists: $DEST" \
  "a file with the same name is already in $STATUS/" \
  "rename or remove the existing file first"

# --- Deferred reason block ---------------------------------------------------
deferred_block_added=false
if [ "$STATUS" = "deferred" ]; then
  if ! grep -qF '**Deferred 理由**' "$SRC"; then
    today="$(date +%F)"
    printf '\n**Deferred 理由**: TODO:DeferredReason\n**再起票 trigger**: TODO:ReopenTrigger\n**Deferred 日付**: %s\n' \
      "$today" >> "$SRC"
    deferred_block_added=true
  fi
fi

# --- Move --------------------------------------------------------------------
mkdir -p "$DEST_DIR"
mv "$SRC" "$DEST"

display_dest="$DEST"
case "$DEST" in
  "$PWD"/*) display_dest="${DEST#"$PWD"/}" ;;
esac

printf '{"ok":true,"from":"%s","to":"%s","deferred_block_added":%s}\n' \
  "$FILE" "$display_dest" "$deferred_block_added"
