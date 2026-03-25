#!/usr/bin/env bash
# Step 1: baseline commit の git worktree を作成
#
# 概要:
#   $BEFORE で指定された commit/branch の worktree を $WORKTREE_DIR に作成する。
#   後続の step2 でこの worktree 内でスナップショットを record する。
#
# Input (環境変数):
#   BEFORE        — ベースラインの commit/branch
#   WORKTREE_DIR  — worktree の作成先パス
#
# Output:
#   $WORKTREE_DIR に git worktree が作成される
set -euo pipefail

echo "==="
echo "=== [1/5] Creating worktree for: $BEFORE ==="
echo "===   command: git worktree add ... $BEFORE ==="
echo "==="
git worktree add --detach "$WORKTREE_DIR" "$BEFORE" --quiet
