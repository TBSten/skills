#!/usr/bin/env bash
# simple-loader install script.
# Copies example sources into a project and renames the package.
# AI agents: run this script as-is. Do not read, modify, or reimplement it.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXAMPLE_DIR="${SCRIPT_DIR}/../example"
SRC_PACKAGE="me.tbsten.simpleloader"
ALL_PARTS="core logger handler ext partial ui"
REQUIRED_PARTS="core logger handler"

usage() {
  cat >&2 <<'EOF'
Usage: install.sh --package <pkg> --dest <dir> [--parts <csv>] [--force] [--dry-run]

  --package <pkg>   Target Kotlin package (replaces me.tbsten.simpleloader)
  --dest <dir>      Destination source directory corresponding to <pkg>
                    (e.g. shared/src/commonMain/kotlin/com/myapp/loader)
  --parts <csv>     Parts to install, comma-separated (default: all):
                      core    SimpleLoader.kt + SimpleLoaderFactory.kt
                      logger  SimpleLoaderLogger.kt
                      handler IllegalStateTransitionHandler.kt
                      ext     SimpleLoaderExt.kt
                      partial SimpleLoaderWithPartialData.kt
                      ui      simple/ui/*.kt (4 files)
                    core/logger/handler are interdependent and always installed.
  --force           Overwrite existing files
  --dry-run         Show what would be installed without writing anything
EOF
}

die() { # die <what> <why> <fix>
  printf 'ERROR: %s\nWHY: %s\nFIX: %s\n' "$1" "$2" "$3" >&2
  exit 1
}

files_for_part() {
  case "$1" in
    core) echo "simple/SimpleLoader.kt simple/SimpleLoaderFactory.kt" ;;
    logger) echo "simple/SimpleLoaderLogger.kt" ;;
    handler) echo "IllegalStateTransitionHandler.kt" ;;
    ext) echo "simple/SimpleLoaderExt.kt" ;;
    partial) echo "simple/SimpleLoaderWithPartialData.kt" ;;
    ui) echo "simple/ui/View.kt simple/ui/AnimatedView.kt simple/ui/InitialLoadingView.kt simple/ui/InitialErrorView.kt" ;;
    *) return 1 ;;
  esac
}

# --- Parse arguments ---------------------------------------------------------
PACKAGE=""
DEST=""
PARTS_CSV=""
FORCE=false
DRY_RUN=false

while [ $# -gt 0 ]; do
  case "$1" in
    --package)
      [ $# -ge 2 ] || { usage; die "--package requires a value" "no value was given" "pass e.g. --package com.myapp.loader"; }
      PACKAGE="$2"; shift 2 ;;
    --dest)
      [ $# -ge 2 ] || { usage; die "--dest requires a value" "no value was given" "pass e.g. --dest shared/src/commonMain/kotlin/com/myapp/loader"; }
      DEST="$2"; shift 2 ;;
    --parts)
      [ $# -ge 2 ] || { usage; die "--parts requires a value" "no value was given" "pass e.g. --parts core,ext,ui"; }
      PARTS_CSV="$2"; shift 2 ;;
    --force) FORCE=true; shift ;;
    --dry-run) DRY_RUN=true; shift ;;
    -h|--help) usage; exit 0 ;;
    *)
      usage
      die "unknown argument: $1" "this script only accepts the options shown above" "remove '$1' and re-run" ;;
  esac
done

# --- Preflight ---------------------------------------------------------------
[ -n "$PACKAGE" ] || { usage; die "--package is required" "the target package cannot be guessed" "pass e.g. --package com.myapp.loader"; }
[ -n "$DEST" ] || { usage; die "--dest is required" "the destination directory cannot be guessed" "pass e.g. --dest shared/src/commonMain/kotlin/com/myapp/loader"; }

if ! printf '%s' "$PACKAGE" | grep -Eq '^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)*$'; then
  die "invalid package name: $PACKAGE" "it is not a valid Kotlin package (dot-separated identifiers)" "pass e.g. --package com.myapp.loader"
fi

[ -d "$EXAMPLE_DIR" ] || die "example directory not found: $EXAMPLE_DIR" "the skill installation is incomplete" "re-install the skill (gh skill install tbsten/skills simple-loader)"

REPLACER=""
if command -v perl >/dev/null 2>&1; then REPLACER="perl";
elif command -v python3 >/dev/null 2>&1; then REPLACER="python3";
else die "neither perl nor python3 found" "one of them is needed for portable in-place text replacement" "install perl or python3 and re-run"; fi

# Resolve selected parts (canonical order, required parts always included).
SELECTED_PARTS=""
if [ -z "$PARTS_CSV" ]; then
  SELECTED_PARTS="$ALL_PARTS"
else
  REQUESTED=" $(printf '%s' "$PARTS_CSV" | tr ',' ' ') "
  for part in $(printf '%s' "$REQUESTED"); do
    files_for_part "$part" >/dev/null || die "unknown part: $part" "valid parts are: ${ALL_PARTS// /, }" "fix the --parts value and re-run"
  done
  for part in $REQUIRED_PARTS; do
    case "$REQUESTED" in
      *" $part "*) ;;
      *)
        echo "NOTE: part '$part' is required by core and was added automatically." >&2
        REQUESTED="$REQUESTED$part " ;;
    esac
  done
  for part in $ALL_PARTS; do
    case "$REQUESTED" in
      *" $part "*) SELECTED_PARTS="$SELECTED_PARTS $part" ;;
    esac
  done
  SELECTED_PARTS="${SELECTED_PARTS# }"
fi

# Build file list and verify sources exist.
FILES=""
for part in $SELECTED_PARTS; do
  for f in $(files_for_part "$part"); do
    [ -f "$EXAMPLE_DIR/$f" ] || die "example file not found: $EXAMPLE_DIR/$f" "the skill installation is incomplete or corrupted" "re-install the skill (gh skill install tbsten/skills simple-loader)"
    FILES="$FILES $f"
  done
done
FILES="${FILES# }"

# Idempotency: refuse to overwrite without --force.
if [ "$FORCE" != true ]; then
  CONFLICTS=""
  for f in $FILES; do
    [ -e "$DEST/$f" ] && CONFLICTS="$CONFLICTS $DEST/$f"
  done
  if [ -n "$CONFLICTS" ]; then
    die "destination files already exist:$CONFLICTS" "overwriting without consent could destroy local modifications" "re-run with --force to overwrite, or choose another --dest"
  fi
fi

# --- Helpers -----------------------------------------------------------------
replace_package() { # replace_package <file>
  if [ "$REPLACER" = "perl" ]; then
    SRC_PKG="$SRC_PACKAGE" DST_PKG="$PACKAGE" perl -pi -e 's/\Q$ENV{SRC_PKG}\E/$ENV{DST_PKG}/g' "$1"
  else
    SRC_PKG="$SRC_PACKAGE" DST_PKG="$PACKAGE" python3 - "$1" <<'PY'
import os, sys
path = sys.argv[1]
with open(path, encoding="utf-8") as fp:
    text = fp.read()
with open(path, "w", encoding="utf-8") as fp:
    fp.write(text.replace(os.environ["SRC_PKG"], os.environ["DST_PKG"]))
PY
  fi
}

json_array() {
  local out="" item
  for item in "$@"; do out="$out\"$item\","; done
  printf '[%s]' "${out%,}"
}

result_json() { # result_json <dry_run:true|false>
  local count=0 f
  for f in $FILES; do count=$((count + 1)); done
  printf '{"ok":true,"skill":"simple-loader","dry_run":%s,"package":"%s","dest":"%s","parts":%s,"files":%s,"count":%d}\n' \
    "$1" "$PACKAGE" "$DEST" "$(json_array $SELECTED_PARTS)" "$(json_array $FILES)" "$count"
}

# --- Dry run -----------------------------------------------------------------
if [ "$DRY_RUN" = true ]; then
  for f in $FILES; do
    echo "DRY-RUN: would install $DEST/$f" >&2
  done
  result_json true
  exit 0
fi

# --- Install -----------------------------------------------------------------
for f in $FILES; do
  target="$DEST/$f"
  mkdir -p "$(dirname "$target")"
  cp "$EXAMPLE_DIR/$f" "$target"
  replace_package "$target"
  echo "Installed: $target" >&2
done

# Post-check: no source package occurrences must remain.
for f in $FILES; do
  if grep -Fq "$SRC_PACKAGE" "$DEST/$f"; then
    die "package replacement left '$SRC_PACKAGE' in $DEST/$f" "the in-place replacement did not complete" "delete the copied files and re-run (report this as a skill bug if it persists)"
  fi
done

result_json false
