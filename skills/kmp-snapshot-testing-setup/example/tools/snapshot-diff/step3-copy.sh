#!/usr/bin/env bash
# Step 3: worktree のスナップショットをメインツリーにコピーし、worktree を削除
#
# 概要:
#   1. worktree 内のスナップショットを $SNAPSHOT_DST (build/snapshots/) に rsync
#   2. worktree を削除
#   3. baseline のファイル一覧を $BASELINE_MANIFEST に保存 (後続の report 生成で使用)
#
# Input (環境変数):
#   WORKTREE_DIR       — baseline worktree のパス
#   SNAPSHOT_DST       — コピー先ディレクトリ (build/snapshots/)
#   BASELINE_MANIFEST  — baseline ファイル一覧の出力先 (NUL 区切りテンポラリファイル)
#   PREFIX             — ログ出力のプレフィックス
#
# Output:
#   $SNAPSHOT_DST にスナップショットファイルがコピーされる
#   $BASELINE_MANIFEST に baseline ファイル一覧が保存される
#   worktree が削除される
set -euo pipefail

echo "==="
echo "=== [3/5] Copying snapshots & removing worktree ==="
echo "===   command: rsync + git worktree remove ==="
echo "==="

SNAPSHOT_SRC="$WORKTREE_DIR/build/snapshots/"
mkdir -p "$SNAPSHOT_DST"
rsync -a --delete "$SNAPSHOT_SRC" "$SNAPSHOT_DST" 2>&1 | sed "s/^/${PREFIX}/"
git worktree remove --force "$WORKTREE_DIR" 2>&1 | sed "s/^/${PREFIX}/"

# Save baseline manifest (before verify modifies the directory)
find "$SNAPSHOT_DST" -type f \
  ! -name "*.actual.*" \
  ! -name "*.actual" \
  ! -name "*.diff.*" \
  ! -name "*.diff" \
  ! -name "*.removed.*" \
  ! -name "*.removed" \
  ! -name "result.json" \
  ! -name "result.md" \
  ! -name "result.sample.json" \
  ! -name "result.sample.md" \
  ! -name "record-report.*" \
  ! -name "verify-report.*" \
  -print0 | sort -z > "$BASELINE_MANIFEST"
