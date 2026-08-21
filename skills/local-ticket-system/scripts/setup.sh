#!/usr/bin/env bash
# local-ticket-system setup script.
# Sets up .local/ticket/ in the current project: copies templates from the
# skill's assets/, creates status subdirectories (done/ closed/ archived/
# deferred/), and adds .local/ to .gitignore if missing.
# Idempotent: safe to run multiple times; existing files are never overwritten.
# AI agents: run this script as-is. Do not read, modify, or reimplement it.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SKILL_DIR="${CLAUDE_SKILL_DIR:-$(cd "${SCRIPT_DIR}/.." && pwd)}"
ASSETS_DIR="${SKILL_DIR}/assets"
TICKET_DIR=".local/ticket"
ASSET_FILES=(about.md task-0xx-template.md bug-0xx-template.md chapter-template.md)
STATUS_DIRS=("done" "closed" "archived" "deferred")

die() { # die <what> <why> <fix>
  printf 'ERROR: %s\nWHY: %s\nFIX: %s\n' "$1" "$2" "$3" >&2
  exit 1
}

usage() {
  cat >&2 <<'EOF'
Usage: setup.sh

Run from the project root. Takes no arguments.
Creates .local/ticket/ with templates and status subdirectories,
and ensures .local/ is listed in .gitignore.
EOF
}

# --- Preflight ---------------------------------------------------------------
if [ $# -ne 0 ]; then
  usage
  die "setup.sh takes no arguments" \
    "got unexpected argument(s): $*" \
    "run it from the project root with no arguments"
fi

[ -d "$ASSETS_DIR" ] || die "assets directory not found: $ASSETS_DIR" \
  "the skill installation is incomplete or CLAUDE_SKILL_DIR points to the wrong place" \
  "reinstall the skill (gh skill install tbsten/skills local-ticket-system) or fix CLAUDE_SKILL_DIR"

for f in "${ASSET_FILES[@]}"; do
  [ -f "$ASSETS_DIR/$f" ] || die "template not found: $ASSETS_DIR/$f" \
    "the skill's assets/ is missing a required template" \
    "reinstall the skill (gh skill install tbsten/skills local-ticket-system)"
done

# --- Create directories (idempotent) -----------------------------------------
mkdir -p "$TICKET_DIR"
for d in "${STATUS_DIRS[@]}"; do
  mkdir -p "$TICKET_DIR/$d"
done

# --- Copy templates (skip existing) ------------------------------------------
copied=()
skipped=()
for f in "${ASSET_FILES[@]}"; do
  if [ -e "$TICKET_DIR/$f" ]; then
    skipped+=("$f")
  else
    cp "$ASSETS_DIR/$f" "$TICKET_DIR/$f"
    copied+=("$f")
  fi
done

# --- .gitignore --------------------------------------------------------------
gitignore="already"
if [ -f .gitignore ]; then
  if ! grep -qxE '\.local/?' .gitignore; then
    # Ensure the file ends with a newline before appending.
    if [ -s .gitignore ] && [ "$(tail -c 1 .gitignore)" != "" ]; then
      echo >> .gitignore
    fi
    printf '.local/\n' >> .gitignore
    gitignore="added"
  fi
else
  printf '.local/\n' > .gitignore
  gitignore="created"
fi

# --- Report ------------------------------------------------------------------
join_json() { # join_json [item...] -> "item","item",...
  local out="" item
  for item in "$@"; do
    out="${out}${out:+,}\"${item}\""
  done
  printf '%s' "$out"
}

printf '{"ok":true,"ticket_dir":"%s","copied":[%s],"skipped":[%s],"gitignore":"%s"}\n' \
  "$TICKET_DIR" \
  "$(join_json ${copied[@]+"${copied[@]}"})" \
  "$(join_json ${skipped[@]+"${skipped[@]}"})" \
  "$gitignore"
