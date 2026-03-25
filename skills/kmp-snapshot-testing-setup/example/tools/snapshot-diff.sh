#!/usr/bin/env bash
set -euo pipefail
#
# snapshot-diff.sh — スナップショット差分比較のオーケストレーター
#
# 概要:
#   before (デフォルト: main) と after (デフォルト: 現在の working tree) の間で
#   スナップショットテストを実行し、差分を検出してレポートを生成する。
#   step1〜step5 のスクリプトを順番に source して実行する。
#
# Input:
#   -before=<commit>      ベースラインの commit/branch (デフォルト: main)
#   -after=<commit>       比較対象の commit/branch (デフォルト: 現在の working tree)
#   -pbt-iteration=<N>    PBT のイテレーション回数 (デフォルト: ProjectConfig のデフォルト値)
#
# Output:
#   build/snapshots/result.json  — 差分サマリー (JSON)
#   build/snapshots/result.md    — 差分レポート (Markdown)
#   終了コード: verify の結果 (0=差分なし, 非0=差分あり)
#

# --- Shared variables (exported for step scripts via source) ---
export PREFIX="  | "

# --- Default values ---
export BEFORE="main"
export AFTER="" # empty = current working tree (staged + unstaged + untracked)
export PBT_ITERATION="" # empty = use ProjectConfig default (2000)
export RECORD_EXTRA_ARGS=""
export VERIFY_EXTRA_ARGS=""

# --- Parse arguments ---
for arg in "$@"; do
  case "$arg" in
    -before=*) BEFORE="${arg#-before=}" ;;
    -after=*)  AFTER="${arg#-after=}" ;;
    -pbt-iteration=*) PBT_ITERATION="${arg#-pbt-iteration=}" ;;
    --record-args=*) RECORD_EXTRA_ARGS="${arg#--record-args=}" ;;
    --verify-args=*) VERIFY_EXTRA_ARGS="${arg#--verify-args=}" ;;
    -h|--help)
      echo "Usage: $0 [-before=<commit>] [-after=<commit>] [-pbt-iteration=<N>] [--record-args=<args>] [--verify-args=<args>]"
      echo ""
      echo "  -before=<commit>      Baseline commit to record snapshots (default: main)"
      echo "  -after=<commit>       Commit to verify against (default: current working tree)"
      echo "  -pbt-iteration=<N>    PBT iteration count (default: 2000)"
      echo "  --record-args=<args>  Extra arguments for ./gradlew jvmSnapshotTestRecord"
      echo "  --verify-args=<args>  Extra arguments for ./gradlew jvmSnapshotTestVerify"
      echo ""
      echo "Examples:"
      echo "  $0                                  # main vs current working tree"
      echo "  $0 -before=HEAD~3                   # 3 commits ago vs current working tree"
      echo "  $0 -before=main -after=feature/xyz  # main vs feature/xyz branch"
      echo "  $0 -pbt-iteration=10                # fast check with 10 iterations"
      echo "  $0 --record-args=\"--tests='com.example.FooTest'\" --verify-args=\"--tests='com.example.FooTest'\""
      exit 0
      ;;
    *)
      echo "Error: Unknown argument: $arg" >&2
      echo "Run '$0 --help' for usage." >&2
      exit 1
      ;;
  esac
done

# --- Setup ---
export PROJECT_ROOT="$(git rev-parse --show-toplevel)"
cd "$PROJECT_ROOT"

STEP_DIR="$(cd "$(dirname "$0")/snapshot-diff" && pwd)"

export VERIFY_LOG="$(mktemp "${TMPDIR:-/tmp}/snapshot-verify-XXXXXX")"
export BASELINE_MANIFEST="$(mktemp "${TMPDIR:-/tmp}/snapshot-baseline-XXXXXX")"
export WORKTREE_DIR="$(mktemp -d "${TMPDIR:-/tmp}/snapshot-diff-XXXXXX")"
rmdir "$WORKTREE_DIR" # git worktree add requires a non-existing directory

export SNAPSHOT_DST="$PROJECT_ROOT/build/snapshots/"

ORIGINAL_REF=""
if [ -n "$AFTER" ]; then
  ORIGINAL_REF="$(git symbolic-ref --short HEAD 2>/dev/null || git rev-parse HEAD)"
fi

cleanup() {
  echo "==="
  echo "=== Cleaning up... ==="
  echo "==="
  rm -f "$VERIFY_LOG" "$BASELINE_MANIFEST"
  if [ -d "$WORKTREE_DIR" ]; then
    git worktree remove --force "$WORKTREE_DIR" 2>/dev/null || true
  fi
  if [ -n "$ORIGINAL_REF" ]; then
    git checkout "$ORIGINAL_REF" --quiet 2>/dev/null || true
  fi
}
trap cleanup EXIT

# --- Validate refs ---
if ! git rev-parse --verify "$BEFORE" >/dev/null 2>&1; then
  echo "Error: Invalid ref for -before: $BEFORE" >&2
  exit 1
fi
if [ -n "$AFTER" ] && ! git rev-parse --verify "$AFTER" >/dev/null 2>&1; then
  echo "Error: Invalid ref for -after: $AFTER" >&2
  exit 1
fi

# --- Build Gradle PBT option ---
export PBT_GRADLE_OPT=""
if [ -n "$PBT_ITERATION" ]; then
  PBT_GRADLE_OPT="-Ppbt.iteration.count=$PBT_ITERATION"
fi

# --- Banner ---
AFTER_LABEL="${AFTER:-current working tree}"
PBT_LABEL="${PBT_ITERATION:-default}"
echo "==="
echo "=== snapshot-diff: Snapshot の diff を report します。"
echo "===   inputs:"
echo "===     before: $BEFORE"
echo "===     after:  $AFTER_LABEL"
echo "===     pbt-iteration: $PBT_LABEL"
if [ -n "$RECORD_EXTRA_ARGS" ]; then
echo "===     record-args:   $RECORD_EXTRA_ARGS"
fi
if [ -n "$VERIFY_EXTRA_ARGS" ]; then
echo "===     verify-args:   $VERIFY_EXTRA_ARGS"
fi
echo "==="
echo ""

# --- Step timer helper ---
step_start_time=0
step_start() {
  step_start_time=$(date +%s)
}
step_end() {
  local label="$1"
  local elapsed=$(( $(date +%s) - step_start_time ))
  local mins=$(( elapsed / 60 ))
  local secs=$(( elapsed % 60 ))
  echo "=== ${label}: ${mins}m ${secs}s"
}

# --- Execute steps ---
TOTAL_START=$(date +%s)

step_start
source "$STEP_DIR/step1-worktree.sh"
step_end "Step 1 (worktree)"

step_start
source "$STEP_DIR/step2-record.sh"
step_end "Step 2 (record)"

step_start
source "$STEP_DIR/step3-copy.sh"
step_end "Step 3 (copy)"

step_start
source "$STEP_DIR/step4-verify.sh"
step_end "Step 4 (verify)"

step_start
source "$STEP_DIR/step5-report.sh"
step_end "Step 5 (report)"

TOTAL_ELAPSED=$(( $(date +%s) - TOTAL_START ))
TOTAL_MINS=$(( TOTAL_ELAPSED / 60 ))
TOTAL_SECS=$(( TOTAL_ELAPSED % 60 ))
echo "=== Total: ${TOTAL_MINS}m ${TOTAL_SECS}s"

exit "$VERIFY_EXIT"
