#!/bin/sh
set -eu

PLAYWRIGHT_VERSION="1.61.0"
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
CACHE_HOME=${XDG_CACHE_HOME:-"$HOME/.cache"}
RUNTIME_ROOT="$CACHE_HOME/github-get-attachment-url"
VENV_DIR="$RUNTIME_ROOT/venv-$PLAYWRIGHT_VERSION"
READY_MARKER="$RUNTIME_ROOT/playwright-$PLAYWRIGHT_VERSION.ready"
ALLOW_INSTALL=0

if [ "${1:-}" = "--allow-install" ]; then
  ALLOW_INSTALL=1
  shift
fi

find_python() {
  for candidate in python3.13 python3.12 python3.11 python3; do
    if command -v "$candidate" >/dev/null 2>&1 &&
      "$candidate" -c 'import sys; raise SystemExit(0 if sys.version_info >= (3, 11) else 1)' >/dev/null 2>&1; then
      command -v "$candidate"
      return 0
    fi
  done
  return 1
}

PYTHON=$(find_python || true)
if [ -z "$PYTHON" ]; then
  printf '%s\n' \
    '{"error":"Python 3.11 以上が必要です","issue_created":false,"ok":false,"status":"python_required"}'
  exit 21
fi

if [ "${1:-}" = "--self-test" ] || [ "${1:-}" = "--help" ] || [ "${1:-}" = "-h" ]; then
  exec "$PYTHON" "$SCRIPT_DIR/upload.py" "$@"
fi

set +e
"$PYTHON" "$SCRIPT_DIR/upload.py" --preflight "$@"
PREFLIGHT_STATUS=$?
set -e
if [ "$PREFLIGHT_STATUS" -ne 0 ]; then
  exit "$PREFLIGHT_STATUS"
fi

if [ ! -x "$VENV_DIR/bin/python" ] || [ ! -f "$READY_MARKER" ]; then
  if [ "$ALLOW_INSTALL" -ne 1 ]; then
    printf '%s\n' \
      '{"error":"Playwright 1.61.0 と Chromium の初回インストールが必要です","issue_created":false,"ok":false,"status":"setup_required"}'
    exit 20
  fi

  mkdir -p "$RUNTIME_ROOT"
  if [ ! -x "$VENV_DIR/bin/python" ]; then
    "$PYTHON" -m venv "$VENV_DIR"
  fi
  "$VENV_DIR/bin/python" -m pip install --disable-pip-version-check --upgrade \
    "playwright==$PLAYWRIGHT_VERSION" >&2
  "$VENV_DIR/bin/python" -m playwright install chromium >&2
  printf '%s\n' "$PLAYWRIGHT_VERSION" >"$READY_MARKER"
fi

export GITHUB_ATTACHMENT_RUNTIME_ROOT="$RUNTIME_ROOT"
exec "$VENV_DIR/bin/python" "$SCRIPT_DIR/upload.py" "$@"
