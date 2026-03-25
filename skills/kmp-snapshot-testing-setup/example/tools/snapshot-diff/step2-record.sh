#!/usr/bin/env bash
# Step 2: worktree 内で baseline のスナップショットを record
#
# 概要:
#   step1 で作成した worktree 内で `./gradlew jvmSnapshotTestRecord` を実行し、
#   baseline のスナップショットファイルを生成する。
#
# Input (環境変数):
#   WORKTREE_DIR    — baseline worktree のパス
#   BEFORE          — ベースラインの commit/branch (表示用)
#   PBT_GRADLE_OPT      — PBT イテレーション数の Gradle オプション (空文字の場合あり)
#   RECORD_EXTRA_ARGS   — Record 用の追加 Gradle 引数 (空文字の場合あり)
#   PREFIX              — ログ出力のプレフィックス
#
# Output:
#   $WORKTREE_DIR/build/snapshots/ に
#   スナップショットファイルが生成される
set -euo pipefail

echo "==="
echo "=== [2/5] Recording snapshots at: $BEFORE (in worktree) ==="
echo "===   command: ./gradlew jvmSnapshotTestRecord ==="
echo "==="
(cd "$WORKTREE_DIR" && ./gradlew jvmSnapshotTestRecord --stacktrace --no-daemon --console=rich $PBT_GRADLE_OPT $RECORD_EXTRA_ARGS) 2>&1 | sed "s/^/${PREFIX}/"
echo "${PREFIX}"
