#!/usr/bin/env bash
# Step 4: after 側でスナップショットを verify し、差分を検出
#
# 概要:
#   1. $AFTER が指定されていれば git checkout で切り替え
#   2. `./gradlew jvmSnapshotTestVerify` を実行
#   3. verify の終了コードを $VERIFY_EXIT に保存
#
# Input (環境変数):
#   AFTER           — 比較対象の commit/branch (空文字 = 現在の working tree)
#   PBT_GRADLE_OPT      — PBT イテレーション数の Gradle オプション
#   VERIFY_EXTRA_ARGS   — Verify 用の追加 Gradle 引数 (空文字の場合あり)
#   VERIFY_LOG          — verify ログの出力先テンポラリファイル
#   PREFIX              — ログ出力のプレフィックス
#
# Output (環境変数):
#   VERIFY_EXIT  — verify の終了コード (0=差分なし, 非0=差分あり)
#   $VERIFY_LOG にログが書き込まれる
#   build/snapshots/ に .actual.* ファイルが生成される (差分がある場合)
set -euo pipefail

AFTER_LABEL="${AFTER:-current working tree}"
echo ""
echo "==="
echo "=== [4/5] Verifying snapshots against: $AFTER_LABEL ==="
echo "===   command: ./gradlew jvmSnapshotTestVerify ==="
echo "==="
if [ -n "$AFTER" ]; then
  git checkout "$AFTER" --quiet
fi

set +eo pipefail
./gradlew jvmSnapshotTestVerify --stacktrace --console=rich $PBT_GRADLE_OPT $VERIFY_EXTRA_ARGS 2>&1 | tee "$VERIFY_LOG" | sed "s/^/${PREFIX}/"
VERIFY_EXIT=${PIPESTATUS[0]}
set -eo pipefail

echo ""
if [ "$VERIFY_EXIT" -eq 0 ]; then
  echo "==="
  echo "=== Result: No snapshot differences ==="
  echo "==="
else
  echo "==="
  echo "=== Result: Snapshot differences detected ==="
  echo "==="
fi
